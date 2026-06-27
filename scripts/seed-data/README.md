# Buurman data seeder

Generates production-like data in a **local** Buurman workspace by driving the public
REST API: it registers N landlords ("users"), each with their own team, and fills their
account with properties, contacts, contracts, payments, documents and photos.

- **Signups** follow a startup **growth curve** (exponential by default) across the window.
- **Per-user activity** follows a **normal distribution** (engagement × account tenure).
- **Payments** span each lease month-by-month with a realistic PAID / PENDING / OVERDUE mix.

## How it works

Almost everything is created through the API (`/auth/register`, `/properties`,
`/contacts`, `/contracts` + `change-status`, `/payments`, `…/documents`, `…/photos`).
Contracts are created DRAFT, activated so payments can attach, then expired/terminated
once their lease ends (one active contract per property at a time).

Two things can't go through the API at scale, so they touch the DB directly (the only
non-API steps):

- **Backdating** ([`backdate.py`](seed_data/backdate.py)) — the API stamps `created_at =
  now`, so a single batched pass rewrites the historical timestamps (`created_at`,
  `uploaded_at`, `joined_at`) on the rows it created, keyed by `identifier`. Disable with
  `--no-backdate`.
- **Email verification** ([`db.py`](seed_data/db.py)) — writes are gated on a verified
  email, but `/auth/verify-email` is IP-rate-limited and can't keep up. By default the
  seeder sets `users.email_verified_at` directly (the backend reads it per request). Use
  `--email-verify mailpit` for the real code flow, or `none` to skip.

The seeder also **auto-manages the global `invitation_required` flag**: it's on by default
and would reject every `/auth/register`, so the seeder disables it (via the backoffice
admin API, which also evicts the flag cache) for the run and restores the original value
afterwards. Disable this with `--no-manage-invitation-flag`.

## Prerequisites

- The local stack is up (`make dev` + a running backend, or `make up`).
- Postgres reachable from the host (Traefik publishes it on `PG_HOST_PORT`, default 6432)
  — needed for db-based email verification and backdating.
- The seeded backoffice admin exists (`buurmy@buurman.io` / `buurmy`, role
  `BACKOFFICE_ADMIN`) to manage the invitation flag — or pass `--backoffice-user/-password`.
- Poetry (`pipx install poetry` or `brew install poetry`).

## Usage

From the repo root (recommended — the Makefile injects workspace-aware URLs):

```bash
make seed-data                                   # 1000 users / 24 months (slow!)
make seed-data ARGS="--users 25 --months 6"      # small, fast run
make seed-data ARGS="--users 50 --dry-run"       # just print the planned volume
```

Or directly:

```bash
cd scripts/seed-data
poetry install
poetry run python -m seed_data --users 25 --months 6 --seed 1
```

### Key options

| Flag | Default | Meaning |
|------|---------|---------|
| `--users` | `1000` | number of users/teams |
| `--months` | `24` | usage window |
| `--growth` | `exponential` | `exponential` \| `logistic` \| `linear` |
| `--seed` | `42` | reproducible dataset |
| `--concurrency` | `6` | parallel users |
| `--no-documents` / `--no-photos` | on | skip slow S3 uploads |
| `--email-verify` | `db` | `db` (fast) \| `mailpit` (real flow) \| `none` |
| `--no-manage-invitation-flag` | off | don't touch `invitation_required` (you disabled it) |
| `--backoffice-user` / `--backoffice-password` | seeded admin | backoffice creds for the flag toggle |
| `--no-backdate` | on | leave `created_at` = now |
| `--dry-run` | off | compute volume only, no calls |
| `--api-url` / `--keycloak-url` / `--mailpit-url` / `--db-url` | from env | endpoint overrides |

Endpoints default from `$SEED_API_URL`, `$SEED_KEYCLOAK_URL`, `$SEED_MAILPIT_URL`,
`$SEED_DB_URL` (the Makefile sets these from your workspace `.env`).

## Output

- `.out/users.csv` — `email,password,team_identifier,…` for every seeded account, so you
  can log into any of them (all share the password `SeedData123!`).

## Performance note

With documents and photos on (the default), a full **1000-user** run performs **hundreds of
thousands** of API calls including S3 uploads — expect it to run for a long time. Start with a
small `--users` value, or add `--no-documents --no-photos`, and use `--dry-run` to size a run
before launching it.
