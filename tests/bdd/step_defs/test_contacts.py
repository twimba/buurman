"""Step definitions for contact management scenarios."""

from __future__ import annotations

from pytest_bdd import parsers, scenarios, then, when

from utils.api_client import BuurmanApiClient
from utils.date_utils import resolve_datetime

from .conftest import ScenarioContext

scenarios("../features/contacts/managing-contacts.feature")


# ---------------------------------------------------------------------------
# When steps
# ---------------------------------------------------------------------------


@when("I create a contact with:")
def create_contact(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    data = {row[0]: row[1] for row in datatable}
    ctx.last_response = api.post("/contacts", json={
        "contactType": data.get("contactType", "INDIVIDUAL"),
        "firstName": data.get("firstName"),
        "lastName": data.get("lastName"),
        "email": data.get("email"),
        "companyName": data.get("companyName"),
    })
    if ctx.last_response.status_code == 201:
        ctx.current_contact_id = ctx.last_response.json()["identifier"]


@when("I list all contacts")
def list_contacts(ctx: ScenarioContext, api: BuurmanApiClient):
    ctx.last_response = api.get("/contacts")


@when("I retrieve the contact by its identifier")
def retrieve_contact(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contact_id, "No contact identifier set"
    ctx.last_response = api.get(f"/contacts/{ctx.current_contact_id}")


@when(parsers.parse('I update the contact email to "{email}"'))
def update_contact_email(ctx: ScenarioContext, api: BuurmanApiClient, email: str):
    assert ctx.current_contact_id, "No contact identifier set"
    ctx.last_response = api.put(
        f"/contacts/{ctx.current_contact_id}",
        json={"contactType": "INDIVIDUAL", "email": email},
    )


@when("I delete the contact")
def delete_contact(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contact_id, "No contact identifier set"
    ctx.last_response = api.delete(f"/contacts/{ctx.current_contact_id}")


@when(parsers.parse('I add the tag "{tag}" to the contact'))
def add_tag_to_contact(ctx: ScenarioContext, api: BuurmanApiClient, tag: str):
    assert ctx.current_contact_id, "No contact identifier set"
    ctx.last_response = api.post(
        f"/contacts/{ctx.current_contact_id}/tags",
        json={"tag": tag},
    )


@when(parsers.parse('I add a note "{note}" to the contact'))
def add_note_to_contact(ctx: ScenarioContext, api: BuurmanApiClient, note: str):
    assert ctx.current_contact_id, "No contact identifier set"
    ctx.last_response = api.post(
        f"/contacts/{ctx.current_contact_id}/notes",
        json={"interactionType": "NOTE", "body": note, "occurredAt": resolve_datetime("today")},
    )


# ---------------------------------------------------------------------------
# Then steps
# ---------------------------------------------------------------------------


@then(parsers.parse('the contact first name should be "{name}"'))
def check_contact_first_name(ctx: ScenarioContext, name: str):
    assert ctx.last_response.json()["firstName"] == name


@then(parsers.parse('the contact last name should be "{name}"'))
def check_contact_last_name(ctx: ScenarioContext, name: str):
    assert ctx.last_response.json()["lastName"] == name


@then(parsers.parse('the contact type should be "{contact_type}"'))
def check_contact_type(ctx: ScenarioContext, contact_type: str):
    assert ctx.last_response.json()["contactType"] == contact_type


@then(parsers.parse('the contact email should be "{email}"'))
def check_contact_email(ctx: ScenarioContext, email: str):
    assert ctx.last_response.json()["email"] == email
