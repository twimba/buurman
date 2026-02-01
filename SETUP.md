# Buurman - Phase 1.2 Authentication Setup Guide

This guide covers the automatic setup and configuration of authentication for the Buurman application using Keycloak.

## Automated Keycloak Configuration

The Keycloak realm is **automatically configured** on startup - no manual configuration required!

### What Gets Configured Automatically

When you start the Docker services, Keycloak will automatically import the `buurman` realm with:

- **Realm Settings**:
  - User registration enabled
  - Email as username
  - Login with email enabled
  - Password policy: minimum 8 characters, 1 uppercase, 1 lowercase, 1 digit

- **Token Configuration**:
  - Access Token Lifespan: 5 minutes
  - Refresh Token Lifespan: 30 minutes
  - SSO Session Idle: 30 minutes
  - SSO Session Max: 10 hours

- **Client Configuration**:
  - Client ID: `buurman-web`
  - Public client (PKCE enabled)
  - Redirect URIs: `http://localhost:5173/*`
  - Web Origins: `http://localhost:5173`

- **Realm Roles**:
  - `TEAM_ADMIN` - Full team management access
  - `TEAM_EDITOR` - Create and edit properties/tenants
  - `TEAM_VIEWER` - Read-only access

## Quick Start

### 1. Start All Services (Complete Stack)

```bash
# From project root - starts everything at once!
docker compose up -d
```

This single command will start:
- **PostgreSQL** (port 5432) - Database for both app and Keycloak
- **Keycloak** (port 8080) - Authentication server with realm auto-imported
- **LocalStack** (port 4566) - S3-compatible storage
- **Backend** (port 8081) - Spring Boot API (automatically built and started)
- **Frontend** (port 5173) - React application served via Nginx

Wait about 90-120 seconds for all services to start and become healthy. You can watch the logs:

```bash
# Watch all logs
docker compose logs -f

# Watch specific service
docker compose logs -f backend
docker compose logs -f frontend
```

### 2. Verify Services Are Running

Check all services are healthy:

```bash
# Check service status
docker compose ps

# All services should show "healthy" or "running"
```

Verify each component individually:

**Frontend**: http://localhost:5173 (should show login/register page)

**Backend Health**:
```bash
curl http://localhost:8081/actuator/health
# Should return: {"status":"UP"}
```

**Keycloak Realm**:
```bash
curl http://localhost:8080/realms/buurman/.well-known/openid-configuration
# Should return JSON configuration
```

**Keycloak Admin Console**:
- URL: http://localhost:8080
- Username: `admin`
- Password: `admin`
- Navigate to: Realm selector (top left) → You should see "buurman" realm

### 3. You're Done!

Navigate to http://localhost:5173 and start using the application.

---

## Alternative: Development Mode (Manual Start)

If you prefer to develop with hot-reload and faster feedback:

### Start Infrastructure Only

```bash
# Start only databases and external services
docker compose up -d postgres keycloak localstack
```

### Start Backend Manually

```bash
cd backend
mvn spring-boot:run
```

Backend runs on port 8081 with:
- Hot reload enabled
- Debug logging
- Connected to Dockerized postgres/keycloak

### Start Frontend Manually

```bash
cd frontend
yarn dev
```

Frontend runs on port 5173 with:
- Vite hot module replacement
- Development mode optimizations
- Connected to backend on port 8081

## Docker Management

### Stop All Services

```bash
# Stop all containers
docker compose stop

# Stop specific service
docker compose stop backend
```

### Restart Services

```bash
# Restart all services
docker compose restart

# Restart specific service (e.g., after code changes)
docker compose restart backend
```

### Rebuild Services

```bash
# Rebuild after code changes (backend or frontend)
docker compose up -d --build backend frontend

# Force rebuild without cache
docker compose build --no-cache backend
docker compose up -d backend
```

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend

# Last 100 lines
docker compose logs --tail=100 backend
```

### Clean Up

```bash
# Stop and remove containers (keeps volumes)
docker compose down

# Remove containers and volumes (fresh start)
docker compose down -v

# Remove containers, volumes, and images
docker compose down -v --rmi all
```

## Testing the Authentication Flow

### Option 1: Register New User

1. Navigate to http://localhost:5173
2. Click "Register"
3. Fill in the registration form:
   - Email: test@example.com
   - Name: Test User
   - Team Name: Test Team
   - Password: Test123!
   - Confirm Password: Test123!
4. Click "Register"
5. You'll be redirected to login
6. Login with the credentials you just created

### Option 2: Direct Registration via API

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "name": "Admin User",
    "password": "Admin123!",
    "teamName": "Admin Team"
  }'
```

This creates:
- A user in Keycloak
- A user record in the database
- A team in the database
- A team_member record with TEAM_ADMIN role

