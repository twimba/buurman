"""Step definitions for contact import scenarios."""

from __future__ import annotations

import io

from pytest_bdd import given, parsers, scenarios, then, when

from utils.api_client import BuurmanApiClient

from .conftest import ScenarioContext

scenarios("../features/contacts/importing-contacts.feature")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _build_csv_bytes(headers: list[str], rows: list[list[str]]) -> bytes:
    """Build CSV content from headers and rows."""
    buf = io.StringIO()
    buf.write(",".join(headers) + "\n")
    for row in rows:
        buf.write(",".join(row) + "\n")
    return buf.getvalue().encode("utf-8")


def _build_csv_bytes_no_header(rows: list[list[str]]) -> bytes:
    """Build CSV content without a header row."""
    buf = io.StringIO()
    for row in rows:
        buf.write(",".join(row) + "\n")
    return buf.getvalue().encode("utf-8")


def _upload_csv(api: BuurmanApiClient, csv_bytes: bytes, header_row: bool = True):
    """Upload a CSV file and return the raw response."""
    params = {"headerRow": "true" if header_row else "false"}
    resp = api.post_multipart(
        "/imports/upload",
        files={"file": ("contacts.csv", csv_bytes, "text/csv")},
        params=params,
    )
    return resp


def _mappings_from_datatable(datatable) -> dict[str, str]:
    """Convert a datatable of source->target mappings to a dict."""
    return {row[0]: row[1] for row in datatable}


# ---------------------------------------------------------------------------
# Given steps
# ---------------------------------------------------------------------------


