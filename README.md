# Buurman - Property Management for Small Landlords

Modern dashboard application to help small landlords manage rental properties, tenants, and finances.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.2, PostgreSQL, Flyway, JOOQ
- **Frontend**: React 18, TypeScript, Vite, TailwindCSS
- **Infrastructure**: Docker Compose, LocalStack, Keycloak

## Prerequisites

- Docker & Docker Compose
- Java 21 (for local backend development)
- Node.js 24+ (for local frontend development)
- Maven 3.9+ (for local backend development)

## Quick Start

### 1. Start Infrastructure Services

```bash
# Start PostgreSQL, Keycloak, and LocalStack
docker-compose up -d

# Verify services are healthy
docker-compose ps
```

### 2. Run Backend (Development Mode)

```bash
cd backend
mvn spring-boot:run
```

Backend will be available at http://localhost:8081

### 3. Run Frontend (Development Mode)

```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at http://localhost:5173

## Project Structure

```
buurman/
├── backend/               # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/buurman/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
├── frontend/              # React application
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── docker/                # Docker initialization scripts
├── docker-compose.yml
└── README.md
```

## Development Workflow

See [CLAUDE.md](./CLAUDE.md) for detailed development guidance.

## Phase 1.1 Status

- [x] Docker Compose infrastructure
- [x] PostgreSQL with Flyway migrations
- [x] Spring Boot backend with health endpoints
- [x] React frontend with basic dashboard
- [x] LocalStack for S3 simulation
- [x] Keycloak setup (authentication in Phase 1.2)

## Next Steps (Phase 1.2)

- [ ] JWT authentication with Keycloak
- [ ] User registration and team creation
- [ ] Protected routes
- [ ] Team invitation system
