"""Step definitions for property management scenarios."""

from __future__ import annotations

from pytest_bdd import parsers, scenarios, then, when

from utils.api_client import BuurmanApiClient

from .conftest import ScenarioContext

scenarios("../features/property-management/onboarding-a-property.feature")


# ---------------------------------------------------------------------------
# When steps
# ---------------------------------------------------------------------------


@when("I create a property with:")
def create_property(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    data = {row[0]: row[1] for row in datatable}
    ctx.last_response = api.post("/properties", json={
        "street": data.get("street", "Test Street"),
        "city": data.get("city", "Amsterdam"),
        "postalCode": data.get("postalCode", "1000AA"),
        "country": data.get("country", "NL"),
        "propertyCategory": data.get("category", "RESIDENTIAL"),
        "propertyType": data.get("type", "APARTMENT"),
        "status": "VACANT",
    })
    if ctx.last_response.status_code == 201:
        ctx.current_property_id = ctx.last_response.json()["identifier"]


@when("I list all properties")
def list_properties(ctx: ScenarioContext, api: BuurmanApiClient):
    ctx.last_response = api.get("/properties")


@when(parsers.parse("I list properties with page {page:d} and size {size:d}"))
def list_properties_paginated(
    ctx: ScenarioContext, api: BuurmanApiClient, page: int, size: int
):
    ctx.last_response = api.get("/properties", params={"page": page, "size": size})


@when("I retrieve the property by its identifier")
def retrieve_property(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_property_id, "No property identifier set"
    ctx.last_response = api.get(f"/properties/{ctx.current_property_id}")


@when(parsers.parse('I try to retrieve a property with identifier "{identifier}"'))
def retrieve_property_by_id(ctx: ScenarioContext, api: BuurmanApiClient, identifier: str):
    ctx.last_response = api.get(f"/properties/{identifier}")


@when(parsers.parse('I update the property street to "{street}"'))
def update_property_street(ctx: ScenarioContext, api: BuurmanApiClient, street: str):
    assert ctx.current_property_id, "No property identifier set"
    ctx.last_response = api.put(
        f"/properties/{ctx.current_property_id}",
        json={"street": street},
    )


@when("I delete the property")
def delete_property(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_property_id, "No property identifier set"
    ctx.last_response = api.delete(f"/properties/{ctx.current_property_id}")


# ---------------------------------------------------------------------------
# Then steps
# ---------------------------------------------------------------------------


@then(parsers.parse('the property category should be "{category}"'))
def check_property_category(ctx: ScenarioContext, category: str):
    assert ctx.last_response.json()["propertyCategory"] == category


@then(parsers.parse('the property type should be "{prop_type}"'))
def check_property_type(ctx: ScenarioContext, prop_type: str):
    assert ctx.last_response.json()["propertyType"] == prop_type


@then(parsers.parse('the property status should be "{status}"'))
def check_property_status(ctx: ScenarioContext, status: str):
    assert ctx.last_response.json()["status"] == status


@then(parsers.parse('the property street should be "{street}"'))
def check_property_street(ctx: ScenarioContext, street: str):
    assert ctx.last_response.json()["street"] == street
