# Deployment Instructions

Version-specific deployment steps that require manual configuration beyond `make deploy-prod`.

> **Deploying now?** Production is on `v0.104.0`. Follow
> [Release after v0.104.0 — full runbook](#release-after-v01040--full-runbook-buur-101-buur-105-buur-106-localized-notifications)
> at the bottom. Sections up to and including *Booklets v2* are already live.

---

## BUUR-69: Calendar Feed Routing

**Date**: 2026-03-10

### Context

Calendar feed URLs changed from `api.buurman.io/calendar/ical/{token}` to `app.buurman.io/calendar/ical/{token}`. This requires a Traefik routing rule in Dokploy so that `app.buurman.io/calendar/*` requests are forwarded to the **backend** instead of the app (nginx/SPA).

**If this routing rule is missing, calendar feed URLs will return HTML instead of iCal data, breaking all calendar subscriptions.**

### Prerequisites

- Access to Dokploy dashboard at `https://dokploy.buurman.io`

### Steps (Dokploy v0.28.6)

#### 1. Add Calendar Domain to the Backend Service

1. Log in to `https://dokploy.buurman.io`
2. Navigate to your **Project** → **backend** service → **Domains** tab
3. Click **"Add Domain"**:

| Field | Value |
|-------|-------|
| **Host** | `app.buurman.io` |
| **Path** | `/calendar/` |
| **Container Port** | `8081` |
| **HTTPS** | Enabled |
| **Certificate** | `letsencrypt` |
| **Strip Path** | Disabled |

4. Click **Save**

#### 2. Verify Priority

Traefik auto-calculates priority based on rule length. The calendar router's rule is longer than the app router's, so it automatically gets higher priority. No manual configuration needed.

#### 3. Deploy and Verify

```bash
make deploy-prod

# Verify calendar endpoint returns iCal data (not HTML)
curl -sI https://app.buurman.io/calendar/ical/test-invalid-token | head -5
# Expected: HTTP 404 (from backend, not nginx SPA fallback)

# Verify the app still works for non-calendar paths
curl -sI https://app.buurman.io/ | head -5
# Expected: HTTP 200 with Content-Type: text/html
```

### Rollback

Remove the `backend-calendar` domain entry from the backend service in Dokploy. Old `api.buurman.io/calendar/ical/*` URLs continue to work.

---

## BUUR-20: Impersonation Step-Up Authentication

**Date**: 2026-03-15

### Context

Replaced the redirect-based Keycloak re-authentication flow with an in-modal password confirmation dialog. The backend validates admin passwords via Keycloak's ROPC grant (`grant_type=password`). This requires "Direct Access Grants" enabled on the `buurman-backoffice-web` client.

### Steps: Enable Direct Access Grants in Keycloak

**Via Keycloak Admin Console:**

1. Log in to Keycloak Admin Console
2. Select the `buurman-backoffice` realm
3. Go to **Clients** → `buurman-backoffice-web`
4. Under **Capability config**, enable **Direct access grants**
5. Click **Save**

**Via realm JSON import** (if reimporting):

The updated `keycloak/buurman-backoffice-realm.json` already has `"directAccessGrantsEnabled": true`. If your import strategy is `IGNORE_EXISTING`, apply the change manually via the Admin Console instead.

### Verification

1. Go to backoffice → Users → select a user → click **Impersonate**
2. Fill in team, reason, and duration → click **Start in Read Mode** or **Start in Full Access**
3. Password confirmation dialog should appear
4. Enter the admin's correct password → session should be created
5. Test with a wrong password → should show "Invalid password"
6. Test rejoin from Impersonation Sessions → should also prompt for password

---

## BUUR-96: Backoffice Dashboard v2 + Cost Tracking + FX Rates

**Date**: 2026-06-27

### Context

Adds the mission-control backoffice dashboard, per-provider cost tracking (normalized to EUR), and dated FX rate history. New backoffice pages: **Costs** and **FX Rates**.

The live metrics panels (p95, error rate, latency heatmap) read from Prometheus; the cost panels read from provider APIs. **Every new integration is optional** — with no config the migration still applies cleanly, metrics panels render in PREVIEW state, and cost sources fall back to admin-editable manual amounts. Set the variables below to light up the live data.

### Database

Migration `V059__backoffice_dashboard_cost_fx.sql` is **additive** (new tables only: `backoffice_user_dashboard_layout`, `backoffice_action_item_snooze`, `cost_snapshot`, `cost_manual_amount`, `fx_rate`, `fx_pair`, `cost_config`; drops the `team_preferences.default_country_code` default). Applied automatically by Flyway on backend startup — no manual step.

> Note: V059 is the only **new** migration on this branch.

#### ⚠️ V056–V058 checksum repair (formatting-only change)

The bodies of the already-deployed `V056`–`V058` (rent-regulation refresh) were **reformatted** on
this branch — same SQL semantics, but the file bytes changed, so Flyway computes new checksums.
Because these versions are already recorded in production's `flyway_schema_history` with the **old**
checksums, the backend will **fail to start** on `ValidationException` (checksum mismatch) until the
stored checksums are updated to match the new files.

**Apply the checksum update to the prod DB before/at deploy time.** The values below are from a
**local** run — the checksum is derived from file content and is the same everywhere, but verify
the `installed_rank` matches production (ranks 56–58 here) before running:

```sql
-- Repair V056–V058 checksums after the formatting-only reformat (run against the prod DB)
UPDATE "public"."flyway_schema_history" SET "checksum" = 134853625  WHERE "installed_rank" = 56; -- V056
UPDATE "public"."flyway_schema_history" SET "checksum" = 203013181  WHERE "installed_rank" = 57; -- V057
UPDATE "public"."flyway_schema_history" SET "checksum" = 2019611573 WHERE "installed_rank" = 58; -- V058
```

> Confirm the `installed_rank` → version mapping first:
> `SELECT installed_rank, version, checksum FROM flyway_schema_history WHERE version IN ('056','057','058');`
> Alternatively, run `mvn -pl buurman-jooq flyway:repair` (or `flyway repair`) against prod to let
> Flyway recompute and rewrite the checksums automatically instead of the manual UPDATEs above.

### Environment Variables

Grouped by the service that consumes them (Dokploy → _service_ → Environment). All backend vars are optional — omit to leave that source disabled. Frontend `VITE_*` vars are build-time and require a rebuild of that app to take effect.

#### Backend (`backend` service)

Copy-paste and fill in the placeholders:

```env
# Live metrics panels (latency / error-rate / p95). Empty => panels show PREVIEW.
# Cluster-wide Prometheus; queries scope via the application="buurman" selector.
PROMETHEUS_URL=https://prometheus.buurman.io
# FX source (frankfurter.app / ECB data, no API key). Leave as-is unless self-hosting.
COST_FX_API_URL=https://api.frankfurter.dev
# Hetzner Cloud — infra cost. Empty => Hetzner cost source disabled.
HETZNER_BEARER_TOKEN=<hetzner-cloud-api-token>
# Cloudflare — bandwidth/usage cost. Both required together; empty => source disabled.
CLOUDFLARE_API_TOKEN=<cloudflare-api-token>
CLOUDFLARE_ACCOUNT_ID=<cloudflare-account-id>
```

**Editable cost amounts are DB-only — there is no env/yml seed.** The Mailgun plan fee / per-email
rate (`cost_config`) and flat provider amounts like Better Stack (`cost_manual_amount`) are set
exclusively on the backoffice **Costs** page; edits save instantly and write the current-month
snapshot immediately (no Refresh needed). The former `COST_MAILGUN_BASE_EUR`,
`COST_MAILGUN_PER_EMAIL_EUR`, and `COST_BETTER_STACK_EUR` env vars **have been removed** — do not
set them. Mailgun is **formula-only** (plan fee + per-email rate × live volume); it never falls back
to a manual amount and is not inline-editable. Manual amounts remain for providers that need them
(Better Stack, or as an API-down fallback).

#### Frontend — Backoffice (`backoffice` service)

Both are **new** for this branch (the geo / property world maps). The Map ID enables vector maps + AdvancedMarkers; reuse the same Google Maps key as the app:

```env
VITE_GOOGLE_MAPS_API_KEY=<google-maps-browser-api-key>
VITE_GOOGLE_MAPS_MAP_ID=<google-maps-map-id>
```

#### Frontend — App (`app` service)

`VITE_GOOGLE_MAPS_API_KEY` is already configured in production. Only the Map ID is **new** for this branch (modernized maps):

```env
VITE_GOOGLE_MAPS_MAP_ID=<google-maps-map-id>
```

Notes:
- `PROMETHEUS_URL` should point at a cluster-wide Prometheus; queries aggregate across nodes via the `application="buurman"` selector.
- EUR cost amounts live only in the DB (`cost_manual_amount` / `cost_config`) — admins set them on the **Costs** page, no redeploy needed. Manual edits save instantly and reflect on the overview without a Refresh.
- Cost snapshots run on a daily cron (`0 0 6 * * ?`); the **Costs** page also exposes a manual **Refresh**.
- The Map ID is created in Google Cloud Console → Maps → Map IDs (vector type). One Map ID can be shared by app + backoffice.

### Deploy & Verify

```bash
make deploy-prod
```

1. Backoffice → **Dashboard**: panels render (live data if `PROMETHEUS_URL` set, otherwise PREVIEW).
2. Backoffice → **Costs**: provider rows show; click **Refresh** → current-month figures populate; edit a manual amount and confirm it persists.
3. Backoffice → **FX Rates**: USD pair seeded; trigger a refresh → a dated `fx_rate` row appears.

### Rollback

V059 is additive and safe to leave in place. To fully revert, redeploy the previous backend image; the new tables are unused by older code. No data migration to undo.

---

## Booklets v2: Gotenberg PDF Renderer Sidecar (REQUIRED)

**Date**: 2026-06-29

### Context

All PDFs (booklets, summary cards, legal documents) are rendered by a headless-Chromium **Gotenberg** sidecar — full modern CSS, embedded brand fonts, flawless multilingual typography incl. Greek. The in-JVM iText engine (and its AGPL dependency) has been **removed**: Gotenberg is now the **sole renderer**, so the sidecar is a **hard runtime dependency** — the backend cannot produce PDFs without it. It runs as **one new internal-only container** in the Dokploy project (reached over the internal network only — **no public domain**).

**Deploy the `gotenberg` service before (or together with) the backend release that removes iText.** There is no in-JVM fallback.

### Prerequisites

- Access to Dokploy dashboard at `https://dokploy.buurman.io`
- The custom image `docker/gotenberg/` (bakes Noto fonts so Greek renders without tofu). Either let Dokploy build it from the repo, or build & push it to your registry. The stock `gotenberg/gotenberg:8` image also works but lacks the bundled fallback fonts.

### Steps (Dokploy v0.28.6)

#### 1. Create the `gotenberg` service

1. Log in to `https://dokploy.buurman.io` → your **Project** → **Create Service** (Application).
2. **Source**: either
   - **Docker image** — `gotenberg/gotenberg:8` (quick), or your pushed `buurman-gotenberg:8`; **or**
   - **Git / Dockerfile** — build context `docker/gotenberg/` (recommended; includes the fonts).
3. **Command** (override): `gotenberg --api-timeout=60s`
4. **Container Port**: `3000`
5. **Domains**: **none** — do NOT add a public domain. Keep it internal to the project network.
6. **Resources**: Chromium is memory-hungry; allocate ~512MB–1GB and enable restart-on-failure.
7. Deploy the service. Confirm it's healthy: from the backend container, `curl -fsS http://gotenberg:3000/health` returns `{"status":"up"...}`.

#### 2. Point the backend at it

In **backend** service → **Environment**, add:

| Variable | Value | Notes |
|----------|-------|-------|
| `GOTENBERG_URL` | `http://gotenberg:3000` | Internal service hostname (matches the service name). Required — the backend has no other PDF engine. |

#### 3. Deploy and Verify

```bash
make deploy-prod

# From the backend container, the sidecar must be reachable internally:
#   curl -fsS http://gotenberg:3000/health   → status "up"

# Generate a booklet and confirm a valid PDF (Greek exercises font fallback):
#   GET https://api.buurman.io/booklets/properties/{id}?lang=el        → %PDF, no tofu
#   GET https://api.buurman.io/properties/{id}/summary?lang=el          → %PDF (landscape card)
```

### Rollback

There is no in-JVM fallback — if PDF generation breaks, fix or restart the `gotenberg` sidecar (it is stateless), or redeploy the previous backend release (the one that still bundled iText). No data or schema changes are involved.

---

## BUUR-101: Rent Collection Maturity (reminders, late fees, deposits, plans, letters)

### Context

Adds tenant payment reminders (manual, bulk and a daily automatic ladder), late fees with
jurisdiction caps, write-offs, contact credits, payment plans, deposits with statements, and
formal-notice PDFs attached to FINAL reminders. Migrations V060–V066. Two new Maven modules
(`buurman-documents`, `buurman-letters`) are part of the fat JAR; no new containers.

### Database

Migrations are additive (new columns with defaults, new tables, index rebuilds) and safe for a
rolling deploy. V066 rebuilds `idx_payments_pending`; on a large `payments` table expect a short
write lock (seconds) while Flyway runs.

### Post-deploy (REQUIRED before enabling late fees for any team)

1. Reload the rent-regulation catalogue from the backoffice (Rent regulations → Reload). V064/V065
   add `late_fee_policy` and `formal_notice_days` to `rent_regulation_countries` with
   `UNKNOWN`/empty defaults; the reload fills them from the bundled catalogue.
2. Until the reload, the late-fee job **skips** every rent whose country row is still `UNKNOWN`
   (metric `buurman_payment_late_fee_skipped_total{reason="unknown_policy"}`, one warning per
   country in the logs) and the contract form shows no regime hint. Nothing is charged blind.

### Configuration (all optional, defaults shown)

```yaml
scheduling:
  payment-dunning:
    cron: "0 30 8 * * ?"     # UTC; one run for all teams
    enabled: true            # kill switch for tenant reminder emails
  late-fees:
    cron: "0 15 6 * * ?"     # UTC
    enabled: true            # kill switch for late-fee charging
```

All job times are UTC; "today" in due-date arithmetic is UTC as well. Acceptable for the EU-first
launch; revisit per-team timezones before opening to the Americas.

### Runbook

- **Stop all tenant emails now**: set `SCHEDULING_PAYMENT_DUNNING_ENABLED=false` and restart, or
  pause `paymentDunningJob` from the backoffice scheduler (persisted in the Quartz store).
- **Pause one team**: team settings → automatic reminders off; **one contract**: set "reminders
  paused until" on the contract.
- **Re-run a missed day**: trigger `paymentDunningJob` / `lateFeeJob` from the backoffice
  scheduler. Both are idempotent (a ladder step is sent once per payment, a fee once per rent).
- **Wrongly charged fee**: waive it on the payment (cancels the fee with a reason). Cancelling or
  writing off the parent rent cancels its open fees automatically.
- **Reminder delivery**: the reminder row records the send request; delivery itself goes through
  the notification outbox (retries with backoff, FAILED after the retry budget). Check the
  notifications table for the tenant email when a landlord reports a missing reminder.
- **Formal-notice PDFs** are filed under the contract's documents; if Gotenberg is down a FINAL
  reminder fails (manual) or is counted in `buurman_payment_reminder_failed_total` (automatic).

---

## Release after v0.104.0 — full runbook (BUUR-101, BUUR-105, BUUR-106, localized notifications)

**Baseline in production**: `v0.104.0` (`72dcc925`, 2026-07-05), schema at **V059**.
**Target**: current `main`, schema at **V082** (23 new migrations, V060–V082).

This is the single ordered checklist for the whole jump. The BUUR-101 section above stays valid
as the reference/runbook for rent collection; everything you must *do* is repeated here in order.

### What changed (deploy-relevant only)

| Area | Change | Action |
|------|--------|--------|
| Database | V060–V082. **V070 is irreversible** (drops 16 `properties` columns + `property_amenities` + `property_residential_details`). V072 and V080 `RAISE EXCEPTION` on bad data. V075 adds a unique index that fails on duplicates. | Pre-flight + rehearsal on a snapshot, maintenance window, snapshot = rollback (Phases 1, 4) |
| New container | **Documenso** (`documenso/documenso:v2.19.0`) e-signature sidecar + its own Postgres database | Phase 2 |
| Backend env | `DOCUMENSO_BASE_URL`, `DOCUMENSO_PUBLIC_URL`, `DOCUMENSO_API_KEY`, `DOCUMENSO_WEBHOOK_SECRET` — **defaults in `application.yml` are public dev values and point at `documenso.local.buurman.io`; there is no `application-prod.yml` override** | Phase 3 (mandatory even if e-signature stays off) |
| Backend env (optional) | `SCHEDULING_PAYMENT_DUNNING_ENABLED`, `SCHEDULING_LATE_FEES_ENABLED` kill switches (default `true`) | Phase 3 |
| New Quartz jobs | `paymentDunningJob` (08:30 UTC), `lateFeeJob` (06:15 UTC), `contractTerminationSweepJob` (01:30 UTC) | Decide before deploy whether dunning/late fees go live on day one (Phase 3) |
| Feature flags | `multi_unit` → **ON by default** (V071 + V076). `esignature_enabled` → **OFF by default** (V077) | Phase 6 |
| Reference data | `late_fee_policy`, `formal_notice_days` (V064/V065) default to `UNKNOWN`/empty; `rent_regulation_tenancy_rules` (V068) is empty until reload | Phase 6 |
| Gotenberg | Unchanged image, already deployed. Now also renders letters, formal notices, deposit statements, lease agreements | Verify only |
| Fat JAR | New modules `buurman-documents`, `buurman-letters` (already in `backend/Dockerfile` and CI) | None |
| Runtime | Virtual threads enabled (`spring.threads.virtual.enabled`) | None |
| Frontend | No new `VITE_*` build variables | None |
| Keycloak realm JSON | Unchanged | None |
| Infra images | `keycloak` 26.6.4 → 26.7.4-0, `grafana` 13.1.0 → 13.2.3, `prometheus` v3.13.0 → v3.15.0 | Optional, Phase 7 |
| Flyway checksums | No already-applied migration (≤ V059) was modified | None |

### Phase 0 — Prerequisites

- [ ] Dokploy access (`https://dokploy.buurman.io`), prod Postgres admin credentials, GHCR access.
- [ ] `psql` client locally (or Docker: `docker run --rm -i --network host postgres:18-alpine psql`).
- [ ] A DNS name for Documenso's public UI. This doc uses **`sign.buurman.io`** — substitute your choice everywhere. Create the A/CNAME record now so Let's Encrypt can issue in Phase 2.
- [ ] SMTP credentials Documenso can send with (e.g. Mailgun SMTP) and a from-address (e.g. `signing@buurman.io`).
- [ ] `main` is green (backend + frontend test workflows).
- [ ] Announce a **maintenance window**. V070 is not expand/contract: the old backend cannot serve once it commits. Downtime ≈ CI build time + migration time (measured in Phase 1).

### Phase 1 — Rehearse the migrations on a copy of production (days before)

Nothing here touches production. Do not skip: V070/V072 have only ever run on empty databases.

1. Dump production and restore it somewhere private:

   ```bash
   pg_dump "$PROD_URL" -Fc -f prod-rehearsal.dump
   createdb -h <private-host> -U <admin> buurman_rehearsal
   pg_restore --no-owner -d "$SNAPSHOT_URL" prod-rehearsal.dump
   ```

2. Read-only pre-flight for V070–V072. **GO only if every `abort_*` count is 0.**

   ```bash
   psql "$SNAPSHOT_URL" -v ON_ERROR_STOP=1 -f scripts/preflight/buur-106-preflight.sql
   ```

   Most likely failure is **A4** (a property with more than one ACTIVE contract). If non-zero,
   **stop** and resolve per `scripts/preflight/README.md` (options a/b/c — the recommended
   data-repair migration (c) is not written yet).

3. Extra read-only checks for the migrations the pre-flight does not cover. All three must return **0 rows**:

   ```sql
   -- V075: unique (provider_message_id, channel)
   SELECT channel, provider_message_id, count(*) FROM notifications
   WHERE provider_message_id IS NOT NULL GROUP BY 1, 2 HAVING count(*) > 1;

   -- V072/V080: one in-force contract per property today (= per implicit unit after V070)
   SELECT property_id, count(*) FROM contracts
   WHERE status IN ('ACTIVE', 'NOTICE_GIVEN') AND deleted_at IS NULL
   GROUP BY 1 HAVING count(*) > 1;

   -- Flyway baseline is what this runbook assumes
   SELECT version FROM flyway_schema_history WHERE success = false;
   ```

   Also confirm `SELECT max(version::int) FROM flyway_schema_history;` returns `59`.

4. Transactional rehearsal of V070–V072 (rolls back, snapshot unchanged):

   ```bash
   ./scripts/preflight/buur-106-rehearsal.sh "$SNAPSHOT_URL"
   # without local psql:
   # PSQL_CMD="docker run --rm -i --network host postgres:18-alpine psql" ./scripts/preflight/buur-106-rehearsal.sh "$SNAPSHOT_URL"
   ```

   Must end with the verification block passing. Any "Do NOT deploy" → stop.

5. Full rehearsal of **all** of V060–V082 (the script above only covers V070–V072). Point a local
   backend at the snapshot and let Flyway run, timing it — this sizes the window:

   ```bash
   cd backend && mvn clean install -DskipTests
   DB_URL="jdbc:postgresql://<private-host>:5432/buurman_rehearsal" \
   DB_USERNAME=<user> DB_PASSWORD=<pass> \
     mvn spring-boot:run -pl buurman-app -am
   ```

   Watch the log until `Successfully applied 23 migrations` (now at V082). It is fine if the app
   then fails on unrelated local config — only the Flyway result matters. This run **does** commit
   to the snapshot; re-restore it if you want to rehearse again.

6. Record: abort counts (all 0), rehearsal result, total migration duration.

### Phase 2 — Stand up Documenso (any time before the window; no user impact)

The backend starts fine without Documenso and `esignature_enabled` is OFF, so this phase can be
done ahead of time. It must be finished before you turn the flag on in Phase 6.

#### 2.1 Database

`docker/postgres/init-db.sh` only runs on a brand-new volume, so create the role and database by
hand on the production Postgres (as admin):

```sql
CREATE USER documenso WITH PASSWORD '<strong-password>';
CREATE DATABASE documenso WITH OWNER documenso;
GRANT ALL PRIVILEGES ON DATABASE documenso TO documenso;
```

Documenso creates its own schema on first start (`prisma migrate deploy`). **Do not run
`docker/documenso/seed.sql` in production** — it creates a public, committed login
(`buurmy@buurman.io` / `buurmy`), API token and webhook secret.

#### 2.2 Signing certificate

Documenso refuses to sign without a PKCS#12 certificate. Generate a production one (do **not**
reuse the dev `docker/documenso/cert.p12`, passphrase `buurman-dev`):

