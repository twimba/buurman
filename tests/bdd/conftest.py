"""Root conftest for Buurman BDD tests.

Provides session-scoped fixtures for infrastructure (URLs, clients)
and function-scoped fixtures for per-scenario isolation (team context).
"""

from __future__ import annotations

import os

import pytest
from dotenv import load_dotenv

from utils.api_client import BuurmanApiClient

# Load .env.bdd if present (for local overrides)
load_dotenv(os.path.join(os.path.dirname(__file__), ".env.bdd"), override=True)


# ---------------------------------------------------------------------------
# Session-scoped fixtures (shared across all scenarios in a test run)
# ---------------------------------------------------------------------------


@pytest.fixture(scope="session")
def api_base_url() -> str:
    """Base URL for the Buurman REST API."""
    return os.getenv("BDD_API_URL", "https://api.local.buurman.io")


@pytest.fixture(scope="session")
def keycloak_url() -> str:
    """Base URL for the Keycloak instance."""
    return os.getenv("BDD_KEYCLOAK_URL", "https://keycloak.local.buurman.io")


@pytest.fixture(scope="session")
def mailpit_url() -> str:
    """Base URL for the Mailpit email capture service."""
    return os.getenv("BDD_MAILPIT_URL", "https://mailpit.local.buurman.io")


# ---------------------------------------------------------------------------
# Function-scoped fixtures (fresh per scenario)
# ---------------------------------------------------------------------------


@pytest.fixture()
def api(api_base_url: str) -> BuurmanApiClient:
    """Fresh API client for each scenario. No auth configured initially."""
    return BuurmanApiClient(api_base_url)
