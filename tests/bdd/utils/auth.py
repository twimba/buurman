"""Keycloak authentication and user registration utilities for BDD tests.

Handles the full registration flow:
  1. Register user via /auth/register (creates Keycloak user + DB user + team)
  2. Retrieve verification code from Mailpit (email capture in dev)
  3. Verify email via /auth/verify-email
  4. Obtain JWT access token from Keycloak via password grant

All interaction is via HTTP — no production code imported.
"""

from __future__ import annotations

import re
import time
from dataclasses import dataclass, field
from uuid import uuid4

import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Keycloak client dedicated to BDD tests (has directAccessGrantsEnabled=true)
BDD_CLIENT_ID = "buurman-bdd"


@dataclass
class AuthenticatedUser:
    """Holds credentials and tokens for a registered, verified, authenticated user."""

    email: str
    password: str
    token: str
    user_identifier: str
    team_identifier: str
    role: str
    first_name: str
    last_name: str


@dataclass
class TeamContext:
    """Holds state for a team across a scenario.

    Each scenario gets its own team to ensure isolation.
    """

    admin: AuthenticatedUser
    team_identifier: str
    members: dict[str, AuthenticatedUser] = field(default_factory=dict)
    identifiers: dict[str, str] = field(default_factory=dict)
    last_response: requests.Response | None = None

    def store_identifier(self, key: str, identifier: str) -> None:
        self.identifiers[key] = identifier

    def get_identifier(self, key: str) -> str:
        return self.identifiers[key]


def generate_unique_email(prefix: str = "bdd") -> str:
    """Generate a unique email address for a test user."""
    uid = uuid4().hex[:8]
    return f"{prefix}-{uid}@test.buurman.io"


def register_user(
    api_base_url: str,
    *,
    email: str | None = None,
    password: str = "BddTest123!",
    first_name: str = "BDD",
    last_name: str | None = None,
) -> dict:
    """Register a new user via the public /auth/register endpoint.

    This creates a Keycloak user, a DB user, and a team (auto-named).
    Returns the raw JSON response from the API.
    """
    if email is None:
        email = generate_unique_email()
    if last_name is None:
        last_name = f"User {uuid4().hex[:6]}"

    resp = requests.post(
        f"{api_base_url}/auth/register",
        json={
            "email": email,
            "firstName": first_name,
            "lastName": last_name,
            "password": password,
        },
        verify=False,
    )
    resp.raise_for_status()
    return resp.json()


def get_keycloak_token(keycloak_url: str, email: str, password: str) -> str:
    """Obtain an access token from Keycloak via Resource Owner Password Grant.

    Requires the buurman-bdd client with directAccessGrantsEnabled=true.
    """
    resp = requests.post(
        f"{keycloak_url}/realms/buurman/protocol/openid-connect/token",
        data={
            "grant_type": "password",
            "client_id": BDD_CLIENT_ID,
            "username": email,
            "password": password,
            "scope": "openid email profile",
        },
        verify=False,
    )
    resp.raise_for_status()
    return resp.json()["access_token"]


def get_verification_code_from_mailpit(
    mailpit_url: str,
    email: str,
    *,
    timeout_seconds: int = 15,
    poll_interval: float = 0.5,
) -> str:
    """Poll Mailpit API for the verification email and extract the 6-digit code.

    The verification email contains a 6-digit code in the body.
    Mailpit captures all emails sent in the dev environment.
    """
    deadline = time.time() + timeout_seconds

    while time.time() < deadline:
        resp = requests.get(
            f"{mailpit_url}/api/v1/search",
            params={"query": f"to:{email} subject:verification"},
            verify=False,
        )

        if resp.status_code == 200:
            data = resp.json()
            messages = data.get("messages", [])
            if messages:
                # Get the most recent message
                message_id = messages[0]["ID"]
                msg_resp = requests.get(
                    f"{mailpit_url}/api/v1/message/{message_id}",
                    verify=False,
                )
                if msg_resp.status_code == 200:
                    body = msg_resp.json().get("Text", "") or msg_resp.json().get("HTML", "")
                    # Extract 6-digit verification code
                    match = re.search(r"\b(\d{6})\b", body)
                    if match:
                        return match.group(1)

        time.sleep(poll_interval)

    raise TimeoutError(
        f"Verification email for {email} not found in Mailpit within {timeout_seconds}s"
    )


def verify_email(api_base_url: str, token: str, code: str) -> None:
    """Verify user email via the /auth/verify-email endpoint.

    Requires an auth token (even for unverified users — this path is allowed).
    """
    resp = requests.post(
        f"{api_base_url}/auth/verify-email",
        json={"code": code},
        headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
        verify=False,
    )
    resp.raise_for_status()


def register_and_authenticate(
    api_base_url: str,
    keycloak_url: str,
    mailpit_url: str,
    *,
    email: str | None = None,
    password: str = "BddTest123!",
    first_name: str = "BDD",
    last_name: str | None = None,
) -> AuthenticatedUser:
    """Full registration flow: register -> verify email -> get token.

    Returns an AuthenticatedUser with a valid, verified JWT token.
    """
    if email is None:
        email = generate_unique_email()
    if last_name is None:
        last_name = f"User {uuid4().hex[:6]}"

    # Step 1: Register (creates Keycloak user + DB user + team)
    user_data = register_user(
        api_base_url,
        email=email,
        password=password,
        first_name=first_name,
        last_name=last_name,
    )

    # Step 2: Get initial token (email_verified=false, but allowed for /auth/verify-email)
    initial_token = get_keycloak_token(keycloak_url, email, password)

    # Step 3: Read verification code from Mailpit and verify email
    code = get_verification_code_from_mailpit(mailpit_url, email)
    verify_email(api_base_url, initial_token, code)

    # Step 4: Get a fresh token (now email_verified=true in Keycloak)
    verified_token = get_keycloak_token(keycloak_url, email, password)

    return AuthenticatedUser(
        email=email,
        password=password,
        token=verified_token,
        user_identifier=user_data["identifier"],
        team_identifier=user_data["teamIdentifier"],
        role=user_data.get("role", "TEAM_ADMIN"),
        first_name=first_name,
        last_name=last_name,
    )


def create_team_context(
    api_base_url: str,
    keycloak_url: str,
    mailpit_url: str,
) -> TeamContext:
    """Create a fully isolated team context for a BDD scenario.

    Registers a new admin user (which auto-creates a team), verifies email,
    and returns a TeamContext ready for API interactions.
    """
    admin = register_and_authenticate(api_base_url, keycloak_url, mailpit_url)
    return TeamContext(
        admin=admin,
        team_identifier=admin.team_identifier,
        members={"admin": admin},
    )