### Option 3: Login with Existing User

If you've already registered users, simply:
1. Navigate to http://localhost:5173
2. Click "Login with Keycloak"
3. Enter your credentials
4. You'll be redirected to the dashboard

## Verification Checklist

After starting all services, verify each component:

### ✓ PostgreSQL
```bash
docker exec -it buurman-postgres psql -U buurman -d buurman -c "\dt"
```
Should show tables: teams, users, team_members, team_invitations, etc.

### ✓ Database Migrations
```bash
# Check migration status via API
curl http://localhost:8081/api/info | jq .database

# Or check directly in database
docker exec -it buurman-postgres psql -U buurman -d buurman -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```
Should show all applied migrations with their versions.

### ✓ Keycloak Realm
```bash
curl http://localhost:8080/realms/buurman
```
Should return realm metadata (not 404).

### ✓ Keycloak Client
```bash
curl http://localhost:8080/realms/buurman/.well-known/openid-configuration | jq .
```
Should show OIDC discovery document.

### ✓ Backend Health
```bash
curl http://localhost:8081/api/health
```
Should return: `{"status":"UP"}`

### ✓ Backend Security
```bash
# Should return 401 Unauthorized (no token)
curl -X GET http://localhost:8081/api/auth/me
```

### ✓ Frontend
Navigate to http://localhost:5173 - should redirect to /login

## Database Migrations with Flyway

### Automatic Migration on Startup

The backend automatically applies database migrations on startup using Flyway. This ensures your database schema is always up-to-date with your code version.

**What happens when the backend starts:**

1. Spring Boot connects to PostgreSQL
2. Flyway checks the `flyway_schema_history` table
3. Any unapplied migrations in `src/main/resources/db/migration/` are executed in order
4. Migration status is logged to the console
5. Application starts normally

**Migration files naming convention:**
- Format: `V<version>__<description>.sql`
- Example: `V001__create_base_schema.sql`
- Versions must be unique and sequential

### Viewing Migration Status

**Via API endpoint:**
```bash
# Get detailed migration info
curl http://localhost:8081/api/info

# Pretty print with jq
curl -s http://localhost:8081/api/info | jq .database
```

**Via database:**
```bash
# View all applied migrations
docker exec -it buurman-postgres psql -U buurman -d buurman -c \
  "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

**Via application logs:**
```bash
# View backend logs showing migration status
docker compose logs backend | grep -A 10 "Database Migration Status"
```

### Migration Files Location

All migration files are located in:
```
backend/src/main/resources/db/migration/
├── V001__create_base_schema.sql
├── V002__create_properties.sql
├── V003__create_documents.sql
├── V004__create_audit_log.sql
└── V005__create_team_invitations.sql
```

### Creating New Migrations

When you need to modify the database schema:

1. Create a new migration file with the next version number:
   ```bash
   touch backend/src/main/resources/db/migration/V006__add_new_feature.sql
   ```

2. Write your SQL:
   ```sql
   -- V006__add_new_feature.sql
   ALTER TABLE properties ADD COLUMN new_field VARCHAR(255);
   CREATE INDEX idx_properties_new_field ON properties(new_field);
   ```

3. Restart the backend:
   ```bash
   # Docker mode
   docker compose restart backend

   # Development mode
   mvn spring-boot:run
   ```

4. Verify migration applied:
   ```bash
   curl -s http://localhost:8081/api/info | jq .database.currentVersion
   # Should show: "006"
   ```

### Important Notes

- **Never modify existing migration files** after they've been applied to any environment
- **Migrations are irreversible** by default (no automatic rollback)
- **Always test migrations** in development before applying to production
- **Migrations run in a transaction** - if one fails, the whole migration is rolled back
- **Clean is disabled** for safety - you cannot accidentally drop all tables

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser                              │
│  ┌──────────────┐              ┌──────────────┐            │
│  │  React App   │──── OIDC ───▶│  Keycloak    │            │
│  │ (port 5173)  │◀─── JWT ─────│  (port 8080) │            │
│  └──────────────┘              └──────────────┘            │
│         │                              ▲                     │
│         │ JWT Token                    │                     │
│         │ in Authorization             │ Admin API          │
│         │ header                       │                     │
│         ▼                              │                     │
│  ┌──────────────────────────────────────────┐              │
│  │   Spring Boot Backend (port 8081)        │              │
│  │  ┌─────────────────────────────────┐    │              │
│  │  │  Spring Security JWT Filter     │    │              │
│  │  │  - Validates JWT signature      │────┼──JWK Set────┤
│  │  │  - Extracts user claims         │    │              │
│  │  │  - Populates SecurityContext    │    │              │
│  │  └─────────────────────────────────┘    │              │
│  │  ┌─────────────────────────────────┐    │              │
│  │  │  Controllers (@PreAuthorize)    │    │              │
│  │  └─────────────────────────────────┘    │              │
│  │  ┌─────────────────────────────────┐    │              │
│  │  │  Services (Business Logic)      │    │              │
│  │  └─────────────────────────────────┘    │              │
│  └──────────────────┬───────────────────────┘              │
│                     │                                        │
│                     ▼                                        │
│  ┌─────────────────────────────────────────┐               │
│  │   PostgreSQL (port 5432)                │               │
│  │  - buurman database (app data)          │               │
│  │  - keycloak database (Keycloak data)    │               │
│  └─────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

## Keycloak Realm Configuration File

The realm configuration is stored in: `keycloak/buurman-realm.json`

This file is automatically imported on Keycloak startup via:
- Volume mount: `./keycloak/buurman-realm.json:/opt/keycloak/data/import/buurman-realm.json`
- Command flag: `start-dev --import-realm`

### Modifying Realm Configuration

If you need to change the realm configuration:

1. **Option A: Modify JSON and Recreate**
   ```bash
   # Edit the file
   nano keycloak/buurman-realm.json

   # Remove Keycloak container and volume
   docker-compose down
   docker volume rm buurman_postgres_data  # Only if you want fresh start

   # Start again
   docker-compose up -d
   ```

2. **Option B: Export After Manual Changes**
   ```bash
   # Make changes in Keycloak Admin Console
   # Then export the realm
   docker exec -it buurman-keycloak \
     /opt/keycloak/bin/kc.sh export --realm buurman --dir /tmp

   # Copy the exported file
   docker cp buurman-keycloak:/tmp/buurman-realm.json ./keycloak/
   ```

## Troubleshooting

### Issue: Keycloak realm not imported

**Symptoms**: Accessing `/realms/buurman` returns 404

**Solution**:
```bash
# Check Keycloak logs
docker logs buurman-keycloak

