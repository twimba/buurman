"""Step definitions for contract scenarios (creation + lifecycle)."""

from __future__ import annotations

from pytest_bdd import parsers, scenarios, then, when

from utils.api_client import BuurmanApiClient
from utils.date_utils import resolve_date

from .conftest import ScenarioContext

scenarios("../features/contracts/creating-a-contract.feature")
scenarios("../features/contracts/contract-lifecycle.feature")


# ---------------------------------------------------------------------------
# When steps: Contract creation
# ---------------------------------------------------------------------------


@when("I create a contract with:")
def create_contract(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    data = {row[0]: row[1] for row in datatable}
    assert ctx.current_property_id, "No property created yet"
    assert ctx.current_contact_id, "No contact created yet"

    body = {
        "contractType": data.get("contractType", "FIXED_TERM"),
        "propertyIdentifier": ctx.current_property_id,
        "parties": [{"role": "PRIMARY_TENANT", "contactIdentifier": ctx.current_contact_id}],
        "paymentFrequency": data.get("paymentFrequency", "MONTHLY"),
        "rentAmount": float(data["rentAmount"]),
        "startDate": resolve_date(data["startDate"]),
        "countryMetadata": {},
    }
    if "endDate" in data:
        body["endDate"] = resolve_date(data["endDate"])
    if "depositAmount" in data:
        body["depositAmount"] = float(data["depositAmount"])

    ctx.last_response = api.post("/contracts", json=body)
    if ctx.last_response.status_code == 201:
        ctx.current_contract_id = ctx.last_response.json()["identifier"]


@when("I create a contract without a property:")
def create_contract_no_property(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    data = {row[0]: row[1] for row in datatable}
    assert ctx.current_contact_id, "No contact created yet"

    ctx.last_response = api.post("/contracts", json={
        "contractType": data.get("contractType", "FIXED_TERM"),
        "parties": [{"role": "PRIMARY_TENANT", "contactIdentifier": ctx.current_contact_id}],
        "paymentFrequency": data.get("paymentFrequency", "MONTHLY"),
        "rentAmount": float(data["rentAmount"]),
        "startDate": resolve_date(data["startDate"]),
        "countryMetadata": {},
    })


@when("I create a contract without any parties:")
def create_contract_no_parties(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    data = {row[0]: row[1] for row in datatable}
    assert ctx.current_property_id, "No property created yet"

    ctx.last_response = api.post("/contracts", json={
        "contractType": data.get("contractType", "FIXED_TERM"),
        "propertyIdentifier": ctx.current_property_id,
        "parties": [],
        "paymentFrequency": data.get("paymentFrequency", "MONTHLY"),
        "rentAmount": float(data["rentAmount"]),
        "startDate": resolve_date(data["startDate"]),
        "countryMetadata": {},
    })


# ---------------------------------------------------------------------------
# When steps: Contract lifecycle
# ---------------------------------------------------------------------------


@when(parsers.parse('I change the contract status to "{status}"'))
def change_contract_status(ctx: ScenarioContext, api: BuurmanApiClient, status: str):
    assert ctx.current_contract_id, "No contract created yet"
    ctx.last_response = api.post(
        f"/contracts/{ctx.current_contract_id}/change-status",
        json={"status": status},
    )


@when(parsers.parse('I change the contract status to "{status}" with reason "{reason}"'))
def change_contract_status_with_reason(
    ctx: ScenarioContext, api: BuurmanApiClient, status: str, reason: str
):
    assert ctx.current_contract_id, "No contract created yet"
    ctx.last_response = api.post(
        f"/contracts/{ctx.current_contract_id}/change-status",
        json={"status": status, "reason": reason},
    )


@when("I reopen the contract")
def reopen_contract(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id, "No contract created yet"
    ctx.last_response = api.post(f"/contracts/{ctx.current_contract_id}/reopen")


@when("I list all contracts")
def list_contracts(ctx: ScenarioContext, api: BuurmanApiClient):
    ctx.last_response = api.get("/contracts")


@when("I retrieve the contract by its identifier")
def retrieve_contract(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id, "No contract identifier set"
    ctx.last_response = api.get(f"/contracts/{ctx.current_contract_id}")


@when("I delete the contract")
def delete_contract(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id, "No contract identifier set"
    ctx.last_response = api.delete(f"/contracts/{ctx.current_contract_id}")


# ---------------------------------------------------------------------------
# Then steps
# ---------------------------------------------------------------------------


@then(parsers.parse('the contract status should be "{status}"'))
def check_contract_status(ctx: ScenarioContext, status: str):
    data = ctx.last_response.json()
    assert data["status"] == status, f"Expected status '{status}', got '{data['status']}'"


@then(parsers.parse('the contract type should be "{contract_type}"'))
def check_contract_type(ctx: ScenarioContext, contract_type: str):
    assert ctx.last_response.json()["contractType"] == contract_type


@then(parsers.parse("the contract rent amount should be {amount:g}"))
def check_contract_rent(ctx: ScenarioContext, amount: float):
    actual = ctx.last_response.json()["rentAmount"]
    assert abs(actual - amount) < 0.01, f"Expected rent {amount}, got {actual}"


@then(parsers.parse("the contract deposit amount should be {amount:g}"))
def check_contract_deposit(ctx: ScenarioContext, amount: float):
    actual = ctx.last_response.json()["depositAmount"]
    assert abs(actual - amount) < 0.01, f"Expected deposit {amount}, got {actual}"


@then(parsers.parse('the contract should reference the property "{street}"'))
def check_contract_property(ctx: ScenarioContext, street: str):
    prop = ctx.last_response.json().get("property", {})
    assert prop.get("street") == street, f"Expected property street '{street}', got '{prop}'"


@then(parsers.parse("the contract should have at least {count:d} party"))
def check_contract_parties(ctx: ScenarioContext, count: int):
    parties = ctx.last_response.json().get("parties", [])
    assert len(parties) >= count, f"Expected >= {count} parties, got {len(parties)}"
