# BDD Test Suite

External black-box behavior tests for the Buurman API, written in Python with [pytest-bdd](https://pytest-bdd.readthedocs.io/) and Gherkin feature files.

These tests run against **live Docker containers** — they exercise the real API, database, Keycloak, and email service. No mocking, no in-process shortcuts.

## Prerequisites

- Running Buurman stack (`make up` or `make dev` + local backend)
- Python 3.11+
- A `buurman-bdd` client configured in Keycloak (see below)

## Setup

```bash
cd tests/bdd
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
```

### Keycloak Client

The tests authenticate via Resource Owner Password Grant using a dedicated `buurman-bdd` public client in the `buurman` realm. This client must have:

- `directAccessGrantsEnabled: true`
- `publicClient: true`
- Same `defaultClientScopes` as `buurman-web`

This client is already defined in `keycloak/buurman-realm.json`.

### Environment Variables

Override defaults via a `.env.bdd` file (gitignored) or environment variables:

| Variable | Default | Description |
|---|---|---|
| `BDD_API_URL` | `https://api.local.buurman.io` | Backend API base URL |
| `BDD_KEYCLOAK_URL` | `https://keycloak.local.buurman.io` | Keycloak base URL |
| `BDD_MAILPIT_URL` | `https://mailpit.local.buurman.io` | Mailpit base URL |

## Running Tests

```bash
# All tests
make test-bdd

# Smoke tests only (fastest feedback)
make test-bdd-smoke

# By tag
cd tests/bdd && python -m pytest -m payments
cd tests/bdd && python -m pytest -m "critical and multi_tenancy"

# With verbose output
cd tests/bdd && python -m pytest -v

# Generate HTML report
cd tests/bdd && python -m pytest --html=report.html
```

### Available Markers

| Marker | Description |
|---|---|
| `smoke` | Core happy paths (~4 scenarios, run first) |
| `critical` | Must-pass scenarios including security |
| `properties` | Property management |
| `contacts` | Contact management |
| `contracts` | Contract creation and lifecycle |
| `payments` | Payment tracking |
| `multi_tenancy` | Cross-team data isolation |
| `access_control` | Role-based access (admin/editor/viewer) |

## Architecture

### Test Isolation

Each scenario creates a **new team** via the full registration flow (register user, verify email via Mailpit, authenticate via Keycloak). This guarantees:

- Complete data isolation between scenarios (separate `team_id`)
- No shared mutable state between tests
- Tests can run in any order

### Test Data & Teardown

Tests intentionally do **not** clean up after themselves. Each scenario's data lives in its own team and is invisible to other scenarios.

For local development, test data accumulates in the database but is negligible in size (~40 teams per full run). Reset when needed:

```bash
make down-v   # Stop containers and wipe all volumes (full reset)
```

For CI pipelines, use ephemeral Docker containers that are destroyed after each run — no cleanup needed.

### Dynamic Dates

All dates in feature files use **relative expressions** (`next month`, `in 30 days`, `today`) instead of hardcoded calendar dates. The `utils/date_utils.py` module resolves these to ISO date strings at runtime. This ensures tests never expire.

Supported expressions:

| Expression | Example Result |
|---|---|
| `today` | `2026-03-28` |
| `tomorrow` | `2026-03-29` |
| `yesterday` | `2026-03-27` |
| `in N days` | `2026-04-27` (for `in 30 days`) |
| `N days ago` | `2026-03-25` (for `3 days ago`) |
| `next month` | `2026-04-28` |
| `last month` | `2026-02-28` |
| `in N months` | `2027-03-28` (for `in 12 months`) |
| `N months ago` | `2026-01-28` (for `2 months ago`) |
| `2026-04-01` | `2026-04-01` (ISO passthrough) |

### Project Structure

```
tests/bdd/
├── conftest.py              Root fixtures (URLs, API client)
├── pyproject.toml           Dependencies and pytest config
├── README.md                This file
├── features/                Gherkin feature files
│   ├── property-management/
│   ├── contacts/
│   ├── contracts/
│   ├── payments/
│   ├── multi-tenancy/
│   └── access-control/
├── step_defs/               Step implementations
│   ├── conftest.py          Shared steps (team, auth, entity helpers)
│   ├── test_properties.py
│   ├── test_contacts.py
│   ├── test_contracts.py
│   ├── test_payments.py
│   ├── test_multi_tenancy.py
│   └── test_access_control.py
└── utils/                   Test infrastructure
    ├── api_client.py        HTTP client wrapper
    ├── auth.py              Keycloak auth + registration flow
    └── date_utils.py        Relative date resolution
```

### Adding a New Feature

1. Create `features/<domain>/<name>.feature` with Gherkin scenarios
2. Create `step_defs/test_<domain>.py` with step implementations
3. Reuse shared steps from `step_defs/conftest.py` (team creation, entity helpers, assertions)
4. Use `resolve_date()` / `resolve_datetime()` for any date fields — never hardcode dates
5. Tag scenarios with the appropriate markers