```bash
openssl genrsa -out private.key 2048
openssl req -new -x509 -key private.key -out cert.crt -days 1095 \
  -subj "/C=NL/O=Buurman/CN=Buurman Signing"
openssl pkcs12 -export -out cert.p12 -inkey private.key -in cert.crt \
  -passout pass:'<strong-passphrase>'
rm private.key cert.crt
```

Store `cert.p12` and its passphrase in the password manager. Note the expiry (3 years here) in the
calendar. For legally stronger signatures, use a certificate from a trusted CA instead.

#### 2.3 Secrets

```bash
openssl rand -hex 32   # DOCUMENSO_NEXTAUTH_SECRET
openssl rand -hex 16   # NEXT_PRIVATE_ENCRYPTION_KEY            (>= 32 chars)
openssl rand -hex 16   # NEXT_PRIVATE_ENCRYPTION_SECONDARY_KEY  (>= 32 chars)
openssl rand -hex 32   # DOCUMENSO_WEBHOOK_SECRET (shared with the backend)
```

The two encryption keys must never change after first start (they encrypt stored data).

#### 2.4 Create the Dokploy service

1. Dokploy → **Project** → **Create Service** → Application, name `documenso`.
2. **Source**: Docker image `documenso/documenso:v2.19.0` (same pin as `docker-compose.yml`).
3. **Container port**: `3000`.
4. **Mount** the certificate: Advanced → Volumes/Mounts → File mount, content = `cert.p12`,
   mount path `/opt/documenso/cert.p12` (read-only). If your Dokploy version cannot mount binary
   files, base64 the file and use `NEXT_PRIVATE_SIGNING_LOCAL_FILE_CONTENTS` instead of
   `NEXT_PRIVATE_SIGNING_LOCAL_FILE_PATH` (check the Documenso self-hosting docs for v2.19).
