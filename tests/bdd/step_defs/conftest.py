"""Shared step definitions used across all feature files.

Provides the foundational Given steps for team creation, authentication,
and entity setup that appear in Background sections of most features.
"""

from __future__ import annotations

import pytest
from pytest_bdd import given, parsers, then, when

from utils.api_client import BuurmanApiClient
from utils.auth import (
    TeamContext,
    create_team_context,
    generate_unique_email,
    get_keycloak_token,
    register_and_authenticate,
)
from utils.date_utils import resolve_date


# ---------------------------------------------------------------------------
# Scenario-scoped mutable state
# ---------------------------------------------------------------------------


@pytest.fixture()
def ctx(api_base_url, keycloak_url, mailpit_url):
    """Mutable scenario context. Holds all state across steps."""
    return ScenarioContext(api_base_url, keycloak_url, mailpit_url)


class ScenarioContext:
    """Holds all mutable state for a single scenario."""

    def __init__(self, api_base_url: str, keycloak_url: str, mailpit_url: str):
        self.api_base_url = api_base_url
        self.keycloak_url = keycloak_url
        self.mailpit_url = mailpit_url

        # Named teams for multi-tenancy scenarios
        self.teams: dict[str, TeamContext] = {}
        # Current active team context
        self.current_team: TeamContext | None = None
        # Last HTTP response (for assertions)
        self.last_response = None
        # Named identifiers for cross-step references
        self.stored_identifiers: dict[str, str] = {}
        # Current entity identifiers (last created of each type)
        self.current_property_id: str | None = None
        self.current_contact_id: str | None = None
        self.current_contract_id: str | None = None
        self.current_payment_id: str | None = None
        self.current_expense_id: str | None = None

    def ensure_team(self, name: str = "__default__") -> TeamContext:
        """Get or create a team by name."""
        if name not in self.teams:
            team = create_team_context(
                self.api_base_url, self.keycloak_url, self.mailpit_url
            )
            self.teams[name] = team
        return self.teams[name]


# ---------------------------------------------------------------------------
# Shared Given steps: Team & Auth
# ---------------------------------------------------------------------------


@given("a new team is created")
def create_default_team(ctx: ScenarioContext, api: BuurmanApiClient):
    ctx.current_team = ctx.ensure_team()
    api.set_auth(ctx.current_team.admin.token)


@given(parsers.parse('a new team "{name}" is created'))
def create_named_team(ctx: ScenarioContext, api: BuurmanApiClient, name: str):
    ctx.current_team = ctx.ensure_team(name)
    api.set_auth(ctx.current_team.admin.token)


