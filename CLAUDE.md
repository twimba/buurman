# CLAUDE.md

# Efficiency & Token Management Rules
* **Response Style**: Be extremely laconic. No conversational filler.
* **Code Output**: Never output full file content unless requested. Show only modified functions via partial diffs, and only if super relevant.
* **Explanations**: Provide technical explanations ONLY if non-obvious or specifically asked "why."
* **Context Control**: If conversation exceeds 5-10 turns, proactively suggest /compact or /clear.
* **Search Hygiene**: Use the most specific paths possible to avoid reading unnecessary file metadata.

## Strict File Access Rules
* **Ignore Policy**: Strictly adhere to `.claudeignore` patterns.
* **Prohibition**: Never read, search, or index files matched by `.claudeignore`.
* **Token Efficiency**: Filter out ignored directory results before processing.

## Project Overview

Buurman is a property management dashboard for small landlords (properties, tenants, contracts, finances).

- **Backend**: Java 21, Spring Boot 4.0.2, Spring Security, JOOQ 3.20, Flyway, PostgreSQL
- **Frontend**: React 19, TypeScript, Vite 7, TanStack React Query 5, Tailwind CSS 4
- **Auth**: Keycloak 26 with JWT (OAuth2 Resource Server)
- **Storage**: AWS SDK v2 / S3 (SeaweedFS for dev)
- **Email**: Spring Mail + Thymeleaf templates (MailHog for dev)
- **Jobs**: Quartz Scheduler
- **Docs**: SpringDoc OpenAPI 3.0.1, iText7 (PDF), OpenCSV
- **Monitoring**: Prometheus + Grafana
- **Infrastructure**: Docker Compose for local development

## Development Commands

### Backend (Spring Boot/Maven — Multi-Module)

The backend is a 10-module Maven project:
- `common` — domain, DTOs, utils (no Spring deps)
- `jooq` — JOOQ codegen + Flyway migrations
- `buurman` — core services, repos, mappers, config, security (`buurman-core`)
- `buurman-notifications` — notification system (email/SMS, outbox, webhooks)
- `buurman-backoffice` — backoffice admin controllers/services/repos (depends on core + notifications)
- `buurman-demo-data` — demo data generators + Quartz job
- `buurman-takeout` — data export/takeout
- `buurman-booklets` — PDF/CSV exporters + BookletController
- `app` — thin shell, assembles fat JAR (`buurman-app`)

- Build: `cd backend && mvn clean install -DskipTests`
- Run: `cd backend && mvn spring-boot:run -pl app -am` (port **8081**)
- Quick build (skip formatting): `cd backend && mvn package -DskipTests -Pquick`
- Tests: `cd backend && mvn test` / `mvn test -pl buurman -Dtest=ClassName#methodName`
- Regenerate JOOQ after migration changes: `cd backend && mvn generate-sources -pl jooq -am`
- Build cache: enabled locally (`.mvn/extensions.xml`), disabled in CI (`-Dmaven.build.cache.enabled=false`)

### Frontend (React/Vite/Yarn 4)
All frontend code lives under `frontend/` (Yarn workspaces: `app`, `backoffice`, `packages/ui`).
- Install: `cd frontend && yarn install`
- Dev server: `cd frontend && yarn dev` (app on 5173, backoffice on 5174)
- Build: `cd frontend && yarn build`
- Tests: `cd frontend && yarn test`
- Lint: `cd frontend && yarn lint` / `yarn lint --fix`
- Generate API clients: `cd frontend && yarn generate:api` (or `make generate-api`)

### Docker Services (Traefik Reverse Proxy + HTTPS)

