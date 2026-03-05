# Production Deployment: Calendar Feed Routing (BUUR-69)

## Background

Calendar feed URLs changed from `api.buurman.io/calendar/ical/{token}` to `app.buurman.io/calendar/ical/{token}` to present a cleaner URL to users.

This requires a Traefik routing rule in Dokploy so that `app.buurman.io/calendar/*` requests are forwarded to the **backend** service instead of the **app** (nginx/SPA) service.

**If this routing rule is missing, calendar feed URLs will return HTML (SPA fallback) instead of iCal data, breaking all calendar subscriptions.**

## Prerequisites

- Access to Dokploy dashboard at `https://dokploy.buurman.io`
- Admin credentials for Dokploy

## Steps

### 1. Open Dokploy Traefik Configuration

1. Log in to `https://dokploy.buurman.io`
2. Navigate to **Traefik** settings (typically under Server > Traefik or the Traefik tab)

### 2. Identify Existing Routers

Verify the current routing setup. You should see at least:

| Router | Rule | Service |
|--------|------|---------|
| backend | `Host('api.buurman.io')` | backend (port 8081) |
| app | `Host('app.buurman.io')` | app (port 80) |

Note the **exact service name** used for the backend — you will reference it in the next step.

### 3. Add Calendar Router to Backend Service

In the **backend service** configuration in Dokploy, add a new Traefik router with these labels:

```
traefik.http.routers.backend-calendar.rule=Host(`app.buurman.io`) && PathPrefix(`/calendar`)
traefik.http.routers.backend-calendar.entrypoints=websecure
traefik.http.routers.backend-calendar.tls=true
traefik.http.routers.backend-calendar.tls.certresolver=letsencrypt
traefik.http.routers.backend-calendar.priority=200
traefik.http.routers.backend-calendar.service=<backend-service-name>
```

Replace `<backend-service-name>` with the actual Dokploy service name for the backend (check existing backend router labels for the correct value).

**Key points:**
- `priority=200` must be **higher** than the app router's priority so `/calendar` paths are matched first
- The `certresolver` should match the existing TLS configuration (typically `letsencrypt`)
- The `service` must point to the backend, not the app

### 4. Verify the App Router Priority

Ensure the **app** router has a **lower** priority than 200. If the app router does not have an explicit priority, Traefik auto-calculates it based on rule length. Since `Host('app.buurman.io')` (shorter) has lower auto-priority than `Host('app.buurman.io') && PathPrefix('/calendar')` (longer), this should work by default. However, if the app router has an explicit priority >= 200, lower it.

### 5. Deploy and Verify

1. Save the Traefik configuration changes in Dokploy
2. Deploy the backend with the new code (`make deploy-prod`)
3. Wait for the deployment to complete

### 6. Post-Deployment Verification

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
