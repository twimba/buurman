# Feature Flags: Flagsmith Integration Plan

## Context

Buurman needs a feature flag system to support subscription-based feature gating, gradual rollouts, circuit breakers, and contextual evaluation (per team, user, plan, date). The system must work across backend (Java/Spring Boot) and frontend (React), be manageable from the backoffice, and integrate with external systems like Prometheus/Grafana.

**Decision: Flagsmith (self-hosted, open-source)**

Chosen over Unleash (AGPL license concern for SaaS), GrowthBook (requires MongoDB), Flipt (no Java SDK, no scheduled flags), LaunchDarkly (too expensive, no self-hosting), and custom-built (too much effort for a one-man-shop).

Key reasons: BSD-3 license (SaaS-safe), PostgreSQL backend (already in use), real-time SSE in free tier, trait-based identity model for subscription plans, full REST API for backoffice integration, Docker self-hosting.

---

## Phase 1: Infrastructure (Docker Compose)

### 1.1 Add Flagsmith database to PostgreSQL init script

**File:** `docker/postgres/init-db.sh`

Add a `flagsmith` database and user (same pattern as the existing `keycloak` database).

### 1.2 Add Flagsmith services to Docker Compose

**File:** `docker-compose.yml`

Add two services:
- **flagsmith** — Image `flagsmith/flagsmith:latest`, port 8000, Traefik route `flagsmith.local.buurman.io`, depends on `postgres`
  - Key env: `DATABASE_URL`, `DJANGO_SECRET_KEY`, `TASK_RUN_METHOD=TASK_PROCESSOR`, `PROMETHEUS_ENABLED=true`, `ALLOW_ADMIN_INITIATION_VIA_CLI=true`
  - No Redis needed for dev (`USE_POSTGRES_FOR_ANALYTICS=true`)
- **flagsmith-processor** — Same image, command `run-task-processor`, internal only (no Traefik route)

Wildcard TLS cert `*.local.buurman.io` already covers `flagsmith.local.buurman.io`.

### 1.3 Environment variables

**File:** `.env.example`

Add:
```
DB_FLAGSMITH_USERNAME=flagsmith
DB_FLAGSMITH_PASSWORD=flagsmith
FLAGSMITH_SECRET_KEY=change-me-in-production
FLAGSMITH_SERVER_SIDE_KEY=    # Set after Flagsmith initial setup
```

### 1.4 Makefile

Flagsmith is infrastructure — it starts with `make dev` (alongside postgres, keycloak, etc.). No changes needed if the current `make dev` already starts all non-app services; just confirm Flagsmith is not excluded.

### 1.5 Automated first-run setup

**File:** `docker/flagsmith/setup.sh`

A one-shot Docker Compose service (`flagsmith-setup`) that bootstraps Flagsmith automatically. No extra Makefile targets, no manual steps — it runs as part of `docker compose up`.

**Docker Compose service** (in `docker-compose.yml`):
- Image: `alpine/curl` (lightweight, just needs curl + sh)
- `depends_on: flagsmith: condition: service_healthy`
- Bind mounts: `./docker/flagsmith/setup.sh:/setup.sh:ro` and `./.env:/.env`
- `restart: "no"` — runs once and exits
- Receives `FLAGSMITH_ADMIN_EMAIL` and `FLAGSMITH_ADMIN_PASSWORD` from `.env`

**What the script does (idempotent — safe to run on every `make dev`):**
1. Attempts login to detect if already initialized — if yes, exits early
2. Creates admin account via `POST /api/v1/users/config/init/` (enabled by `ALLOW_ADMIN_INITIATION_VIA_CLI=true`)
3. Logs in via `POST /api/v1/auth/login/` to get auth token
4. Creates project "Buurman" via `POST /api/v1/projects/`
5. Fetches auto-created environments (Development, Production) via `GET /api/v1/environments/`
6. Extracts the server-side environment key and writes `FLAGSMITH_SERVER_SIDE_KEY` into the bind-mounted `.env`

**New env vars in `.env.example`:**
```
FLAGSMITH_ADMIN_EMAIL=buurmy@buurman.io
FLAGSMITH_ADMIN_PASSWORD=changeme
```

Same pattern as existing Keycloak/Postgres credential defaults. No Makefile changes needed — `make dev` stays as-is.

---

## Phase 2: Backend Integration

### 2.1 Maven dependency

**File:** `backend/pom.xml`

Add `com.flagsmith:flagsmith-java-client` (latest stable, currently ~7.x). Direct SDK, not OpenFeature — simpler, and the `FeatureFlagService` wrapper provides sufficient abstraction for a future swap.

### 2.2 Configuration

**File:** `backend/src/main/java/com/buurman/config/FlagsmithProperties.java`

