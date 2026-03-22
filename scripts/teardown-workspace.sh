#!/usr/bin/env bash
# =============================================================================
# Teardown Parallel Workspace
# =============================================================================
# Shuts down the current workspace's Docker services and resets configuration
# back to defaults, freeing the workspace number for use in another worktree.
#
# Usage: ./scripts/teardown-workspace.sh [--keep-volumes]
#
# Options:
#   --keep-volumes  Don't remove Docker volumes (preserves database data)
#
# Without --keep-volumes, runs `docker compose down -v` for a clean slate.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

KEEP_VOLUMES=false
for arg in "$@"; do
  case "$arg" in
    --keep-volumes) KEEP_VOLUMES=true ;;
    *) echo "Unknown option: $arg"; echo "Usage: teardown-workspace.sh [--keep-volumes]"; exit 1 ;;
  esac
done

# --- Detect current workspace from .env ---
WS=""
PROJECT_NAME=""
if [ -f "$PROJECT_DIR/.env" ]; then
  PROJECT_NAME=$(grep '^COMPOSE_PROJECT_NAME=' "$PROJECT_DIR/.env" | tail -1 | cut -d= -f2)
  if [[ "$PROJECT_NAME" =~ ^buurman-w([1-9])$ ]]; then
    WS="${BASH_REMATCH[1]}"
  fi
fi

if [ -z "$WS" ]; then
  echo "No workspace detected (COMPOSE_PROJECT_NAME is '${PROJECT_NAME:-buurman}')."
  echo "This directory appears to be using the default (non-workspace) configuration."
  echo "Nothing to tear down."
  exit 0
fi

echo "=== Tearing Down Workspace ${WS} ==="
echo ""

# =============================================================================
# 1. Stop Docker containers
# =============================================================================
echo "Stopping Docker containers (${PROJECT_NAME})..."

cd "$PROJECT_DIR"
if $KEEP_VOLUMES; then
  docker compose down 2>/dev/null || true
  echo "  Containers stopped (volumes preserved)"
else
  docker compose down -v 2>/dev/null || true
  echo "  Containers stopped and volumes removed"
fi

# =============================================================================
# 2. Restore .env to defaults
# =============================================================================
echo "Restoring .env to defaults..."

cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"
echo "  .env reset from .env.example"

# =============================================================================
# 3. Remove .env.backend
# =============================================================================
if [ -f "$PROJECT_DIR/.env.backend" ]; then
  rm "$PROJECT_DIR/.env.backend"
  echo "  .env.backend removed"
fi

# =============================================================================
# 4. Reset tracked files modified by setup-workspace.sh
# =============================================================================
echo "Resetting tracked files to HEAD..."

TRACKED_FILES=(
  "docker/traefik/dynamic/local-dev.yml"
  "keycloak/buurman-realm.json"
  "keycloak/buurman-backoffice-realm.json"
)

for f in "${TRACKED_FILES[@]}"; do
  if [ -f "$PROJECT_DIR/$f" ]; then
    git -C "$PROJECT_DIR" checkout HEAD -- "$f" 2>/dev/null && echo "  $f restored" || echo "  $f (no changes to reset)"
  fi
done

# =============================================================================
# 5. Release workspace lock
# =============================================================================
LOCK_DIR="$HOME/.buurman/workspaces"
LOCK_FILE="$LOCK_DIR/w${WS}.lock"

if [ -f "$LOCK_FILE" ]; then
  rm "$LOCK_FILE"
  echo "  Workspace ${WS} lock released"
fi

# =============================================================================
# 6. Clean up backup file
# =============================================================================
if [ -f "$PROJECT_DIR/.env.bak" ]; then
  rm "$PROJECT_DIR/.env.bak"
  echo "  .env.bak removed"
fi

# =============================================================================
# 7. Summary
# =============================================================================
echo ""
echo "=== Teardown Complete ==="
echo ""
echo "  Workspace ${WS} has been shut down and configuration reset to defaults."
echo "  This worktree (or another) can now run: ./scripts/setup-workspace.sh <1-9>"
echo ""
