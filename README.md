# Buurman

**Property management dashboard for small landlords** — manage properties, tenants, contracts, and finances from a single application.

<p align="center">
  <img src="assets/logo/logo_horizontal.png" alt="Buurman Logo" width="400">
</p>

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Backend** | Java 21, Spring Boot 4.0.2, Spring Security, JOOQ 3.20, Flyway, PostgreSQL 18 |
| **Frontend** | React 19, TypeScript 5.9, Vite 7, TanStack React Query 5, Tailwind CSS 4 |
| **Auth** | Keycloak 26 (OAuth2/OIDC, JWT) |
| **Storage** | AWS SDK v2 / S3 (SeaweedFS for dev) |
| **Email** | Spring Mail + Thymeleaf templates (MailHog for dev) |
| **Jobs** | Quartz Scheduler |
| **Docs** | SpringDoc OpenAPI 3, iText7 (PDF generation), OpenCSV |
| **Monitoring** | Prometheus + Grafana |
| **Infra** | Docker Compose, multi-profile (local / docker) |

## Prerequisites

- **Docker & Docker Compose** (required for all setups)
- **Java 21** + **Maven 3.9+** (for local backend development)
- **Node.js 22+** + **Yarn 4** (for local app development)

## Quick Start

### 1. Configure environment

```bash
cp .env.example .env
# Defaults work out of the box for local development
```

### 2a. Full Docker Setup (easiest)

```bash
docker compose up -d
```

Wait ~90 seconds for services to become healthy, then open http://localhost:5173.

### 2b. Development Mode (hot reload)

Start infrastructure services, then run backend and app locally:

```bash
# Infrastructure (database, auth, storage, email)
docker compose up -d postgres keycloak seaweedfs mailpit

# Terminal 1 — Backend (port 8081)
cd backend && mvn spring-boot:run

# Terminal 2 — App (port 5173)
cd app && yarn install && yarn dev
```

### 3. Access the app

| Service | URL | Notes |
|---------|-----|-------|
| **App** | http://localhost:5173 | Main application |
| **Backend API** | http://localhost:8081 | REST API |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | API documentation |
| **Keycloak** | http://localhost:8080 | Admin: `admin` / `admin` |
| **MailHog** | http://localhost:8025 | Email inbox (dev) |
| **Grafana** | http://localhost:3000 | Monitoring dashboards |
| **Prometheus** | http://localhost:9090 | Metrics |

## Architecture

### High-Level Overview

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   React SPA │────▶│  Spring Boot │────▶│ PostgreSQL  │
│  (Vite/TS)  │     │   REST API   │     │   (JOOQ)    │
└──────┬──────┘     └──────┬───────┘     └─────────────┘
       │                   │
       │ OAuth2/JWT        ├──▶ S3 (SeaweedFS)
       ▼                   ├──▶ SMTP (MailHog)