New `record` with `@ConfigurationProperties(prefix = "flagsmith")`: `apiKey`, `apiUrl`, `enableAnalytics`, `environmentRefreshIntervalSeconds`.

**File:** `backend/src/main/resources/application.yml`

```yaml
flagsmith:
  api-key: ${FLAGSMITH_SERVER_SIDE_KEY:}
  api-url: ${FLAGSMITH_API_URL:http://flagsmith:8000/api/v1/}
  enable-analytics: true
  environment-refresh-interval-seconds: 60
```

**File:** `application-local.yml` — Override `flagsmith.api-url: https://flagsmith.local.buurman.io/api/v1/` (through Traefik, for host-local dev).

**File:** `application-docker.yml` — Override `flagsmith.api-url: http://flagsmith:8000/api/v1/` (internal Docker network).

### 2.3 FlagsmithConfig bean

**File:** `backend/src/main/java/com/buurman/config/FlagsmithConfig.java`

Follow `S3Config.java` pattern: inject `FlagsmithProperties`, create `FlagsmithClient` bean via builder.

### 2.4 FeatureFlagService

**File:** `backend/src/main/java/com/buurman/service/FeatureFlagService.java`

Central abstraction wrapping `FlagsmithClient`. Key methods:
- `boolean isEnabled(String flagKey)` — Global evaluation (no identity)
- `boolean isEnabled(String flagKey, UserPrincipal principal)` — Identity-aware with traits
- `<T> T getValue(String flagKey, Class<T> type)` — Remote config value
- `<T> T getValue(String flagKey, UserPrincipal principal, Class<T> type)` — Identity-aware config
- `Map<String, Object> getAllFlags(UserPrincipal principal)` — All evaluated flags (for frontend endpoint)

Identity string: `team:{teamId}:user:{userId}` — enables both per-team and per-user targeting.

Traits passed on evaluation:
```
team_id, user_role, is_owner, plan_type (when subscription system exists)
```

**Fallback**: If Flagsmith is unreachable, `isEnabled` returns `false`, `getValue` returns `null`. Log warnings. Features are OFF when the flag system is down (safe default).

### 2.5 Feature flag constants

**File:** `backend/src/main/java/com/buurman/util/FeatureFlags.java`

String constants for all flag keys (e.g., `ADVANCED_REPORTS = "advanced_reports"`). Prevents typos and enables IDE navigation.

### 2.6 REST endpoint for frontend

**File:** `backend/src/main/java/com/buurman/controller/FeatureFlagController.java`

`GET /feature-flags` (authenticated) — Returns all evaluated flags for the current user. Uses `@AuthenticationPrincipal UserPrincipal`. Response cached via HTTP headers (60s).

**Why backend-proxied instead of frontend calling Flagsmith directly:**
- Backend already has full user context (`UserPrincipal` with teamId, role, isOwner)
- Avoids exposing Flagsmith client-side key to browsers
- Single source of truth for identity/traits
- Simpler frontend (just fetch JSON from own API)

### 2.7 Feature gating pattern

**Method-call based** (not annotation/aspect). Annotations are too rigid — real flags need conditional logic within methods. Usage:

```java
if (featureFlagService.isEnabled(FeatureFlags.ADVANCED_REPORTS, principal)) {
    // new behavior
}
```

---

## Phase 3: Frontend Integration (Main App)

### 3.1 API module

**File:** `app/src/api/featureFlags.ts`

Simple `getFeatureFlags()` function calling `GET /feature-flags`.

### 3.2 FeatureFlagContext

**File:** `app/src/context/FeatureFlagContext.tsx`

Follow `TeamContext.tsx` pattern:
- `FeatureFlagProvider` wraps children
- Uses `useQuery` with `queryKey: ['feature-flags']`, `staleTime: 60_000`
- Enabled only when `isAuthenticated`
- Exports `useFeatureFlags()` hook with: `flags`, `isEnabled(key)`, `getValue(key)`, `isLoading`
- `isEnabled` returns `false` for unknown flags (safe default)

### 3.3 FeatureGate component

**File:** `app/src/components/FeatureGate.tsx`

Declarative gating: `<FeatureGate flag="x" fallback={<UpgradePage />}>...</FeatureGate>`

### 3.4 Wire into App.tsx

**File:** `app/src/App.tsx`

Add `FeatureFlagProvider` inside `TeamProvider`, outside `ThemeProvider`:
```
<AuthProvider> → <TeamProvider> → <FeatureFlagProvider> → <ThemeProvider> → ...
```

### 3.5 Cache invalidation on team switch

In `TeamContext.tsx`, the `switchTeamMutation.onSuccess` already does `window.location.reload()` — this naturally re-fetches feature flags. No extra invalidation needed.

### 3.6 Frontend constants

**File:** `app/src/constants/featureFlags.ts`

