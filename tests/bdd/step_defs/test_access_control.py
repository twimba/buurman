"""Step definitions for role-based access control scenarios."""

from __future__ import annotations

from pytest_bdd import parsers, scenarios, when

from utils.api_client import BuurmanApiClient

from .conftest import ScenarioContext

scenarios("../features/access-control/role-based-access.feature")


# ---------------------------------------------------------------------------
# When steps
# ---------------------------------------------------------------------------


@when(parsers.parse('I try to update the property street to "{street}"'))
def try_update_property(ctx: ScenarioContext, api: BuurmanApiClient, street: str):
    assert ctx.current_property_id, "No property identifier set"
    ctx.last_response = api.put(
        f"/properties/{ctx.current_property_id}",
        json={"street": street},
    )


@when("I try to delete the current property")
def try_delete_property(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_property_id, "No property identifier set"
    ctx.last_response = api.delete(f"/properties/{ctx.current_property_id}")


@when("I make an unauthenticated request to list properties")
def unauthenticated_list(ctx: ScenarioContext, api: BuurmanApiClient):
    api.clear_auth()
    ctx.last_response = api.get("/properties")