@given("a CSV file is uploaded with headers:")
def upload_csv_with_headers_given(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    headers = datatable[0]
    rows = datatable[1:]
    csv_bytes = _build_csv_bytes(headers, rows)
    resp = _upload_csv(api, csv_bytes, header_row=True)
    assert resp.status_code == 200, (
        f"Upload failed: {resp.status_code} {resp.text}"
    )
    data = resp.json()
    ctx.upload_file_name = data["fileName"]
    ctx.upload_columns = data["columns"]


@given("the existing contact email is stored")
def store_existing_contact_email(ctx: ScenarioContext, api: BuurmanApiClient):
    assert ctx.current_contact_id, "No contact created yet"
    resp = api.get(f"/contacts/{ctx.current_contact_id}")
    assert resp.status_code == 200
    ctx.existing_contact_email = resp.json()["email"]


@given("a CSV file is uploaded with duplicate email")
def upload_csv_with_duplicate_email(ctx: ScenarioContext, api: BuurmanApiClient):
    assert hasattr(ctx, "existing_contact_email"), "No existing contact email stored"
    email = ctx.existing_contact_email
    csv_bytes = _build_csv_bytes(
        ["firstName", "lastName", "email"],
        [["Duplicate", "Contact", email]],
    )
    resp = _upload_csv(api, csv_bytes, header_row=True)
    assert resp.status_code == 200, (
        f"Upload failed: {resp.status_code} {resp.text}"
    )
    data = resp.json()
    ctx.upload_file_name = data["fileName"]
    ctx.upload_columns = data["columns"]


@given(parsers.parse('the import is executed with contact type "{contact_type}"'))
def execute_import_given(ctx: ScenarioContext, api: BuurmanApiClient, contact_type: str):
    assert hasattr(ctx, "upload_file_name"), "No file uploaded yet"
    assert hasattr(ctx, "upload_columns"), "No columns from upload"

    mappings = {col: col for col in ctx.upload_columns}
    resp = api.post("/imports/execute", json={
        "fileName": ctx.upload_file_name,
        "mappings": mappings,
        "contactType": contact_type,
    })
    assert resp.status_code == 200, (
        f"Execute failed: {resp.status_code} {resp.text}"
    )
    data = resp.json()
    ctx.import_identifier = data["identifier"]


@given("the import is reverted")
def revert_import_given(ctx: ScenarioContext, api: BuurmanApiClient):
    assert hasattr(ctx, "import_identifier"), "No import identifier"
    resp = api.post(f"/imports/{ctx.import_identifier}/revert")
    assert resp.status_code == 200, (
        f"Revert failed: {resp.status_code} {resp.text}"
    )


# ---------------------------------------------------------------------------
# When steps
# ---------------------------------------------------------------------------


@when("I upload a CSV file with headers:")
def upload_csv_with_headers(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    headers = datatable[0]
    rows = datatable[1:]
    csv_bytes = _build_csv_bytes(headers, rows)
    ctx.last_response = _upload_csv(api, csv_bytes, header_row=True)


@when("I upload a CSV file without headers:")
def upload_csv_without_headers(ctx: ScenarioContext, api: BuurmanApiClient, datatable):
    rows = [list(row) for row in datatable]
    csv_bytes = _build_csv_bytes_no_header(rows)
    ctx.last_response = _upload_csv(api, csv_bytes, header_row=False)


@when(parsers.parse('I preview the import with contact type "{contact_type}" and mappings:'))
def preview_import(
    ctx: ScenarioContext, api: BuurmanApiClient, contact_type: str, datatable,
):
    assert hasattr(ctx, "upload_file_name"), "No file uploaded yet"
    mappings = _mappings_from_datatable(datatable)
    ctx.last_response = api.post("/imports/preview", json={
        "fileName": ctx.upload_file_name,
        "mappings": mappings,
        "contactType": contact_type,
    })


@when("I preview the import with the duplicate email mapping")
def preview_import_duplicate(ctx: ScenarioContext, api: BuurmanApiClient):
    assert hasattr(ctx, "upload_file_name"), "No file uploaded yet"
    mappings = {"firstName": "firstName", "lastName": "lastName", "email": "email"}
    ctx.last_response = api.post("/imports/preview", json={
        "fileName": ctx.upload_file_name,
        "mappings": mappings,
        "contactType": "INDIVIDUAL",
    })


@when(parsers.parse('I execute the import with contact type "{contact_type}" and mappings:'))
def execute_import(
    ctx: ScenarioContext, api: BuurmanApiClient, contact_type: str, datatable,
):
    assert hasattr(ctx, "upload_file_name"), "No file uploaded yet"
    mappings = _mappings_from_datatable(datatable)
    ctx.last_response = api.post("/imports/execute", json={
        "fileName": ctx.upload_file_name,
        "mappings": mappings,
        "contactType": contact_type,
    })
    if ctx.last_response.status_code == 200:
        ctx.import_identifier = ctx.last_response.json()["identifier"]


@when("I list all imports")
def list_imports(ctx: ScenarioContext, api: BuurmanApiClient):
    ctx.last_response = api.get("/imports")


@when("I get the import detail")
def get_import_detail(ctx: ScenarioContext, api: BuurmanApiClient):
    assert hasattr(ctx, "import_identifier"), "No import identifier"
    ctx.last_response = api.get(f"/imports/{ctx.import_identifier}")


@when("I revert the import")
def revert_import(ctx: ScenarioContext, api: BuurmanApiClient):
    assert hasattr(ctx, "import_identifier"), "No import identifier"
    ctx.last_response = api.post(f"/imports/{ctx.import_identifier}/revert")


# ---------------------------------------------------------------------------
# Then steps
# ---------------------------------------------------------------------------


@then(parsers.parse('the upload response should contain columns "{columns}"'))
def check_upload_columns(ctx: ScenarioContext, columns: str):
    data = ctx.last_response.json()
    expected = [c.strip() for c in columns.split(",")]
    assert data["columns"] == expected, (
        f"Expected columns {expected}, got {data['columns']}"
    )


@then(parsers.parse("the upload response should have {count:d} total rows"))
def check_upload_total_rows(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["totalRows"] == count, (
        f"Expected {count} total rows, got {data['totalRows']}"
    )


@then(parsers.parse('the upload response file format should be "{fmt}"'))
def check_upload_file_format(ctx: ScenarioContext, fmt: str):
    data = ctx.last_response.json()
    assert data["fileFormat"] == fmt, (
        f"Expected format '{fmt}', got '{data['fileFormat']}'"
    )


@then(parsers.parse("the preview should show {count:d} to create"))
def check_preview_to_create(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["toCreate"] == count, (
        f"Expected {count} to create, got {data['toCreate']}"
    )


@then(parsers.parse("the preview should show {count:d} to skip"))
def check_preview_to_skip(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["toSkip"] == count, (
        f"Expected {count} to skip, got {data['toSkip']}"
    )


@then(parsers.parse("the preview should show {count:d} errors"))
def check_preview_errors(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["errors"] == count, (
        f"Expected {count} errors, got {data['errors']}"
    )


@then(parsers.parse("the execute response should have imported {count:d} contacts"))
def check_execute_imported(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["importedCount"] == count, (
        f"Expected {count} imported, got {data['importedCount']}"
    )


@then(parsers.parse("the execute response should have skipped {count:d} contacts"))
def check_execute_skipped(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["skippedCount"] == count, (
        f"Expected {count} skipped, got {data['skippedCount']}"
    )


@then(parsers.parse("the execute response should have {count:d} errors"))
def check_execute_errors(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["errorCount"] == count, (
        f"Expected {count} errors, got {data['errorCount']}"
    )


@then(parsers.parse('the execute response should contain an identifier starting with "{prefix}"'))
def check_execute_identifier_prefix(ctx: ScenarioContext, prefix: str):
    data = ctx.last_response.json()
    identifier = data.get("identifier", "")
    assert identifier.startswith(prefix), (
        f"Expected identifier starting with '{prefix}', got '{identifier}'"
    )


@then(parsers.parse("the imports list should contain at least {count:d} item"))
def check_imports_list_min_count(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    content = data.get("content", [])
    assert len(content) >= count, (
        f"Expected at least {count} imports, got {len(content)}"
    )


@then(parsers.parse('the import detail status should be "{status}"'))
def check_import_detail_status(ctx: ScenarioContext, status: str):
    data = ctx.last_response.json()
    assert data["status"] == status, (
        f"Expected status '{status}', got '{data['status']}'"
    )


@then(parsers.parse("the import detail should have {count:d} imported rows"))
def check_import_detail_imported_rows(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["importedRows"] == count, (
        f"Expected {count} imported rows, got {data['importedRows']}"
    )


@then(parsers.parse("the import detail should have {count:d} items"))
def check_import_detail_items(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    items = data.get("items", [])
    assert len(items) == count, (
        f"Expected {count} items, got {len(items)}"
    )


@then(parsers.parse("the revert response should show {count:d} deleted contacts"))
def check_revert_deleted_contacts(ctx: ScenarioContext, count: int):
    data = ctx.last_response.json()
    assert data["deletedContactCount"] == count, (
        f"Expected {count} deleted contacts, got {data['deletedContactCount']}"
    )