┌─────────────┐            └──▶ Quartz Jobs
│  Keycloak   │
│   (OIDC)    │
└─────────────┘
```

### Multi-Tenancy

All data is isolated by `team_id` at the database level. Every query filters by `team_id`, extracted from the JWT token. Teams support role-based access: **TEAM_ADMIN** > **TEAM_EDITOR** > **TEAM_VIEWER**.

### ID Strategy

- **Internal**: UUIDs (primary keys, never exposed in APIs)
- **External**: Type-prefixed Sids (26 chars, sortable, user-facing via `identifier` column)

### Authentication Flow

1. User authenticates via Keycloak (OAuth2/OIDC)
2. Frontend receives JWT via Keycloak JS adapter
3. Backend validates JWT as OAuth2 Resource Server
4. Claims (`user_id`, `team_id`, `roles`) are extracted into `UserAuthentication`
5. `@PreAuthorize` annotations enforce role-based access on service methods

## Project Structure

```
buurman/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/buurman/
│   │   ├── config/             #   Spring configuration (Security, S3, JOOQ, Swagger, Quartz)
│   │   ├── controller/         #   REST endpoints (thin, delegates to services)
│   │   ├── service/            #   Business logic + @PreAuthorize authorization
│   │   ├── repository/         #   JOOQ queries (manual team_id filtering)
│   │   ├── domain/             #   POJOs (not JPA entities)
│   │   ├── dto/                #   Request/Response DTOs (records preferred)
│   │   ├── mapper/             #   MapStruct interfaces + manual mappers
│   │   ├── security/           #   JWT converter, UserAuthentication, TeamMembershipAspect
│   │   ├── exception/          #   GlobalExceptionHandler + custom exceptions
│   │   ├── job/                #   Quartz scheduled jobs
│   │   └── util/               #   SidGenerator, PaginationHelper, EntityPrefix
│   └── src/main/resources/
│       └── db/migration/       #   Flyway migrations (V001–V020)
├── app/                        # React application
│   └── src/
│       ├── api/                #   Axios client + API modules per resource
│       ├── components/         #   Feature-organized React components
│       ├── pages/              #   Page components
│       ├── hooks/              #   Custom React Query hooks
│       ├── contexts/           #   AuthContext, TeamContext
│       ├── types/              #   TypeScript type definitions
│       ├── config/             #   Keycloak configuration
│       └── utils/              #   Formatting, validation utilities
├── docker/                     # Docker init scripts
│   ├── grafana/                #   Grafana provisioning & dashboards
│   ├── keycloak/               #   Realm import configuration
│   ├── seaweedfs/              #   S3 bucket init scripts
│   ├── postgres/               #   Database init scripts
│   └── prometheus/             #   Prometheus configuration
├── docs/                       # Documentation
│   ├── METRICS_CATALOG.md      #   Prometheus/Grafana metrics reference
│   └── STYLE_GUIDE.md          #   Code style guide
├── assets/logo/                # Brand assets
├── docker-compose.yml
├── .env.example                # Environment variable template
└── CLAUDE.md                   # AI-assisted development guide
```

## Development

### Backend Commands

```bash
mvn clean install              # Build + run tests
mvn spring-boot:run            # Start dev server (port 8081)
mvn test                       # Run all tests
mvn test -Dtest=Class#method   # Run specific test
mvn clean package -DskipTests  # Package JAR
```

### App Commands

```bash
yarn install     # Install dependencies
yarn dev         # Start dev server (port 5173)
yarn build       # Production build (with type checking)
yarn test        # Run tests (Vitest)
yarn lint        # ESLint check
yarn lint --fix  # ESLint auto-fix
```

### Docker Commands

```bash
docker compose up -d           # Start all services
docker compose up -d postgres keycloak seaweedfs mailpit  # Infrastructure only
docker compose down            # Stop services
docker compose down -v         # Stop + delete volumes (full reset)
docker compose logs -f backend # Tail backend logs
```

### Application Profiles

| Profile | Usage | Service hostnames |
|---------|-------|-------------------|
| `local` (default) | Backend runs on host, services on localhost | `localhost` |
| `docker` | Full Docker Compose setup | Container names (e.g., `postgres`, `keycloak`) |

### Database Migrations

Flyway migrations live in `backend/src/main/resources/db/migration/` and run automatically on startup. Current schema version: **V020**.

Convention: `V<version>__<description>.sql` — **never modify existing migrations**.

### Environment Variables

Copy `.env.example` to `.env`. All defaults work for local development. Key variables:

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_USERNAME` / `DB_PASSWORD` | `buurman` / `buurman` | PostgreSQL credentials |
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | `admin` / `admin` | Keycloak admin login |
| `DEMO_DATA_ENABLED` | `true` | Seed demo data on startup |
| `VITE_KEYCLOAK_URL` | `http://localhost:8080` | Frontend Keycloak endpoint |
| `VITE_GOOGLE_MAPS_API_KEY` | — | Google Maps integration (optional) |

### Key Design Patterns

- **Soft deletes**: All entities use `deleted_at` (never hard delete)
- **Audit trail**: `created_at`, `updated_at`, `created_by`, `updated_by` on all entities
- **No JPA**: Pure JOOQ with `DSLContext` — no base repository class
- **Thin controllers**: Controllers delegate to services, which contain business logic
- **Response DTOs**: Java records exposing only `identifier` (Sid), never internal UUIDs
- **Frontend data layer**: React Query hooks per resource with automatic cache invalidation

### Common Issues

| Problem | Solution |
|---------|----------|
| Port conflict | Check ports 5173, 8080, 8081, 5432, 4566 are free |
| Database connection failure | Ensure PostgreSQL container is running |
| JWT validation error | Verify Keycloak is running and realm is configured |
| CORS errors | Check `SecurityConfig` allowed origin setting |
| Flyway migration failure | Check SQL syntax; rollback may need manual intervention |
| Images not loading | SeaweedFS uses direct URLs; production uses presigned URLs |

## Documentation

- [CLAUDE.md](./CLAUDE.md) — Full development guide & architecture reference
- [docs/STYLE_GUIDE.md](./docs/STYLE_GUIDE.md) — Code style conventions
- [docs/METRICS_CATALOG.md](./docs/METRICS_CATALOG.md) — Prometheus/Grafana metrics reference
- [Swagger UI](http://localhost:8081/swagger-ui.html) — Interactive API docs (when running)

## License

Private — All rights reserved.
