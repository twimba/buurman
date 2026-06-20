# Backoffice Dashboard v2 — Implementation Plan (grounded)

Linear: **BUUR-96**. This file restates the approved design (Linear ticket body) reconciled with the **actual** codebase as of branch `tuxetuxe/port-louis` (migrations at V055). The original spec was written 2026-05-17 against an older tree (V028) and made several assumptions that no longer hold.

## Spec-vs-reality deltas (decisions)

| Spec assumed | Reality | Decision |
|---|---|---|
| Roles `BUURMY_VIEWER` / `BUURMY_EDITOR` | Only `BACKOFFICE_ADMIN` (+ `BACKOFFICE_SYSTEM`) exist; all `/backoffice/**` already require `BACKOFFICE_ADMIN` | Use `@PreAuthorize("hasRole('BACKOFFICE_ADMIN')")` everywhere. No new roles. |
| Migration `V048` | Next free is `V056` | `V056__backoffice_dashboard_layout.sql` |
| `teams.country_code` exists | Does **not** | Geo ships **PREVIEW** |
| In-memory log buffer behind `LoggersPage` | Does **not** exist — `BackofficeLoggerController` only reads/sets log levels | Live tail ships **PREVIEW** in this phase; a logback ring-buffer appender is a follow-up |
| Roll out behind a feature flag | Backoffice has no per-feature flags (those are tenant-app concepts) | **No flag** — v2 replaces the legacy dashboard outright |
| `react-grid-layout`, `recharts` installed | Neither in `frontend/backoffice` | Add as deps in the frontend phase; phase-1 frontend uses a static CSS bento grid |
| Backoffice OpenAPI bundled from `src/` | `openapi/backoffice.yaml` is **hand-maintained** (bundler only emits `openapi/app.yaml`) | Edit `openapi/backoffice.yaml` directly + add `importMappings` in `buurman-backoffice/pom.xml` |
| Generic `PanelResponse<T>` | OpenAPI codegen can't express Java generics cleanly | Each panel returns a concrete record carrying the envelope fields (`status`, `previewCta`, `docsLink`) inline |

`teams.demo` and `teams.deleted_at` **do** exist → all KPI counts exclude `demo = true` and `deleted_at IS NOT NULL`.

## Envelope

```java
enum PanelStatus { LIVE, PREVIEW, DISABLED }
```
Every panel response record includes: `PanelStatus status`, `Optional<String> previewCta`, `Optional<String> docsLink`, plus its own data fields (empty/Optional when not LIVE). Frontend `PanelShell` renders PREVIEW generically — never fake numbers.

## Endpoints (all under `/backoffice/dashboard`, `BACKOFFICE_ADMIN`)

| Endpoint | Status this phase | Source |
|---|---|---|
| `GET /status-strip` | LIVE (paying-teams, outbox, alerts); PREVIEW (mrr, churn, dau, error-rate, p95) | teams, notification_outbox, action queue |
| `GET /action-queue` | LIVE | registrations, outbox, impersonation |
| `GET /product-entities` | LIVE | properties/contracts/tenants counts |
| `GET /funnel` | LIVE | teams + properties + contracts |
| `GET /top-teams` | LIVE (by activity) | aggregate query |
| `GET /scheduler-health` | LIVE | Quartz via `BackofficeSchedulerService` |
| `GET /business` | PREVIEW | billing (not built) |
| `GET /cost-watch` | PREVIEW | provider billing APIs |
| `GET /latency-heatmap` | PREVIEW | Prometheus |
| `GET /geo` | PREVIEW | `teams.country_code` (absent) |
| `GET /live-tail` | PREVIEW | log ring buffer (absent) |
| `GET /layout`, `PUT /layout` | LIVE | `backoffice_user_dashboard_layout` |
| `POST /action-queue/snooze` | LIVE | `backoffice_action_item_snooze` |

## Phasing

- **Phase 1 (done): backend.** Migration, DTOs, repositories, per-panel services + aggregator, controller, OpenAPI, pom mappings.
- **Phase 2 (done): frontend.** DashboardPage rewrite (replaces the legacy dashboard — no feature flag), status strip, bento grid, `PanelShell`/`PreviewState`, per-panel hooks, deeplinks, panel registry.
- **Phase 3 (done):**
  - **Live tail → LIVE.** Logback `RingBufferLogAppender` (cap 1000) attached to the root logger feeds `LiveTailService`; `/live-tail?level=` returns real log lines.
  - **Per-user layout.** Edit mode (topbar "Customize") with native drag-reorder, hide, and restore; debounced persist to `/layout`. (Resize / `react-grid-layout` intentionally omitted — native DnD chosen for zero-dep reliability + React 19 compat.)
  - **Tests.** Backend unit tests for `LogRingBuffer`, `LiveTailService`, `ActionQueueService` (snooze clamp + dead-letter + snooze filtering), and `BackofficePrincipal.userId`. Frontend component tests still pending (the `buurman-backoffice` workspace has no Vitest setup yet).
- **Phase 3: drag/drop + persistence UI (`react-grid-layout`), live-tail ring buffer, tests.**

## Out of scope (per ticket): billing/MRR plumbing, Prometheus/PostHog proxies, provider billing integrations, mobile, dark mode, browser notifications.
