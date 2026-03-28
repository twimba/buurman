"""Step definitions for multi-tenant data isolation scenarios."""

from __future__ import annotations

from pytest_bdd import scenarios, when

from utils.api_client import BuurmanApiClient

from .conftest import ScenarioContext

scenarios("../features/multi-tenancy/data-isolation.feature")


# ---------------------------------------------------------------------------
# When steps: Cross-team access attempts
# ---------------------------------------------------------------------------


@when("I try to retrieve a property with the stored identifier")
def retrieve_stored_property(ctx: ScenarioContext, api: BuurmanApiClient):
    identifier = ctx.stored_identifiers.get("property")
    assert identifier, "No property identifier stored"
    ctx.last_response = api.get(f"/properties/{identifier}")


@when("I try to retrieve a contact with the stored identifier")
def retrieve_stored_contact(ctx: ScenarioContext, api: BuurmanApiClient):
    identifier = ctx.stored_identifiers.get("contact")
    assert identifier, "No contact identifier stored"
    ctx.last_response = api.get(f"/contacts/{identifier}")


@when("I try to update a property with the stored identifier")
def update_stored_property(ctx: ScenarioContext, api: BuurmanApiClient):
    identifier = ctx.stored_identifiers.get("property")
    assert identifier, "No property identifier stored"
    ctx.last_response = api.put(
        f"/properties/{identifier}",
        json={"street": "Hacked Street"},
    )


@when("I try to delete a property with the stored identifier")
def delete_stored_property(ctx: ScenarioContext, api: BuurmanApiClient):
    identifier = ctx.stored_identifiers.get("property")
    assert identifier, "No property identifier stored"
    ctx.last_response = api.delete(f"/properties/{identifier}")


@when("I list all expenses")
def list_expenses(ctx: ScenarioContext, api: BuurmanApiClient):
    ctx.last_response = api.get("/expenses")
