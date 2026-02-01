# Buurman - Property Management for Small Landlords

Modern dashboard application to help small landlords manage rental properties, tenants, and finances.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.2, PostgreSQL, Flyway, JOOQ
- **Frontend**: React 18, TypeScript, Vite, TailwindCSS
- **Infrastructure**: Docker Compose, LocalStack, Keycloak

## Prerequisites

- Docker & Docker Compose
- Java 21 (optional, only for local backend development)
- Node.js 22+ (optional, only for local frontend development)
- Maven 3.9+ (optional, only for local backend development)
- Yarn (optional, only for local frontend development)

## Quick Start

### Option 1: Complete Docker Setup (Recommended)

Start everything with a single command:

```bash
docker compose up -d
```

This starts the complete application stack:
- PostgreSQL (port 5432)
- Keycloak (port 8080)
- LocalStack (port 4566)
- Backend API (port 8081)
- Frontend App (port 5173)

Wait ~90 seconds for all services to become healthy, then open http://localhost:5173

For detailed setup instructions, see [SETUP.md](./SETUP.md)

### Option 2: Development Mode with Hot Reload

Start only infrastructure, run backend and frontend manually:

```bash
# Start databases and services
docker compose up -d postgres keycloak localstack

# In terminal 1: Start backend with hot reload
cd backend
mvn spring-boot:run

# In terminal 2: Start frontend with hot reload
cd frontend
yarn install
yarn dev
```

Backend: http://localhost:8081
Frontend: http://localhost:5173

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
