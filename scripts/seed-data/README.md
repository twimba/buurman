# Buurman data seeder

Generates production-like data in a **local** Buurman workspace by driving the public
REST API: it registers N landlords ("users"), each with their own team, and fills their
account with properties, contacts, contracts, payments, documents and photos.

- **Signups** follow a startup **growth curve** (exponential by default) across the window.
- **Per-user activity** follows a **normal distribution** (engagement × account tenure).
- **Payments** span each lease month-by-month with a realistic PAID / PENDING / OVERDUE mix.

## How it works

Everything is created through the API (`/auth/register`, `/properties`, `/contacts`,
`/contracts`, `/payments`, `…/documents`, `…/photos`). The API stamps `created_at = now`,
so a single **minimal direct-DB pass** ([`backdate.py`](seed_data/backdate.py)) rewrites
only the historical timestamps (`created_at`, `uploaded_at`, `joined_at`) on the rows it
created, keyed by their `identifier`. This is the *only* thing that bypasses the API; turn
it off with `--no-backdate`.

## Prerequisites

- The local stack is up (`make dev` + a running backend, or `make up`).
- `INVITATION_REQUIRED` is effectively off locally (the workspace `.env` sets it `false`);
  otherwise `/auth/register` rejects every signup.
- Email verification is on by default and reads codes from **Mailpit**. Use
  `--skip-email-verify` only if your stack doesn't gate writes on a verified email.
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
| `--skip-email-verify` | off | bypass Mailpit verification |
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
