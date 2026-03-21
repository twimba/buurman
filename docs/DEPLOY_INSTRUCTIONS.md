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
