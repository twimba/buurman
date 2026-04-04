"""Step definitions for document generation scenarios."""

from __future__ import annotations

from pytest_bdd import given, parsers, scenarios, then, when

from utils.api_client import BuurmanApiClient
from utils.date_utils import resolve_date

from .conftest import ScenarioContext

scenarios("../features/documents/generating-documents.feature")


# ---------------------------------------------------------------------------
# Given steps
# ---------------------------------------------------------------------------


@given("a contract extension exists")
def create_contract_extension(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id, "No contract created yet"
    resp = api.post(
        f"/contracts/{ctx.current_contract_id}/extensions",
        json={
            "newEndDate": resolve_date("in 24 months"),
            "newRentAmount": 1350.00,
            "rentAdjustmentType": "FIXED_AMOUNT",
            "rentAdjustmentValue": 100.00,
        },
    )
    assert resp.status_code == 201, (
        f"Failed to create extension: {resp.status_code} {resp.text}"
    )
    ctx.stored_identifiers["extension"] = resp.json()["identifier"]


@given("I clear my authentication")
def clear_auth(api: BuurmanApiClient):
    api.clear_auth()


# ---------------------------------------------------------------------------
# When steps: Extension addendum
# ---------------------------------------------------------------------------


@when("I download the extension addendum")
def download_addendum(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id
    ext_id = ctx.stored_identifiers.get("extension")
    assert ext_id, "No extension created yet"
    ctx.last_response = api.get(
        f"/contracts/{ctx.current_contract_id}/extensions/{ext_id}/addendum",
        headers={"Accept": "application/pdf"},
    )


@when(parsers.parse('I download the extension addendum with language "{lang}"'))
def download_addendum_with_lang(ctx: ScenarioContext, api: BuurmanApiClient, lang: str):
    assert ctx.current_contract_id
    ext_id = ctx.stored_identifiers.get("extension")
    assert ext_id, "No extension created yet"
    ctx.last_response = api.get(
        f"/contracts/{ctx.current_contract_id}/extensions/{ext_id}/addendum",
        params={"lang": lang},
        headers={"Accept": "application/pdf"},
    )


@when("I download the addendum for a non-existent extension")
def download_addendum_not_found(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id
    ctx.last_response = api.get(
        f"/contracts/{ctx.current_contract_id}/extensions/ext_nonexistent000000000000/addendum",
        headers={"Accept": "application/pdf"},
    )


# ---------------------------------------------------------------------------
# When steps: Rent increase letter
# ---------------------------------------------------------------------------


@when("I download the rent increase letter")
def download_letter(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id
    ext_id = ctx.stored_identifiers.get("extension")
    assert ext_id, "No extension created yet"
    ctx.last_response = api.get(
        f"/contracts/{ctx.current_contract_id}/extensions/{ext_id}/rent-increase-letter",
        headers={"Accept": "application/pdf"},
    )


@when(parsers.parse('I download the rent increase letter with language "{lang}"'))
def download_letter_with_lang(ctx: ScenarioContext, api: BuurmanApiClient, lang: str):
    assert ctx.current_contract_id
    ext_id = ctx.stored_identifiers.get("extension")
    assert ext_id, "No extension created yet"
    ctx.last_response = api.get(
        f"/contracts/{ctx.current_contract_id}/extensions/{ext_id}/rent-increase-letter",
        params={"lang": lang},
        headers={"Accept": "application/pdf"},
    )


# ---------------------------------------------------------------------------
# When steps: Rent change document
# ---------------------------------------------------------------------------


@when("I download the rent change document for the first rent period")
def download_rent_change(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id
    period_id = _get_first_rent_period_id(ctx, api)
    ctx.last_response = api.get(
        f"/contracts/{ctx.current_contract_id}/rent-periods/{period_id}/document",
        headers={"Accept": "application/pdf"},
    )


@when(
    parsers.parse(
        'I download the rent change document for the first rent period with language "{lang}"'
    )
)
def download_rent_change_with_lang(
    ctx: ScenarioContext, api: BuurmanApiClient, lang: str
):
    assert ctx.current_contract_id
    period_id = _get_first_rent_period_id(ctx, api)
    ctx.last_response = api.get(
        f"/contracts/{ctx.current_contract_id}/rent-periods/{period_id}/document",
        params={"lang": lang},
        headers={"Accept": "application/pdf"},
    )


@when("I download the rent change document for a non-existent period")
def download_rent_change_not_found(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contract_id
    ctx.last_response = api.get(
        f"/contracts/{ctx.current_contract_id}/rent-periods/crp_nonexistent000000000000/document",
        headers={"Accept": "application/pdf"},
    )


# ---------------------------------------------------------------------------
# Then steps
# ---------------------------------------------------------------------------


@then("the response should be a PDF document")
def check_pdf_response(ctx: ScenarioContext):
    assert ctx.last_response is not None
    content_type = ctx.last_response.headers.get("Content-Type", "")
    assert "application/pdf" in content_type, (
        f"Expected PDF content type, got '{content_type}'"
    )
    body = ctx.last_response.content
    assert len(body) > 0, "Response body is empty"
    assert body[:4] == b"%PDF", f"Response does not start with PDF magic bytes"


@then(parsers.parse('the content-disposition should contain "{text}"'))
def check_content_disposition(ctx: ScenarioContext, text: str):
    cd = ctx.last_response.headers.get("Content-Disposition", "")
    assert text in cd, f"Expected '{text}' in Content-Disposition '{cd}'"


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _get_first_rent_period_id(ctx: ScenarioContext, api: BuurmanApiClient) -> str:
    """Fetch the rent timeline and return the first period's identifier."""
    if "rent_period" in ctx.stored_identifiers:
        return ctx.stored_identifiers["rent_period"]

    resp = api.get(f"/contracts/{ctx.current_contract_id}/rent-periods")
    assert resp.status_code == 200, (
        f"Failed to get rent periods: {resp.status_code} {resp.text}"
    )
    periods = resp.json()
    assert len(periods) > 0, "No rent periods found for contract"
    period_id = periods[0]["identifier"]
    ctx.stored_identifiers["rent_period"] = period_id
    return period_id