All services are routed through Traefik with HTTPS (`*.local.buurman.io`). HTTP automatically redirects to HTTPS. TLS certificates are generated locally via [mkcert](https://github.com/FiloSottile/mkcert) (zero browser warnings).

| Service | URL | Purpose |
|---------|-----|---------|
| traefik | https://traefik.local.buurman.io | Reverse proxy dashboard |
| postgres | postgresql.local.buurman.io:5432 | Database (PostgreSQL 18, TCP via Traefik) |
| keycloak | https://keycloak.local.buurman.io | Authentication |
| seaweedfs | https://seaweedfs.local.buurman.io | S3 storage (dev) |
| seaweedfs-ui | https://seaweedfs-ui.local.buurman.io | S3 file browser (dev) |
| mailpit | https://mailpit.local.buurman.io (SMTP: port 1025) | Email UI / SMTP (dev) |
| backend | https://api.local.buurman.io | Spring Boot API |
| app | https://app.local.buurman.io | React app |
| prometheus | https://prometheus.local.buurman.io | Metrics collection |
| grafana | https://grafana.local.buurman.io | Dashboards |
| PostHog | https://eu.posthog.com | Product analytics (cloud, EU) |

Environment variables for analytics:
- `VITE_POSTHOG_KEY` — PostHog project API key (empty = disabled)
- `VITE_POSTHOG_HOST` — PostHog ingest endpoint (`/ingest` via Traefik proxy)

Commands (via Makefile):
- `make up` — start everything in Docker (including backend + app)
- `make dev` — start infrastructure only (for local backend/app development)
- `make down` — stop all containers
- `make down-v` — stop + remove volumes (full reset)
- `make logs` — tail all service logs
- `make certs` — generate local TLS certificates (one-time)

### Application Profiles
- `local` (default): Infrastructure in Docker behind Traefik, backend/app on host (`make dev`)
- `docker`: All services in Docker behind Traefik (`make up`)
- Credentials externalized via `.env` file

### OpenAPI Spec (API-First)
- Source files: `openapi/src/app.yaml` (root with `$ref` entries + schemas) + `openapi/src/paths/*.yaml` (17 domain-grouped path files)
- Bundled output: `openapi/app.yaml` (single file consumed by openapi-generator + Orval, committed to git)
- Bundle command: `make bundle-openapi` or `python3 scripts/bundle_openapi.py`
- **After editing any `openapi/src/` file, run `make bundle-openapi` to regenerate `openapi/app.yaml`**
- Path files use `$ref: '#/components/schemas/...'` — these resolve correctly in the bundled output

### Database Migrations (Flyway)
- Location: `backend/jooq/src/main/resources/db/migration/`
- Convention: `V<version>__<description>.sql` (currently at V028)
- Auto-applied on startup. **Never modify existing migrations.**

## Architecture & Key Concepts

### Multi-Tenancy
- All data isolated by `team_id` (UUID) at database level
- Every repository query manually filters by `team_id` in WHERE clauses
- JWT tokens contain `team_id` claim extracted by `JwtAuthenticationConverter`
- `TeamMembershipAspect` enforces team-level authorization

### Authentication Flow
1. User authenticates via Keycloak (OAuth2/OIDC)
2. Frontend receives JWT via Keycloak JS adapter
3. JWT claims: `user_id`, `team_id`, `roles` (TEAM_ADMIN, TEAM_EDITOR, TEAM_VIEWER)
4. Backend validates JWT as OAuth2 Resource Server
5. `JwtAuthenticationConverter` extracts claims into `UserAuthentication`

### IDs Pattern
- Internal: UUID (primary keys, never exposed in APIs)
- External: Sid via `identifier` column (26 chars, sortable, user-facing)
- `SidGenerator.java` for generation, `EntityPrefix` for type-prefixed IDs

### Audit Pattern
- All entities: `created_at`, `updated_at`, `created_by`, `updated_by`
- Set manually in repository INSERT/UPDATE queries (no JPA listeners)
- Soft deletes via `deleted_at` column (never hard delete)

### Backend Module Structure
```
backend/
├── common/                  Leaf module (~381 files, no Spring deps)
│   └── com.buurman
│       ├── domain/              POJOs, Sid, typed identifiers, enums
│       ├── dto/                 Request + response DTOs
│       ├── exception/           Custom exceptions (not GlobalExceptionHandler)
│       ├── util/                SidGenerator, EntityPrefix, MoneyAmount, DateUtils
│       └── config/jooq/         SidJooqConverter, MoneyMinorUnitConverter
├── jooq/                    JOOQ codegen + Flyway migrations (0 hand-written Java)
│   ├── src/main/resources/db/migration/  (28 SQL migrations)
│   └── target/generated-sources/jooq/   (generated JOOQ records)
├── buurman/                 Core module (~259 files, buurman-core)
│   └── com.buurman
│       ├── config/              Security, S3, Swagger, Quartz config
│       ├── controller/          REST endpoints (thin, delegates to services)
│       ├── service/             Business logic + @PreAuthorize authorization
│       ├── service/notification/ NotificationService interface + SPI types
│       ├── repository/          JOOQ DSLContext queries (manual team_id filtering)
│       ├── mapper/              MapStruct interfaces + manual @Component mappers
│       ├── security/            JwtAuthenticationConverter, UserAuthentication
│       ├── exception/           GlobalExceptionHandler
│       ├── job/                 Quartz scheduled jobs
│       ├── db/                  FlywayMigrationLogger
│       └── util/                PaginationHelper, S3BucketInitializer
├── buurman-notifications/   Notification system (~30 files)
│   └── com.buurman
│       ├── config/              SendGridConfig, TwilioConfig, NotificationQuartzConfig
│       ├── config/models/       SendGridProperties, TwilioProperties, NotificationOutboxProperties
│       ├── controller/          NotificationController, WebhookController
│       ├── service/notification/ NotificationServiceImpl, NotificationCenterService, WebhookService
│       ├── service/notification/channel/  Email/SMS senders (SendGrid, Twilio, local)
│       ├── repository/          NotificationRepository, NotificationOutboxRepository
│       ├── mapper/              NotificationRecordMapper, NotificationOutboxRecordMapper
│       └── job/                 NotificationOutboxJob, ContractExpiryCheckJob, PaymentReminderCheckJob
├── buurman-backoffice/      Backoffice admin (~28 files, depends on core + notifications)
│   └── com.buurman
│       ├── config/              BackofficeSecurityConfig
│       ├── controller/backoffice/  12 backoffice controllers
│       ├── service/backoffice/     11 backoffice services
│       ├── repository/backoffice/  2 backoffice repositories
│       └── security/            BackofficeJwtAuthenticationConverter
├── buurman-demo-data/       Demo data generation (~18 files)
│   └── com.buurman
│       ├── config/              DemoQuartzConfig, DemoDataProperties
│       ├── service/demo/        DemoDataService + 14 generators
│       └── job/                 DemoDataRegenerationJob
├── buurman-takeout/         Data export/takeout (~4 files)
│   └── com.buurman
│       ├── config/              TakeoutQuartzConfig
│       ├── controller/          TakeoutController
│       ├── service/             TakeoutService
│       ├── repository/          DataTakeoutRepository
│       └── job/                 TakeoutCleanupJob
├── buurman-booklets/        PDF/CSV export (~16 files)
│   └── com.buurman
│       ├── controller/          BookletController
│       └── service/export/      ExportServiceImpl + 14 exporters
└── app/                     Thin shell — fat JAR assembly (1 file)
    └── com.buurman           BuurmanApplication.java + all resources
```

### Frontend Structure
```
frontend/
├── package.json             Yarn workspace root
├── .yarnrc.yml              Yarn config
├── yarn.lock                Lock file
├── orval.config.ts          OpenAPI client generation config
├── scripts/vite-build-info.ts  Build info plugin
├── app/                     Main React app
│   └── src/
│       ├── api/             Axios client + 16 API modules
│       ├── components/      Feature-organized React components
│       ├── pages/           31 page components
│       ├── hooks/           21 custom React Query hooks
│       ├── context/         AuthContext, TeamContext
│       ├── types/           TypeScript type definitions
│       ├── config/          Keycloak configuration
│       ├── utils/           Formatting, validation utilities
│       ├── App.tsx          Main component with routing
│       └── main.tsx         Entry point
├── backoffice/              Backoffice admin app
└── packages/ui/             Shared UI component library
```

### Key Patterns

**Repository**: Manual JOOQ with DSLContext. No base class. Each repo manually adds `team_id` to all WHERE clauses. ~21 repositories.

**Security**: `@PreAuthorize` on service methods for role-based access. `@EnableMethodSecurity(prePostEnabled = true)` in SecurityConfig. Role hierarchy: TEAM_ADMIN > TEAM_EDITOR > TEAM_VIEWER.

**DTOs**: Response DTOs are Java `record` types exposing only `identifier` (Sid), never internal UUIDs.

**Pagination**: `PageRequest` / `PageResponse` DTOs with `PaginationHelper` utility for JOOQ LIMIT/OFFSET.

**Mappers**: Mix of MapStruct interfaces (`componentModel = "spring"`) and manual `@Component` mapper classes. Map between JOOQ Records, domain POJOs, and DTOs.

**Frontend API**: Axios instance with Keycloak token interceptor. React Query hooks per resource with automatic cache invalidation on mutations.

**Document Storage**: S3 with metadata in `documents` + `photos` tables. SeaweedFS uses direct URLs; production uses presigned URLs.

## Database Schema Conventions

- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `identifier VARCHAR(26) NOT NULL` (Sid)
- `team_id UUID NOT NULL REFERENCES teams(id)`
- Audit: `created_at`, `updated_at`, `created_by`, `updated_by`
- `deleted_at TIMESTAMP` for soft deletes (NULL = active)
- Indexes on `team_id`, foreign keys, status columns, dates
- `UNIQUE(team_id, identifier)` constraint
- CHECK constraints for validation (positive amounts, date ranges)

## Testing Status

**Currently zero test coverage.** Test dependencies are configured (Spring Boot Test, Testcontainers, Vitest) but no test files exist yet.

### Planned Strategy
- Backend: JUnit 5 + Mockito (unit), Testcontainers (integration)
- Frontend: Vitest + React Testing Library
- Always test multi-tenant isolation

## Adding a New Entity (Checklist)
1. Flyway migration in `backend/jooq/src/main/resources/db/migration/` (next version after V028)
2. Regenerate JOOQ: `cd backend && mvn generate-sources -pl jooq -am`
3. Domain POJO in `backend/common/src/.../domain/`
4. Request/Response DTOs in `backend/common/src/.../dto/`
5. JOOQ repository in `backend/buurman/src/.../repository/` with manual `team_id` filtering
6. MapStruct mapper in `backend/buurman/src/.../mapper/`
7. Service with `@Transactional` and `@PreAuthorize` in `backend/buurman/src/.../service/`
8. REST controller (thin) in `backend/buurman/src/.../controller/`
9. Frontend: API module in `frontend/app/src/`, React Query hook, page components, routes

## Important Rules

### Security
- NEVER bypass team_id filtering in queries
- USE `@PreAuthorize` for role-based access control
- NEVER expose internal UUIDs in APIs (use identifier)
- NEVER trust client-side data (validate on backend)

### Data Integrity
- ALWAYS soft delete via `deleted_at` (never hard delete)
- ALWAYS set `created_by`/`updated_by` in repository queries
- USE database constraints for validation

### Code Quality
- Google Java Style Guide (backend), Airbnb JS Style Guide (app)
- Conventional commits: `feat:`, `fix:`, `docs:`, `chore:`
- Self-documenting code, comments only for complex business logic
- **MANDATORY**: All `if`, `else`, `for`, `while` bodies MUST use curly braces — no brace-less single-statement bodies, ever
- **MANDATORY**: Use idiomatic Optional API (`map`, `orElse`, `orElseThrow`, `ifPresent`, `flatMap`) — never `if (opt != null)` or `opt.get()` without `isPresent()` check

## Development Workflow

### First-Time Setup (one-time)
1. Install mkcert: `brew install mkcert` (macOS) — see [mkcert docs](https://github.com/FiloSottile/mkcert) for other OS
2. Install local CA: `mkcert -install` (may prompt for sudo password)
3. Generate certificates: `bash scripts/setup-local-certs.sh`

### Starting Local Development
1. `make dev` (infrastructure + Traefik only, backend/app excluded)
2. Wait ~30s for PostgreSQL + Keycloak
3. `cd backend && mvn spring-boot:run` (backend on 8081)
4. `cd frontend && yarn dev` (app on 5173, backoffice on 5174)
5. Access everything via the same HTTPS URLs — Traefik routes to your host machine:
   - App: https://app.local.buurman.io | API: https://api.local.buurman.io
   - Keycloak: https://keycloak.local.buurman.io | Mailpit: https://mailpit.local.buurman.io

### Starting Full Docker (everything containerized)
1. `make up` (all services including backend + app containers)
2. App: https://app.local.buurman.io | API: https://api.local.buurman.io | Traefik: https://traefik.local.buurman.io

### Common Issues
- **Port 80/443 conflict**: Traefik needs both — check for other web servers
- **TLS cert errors**: Run `bash scripts/setup-local-certs.sh` (requires mkcert)
- **DNS resolution**: All `*.local.buurman.io` must resolve to `127.0.0.1`
- **Database connection**: Ensure PostgreSQL container running (`postgresql.local.buurman.io:5432`)
- **JWT validation**: Check Keycloak running at `https://keycloak.local.buurman.io` and realm configured
- **CORS errors**: Verify SecurityConfig allowed origin includes `https://app.local.buurman.io`
- **Flyway failure**: Check syntax; rollback may need manual intervention
- **S3/images**: SeaweedFS uses direct URLs via `https://seaweedfs.local.buurman.io`, production uses presigned URLs
