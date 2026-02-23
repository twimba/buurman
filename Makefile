.PHONY: up dev down down-v restart restart-dev logs ps certs stats stats-by-file deploy-prod

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

scc:
	scc --gen --no-gen --min --no-min --min-gen --no-min-gen --sort complexity  --overhead 1.05  --avg-wage 70000 --sloccount-format

stats:
	scc --gen --no-gen --min --no-min --min-gen --no-min-gen --sort complexity

stats-by-file:
	scc --gen --no-gen --min --no-min --min-gen --no-min-gen --sort complexity

## Move the 'prod' tag to latest main and push to GitHub
deploy-prod:
	git fetch origin main
	git tag -f prod origin/main
	git push origin prod --force