Mirror of backend constants for type-safe flag references.

---

## Phase 4: Backoffice Integration

### 4.1 Embed Flagsmith dashboard

**File:** `backoffice/src/pages/ToolEmbedPage.tsx`

Add to `getToolsConfig()`:
```typescript
flagsmith: { name: "Flagsmith", subdomain: "flags" },
```

This gives immediate access to the full Flagsmith admin UI via iframe (same pattern as Grafana, Keycloak, etc.).

### 4.2 Traefik CSP headers

**File:** `docker-compose.yml` (Flagsmith service labels)

Add `Content-Security-Policy: frame-ancestors https://backoffice.local.buurman.io` so the iframe works.

### 4.3 Navigation entry

**File:** `backoffice/src/components/Layout.tsx` (or equivalent sidebar/nav component)

Add "Feature Flags" nav item pointing to `/tools/flagsmith`.

---

## Phase 5: Subscription Plan Integration (future, when billing exists)

This phase documents how feature flags will tie into subscription plans. No code changes now — just the design.

### Trait-based targeting

When a team has a `plan_type` (stored in `teams` table), `FeatureFlagService` passes it as a trait during evaluation. Flagsmith segments target traits:
- "Free Plan" segment: `plan_type = free`
- "Starter+" segment: `plan_type IN [starter, professional, enterprise]`
- "Pro+" segment: `plan_type IN [professional, enterprise]`

Flags are configured per segment in Flagsmith's dashboard (e.g., `advanced_reports` ON for "Pro+", OFF otherwise).

### Upgrade/downgrade flow

When a team's plan changes, the next flag evaluation automatically picks up the new `plan_type` — no Flagsmith API call needed to "sync" traits, because traits are passed at evaluation time from the backend's own data.

---

## Phase 6: External Triggers & Monitoring (later phase)

### Prometheus scraping

**File:** `docker/prometheus/prometheus.yml`

Add Flagsmith as a scrape target (`flagsmith:8000/metrics`).

### Grafana alert → flag toggle

Grafana alerts can call webhooks. A backend endpoint (`POST /webhooks/grafana-alerts`) validates a shared secret, maps alert names to flag keys, and calls Flagsmith's REST API to toggle circuit-breaker flags.

### Flagsmith → audit log

Configure Flagsmith outgoing webhooks to `POST /webhooks/flagsmith`, which logs flag changes to the existing audit system.

---

## Files to Create

| File | Description |
|------|-------------|
| `backend/.../config/FlagsmithProperties.java` | Typed config properties record |
| `backend/.../config/FlagsmithConfig.java` | FlagsmithClient bean |
| `backend/.../service/FeatureFlagService.java` | Flag evaluation service |
| `backend/.../controller/FeatureFlagController.java` | `GET /feature-flags` endpoint |
| `backend/.../util/FeatureFlags.java` | Flag key constants |
| `app/src/api/featureFlags.ts` | API module |
| `app/src/context/FeatureFlagContext.tsx` | Context provider + hook |
| `app/src/components/FeatureGate.tsx` | Declarative gating component |
| `app/src/constants/featureFlags.ts` | Flag key constants |

## Files to Modify

| File | Change |
|------|--------|
| `docker/postgres/init-db.sh` | Add `flagsmith` database + user |
| `docker-compose.yml` | Add `flagsmith` + `flagsmith-processor` services with Traefik labels |
| `.env.example` | Add Flagsmith env vars |
| `backend/pom.xml` | Add flagsmith-java-client dependency |
| `backend/.../resources/application.yml` | Add `flagsmith:` config section |
| `backend/.../resources/application-local.yml` | Flagsmith URL override (Traefik) |
| `backend/.../resources/application-docker.yml` | Flagsmith URL override (internal) |
| `app/src/App.tsx` | Wrap with `FeatureFlagProvider` |
| `backoffice/src/pages/ToolEmbedPage.tsx` | Add Flagsmith to tools config |

## Verification

1. `make down-v && make dev` — Flagsmith starts alongside other infra
2. Visit `https://flagsmith.local.buurman.io` — Flagsmith dashboard loads
3. Create a test flag `test_flag` = ON in Development environment
4. Start backend locally (`mvn spring-boot:run`) — `FeatureFlagService` connects
5. `curl https://api.local.buurman.io/feature-flags` (with JWT) — returns `{ "flags": { "test_flag": true } }`
6. Toggle `test_flag` OFF in Flagsmith dashboard
7. Wait 60s (or shorter refresh interval), re-curl — returns `false`
8. Start app locally (`yarn dev`) — feature flags context loads, `useFeatureFlags().isEnabled('test_flag')` returns the correct value
9. Visit backoffice → Tools → Flagsmith — embedded dashboard loads in iframe
