"""Step definitions for payment tracking scenarios."""

from __future__ import annotations

from pytest_bdd import parsers, scenarios, then, when

from utils.api_client import BuurmanApiClient
from utils.date_utils import resolve_date

from .conftest import ScenarioContext

scenarios("../features/payments/recording-payments.feature")


# ---------------------------------------------------------------------------
# When steps
# ---------------------------------------------------------------------------


@when("I create a payment with:")
def create_payment(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    data = {row[0]: row[1] for row in datatable}
    assert ctx.current_contract_id, "No contract created yet"

    body = {
        "contractIdentifier": ctx.current_contract_id,
        "amount": float(data["amount"]),
        "currency": data.get("currency", "EUR"),
        "dueDate": resolve_date(data["dueDate"]),
    }
    if "notes" in data:
        body["notes"] = data["notes"]
    if "markAsPaid" in data:
        body["markAsPaid"] = data["markAsPaid"].lower() == "true"
    if "paymentDate" in data:
        body["paymentDate"] = resolve_date(data["paymentDate"])

    ctx.last_response = api.post("/payments", json=body)
    if ctx.last_response.status_code == 201:
        ctx.current_payment_id = ctx.last_response.json()["identifier"]


@when("I mark the payment as paid")
def mark_payment_paid(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_payment_id, "No payment created yet"
    ctx.last_response = api.post(
        f"/payments/{ctx.current_payment_id}/mark-paid",
        json={"paymentDate": resolve_date("today")},
    )


@when(parsers.parse('I record a receival of {amount:g} on "{date}"'))
def record_receival(
    ctx: ScenarioContext, api: BuurmanApiClient, amount: float, date: str
):
    assert ctx.current_payment_id, "No payment created yet"
    ctx.last_response = api.post(
        f"/payments/{ctx.current_payment_id}/receivals",
        json={
            "amount": amount,
            "receivalDate": resolve_date(date),
        },
    )


@when("I retrieve the payment by its identifier")
def retrieve_payment(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_payment_id, "No payment identifier set"
    ctx.last_response = api.get(f"/payments/{ctx.current_payment_id}")


@when("I list all payments")
def list_payments(ctx: ScenarioContext, api: BuurmanApiClient):
    ctx.last_response = api.get("/payments")


@when("I delete the payment")
def delete_payment(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_payment_id, "No payment identifier set"
    ctx.last_response = api.delete(f"/payments/{ctx.current_payment_id}")


# ---------------------------------------------------------------------------
# Then steps
# ---------------------------------------------------------------------------


@then(parsers.parse('the payment status should be "{status}"'))
def check_payment_status(ctx: ScenarioContext, status: str):
    data = ctx.last_response.json()
    assert data["status"] == status, f"Expected status '{status}', got '{data['status']}'"


@then(parsers.parse("the payment amount should be {amount:g}"))
def check_payment_amount(ctx: ScenarioContext, amount: float):
    actual = ctx.last_response.json()["amount"]
    assert abs(actual - amount) < 0.01, f"Expected amount {amount}, got {actual}"


@then(parsers.parse("the payment received amount should be {amount:g}"))
def check_received_amount(ctx: ScenarioContext, amount: float):
    actual = ctx.last_response.json().get("receivedAmount", 0)
    assert abs(actual - amount) < 0.01, f"Expected receivedAmount {amount}, got {actual}"


@then(parsers.parse("the payment balance should be {amount:g}"))
def check_payment_balance(ctx: ScenarioContext, amount: float):
    actual = ctx.last_response.json().get("balance", 0)
    assert abs(actual - amount) < 0.01, f"Expected balance {amount}, got {actual}"
