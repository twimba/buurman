# Deployment Instructions

Version-specific deployment steps that require manual configuration beyond `make deploy-prod`.

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

> Note: V056–V058 are unchanged from production (rent-regulation refresh, already deployed). Only V059 is new on this branch.

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

**Editable cost amounts are managed in the DB, not env.** The Mailgun plan fee / per-email rate
(`cost_config`) and flat provider amounts like BetterStack (`cost_manual_amount`) are set on the
backoffice **Costs** page and take precedence over any config. The matching env vars
(`COST_MAILGUN_BASE_EUR`, `COST_MAILGUN_PER_EMAIL_EUR`, `COST_BETTER_STACK_EUR`) exist only as an
optional cold-start seed (default `0` = unset) and should normally be **left unset** in production —
set the real numbers in-app instead.

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
- Manual/seed EUR costs are starting values only — admins override them on the **Costs** page (`cost_manual_amount` / `cost_config` tables), no redeploy needed.
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
