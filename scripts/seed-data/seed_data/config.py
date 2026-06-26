"""Runtime configuration resolved from CLI args and environment variables."""

from __future__ import annotations

import os
from dataclasses import dataclass

DEFAULT_API_URL = "https://api.local.buurman.io"
DEFAULT_KEYCLOAK_URL = "https://keycloak.local.buurman.io"
DEFAULT_MAILPIT_URL = "https://mailpit.local.buurman.io"
# Postgres is reached from the host via Traefik's published TCP port (PG_HOST_PORT, 6432).
DEFAULT_DB_URL = "postgresql://buurman:buurman@localhost:6432/buurman"

# Fixed password used for every seeded account (>= 8 chars, see RegisterRequest).
SEED_PASSWORD = "SeedData123!"

# Default seeded backoffice admin (buurman-backoffice realm) used to toggle the
# global invitation_required flag. Override via --backoffice-user/--backoffice-password.
DEFAULT_BACKOFFICE_USER = "buurmy@buurman.io"
DEFAULT_BACKOFFICE_PASSWORD = "buurmy"
BACKOFFICE_CLIENT_ID = "buurman-backoffice-web"


@dataclass(frozen=True)
class Config:
    users: int
    months: int
    growth: str
    seed: int
    concurrency: int

    api_url: str
    keycloak_url: str
    mailpit_url: str
    db_url: str

    documents: bool
    photos: bool
    verify_email: bool
    backdate: bool
    dry_run: bool

    manage_invitation_flag: bool
    backoffice_user: str
    backoffice_password: str

    out_dir: str

    @property
    def max_properties(self) -> int:
        return 12

    @property
    def max_leases_per_property(self) -> int:
        return 3


def _env(name: str, fallback: str) -> str:
    value = os.environ.get(name, "").strip()
    return value or fallback


def build_config(ns) -> Config:
    """Merge argparse namespace with env-var defaults into an immutable Config."""
    return Config(
        users=ns.users,
        months=ns.months,
        growth=ns.growth,
        seed=ns.seed,
        concurrency=ns.concurrency,
        api_url=(ns.api_url or _env("SEED_API_URL", DEFAULT_API_URL)).rstrip("/"),
        keycloak_url=(
            ns.keycloak_url or _env("SEED_KEYCLOAK_URL", DEFAULT_KEYCLOAK_URL)
        ).rstrip("/"),
        mailpit_url=(ns.mailpit_url or _env("SEED_MAILPIT_URL", DEFAULT_MAILPIT_URL)).rstrip("/"),
        db_url=ns.db_url or _env("SEED_DB_URL", DEFAULT_DB_URL),
        documents=ns.documents,
        photos=ns.photos,
        verify_email=not ns.skip_email_verify,
        backdate=ns.backdate,
        dry_run=ns.dry_run,
        manage_invitation_flag=ns.manage_invitation_flag,
        backoffice_user=ns.backoffice_user or _env("SEED_BACKOFFICE_USER", DEFAULT_BACKOFFICE_USER),
        backoffice_password=(
            ns.backoffice_password or _env("SEED_BACKOFFICE_PASSWORD", DEFAULT_BACKOFFICE_PASSWORD)
        ),
        out_dir=ns.out_dir,
    )