@given("I am logged in as admin of that team")
def login_as_admin(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_team is not None, "No team created yet"
    api.set_auth(ctx.current_team.admin.token)


@given(parsers.parse('I am logged in as admin of team "{name}"'))
def login_as_admin_of_named_team(ctx: ScenarioContext, api: BuurmanApiClient, name: str):
    team = ctx.teams.get(name)
    assert team is not None, f"Team '{name}' not found"
    ctx.current_team = team
    api.set_auth(team.admin.token)


@given("a viewer member is added to the team")
def add_viewer_to_team(ctx: ScenarioContext):
    _add_member_to_team(ctx, "viewer", "TEAM_VIEWER")


@given("an editor member is added to the team")
def add_editor_to_team(ctx: ScenarioContext):
    _add_member_to_team(ctx, "editor", "TEAM_EDITOR")


@given("I am logged in as viewer of that team")
def login_as_viewer(ctx: ScenarioContext, api: BuurmanApiClient):
    _login_as_role(ctx, api, "viewer")


@given("I am logged in as editor of that team")
def login_as_editor(ctx: ScenarioContext, api: BuurmanApiClient):
    _login_as_role(ctx, api, "editor")


def _add_member_to_team(ctx: ScenarioContext, role_name: str, role: str):
    """Register a new user and add them to the current team via invitation flow."""
    assert ctx.current_team is not None

    # Register a new user (creates their own team)
    new_user = register_and_authenticate(
        ctx.api_base_url, ctx.keycloak_url, ctx.mailpit_url
    )

    # Invite them to the current team
    admin_api = BuurmanApiClient(ctx.api_base_url)
    admin_api.set_auth(ctx.current_team.admin.token)

    invite_resp = admin_api.post(
        f"/teams/{ctx.current_team.team_identifier}/invitations",
        json={"email": new_user.email},
    )

    assert invite_resp.status_code == 201, (
        f"Failed to invite member: {invite_resp.status_code} {invite_resp.text}"
    )

    # Accept the invitation
    invite_data = invite_resp.json()
    token = invite_data.get("token", "")

    user_api = BuurmanApiClient(ctx.api_base_url)
    user_api.set_auth(new_user.token)
    user_api.post(f"/invitations/{token}/accept")

    # Change role if needed (default is TEAM_VIEWER for invited members)
    if role != "TEAM_VIEWER":
        admin_api.put(
            f"/teams/{ctx.current_team.team_identifier}/members/{new_user.user_identifier}/role",
            json={"role": role},
        )

    # Switch the new user to the invited team
    user_api.post("/users/switch-team", json={"teamIdentifier": ctx.current_team.team_identifier})

    # Get a fresh token reflecting the new team context
    fresh_token = get_keycloak_token(ctx.keycloak_url, new_user.email, new_user.password)
    new_user = new_user.__class__(
        email=new_user.email,
        password=new_user.password,
        token=fresh_token,
        user_identifier=new_user.user_identifier,
        team_identifier=ctx.current_team.team_identifier,
        role=role,
        first_name=new_user.first_name,
        last_name=new_user.last_name,
    )

    ctx.current_team.members[role_name] = new_user


def _login_as_role(ctx: ScenarioContext, api: BuurmanApiClient, role_name: str):
    assert ctx.current_team is not None
    member = ctx.current_team.members.get(role_name)
    assert member is not None, f"No {role_name} member added to team"
    api.set_auth(member.token)


# ---------------------------------------------------------------------------
# Shared Given steps: Entity creation helpers
# ---------------------------------------------------------------------------


@given(parsers.parse('a property "{street}" exists in "{city}"'))
def create_property(ctx: ScenarioContext, api: BuurmanApiClient, street: str, city: str):
    resp = api.post("/properties", json={
        "street": street,
        "city": city,
        "postalCode": "1000AA",
        "country": "NL",
        "propertyCategory": "RESIDENTIAL",
        "propertyType": "APARTMENT",
        "status": "VACANT",
    })
    assert resp.status_code == 201, f"Failed to create property: {resp.status_code} {resp.text}"
    ctx.current_property_id = resp.json()["identifier"]
    ctx.current_team.store_identifier(f"property:{street}", ctx.current_property_id)


@given(parsers.parse("{count:d} properties exist"))
def create_multiple_properties(ctx: ScenarioContext, api: BuurmanApiClient, count: int):
    for i in range(count):
        resp = api.post("/properties", json={
            "street": f"Test Street {i + 1}",
            "city": "Amsterdam",
            "postalCode": "1000AA",
            "country": "NL",
            "propertyCategory": "RESIDENTIAL",
            "propertyType": "APARTMENT",
            "status": "VACANT",
        })
        assert resp.status_code == 201


@given(parsers.parse('a contact "{name}" exists'))
def create_contact(ctx: ScenarioContext, api: BuurmanApiClient, name: str):
    parts = name.split(" ", 1)
    first_name = parts[0]
    last_name = parts[1] if len(parts) > 1 else ""
    email = generate_unique_email("contact")

    resp = api.post("/contacts", json={
        "contactType": "INDIVIDUAL",
        "firstName": first_name,
        "lastName": last_name,
        "email": email,
    })
    assert resp.status_code == 201, f"Failed to create contact: {resp.status_code} {resp.text}"
    ctx.current_contact_id = resp.json()["identifier"]
    ctx.current_team.store_identifier(f"contact:{name}", ctx.current_contact_id)


@given(parsers.parse(
    'a draft contract exists for the property with tenant "{tenant_name}"'
))
def create_draft_contract(ctx: ScenarioContext, api: BuurmanApiClient, tenant_name: str):
    assert ctx.current_property_id, "No property created yet"
    contact_id = ctx.current_team.get_identifier(f"contact:{tenant_name}")

    resp = api.post("/contracts", json={
        "contractType": "FIXED_TERM",
        "propertyIdentifier": ctx.current_property_id,
        "parties": [{"role": "PRIMARY_TENANT", "contactIdentifier": contact_id}],
        "paymentFrequency": "MONTHLY",
        "rentAmount": 1250.00,
        "startDate": resolve_date("next month"),
        "endDate": resolve_date("in 12 months"),
        "countryMetadata": {},
    })
    assert resp.status_code == 201, f"Failed to create contract: {resp.status_code} {resp.text}"
    ctx.current_contract_id = resp.json()["identifier"]
    ctx.current_team.store_identifier("contract:current", ctx.current_contract_id)


@given(parsers.parse("an active contract exists with monthly rent of {rent:g}"))
def create_active_contract(ctx: ScenarioContext, api: BuurmanApiClient, rent: float):
    assert ctx.current_property_id, "No property created yet"
    assert ctx.current_contact_id, "No contact created yet"

    resp = api.post("/contracts", json={
        "contractType": "FIXED_TERM",
        "propertyIdentifier": ctx.current_property_id,
        "parties": [{"role": "PRIMARY_TENANT", "contactIdentifier": ctx.current_contact_id}],
        "paymentFrequency": "MONTHLY",
        "rentAmount": rent,
        "startDate": resolve_date("next month"),
        "endDate": resolve_date("in 12 months"),
        "countryMetadata": {},
    })
    assert resp.status_code == 201, f"Failed to create contract: {resp.status_code} {resp.text}"
    ctx.current_contract_id = resp.json()["identifier"]
    ctx.current_team.store_identifier("contract:current", ctx.current_contract_id)

    # Activate the contract
    status_resp = api.post(
        f"/contracts/{ctx.current_contract_id}/change-status",
        json={"status": "ACTIVE"},
    )
    assert status_resp.status_code == 200, (
        f"Failed to activate contract: {status_resp.status_code} {status_resp.text}"
    )


@given("the contract is activated")
def activate_contract(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id, "No contract created yet"
    resp = api.post(
        f"/contracts/{ctx.current_contract_id}/change-status",
        json={"status": "ACTIVE"},
    )
    assert resp.status_code == 200, f"Failed to activate: {resp.status_code} {resp.text}"


@given(parsers.parse('the contract status is changed to "{status}"'))
def change_contract_status_given(ctx: ScenarioContext, api: BuurmanApiClient, status: str):
    assert ctx.current_contract_id, "No contract created yet"
    resp = api.post(
        f"/contracts/{ctx.current_contract_id}/change-status",
        json={"status": status},
    )
    assert resp.status_code == 200, (
        f"Failed to change status to {status}: {resp.status_code} {resp.text}"
    )


@given(parsers.parse('the contract status is changed to "{status}" with reason "{reason}"'))
def change_contract_status_with_reason_given(
    ctx: ScenarioContext, api: BuurmanApiClient, status: str, reason: str
):
    assert ctx.current_contract_id, "No contract created yet"
    resp = api.post(
        f"/contracts/{ctx.current_contract_id}/change-status",
        json={"status": status, "reason": reason},
    )
    assert resp.status_code == 200, (
        f"Failed to change status to {status}: {resp.status_code} {resp.text}"
    )


@given(parsers.parse(
    'a pending payment of {amount:g} EUR exists due on "{due_date}"'
))
def create_pending_payment(
    ctx: ScenarioContext, api: BuurmanApiClient, amount: float, due_date: str
):
    assert ctx.current_contract_id, "No contract created yet"
    resp = api.post("/payments", json={
        "contractIdentifier": ctx.current_contract_id,
        "amount": amount,
        "currency": "EUR",
        "dueDate": resolve_date(due_date),
    })
    assert resp.status_code == 201, f"Failed to create payment: {resp.status_code} {resp.text}"
    ctx.current_payment_id = resp.json()["identifier"]
    ctx.current_team.store_identifier("payment:current", ctx.current_payment_id)


@given(parsers.parse("an expense of {amount:g} EUR exists for the property"))
def create_expense(ctx: ScenarioContext, api: BuurmanApiClient, amount: float):
    assert ctx.current_property_id, "No property created yet"
    resp = api.post("/expenses", json={
        "propertyIdentifier": ctx.current_property_id,
        "category": "MAINTENANCE",
        "amount": amount,
        "currency": "EUR",
        "expenseDate": resolve_date("today"),
        "description": "BDD test expense",
    })
    assert resp.status_code == 201, f"Failed to create expense: {resp.status_code} {resp.text}"
    ctx.current_expense_id = resp.json()["identifier"]


# ---------------------------------------------------------------------------
# Shared Given steps: Identifier storage (for cross-team scenarios)
# ---------------------------------------------------------------------------


@given("I store the property identifier")
def store_property_id(ctx: ScenarioContext):
    assert ctx.current_property_id, "No property to store"
    ctx.stored_identifiers["property"] = ctx.current_property_id


@given("I store the contact identifier")
def store_contact_id(ctx: ScenarioContext):
    assert ctx.current_contact_id, "No contact to store"
    ctx.stored_identifiers["contact"] = ctx.current_contact_id


@given("I store the contract identifier")
def store_contract_id(ctx: ScenarioContext):
    assert ctx.current_contract_id, "No contract to store"
    ctx.stored_identifiers["contract"] = ctx.current_contract_id


# ---------------------------------------------------------------------------
# Shared Then steps: Response assertions
# ---------------------------------------------------------------------------


@then(parsers.parse("the response status should be {status:d}"))
def check_response_status(ctx: ScenarioContext, status: int):
    assert ctx.last_response is not None, "No response to check"
    assert ctx.last_response.status_code == status, (
        f"Expected {status}, got {ctx.last_response.status_code}. "
        f"Body: {ctx.last_response.text[:500]}"
    )


@then(parsers.parse('the response should contain an identifier starting with "{prefix}"'))
def check_identifier_prefix(ctx: ScenarioContext, prefix: str):
    data = ctx.last_response.json()
    identifier = data.get("identifier", "")
    assert identifier.startswith(prefix), (
        f"Expected identifier starting with '{prefix}', got '{identifier}'"
    )


@then(parsers.parse("I should see {count:d} items in the list"))
def check_list_count(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    content = data.get("content", [])
    assert len(content) == count, (
        f"Expected {count} items, got {len(content)}. "
        f"Total elements: {data.get('totalElements', 'N/A')}"
    )


@then(parsers.parse("the total elements should be {count:d}"))
def check_total_elements(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["totalElements"] == count, (
        f"Expected totalElements={count}, got {data['totalElements']}"
    )


@then(parsers.parse("the total pages should be {count:d}"))
def check_total_pages(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["totalPages"] == count, (
        f"Expected totalPages={count}, got {data['totalPages']}"
    )
