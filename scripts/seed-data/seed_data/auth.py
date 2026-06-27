"""Registration + Keycloak token + Mailpit email verification.

Ports the proven flow from tests/bdd/utils/auth.py. Each seeded "user" registers
via the public /auth/register endpoint (which also creates a Keycloak user and an
auto-named team where the user is TEAM_ADMIN), optionally verifies the email via
Mailpit, and obtains a JWT through the buurman-bdd password-grant client.
"""

from __future__ import annotations

import re
import time
from dataclasses import dataclass

import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Public Keycloak client with directAccessGrantsEnabled=true (Resource Owner Password Grant).
BDD_CLIENT_ID = "buurman-bdd"


class RegistrationError(RuntimeError):
    """Raised when /auth/register fails (e.g. invitation required, duplicate email)."""


@dataclass
class SeededAccount:
    email: str
    password: str
    token: str
    user_identifier: str
    team_identifier: str
    first_name: str
    last_name: str


def register(api_url: str, email: str, password: str, first_name: str, last_name: str) -> dict:
    resp = requests.post(
        f"{api_url}/auth/register",
        json={
            "email": email,
            "firstName": first_name,
            "lastName": last_name,
            "password": password,
        },
        verify=False,
        timeout=30,
    )
    if resp.status_code >= 400:
        raise RegistrationError(
            f"register {email} -> {resp.status_code}: {resp.text[:300]}"
        )
    return resp.json()


def get_token(keycloak_url: str, email: str, password: str) -> str:
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
        timeout=30,
    )
    resp.raise_for_status()
    return resp.json()["access_token"]


def _verification_code(mailpit_url: str, email: str, timeout_s: int = 30) -> str:
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        # Subject is "Verify your email - Buurman"; match by recipient and take the newest.
        resp = requests.get(
            f"{mailpit_url}/api/v1/search",
            params={"query": f"to:{email}"},
            verify=False,
            timeout=15,
        )
        if resp.status_code == 200:
            messages = resp.json().get("messages", [])
            if messages:
                msg = requests.get(
                    f"{mailpit_url}/api/v1/message/{messages[0]['ID']}",
                    verify=False,
                    timeout=15,
                )
                if msg.status_code == 200:
                    body = msg.json().get("Text", "") or msg.json().get("HTML", "")
                    code = _extract_code(body)
                    if code:
                        return code
        time.sleep(0.5)
    raise TimeoutError(f"Verification email for {email} not found within {timeout_s}s")


def _extract_code(body: str) -> str | None:
    # Prefer the digits that follow the "code ... :" phrase, else any 6-digit run.
    labelled = re.search(r"code[^0-9]{0,20}(\d{6})", body, re.IGNORECASE)
    if labelled:
        return labelled.group(1)
    loose = re.search(r"\b(\d{6})\b", body)
    return loose.group(1) if loose else None


def _verify_email(api_url: str, token: str, code: str, retries: int = 5) -> None:
    for attempt in range(retries):
        resp = requests.post(
            f"{api_url}/auth/verify-email",
            json={"code": code},
            headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
            verify=False,
            timeout=30,
        )
        if resp.status_code < 400:
            return
        # /auth/verify-email is IP rate-limited; back off and retry on 429.
        if resp.status_code == 429:
            time.sleep(2 ** attempt)
            continue
        resp.raise_for_status()
    raise RuntimeError("verify-email exhausted retries (rate limited)")


def provision_account(
    *,
    api_url: str,
    keycloak_url: str,
    mailpit_url: str,
    email: str,
    password: str,
    first_name: str,
    last_name: str,
    mailpit_verify: bool,
) -> SeededAccount:
    """Register -> token, optionally running the real Mailpit email-verification flow.

    For the default db-based verification the caller marks the email verified directly
    (see db.DbClient); only mailpit_verify=True exercises /auth/verify-email here.
    """
    user = register(api_url, email, password, first_name, last_name)
    token = get_token(keycloak_url, email, password)

    if mailpit_verify:
        code = _verification_code(mailpit_url, email)
        _verify_email(api_url, token, code)
        token = get_token(keycloak_url, email, password)

    return SeededAccount(
        email=email,
        password=password,
        token=token,
        user_identifier=user["identifier"],
        team_identifier=user["teamIdentifier"],
        first_name=first_name,
        last_name=last_name,
    )
