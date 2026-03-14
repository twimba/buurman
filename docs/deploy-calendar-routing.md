# Production Deployment: Calendar Feed Routing (BUUR-69)

## Background

Calendar feed URLs changed from `api.buurman.io/calendar/ical/{token}` to `app.buurman.io/calendar/ical/{token}` to present a cleaner URL to users.

This requires a Traefik routing rule in Dokploy so that `app.buurman.io/calendar/*` requests are forwarded to the **backend** service instead of the **app** (nginx/SPA) service.

**If this routing rule is missing, calendar feed URLs will return HTML (SPA fallback) instead of iCal data, breaking all calendar subscriptions.**

## Prerequisites

- Access to Dokploy dashboard at `https://dokploy.buurman.io`
- Admin credentials for Dokploy

## Steps (Dokploy v0.28.6)

### 1. Add Calendar Domain to the Backend Service

1. Log in to `https://dokploy.buurman.io`
2. Navigate to your **Project** and click on the **backend** service
3. Go to the **Domains** tab
4. Click **"Add Domain"** and fill in:

| Field | Value |
|-------|-------|
| **Host** | `app.buurman.io` |
| **Path** | `/calendar/` |
| **Container Port** | `8081` |
| **HTTPS** | Enabled |
| **Certificate** | `letsencrypt` |
| **Strip Path** | Disabled (backend expects the full `/calendar/ical/{token}` path) |

5. Click **Save**

For Applications, Dokploy generates a Traefik file-provider config and hot-reloads immediately — no redeploy needed. For Compose services, a redeploy is required.

### 2. Verify Priority (automatic)

Traefik auto-calculates priority based on rule length. The calendar router's rule (`Host('app.buurman.io') && PathPrefix('/calendar/')`) is longer than the app router's rule (`Host('app.buurman.io')`), so it automatically gets higher priority. No manual priority configuration needed.

### 3. Verify via Traefik File System (optional)

To confirm the generated config:

1. In the Dokploy dashboard, navigate to **Traefik File System** (sidebar or Settings > Servers > Show Traefik File System)
2. Browse the `dynamic/` directory
3. Find the newly generated YAML file for the backend calendar domain
4. Verify it contains a router with rule `Host('app.buurman.io') && PathPrefix('/calendar/')` pointing to the backend service on port 8081

### 4. Deploy and Verify

1. Deploy all services via `make deploy-prod` (tags `main` as `prod`, triggers CI for backend + app + backoffice)
2. Wait for the deployment to complete

### 7. Post-Deployment Verification

Run these checks to confirm routing works:

```bash
# 1. Verify calendar endpoint returns iCal data (not HTML)
curl -sI https://app.buurman.io/calendar/ical/test-invalid-token | head -5
# Expected: HTTP 404 (from backend, not nginx SPA fallback)
# If broken: HTTP 200 with Content-Type: text/html (nginx serving index.html)

# 2. Verify with a real feed token (get one from the database or UI)
curl -s https://app.buurman.io/calendar/ical/<real-token> | head -3
# Expected: BEGIN:VCALENDAR

# 3. Verify the app still works for non-calendar paths
curl -sI https://app.buurman.io/ | head -5
# Expected: HTTP 200 with Content-Type: text/html (SPA index.html)
```

## Rollback

If something goes wrong, remove the `backend-calendar` router labels from the backend service in Dokploy. The old `api.buurman.io/calendar/ical/*` URLs still work since the backend continues to serve the endpoint on the API domain.

## Notes

- Old calendar feed URLs (`api.buurman.io/calendar/ical/...`) continue to work — the backend serves the endpoint on both domains
- New feeds created in the UI will show `app.buurman.io` URLs going forward
- Users with existing subscriptions do **not** need to re-subscribe
