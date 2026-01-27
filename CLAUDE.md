# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Buurman is a property management dashboard application for small landlords to manage rental properties, tenants, contracts, and finances. The application uses:

- **Backend**: Java 21, Spring Boot, Spring Security, JOOQ, Flyway, PostgreSQL
- **Frontend**: React, TypeScript, Vite, React Query, Tailwind CSS, Bootstrap 5
- **Authentication**: Keycloak with JWT tokens
- **Storage**: AWS S3 (LocalStack for development)
- **Infrastructure**: Docker Compose for local development

## Development Commands

### Backend (Spring Boot/Maven)
- Build the project: `mvn clean install`
- Run the application: `mvn spring-boot:run`
- Run tests: `mvn test`
- Run specific test: `mvn test -Dtest=ClassName#methodName`
- Package without tests: `mvn clean package -DskipTests`

### Frontend (React/Vite/Yarn)
- Install dependencies: `yarn install`
- Start dev server: `yarn dev` (runs on http://localhost:5173)
- Build for production: `yarn build`
- Run tests: `yarn test`
- Run linting: `yarn lint`
- Fix linting issues: `yarn lint --fix`

### Docker & Local Development
- Start all services: `docker-compose up -d`
- Start specific service: `docker-compose up -d postgres` (or `keycloak`, `localstack`)
- View logs: `docker-compose logs -f [service-name]`
- Stop all services: `docker-compose down`
- Reset everything: `docker-compose down -v` (removes volumes)

### Database Migrations (Flyway)
- Migrations are in: `src/main/resources/db/migration/`
- Naming convention: `V<version>__<description>.sql` (e.g., `V001__create_base_schema.sql`)
- Migrations run automatically on application startup
- Check migration history: Query `flyway_schema_history` table

## Architecture & Key Concepts

### Multi-Tenancy Architecture
- All data is isolated by `team_id` (UUID) at the database level
- Every table includes a `team_id` foreign key to the `teams` table
- `TeamContextHolder` (thread-local) stores the current user's team_id
- All repository queries automatically filter by team_id to prevent cross-team data access
- JWT tokens contain `team_id` claim extracted by `JwtAuthenticationFilter`

### Authentication Flow
1. User authenticates via Keycloak (OAuth2/OIDC)
2. Frontend receives JWT access token and refresh token
3. JWT contains claims: `user_id`, `team_id`, `roles` (TEAM_ADMIN, TEAM_EDITOR, TEAM_VIEWER)
4. Backend validates JWT signature using Keycloak public key
5. `JwtAuthenticationFilter` extracts claims and populates `SecurityContext`
6. All API requests include `Authorization: Bearer <token>` header

### Business ID Pattern
- Internal IDs use UUID (primary keys)
- External/business IDs use ULID (26 characters, sortable, user-facing)
- Every entity has both `id` (UUID) and `business_id` (ULID)
- Use `UlidGenerator.java` utility for generating business IDs

### Audit Pattern
- All entities have audit columns: `created_at`, `updated_at`, `created_by`, `updated_by`
- JPA entity listeners automatically populate these fields
- All modifications recorded in audit log
- Soft deletes implemented via `deleted_at` column (never hard delete)

### Backend Package Structure
```
com.buurman
├── config/          Security, S3, JOOQ, Swagger configuration
├── controller/      REST endpoints (thin layer, delegates to services)
├── service/         Business logic (auth, property, tenant, contract, financial, document, audit)
├── repository/      Data access layer (extends TeamAwareRepository)
├── domain/          JPA entities (Team, Property, Tenant, Contract, Payment, Expense, Document)
├── dto/             Request/response DTOs (request/, response/)
├── mapper/          MapStruct mappers (entity <-> DTO)
├── security/        JWT filter, UserPrincipal, TeamContextHolder
├── exception/       Exception handlers and custom exceptions
└── util/            Utilities (UlidGenerator, DateUtils)
```

### Frontend Folder Structure
```
src/
├── api/             Axios client and API methods (client.ts has interceptors)
├── components/      React components organized by feature (auth, dashboard, properties, tenants, etc.)
├── hooks/           Custom React hooks (useAuth, useProperties, useTenants, etc.)
├── context/         React contexts (AuthContext, TeamContext)
├── types/           TypeScript type definitions
├── utils/           Utility functions (formatting, validation)
├── routes/          React Router configuration
├── App.tsx          Main app component
└── main.tsx         Entry point
```

### Key Backend Components

#### TeamAwareRepository Pattern
All repositories extend a base that automatically filters by team_id:
```java
// All queries automatically append: WHERE team_id = ?
// Prevents accidental cross-team data leaks
```

#### Security Configuration
- JWT validation with Keycloak public key
- Role-based access control: `@PreAuthorize("hasRole('TEAM_ADMIN')")`
- CORS configured for frontend origin
- Health endpoints exempted from authentication

#### Document Storage
- Files stored in S3 (LocalStack for local dev)
- Documents table stores metadata and S3 keys
- Generic attachment system via `entity_type` + `entity_id`
- Pre-signed URLs for downloads (time-limited)

### Key Frontend Patterns

#### API Client (api/client.ts)
- Axios instance with base URL and interceptors
- Request interceptor: Attaches JWT token from localStorage
- Response interceptor: Handles 401 (token refresh/redirect to login)

#### React Query Usage
- All API calls use React Query hooks
- Cache configuration: 5min stale time, 10min cache time
- Query keys follow pattern: `['resource', id?, filters?]`
- Mutations automatically invalidate related queries

#### Custom Hooks Pattern
```typescript
// Example: useProperties.ts
export const useProperties = () => useQuery(['properties'], getProperties);
export const useCreateProperty = () => {
  const queryClient = useQueryClient();
  return useMutation(createProperty, {
    onSuccess: () => queryClient.invalidateQueries(['properties'])
  });
};
```

## Database Schema Conventions

- **Primary keys**: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- **Business IDs**: `business_id VARCHAR(26) NOT NULL` (ULID format)
- **Team isolation**: `team_id UUID NOT NULL REFERENCES teams(id)`
- **Audit fields**: `created_at`, `updated_at`, `created_by`, `updated_by`
- **Soft deletes**: `deleted_at TIMESTAMP` (NULL means active)
- **Indexes**: Always index `team_id`, status columns, foreign keys, and date columns
- **Constraints**: Use CHECK constraints for validation (e.g., positive amounts, date ranges)
- **Unique constraints**: Combine `team_id` + `business_id` for uniqueness within team

## Testing Strategy

### Backend Testing
- **Unit tests**: Service layer logic with Mockito (JUnit 5)
- **Integration tests**: Spring Boot Test + Testcontainers (PostgreSQL)
- **API tests**: REST Assured for end-to-end testing
- Target: 80% code coverage
- Always test multi-tenant isolation (cannot access other team's data)

### Frontend Testing
- **Unit tests**: Vitest + React Testing Library
- **Integration tests**: User flows with MSW (Mock Service Worker) for API mocking
- Target: 70% code coverage
- Test accessibility (screen reader support, keyboard navigation)

## Environment Variables

### Backend (.env or application.yml)
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/buurman
SPRING_DATASOURCE_PASSWORD=<password>
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_CLIENT_SECRET=<secret>
S3_ENDPOINT=http://localhost:4566  # LocalStack
S3_ACCESS_KEY=<key>
S3_SECRET_KEY=<secret>
```

### Frontend (.env)
```
VITE_API_URL=http://localhost:8080/api
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=buurman
VITE_KEYCLOAK_CLIENT_ID=buurman-web
```

## Common Development Tasks

### Adding a New Entity
1. Create Flyway migration in `src/main/resources/db/migration/`
2. Create JPA entity in `domain/` with audit annotations
3. Create repository extending `TeamAwareRepository`
4. Create service with CRUD operations (automatic team_id filtering)
5. Create DTOs (request/response) and MapStruct mapper
6. Create REST controller with proper security annotations
7. Write unit and integration tests
8. Create frontend API client methods
9. Create React components and custom hooks
10. Add routes and navigation

### Creating a New Database Migration
1. Determine next version number (check existing migrations)
2. Create file: `V<version>__<description>.sql`
3. Include:
   - `team_id UUID NOT NULL REFERENCES teams(id)`
   - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
   - `business_id VARCHAR(26) NOT NULL`
   - Audit columns (created_at, updated_at, created_by, updated_by)
   - `deleted_at TIMESTAMP` for soft deletes
   - Indexes on `team_id`, foreign keys, frequently queried columns
   - `UNIQUE(team_id, business_id)` constraint
4. Test migration: Restart application and verify schema

### Adding a New API Endpoint
1. Add method to controller with annotations:
   - `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping`
   - `@PreAuthorize` for role-based access control
   - OpenAPI annotations for documentation
2. Implement service method (business logic)
3. Create/update DTOs and mappers
4. Write unit test (MockMvc) and integration test
5. Add frontend API client method in `src/api/`
6. Create custom hook if needed
7. Update components to use new endpoint

## Important Rules

### Security
- NEVER bypass team_id filtering in queries
- ALWAYS validate JWT tokens on backend
- NEVER trust client-side data (validate on backend)
- USE `@PreAuthorize` for role-based access control
- NEVER expose internal UUIDs in URLs (use business_id)

### Data Integrity
- ALWAYS use soft deletes (`deleted_at` column)
- NEVER hard delete records (breaks audit trail)
- ALWAYS record who made changes (created_by, updated_by)
- USE database constraints for data validation
- ALWAYS validate date ranges (start_date < end_date)

### Code Quality
- FOLLOW Google Java Style Guide (backend)
- FOLLOW Airbnb JavaScript Style Guide (frontend)
- USE meaningful variable names (no single-letter variables except loop counters)
- WRITE self-documenting code (clear method/function names)
- ADD comments only for complex business logic
- USE conventional commits format (feat:, fix:, docs:, etc.)

## Development Workflow

### Starting Local Development
1. Ensure Docker is running
2. Start services: `docker-compose up -d`
3. Wait for PostgreSQL and Keycloak to be ready (~30 seconds)
4. Start backend: `mvn spring-boot:run` (runs on port 8080)
5. Start frontend: `yarn dev` (runs on port 5173)
6. Access app at: http://localhost:5173
7. Keycloak admin console: http://localhost:8080 (admin/admin)

### Debugging
- Backend: Use IDE debugger, attach to port 5005 if remote debugging enabled
- Frontend: Use browser DevTools, React DevTools extension
- Database: Connect to PostgreSQL on localhost:5432 (user: buurman, db: buurman)
- View logs: `docker-compose logs -f <service>`

### Common Issues
- **Port conflicts**: Check if ports 5173, 8080, 5432, 4566 are available
- **Database connection failed**: Ensure PostgreSQL container is running
- **JWT validation failed**: Check Keycloak is running and realm configured
- **CORS errors**: Verify backend SecurityConfig has correct frontend origin
- **Flyway migration failed**: Check migration syntax, rollback may require manual intervention

## Project Status

This project is currently in the planning phase. The comprehensive plan is documented in `plan.md` which outlines:
- Phase 1: Foundation & Core Features (Authentication, Properties, Dashboard)
- Phase 2: Tenant & Contract Management
- Phase 3: Financial Tracking (Payments, Expenses)
- Phase 4: Financial Reporting & Analytics

When implementing features, follow the phased approach in the plan document.
