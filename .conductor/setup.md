# Conductor Workspace Setup

This project supports running multiple isolated Buurman instances in parallel via Conductor workspaces.

## Automatic Setup

When starting work in a new Conductor workspace, run the workspace setup script to configure isolated Docker services, ports, and hostnames.

### Step 1: Determine workspace number

Pick a unique number 1-9 for this workspace. Each number maps to a unique set of ports and hostnames. If other workspaces are already running, pick a number that isn't in use. You can check by looking at running Docker containers:

```bash
docker ps --format '{{.Names}}' | grep '^buurman-w' | head -5
```

### Step 2: Run setup

```bash
make workspace-setup WS=<number>
```

This generates:
- `.env` with workspace-specific ports and hostnames
- `docker/traefik/dynamic/local-dev.yml` with workspace routing
- `.env.backend` with Spring Boot environment overrides
- Keycloak realm patches for workspace redirect URIs

### Step 3: DNS entries

Ensure the workspace hostnames resolve to 127.0.0.1. Check if they already do:

```bash
ping -c1 w<N>-app.local.buurman.io
```

If not, add to `/etc/hosts`:

```
127.0.0.1 w<N>-app.local.buurman.io w<N>-api.local.buurman.io w<N>-keycloak.local.buurman.io w<N>-seaweedfs.local.buurman.io w<N>-seaweedfs-ui.local.buurman.io w<N>-mailpit.local.buurman.io w<N>-traefik.local.buurman.io w<N>-prometheus.local.buurman.io w<N>-grafana.local.buurman.io
```

### Step 4: Start services

```bash
# Start Docker infrastructure (PostgreSQL, Keycloak, SeaweedFS, etc.)
make dev

# Start backend (reads .env.backend automatically)
make backend

# Start frontend app (reads ports from .env automatically)
make frontend-app

# Start frontend backoffice (optional, reads ports from .env automatically)
make frontend-backoffice
```

### Buurman Hub (optional)

Run `make hub` to open a workspace directory dashboard at **http://localhost:3333**. It auto-detects all active workspaces, shows service links with live status (Docker/host), and refreshes every 10 seconds. Useful when running multiple workspaces simultaneously.

Port reference for each workspace number:

| WS | HTTPS | PostgreSQL | Backend | App (Vite) | Backoffice (Vite) | SMTP |
|----|-------|------------|---------|------------|-------------------|------|
| 1  | 1443  | 6432       | 8181    | 5183       | 5184              | 1026 |
| 2  | 2443  | 7432       | 8281    | 5193       | 5194              | 1027 |
| 3  | 3443  | 8432       | 8381    | 5203       | 5204              | 1028 |
| 4  | 4443  | 9432       | 8481    | 5213       | 5214              | 1029 |
| 5  | 5443  | 10432      | 8581    | 5223       | 5224              | 1030 |

URLs follow the pattern: `https://w<N>-app.local.buurman.io:<HTTPS_PORT>`

## Teardown

When done with a workspace, tear it down to free the workspace number:

```bash
make workspace-teardown
```

This:
- Stops Docker containers and removes volumes
- Resets `.env` to defaults (from `.env.example`)
- Removes `.env.backend`
- Restores `local-dev.yml` to default routing
- Cleans workspace-specific URIs from Keycloak realm configs

To keep database volumes (faster restart later):

```bash
make workspace-teardown KEEP_VOLUMES=1
```

## Important Notes

- **First start**: Keycloak imports realms from JSON on first start only. If you need to re-run setup after Keycloak has already initialized, run `make down-v` first to reset volumes.
- **TLS certificates**: The wildcard cert `*.local.buurman.io` covers all workspace hostnames. Run `make certs` if certificates don't exist yet.
- **Base workspace**: The default (non-workspace) setup uses standard ports (443, 5432, 8081, etc.) and hostnames (`app.local.buurman.io`). No setup script needed.
