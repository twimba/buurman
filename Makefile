.PHONY: up dev down down-v restart restart-dev logs ps certs stats deploy-prod generate-api bundle-openapi backend frontend-app frontend-backoffice workspace-setup workspace-teardown test test-coverage test-bdd test-bdd-smoke test-bdd-run hub

## Start everything in Docker (including backend + app containers)
up:
	docker compose up -d

## Start infrastructure only (for local backend/app development)
dev:
	docker compose up -d --scale backend=0 --scale app=0 --scale backoffice=0

dev-fg:
	docker compose up --scale backend=0 --scale app=0 --scale backoffice=0

## Stop all containers
down:
	docker compose down

## Stop all containers and remove volumes (full reset)
down-v:
	docker compose down -v

## Restart everything in Docker
restart: down up

## Restart infrastructure only
restart-dev: down dev

## Tail logs (all services, follow)
logs:
	docker compose logs -f

## Show running containers
ps:
	docker compose ps

## Generate local TLS certificates (one-time setup)
certs:
	bash scripts/setup-local-certs.sh

stats:
	scc --gen --no-gen --min --no-min --min-gen --no-min-gen --sort complexity  --avg-wage 100000 --sloccount-format --cocomo-project-type "ai-solo,0.25,1.03,1.0,1.0"

## Run backend unit tests
test:
	cd backend && mvn test -pl common,buurman-core,buurman-notifications,buurman-booklets -Pquick -Dmaven.build.cache.enabled=false

## Run backend unit tests with JaCoCo coverage report (per-module + aggregated)
test-coverage:
	cd backend && mvn verify -pl common,buurman-core,buurman-notifications,buurman-booklets,coverage-report -Pquick,coverage -Dmaven.build.cache.enabled=false
	@echo ""
	@echo "Coverage reports:"
	@echo "  Aggregated:          backend/coverage-report/target/site/jacoco-aggregate/index.html"
	@echo "  common:              backend/common/target/site/jacoco/index.html"
	@echo "  buurman-core:        backend/buurman-core/target/site/jacoco/index.html"
	@echo "  buurman-notifications: backend/buurman-notifications/target/site/jacoco/index.html"
	@echo "  buurman-booklets:    backend/buurman-booklets/target/site/jacoco/index.html"

## Run backend locally (sources workspace env overrides if present)
## Two-phase: install all modules with build cache, then run app module only.
## spring-boot:run forks a lifecycle that bypasses the build cache, so we compile
## separately to avoid re-running JOOQ/OpenAPI codegen on every restart.
backend:
	@set -a; [ -f .env ] && . ./.env; set +a; \
	if [ -f .env.backend ]; then \
		echo "Sourcing workspace backend config from .env.backend"; \
		. ./.env.backend && cd backend && mvn install -pl buurman-app -am -DskipTests -Pquick && mvn spring-boot:run -pl buurman-app; \
	else \
		cd backend && mvn install -pl buurman-app -am -DskipTests -Pquick && mvn spring-boot:run -pl buurman-app; \
	fi

## Run frontend app locally (loads .env for VITE_* variables)
frontend-app:
	@set -a; [ -f .env ] && . ./.env; set +a; \
	VITE_DEV_PORT=$${LOCAL_APP_PORT:-5173}; \
	VITE_HMR_PORT=$${HTTPS_PORT:-443}; \
	echo "Starting app on port $$VITE_DEV_PORT (HMR via $$VITE_HMR_PORT)"; \
	cd frontend && VITE_DEV_PORT=$$VITE_DEV_PORT VITE_HMR_PORT=$$VITE_HMR_PORT yarn dev:app

## Run frontend backoffice locally (loads .env for VITE_* variables)
frontend-backoffice:
	@set -a; [ -f .env ] && . ./.env; set +a; \
	VITE_DEV_PORT=$${LOCAL_BACKOFFICE_PORT:-5174}; \
	VITE_HMR_PORT=$${HTTPS_PORT:-443}; \
	echo "Starting backoffice on port $$VITE_DEV_PORT (HMR via $$VITE_HMR_PORT)"; \
	cd frontend && VITE_DEV_PORT=$$VITE_DEV_PORT VITE_HMR_PORT=$$VITE_HMR_PORT yarn dev:backoffice

## Buurman Hub — workspace directory & service status dashboard (http://localhost:3333)
hub:
	python3 scripts/hub/server.py

BDD_PYTHON := tests/bdd/.venv/bin/python3
BDD_PYTEST := $(BDD_PYTHON) -m pytest -c tests/bdd/pyproject.toml --rootdir=tests/bdd

