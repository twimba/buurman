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
- **Storage**: AWS SDK v2 / S3 (LocalStack for dev)
- **Email**: Spring Mail + Thymeleaf templates (MailHog for dev)
- **Jobs**: Quartz Scheduler
- **Docs**: SpringDoc OpenAPI 3.0.1, iText7 (PDF), OpenCSV
- **Monitoring**: Prometheus + Grafana
- **Infrastructure**: Docker Compose for local development

## Development Commands

### Backend (Spring Boot/Maven)
- Build: `mvn clean install`
- Run: `mvn spring-boot:run` (port **8081**)
- Tests: `mvn test` / `mvn test -Dtest=ClassName#methodName`
- Package: `mvn clean package -DskipTests`

### Frontend (React/Vite/Yarn 4)
- Install: `yarn install`
- Dev server: `yarn dev` (port 5173)
- Build: `yarn build`
- Tests: `yarn test`
- Lint: `yarn lint` / `yarn lint --fix`

### Docker Services

| Service | Port | Purpose |
|---------|------|---------|
| postgres | 5432 | Database (PostgreSQL 18) |
| keycloak | 8080 | Authentication |
| localstack | 4566 | S3 storage (dev) |
| mailhog | 1025/8025 | SMTP / Email UI (dev) |
| backend | 8081 | Spring Boot API |
| frontend | 5173 | React app |
| prometheus | 9090 | Metrics collection |
| grafana | 3000 | Dashboards |

Commands: `docker-compose up -d`, `docker-compose down`, `docker-compose down -v` (reset)

### Application Profiles
- `local` (default): Services on localhost
- `docker`: Services use container hostnames
- Credentials externalized via `.env` file

### Database Migrations (Flyway)
- Location: `src/main/resources/db/migration/`
- Convention: `V<version>__<description>.sql` (currently at V020)
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
- External: ULID via `identifier` column (26 chars, sortable, user-facing)
- `UlidGenerator.java` for generation, `EntityPrefix` for type-prefixed IDs

### Audit Pattern
- All entities: `created_at`, `updated_at`, `created_by`, `updated_by`
- Set manually in repository INSERT/UPDATE queries (no JPA listeners)
- Soft deletes via `deleted_at` column (never hard delete)

### Backend Package Structure
```
com.buurman
├── config/          Security, S3, JOOQ, Swagger, Quartz config
├── controller/      REST endpoints (thin, delegates to services)
├── service/         Business logic + @PreAuthorize authorization
├── repository/      JOOQ DSLContext queries (no base class, manual team_id filtering)
├── domain/          POJOs (not JPA entities)
├── dto/
│   ├── request/     Request DTOs (classes + records)
│   └── response/    Response DTOs (Java records, expose identifier only)
├── mapper/          MapStruct interfaces + manual @Component mappers
├── security/        JwtAuthenticationConverter, UserAuthentication, TeamMembershipAspect
├── exception/       GlobalExceptionHandler + custom exceptions
├── job/             Quartz scheduled jobs
├── db/              FlywayMigrationLogger
└── util/            UlidGenerator, PaginationHelper, EntityPrefix, DateUtils
```

### Frontend Structure
```
src/
├── api/             Axios client + 16 API modules (properties.ts, tenants.ts, etc.)
├── components/      Feature-organized React components
├── pages/           31 page components
├── hooks/           21 custom React Query hooks
├── context/         AuthContext, TeamContext
├── types/           TypeScript type definitions
├── config/          Keycloak configuration
├── utils/           Formatting, validation utilities
├── App.tsx          Main component with routing
└── main.tsx         Entry point
```

### Key Patterns

**Repository**: Manual JOOQ with DSLContext. No base class. Each repo manually adds `team_id` to all WHERE clauses. ~21 repositories.

**Security**: `@PreAuthorize` on service methods for role-based access. `@EnableMethodSecurity(prePostEnabled = true)` in SecurityConfig. Role hierarchy: TEAM_ADMIN > TEAM_EDITOR > TEAM_VIEWER.

**DTOs**: Response DTOs are Java `record` types exposing only `identifier` (ULID), never internal UUIDs.

**Pagination**: `PageRequest` / `PageResponse` DTOs with `PaginationHelper` utility for JOOQ LIMIT/OFFSET.

**Mappers**: Mix of MapStruct interfaces (`componentModel = "spring"`) and manual `@Component` mapper classes. Map between JOOQ Records, domain POJOs, and DTOs.

**Frontend API**: Axios instance with Keycloak token interceptor. React Query hooks per resource with automatic cache invalidation on mutations.

**Document Storage**: S3 with metadata in `documents` + `photos` tables. LocalStack uses direct URLs; production uses presigned URLs.

## Database Schema Conventions

- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `identifier VARCHAR(26) NOT NULL` (ULID)
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
1. Flyway migration in `db/migration/` (next version after V020)
2. Domain POJO in `domain/`
3. JOOQ repository with manual `team_id` filtering in all queries
4. Service with `@Transactional` and `@PreAuthorize`
5. Request/Response DTOs (records preferred) + MapStruct mapper
6. REST controller (thin, delegates to service)
7. Frontend: API module, React Query hook, page components, routes

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
- Google Java Style Guide (backend), Airbnb JS Style Guide (frontend)
- Conventional commits: `feat:`, `fix:`, `docs:`, `chore:`
- Self-documenting code, comments only for complex business logic

## Development Workflow

### Starting Local Development
1. `docker-compose up -d`
2. Wait ~30s for PostgreSQL + Keycloak
3. `mvn spring-boot:run` (backend on **8081**)
4. `yarn dev` (frontend on 5173)
5. App: http://localhost:5173 | Keycloak: http://localhost:8080 | Grafana: http://localhost:3000 | MailHog: http://localhost:8025

### Common Issues
- **Port conflicts**: Check 5173, 8080, 8081, 5432, 4566
- **Database connection**: Ensure PostgreSQL container running
- **JWT validation**: Check Keycloak running and realm configured
- **CORS errors**: Verify SecurityConfig frontend origin
- **Flyway failure**: Check syntax; rollback may need manual intervention
- **S3/images**: LocalStack uses direct URLs, production uses presigned URLs
