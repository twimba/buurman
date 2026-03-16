.PHONY: up dev down down-v restart restart-dev logs ps certs stats deploy-prod generate-api bundle-openapi backend

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

## Run backend locally
backend:
	cd backend && mvn spring-boot:run -pl app -am

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
		-e '      write text "cd /Users/luis.santos/projects/buurman/backend && sleep 10 && mvn -pl app -am spring-boot:run"' \
		-e '      set frontendPane to (split horizontally with default profile)' \
		-e '    end tell' \
		-e '    tell frontendPane' \
		-e '      write text "cd /Users/luis.santos/projects/buurman/frontend && yarn dev"' \
		-e '    end tell' \
		-e '  end tell' \
		-e 'end tell'