# Build workspace-aware BDD URLs from .env (HOSTNAME_PREFIX, HTTPS_PORT)
# Falls back to defaults if .env is missing or vars are unset
-include .env
BDD_HP    := $(HOSTNAME_PREFIX)
BDD_PORT  := $(or $(HTTPS_PORT),443)
BDD_PORT_SUFFIX := $(if $(filter 443,$(BDD_PORT)),,$(addprefix :,$(BDD_PORT)))
BDD_ENV   := BDD_API_URL=https://$(BDD_HP)api.local.buurman.io$(BDD_PORT_SUFFIX) \
             BDD_KEYCLOAK_URL=https://$(BDD_HP)keycloak.local.buurman.io$(BDD_PORT_SUFFIX) \
             BDD_MAILPIT_URL=https://$(BDD_HP)mailpit.local.buurman.io$(BDD_PORT_SUFFIX)

## Run BDD behavior tests against the live system (requires `make up` first)
test-bdd:
	$(BDD_ENV) $(BDD_PYTEST)

## Run BDD smoke tests only (fast subset for local development)
test-bdd-smoke:
	$(BDD_ENV) $(BDD_PYTEST) -m smoke

## Start the system, wait for readiness, run BDD tests, then stop everything
test-bdd-run:
	@echo "Starting Docker containers..."
	docker compose up -d
	@echo "Waiting for backend to be ready..."
	@timeout=180; elapsed=0; \
	while ! curl -sf -o /dev/null https://$(BDD_HP)api.local.buurman.io$(BDD_PORT_SUFFIX)/actuator/health --insecure 2>/dev/null; do \
		elapsed=$$((elapsed + 3)); \
		if [ $$elapsed -ge $$timeout ]; then \
			echo "Timed out waiting for backend after $${timeout}s"; \
			docker compose logs backend --tail=50; \
			exit 1; \
		fi; \
		printf "\r  Waiting... %ds / %ds" $$elapsed $$timeout; \
		sleep 3; \
	done
	@echo "\nSystem ready. Running BDD tests..."
	$(BDD_ENV) $(BDD_PYTEST) -v; rc=$$?; \
	echo ""; \
	if [ $$rc -eq 0 ]; then echo "All tests passed."; else echo "Some tests failed (exit code $$rc)."; fi; \
	exit $$rc

backend-upgradable-dependencies:
	mvn versions:display-dependency-updates -DallowMajorUpdates=false -Dversions.outputLineWidth=145 -Dmaven.version.ignore='(?i).*-(alpha|beta|rc|m)([-.]?\d+)?' -DprocessDependencyManagementTransitive=false

## Generate TypeScript API clients from OpenAPI specs
## Bundle split OpenAPI source files into openapi/app.yaml
bundle-openapi:
	python3 scripts/bundle_openapi.py

generate-api:
	cd frontend && yarn generate:api

deploy-prod:
	git fetch origin main
	git tag -f prod origin/main
	git push origin prod --force

## Configure this directory as a parallel workspace (1-9)
## Usage: make workspace-setup WS=1
workspace-setup:
	@if [ -z "$(WS)" ]; then echo "Usage: make workspace-setup WS=<1-9>"; exit 1; fi
	bash scripts/setup-workspace.sh $(WS)

## Tear down the current workspace (stop containers, reset config to defaults)
## Usage: make workspace-teardown [KEEP_VOLUMES=1]
workspace-teardown:
	@if [ -n "$(KEEP_VOLUMES)" ]; then \
		bash scripts/teardown-workspace.sh --keep-volumes; \
	else \
		bash scripts/teardown-workspace.sh; \
	fi

## Open iTerm2 tab with 3 panes: infrastructure (top), backend (middle), frontend (bottom)
local:
	colima start
	@osascript \
		-e 'tell application "iTerm2"' \
		-e '  tell current window' \
		-e '    set newTab to (create tab with default profile)' \
		-e '    tell current session of newTab' \
		-e '      write text "cd /Users/luis.santos/projects/buurman && make dev-fg"' \
		-e '      set backendPane to (split horizontally with default profile)' \
		-e '    end tell' \
		-e '    tell backendPane' \
		-e '      write text "cd /Users/luis.santos/projects/buurman/backend && sleep 10 && mvn install -pl buurman-app -am -DskipTests -Pquick && mvn spring-boot:run -pl buurman-app"' \
		-e '      set frontendPane to (split horizontally with default profile)' \
		-e '    end tell' \
		-e '    tell frontendPane' \
		-e '      write text "cd /Users/luis.santos/projects/buurman/frontend && yarn dev"' \
		-e '    end tell' \
		-e '  end tell' \
		-e 'end tell'