5. **Environment** (names mirror `docker-compose.yml`; `<postgres-host>` is the internal Postgres hostname the backend's `DB_URL` already uses):

   ```env
   PORT=3000
   NEXTAUTH_SECRET=<DOCUMENSO_NEXTAUTH_SECRET>
   NEXT_PRIVATE_ENCRYPTION_KEY=<key-1>
   NEXT_PRIVATE_ENCRYPTION_SECONDARY_KEY=<key-2>
   NEXT_PUBLIC_WEBAPP_URL=https://sign.buurman.io
   # Documenso's job runner POSTs to itself; must be reachable from inside the container.
   # Without this, signing-request and reminder emails are silently never sent.
   NEXT_PRIVATE_INTERNAL_WEBAPP_URL=http://localhost:3000
   NEXT_PRIVATE_DATABASE_URL=postgres://documenso:<password>@<postgres-host>:5432/documenso
   NEXT_PRIVATE_DIRECT_DATABASE_URL=postgres://documenso:<password>@<postgres-host>:5432/documenso
   NEXT_PRIVATE_SMTP_TRANSPORT=smtp-auth
   NEXT_PRIVATE_SMTP_HOST=<smtp-host>
   NEXT_PRIVATE_SMTP_PORT=587
   NEXT_PRIVATE_SMTP_USERNAME=<smtp-user>
   NEXT_PRIVATE_SMTP_PASSWORD=<smtp-password>
   NEXT_PRIVATE_SMTP_FROM_NAME=Buurman Signing
   NEXT_PRIVATE_SMTP_FROM_ADDRESS=signing@buurman.io
   NEXT_PRIVATE_SIGNING_TRANSPORT=local
   NEXT_PRIVATE_SIGNING_LOCAL_FILE_PATH=/opt/documenso/cert.p12
   NEXT_PRIVATE_SIGNING_PASSPHRASE=<strong-passphrase>
   ```

   Only add `NEXT_PRIVATE_WEBHOOK_SSRF_BYPASS_HOSTS=<backend-internal-hostname>` if you choose the
   internal webhook URL in 2.6 (Documenso rejects webhook targets that resolve to private IPs).
6. **Domain**: Host `sign.buurman.io`, container port `3000`, HTTPS on, certificate `letsencrypt`.
   Unlike Gotenberg, Documenso **needs** a public domain: signers open signing links in a browser.
7. **Resources**: ~1 GB memory, restart on failure.
8. Deploy. Verify:

   ```bash
   curl -fsS https://sign.buurman.io/api/health        # 200
   ```

   and in the service logs: Prisma migrations applied, no certificate error.

#### 2.5 Create the Documenso account, team and API token

1. Open `https://sign.buurman.io` → sign up the service account (e.g. `signing@buurman.io`),
   confirm via the email it sends (this also proves SMTP works).
2. Create/select the team Buurman will send from.
3. **Team settings → API Tokens → Create token** (no expiry). Copy it → this is `DOCUMENSO_API_KEY`. It is shown once.
4. **Branding** (optional, dev seed does this automatically): team/organisation settings → upload
   `frontend/app/public/assets/logo/logo_square_no_text.png`, primary colour `#0284c7`.
5. Lock the instance down: set `NEXT_PUBLIC_DISABLE_SIGNUP=true` on the `documenso` service and redeploy.

#### 2.6 Register the webhook

Team settings → **Webhooks → Create webhook**:

| Field | Value |
|-------|-------|
| URL | `https://api.buurman.io/webhooks/documenso/events` |
| Secret | `<DOCUMENSO_WEBHOOK_SECRET>` |
| Events | `DOCUMENT_OPENED`, `DOCUMENT_SIGNED`, `DOCUMENT_COMPLETED`, `DOCUMENT_REJECTED`, `DOCUMENT_CANCELLED` |
| Enabled | yes |

The backend compares the `X-Documenso-Secret` header to `DOCUMENSO_WEBHOOK_SECRET` (constant-time
string compare, not HMAC) and **fails closed** on mismatch. `/webhooks/**` is already `permitAll`
and `api.buurman.io` already routes to the backend — no new Traefik rule.

Alternative (traffic stays internal): URL `http://<backend-internal-hostname>:8081/webhooks/documenso/events`
plus `NEXT_PRIVATE_WEBHOOK_SSRF_BYPASS_HOSTS=<backend-internal-hostname>` on the `documenso` service.

Without the webhook, Buurman's "Pending N/M signed" never updates and the signed PDF is never filed.

### Phase 3 — Backend environment (set before the window; takes effect on the deploy)

Dokploy → **backend** service → **Environment**. Saving does not restart the container; the values
are picked up by the Phase 4 deploy.

```env
# REQUIRED. application.yml falls back to public dev values otherwise.
DOCUMENSO_BASE_URL=http://<documenso-internal-hostname>:3000
DOCUMENSO_PUBLIC_URL=https://sign.buurman.io
DOCUMENSO_API_KEY=<token from 2.5>
DOCUMENSO_WEBHOOK_SECRET=<secret from 2.3 / 2.6>
```

- `DOCUMENSO_BASE_URL` is the server-to-server API origin (internal service hostname as shown in
  Dokploy for the `documenso` service; `https://sign.buurman.io` also works).
- `DOCUMENSO_PUBLIC_URL` is what goes into signing links; it must equal Documenso's `NEXT_PUBLIC_WEBAPP_URL`.
- **Set `DOCUMENSO_WEBHOOK_SECRET` even if you postpone Phase 2.** Its default is a secret committed
  to git; with the default in place anyone could forge a "signed" webhook.

Optional — decide now whether tenant dunning emails and late-fee charging start on the first day:

```env
# Both default to true. Set false to deploy dark, enable later (restart required).
SCHEDULING_PAYMENT_DUNNING_ENABLED=false
SCHEDULING_LATE_FEES_ENABLED=false
```

Recommended: deploy with both `false`, do Phase 6 (regulation reload), then flip to `true`.
Late fees are skipped anyway for countries whose policy is still `UNKNOWN`, but dunning emails to
tenants are not gated by the reload.

Check that these existing variables are still present (unchanged, but everything depends on them):
`SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GOTENBERG_URL`.

### Phase 4 — Maintenance window: deploy

1. **Confirm Gotenberg is healthy** (letters and formal notices now depend on it):
   from the backend container `curl -fsS http://gotenberg:3000/health`.
2. **Record the current image tags** of backend, app and backoffice (Dokploy → service → General →
   Docker image; format `ghcr.io/<owner>/buurman/<service>:0.104.0-<sha7>`). CI overwrites this
   field on deploy and these are your rollback targets. Then **stop the backend** in Dokploy
   (backend service → Stop). This stops all traffic and all Quartz
   jobs, so no scheduled job holds a lock the `ALTER TABLE`s queue behind. App and backoffice may
   stay up (they will show API errors) or be stopped too.
3. **Snapshot production, stop-the-world. This is the only rollback for V070.**

   ```bash
   pg_dump "$PROD_URL" -Fc -f prod-pre-v082-$(date +%Y%m%d-%H%M).dump
   pg_restore --list prod-pre-v082-*.dump | head     # sanity: the dump is readable
   ```

   Keep it off the database host.
4. Re-run the Phase 1 step 2 and step 3 queries against **production** (read-only). All abort
   counts / row counts must still be 0. If not: start the old backend again, abort the window.
5. Deploy:

   ```bash
   make deploy-prod
   ```

   This force-moves the `prod` tag to `origin/main`, which triggers **Build & PROD Deploy**
   (`.github/workflows/build-images.yml`): version bump commit + tag, build backend/app/backoffice
   images to GHCR, then Dokploy `application.update` + `application.deploy` for all three.
6. Watch the workflow (`gh run watch` or GitHub → Actions). All of `Bump Version`, the three
   builds and `Deploy to Dokploy` must be green.
7. Watch the **backend** logs in Dokploy during startup:
   - `Successfully applied 23 migrations to schema "public", now at version v082`
   - no `RAISE EXCEPTION` text (`... already carry more than one ...`, `... unit of a different property ...`)
   - application started on port 8081.

   Expect short locks: V066 rebuilds `idx_payments_pending`, V070 rewrites `contracts`,
   `property_occupancy_periods`, `wws_calculations`; V082 inserts ~15k lines of clause seed data.

**If Flyway fails**: the failing migration's transaction rolls back, earlier ones stay applied and
the container crash-loops. Do not hand-edit `flyway_schema_history`. Either fix the data cause and
redeploy, or roll back (below).

### Phase 5 — Smoke test (still inside the window)

```bash
curl -fsS https://api.buurman.io/actuator/health          # UP
curl -sI  https://app.buurman.io/ | head -1               # 200
curl -sI  https://backoffice.buurman.io/ | head -1        # 200
curl -sI  https://app.buurman.io/calendar/ical/test-invalid-token | head -1   # 404 from backend
```

```sql
SELECT max(version::int) FROM flyway_schema_history WHERE success;            -- 82
SELECT count(*) FROM properties;                                             -- N
SELECT count(*) FROM units WHERE is_implicit;                                -- N (one per property, soft-deleted included)
SELECT key, default_enabled FROM feature_flags WHERE key IN ('multi_unit','esignature_enabled');
-- multi_unit = true, esignature_enabled = false
```

In the app, as a real team:

1. Properties list and a property detail load; area, rooms, amenities are intact (now read from the implicit unit).
2. A contract opens; its timeline renders; payments list loads.
3. Generate a property booklet PDF and a rent-change/extension letter → valid PDFs (Gotenberg).
4. Backoffice → System/health: database, S3, Keycloak, mail, Documenso all green (Documenso only if Phase 2 is done).
5. Backoffice → Scheduler: `paymentDunningJob`, `lateFeeJob`, `contractTerminationSweepJob` are listed.

End the maintenance window here if all green.

### Phase 6 — Post-deploy configuration

1. **Reload the rent-regulation catalogue** (REQUIRED): Backoffice → Rent regulations → **Reload**.
   Fills `late_fee_policy` + `formal_notice_days` (V064/V065) and `rent_regulation_tenancy_rules`
   (V068). Until then: late fees are skipped (`buurman_payment_late_fee_skipped_total{reason="unknown_policy"}`),
   the contract form shows no regime hint, and the tenancy-rules reference is empty.

   ```sql
   SELECT count(*) FROM rent_regulation_countries WHERE late_fee_policy = 'UNKNOWN';   -- expect 0 (or only unsupported countries)
   SELECT count(*) FROM rent_regulation_tenancy_rules;                                -- > 0
   SELECT count(*) FROM rent_regulation_termination_rules;                            -- > 0 (seeded by V079)
   SELECT count(*) FROM lease_clause_templates;                                       -- > 0 (seeded by V082)
   ```

2. **Enable dunning / late fees** if you deployed them dark: set
   `SCHEDULING_PAYMENT_DUNNING_ENABLED=true` and `SCHEDULING_LATE_FEES_ENABLED=true` (or remove
   both), redeploy the backend. Alternative without redeploy: leave env `true` from the start and
   pause/resume `paymentDunningJob` / `lateFeeJob` in Backoffice → Scheduler (persisted in Quartz).
3. **Turn on e-signature** (after Phase 2 + 3): Backoffice → Feature flags → `esignature_enabled`.
   Start with a per-team override for an internal team, then:
   1. generate a letter/addendum on a contract → **Send for signature** to an address you control;
   2. the Documenso email arrives, the link opens on `https://sign.buurman.io` (not an internal host);
   3. sign → within seconds the contract shows the signer as signed (webhook works);
   4. after the last signer, the signed PDF + signing certificate appear under the contract's documents, grouped under the original;
   5. Documenso → Webhooks → recent deliveries show `200`.

   Then enable the flag by default.
4. **`multi_unit` is ON for every team** after V076. If you want it pilot-only, set the default off
   in Backoffice → Feature flags and add per-team overrides. Known caveats are tracked in the
   BUUR-106 follow-up register under `docs/superpowers/`.
5. **Termination notice periods** seeded by V079 (NL/DE/FR) are example figures from the ticket,
   not verified legal data — review them before promoting the termination workflow to customers.

### Phase 7 — Optional: infrastructure image bumps

Independent of the release, each via its own manual workflow (GitHub → Actions → Run workflow,
"Deploy after build" checked). Skip any that were already run since July.

| Workflow | Bump | Notes |
|----------|------|-------|
| Build & Deploy Keycloak | 26.6.4 → 26.7.4-0 | Back up the Keycloak database first; Keycloak migrates its schema on start. Verify login to app and backoffice afterwards, and the BUUR-20 impersonation password prompt. |
| Build & Deploy Grafana | 13.1.0 → 13.2.3 | Verify dashboards load. |
| Build & Deploy Prometheus | v3.13.0 → v3.15.0 | Verify targets are up and backoffice dashboard panels are live. |

Do these outside the release window so a failure is attributable.

### Rollback

- **Failure before V070 committed** (V060–V069 are additive): redeploy the previous images
  (Dokploy → each of backend/app/backoffice → Docker image → the tag recorded in Phase 4 step 2 → Deploy). The old code ignores the
  extra tables/columns. Restoring the snapshot is still the cleanest state.
- **After V070 committed**: there is no down-migration. Restore the Phase 4 snapshot, then redeploy the previous images:

  ```bash
  # backend stopped
  psql "$PROD_ADMIN_URL" -c 'DROP DATABASE buurman;' -c 'CREATE DATABASE buurman WITH OWNER buurman;'
  pg_restore --no-owner --role=buurman -d "$PROD_URL" prod-pre-v082-<timestamp>.dump
  ```

  Then set the three services back to the image tags recorded in Phase 4 step 2 and deploy. Anything
  written after the snapshot is lost — which is why the backend is stopped before the snapshot
  and the smoke test happens inside the window.
- **Documenso**: independent. Turn `esignature_enabled` off; the sidecar and its database can stay.
- **Dunning / late fees**: kill switches and per-job pause as in the BUUR-101 runbook above.

### Final checklist

- [ ] Phase 1: pre-flight all `abort_*` = 0, extra checks 0 rows, rehearsal passed, full V060–V082 run timed
- [ ] Phase 2: Documenso up on `sign.buurman.io`, account + team + API token + webhook, signup disabled
- [ ] Phase 3: four `DOCUMENSO_*` vars on backend; dunning/late-fee switches decided
- [ ] Phase 4: backend stopped → snapshot taken and verified → `make deploy-prod` → workflow green → Flyway at v082
- [ ] Phase 5: smoke test green, window closed
- [ ] Phase 6: rent regulations reloaded; dunning/late fees enabled; e-signature verified end-to-end and flag on; `multi_unit` rollout decided
- [ ] Phase 7: Keycloak / Grafana / Prometheus bumped (optional)
- [ ] Snapshot retained for at least 30 days