# Look for import messages like:
# "Imported realm buurman from file /opt/keycloak/data/import/buurman-realm.json"

# If not present, check file mount
docker exec -it buurman-keycloak ls -la /opt/keycloak/data/import/

# Restart with fresh import
docker-compose restart keycloak
```

### Issue: JWT validation fails

**Symptoms**: Backend returns 401 even with valid token

**Solution**:
```bash
# Verify JWK Set is accessible
curl http://localhost:8080/realms/buurman/protocol/openid-connect/certs

# Check backend logs for JWT validation errors
# Ensure issuer URI matches: http://localhost:8080/realms/buurman
```

### Issue: User registration fails

**Symptoms**: Backend returns error when registering

**Solution**:
```bash
# Check Keycloak admin credentials
# Verify application.yml has correct Keycloak admin settings
grep -A 5 "keycloak.admin" backend/src/main/resources/application.yml

# Test Keycloak Admin API manually
curl -X POST http://localhost:8080/admin/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli"
```

### Issue: CORS errors

**Symptoms**: Browser console shows CORS errors

**Solution**:
- Verify Keycloak client has correct web origins: `http://localhost:5173`
- Check backend SecurityConfig allows CORS
- Ensure frontend uses correct API URL in `.env`

### Issue: Token refresh fails

**Symptoms**: User logged out after 5 minutes

**Solution**:
- Check AuthContext has token refresh logic (60s interval)
- Verify refresh token lifespan in Keycloak (30 minutes)
- Check browser console for refresh errors

## Next Steps

Now that authentication is set up:

1. **Test Team Invitations**:
   - Admin users can invite members via `/api/teams/{teamId}/invitations`
   - Invited users receive invitation URLs

2. **Test Role-Based Access**:
   - Create users with different roles (ADMIN, EDITOR, VIEWER)
   - Verify access control on protected endpoints

3. **Continue to Phase 1.3**:
   - Property Management implementation
   - Use authenticated endpoints with team isolation

## Security Notes

### Development vs Production

Current configuration is for **development only**:

- Keycloak uses HTTP (not HTTPS)
- Admin credentials are hardcoded
- No SSL/TLS for database connections
- LocalStack instead of real AWS S3

### Production Checklist

Before deploying to production:

- [ ] Enable HTTPS for Keycloak
- [ ] Use environment-specific secrets
- [ ] Configure proper CORS origins
- [ ] Enable database SSL/TLS
- [ ] Use real AWS S3 (not LocalStack)
- [ ] Configure proper token lifespans
- [ ] Enable email verification
- [ ] Set up SMTP for password reset
- [ ] Configure brute force protection
- [ ] Enable Keycloak events logging
- [ ] Set up backup for Keycloak database

## Additional Resources

- Keycloak Documentation: https://www.keycloak.org/documentation
- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Keycloak.js: https://www.keycloak.org/docs/latest/securing_apps/#_javascript_adapter
