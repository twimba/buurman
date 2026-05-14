#!/bin/bash
set -euo pipefail

echo "Starting workspace setup..."

# ---------------------------------------------------------------------------
# Step 1: Find workspace number — reuse existing lock or pick first available
# ---------------------------------------------------------------------------
LOCK_DIR="$HOME/.buurman/workspaces"
mkdir -p "$LOCK_DIR"
PROJECT_DIR="$(pwd)"

# Check if this directory already owns a workspace lock
WS=""
for n in $(seq 1 9); do
  if [ -f "$LOCK_DIR/w${n}.lock" ] && [ "$(cat "$LOCK_DIR/w${n}.lock")" = "$PROJECT_DIR" ]; then
    WS=$n
    echo "Re-using existing workspace $WS (already reserved by this directory)"
    break
  fi
done

# If not found, pick the first available number
if [ -z "$WS" ]; then
  for n in $(seq 1 9); do
    if [ ! -f "$LOCK_DIR/w${n}.lock" ]; then
      WS=$n
      break
    fi
  done
fi

if [ -z "$WS" ]; then
  echo "ERROR: All workspace numbers (1-9) are reserved."
  echo "Active reservations:"
  for n in $(seq 1 9); do
    if [ -f "$LOCK_DIR/w${n}.lock" ]; then
      echo "  w${n}: $(cat "$LOCK_DIR/w${n}.lock")"
    fi
  done
  exit 1
fi

echo "Selected workspace number: $WS"

# ---------------------------------------------------------------------------
# Step 2: Run make workspace-setup (generates .env, traefik config, etc.)
# ---------------------------------------------------------------------------
echo "Running workspace setup for WS=$WS..."
make workspace-setup WS="$WS"

# Read generated ports from .env (tail -1: .env has base + workspace override, take the last)
HTTPS_PORT=$(grep '^HTTPS_PORT=' .env | tail -1 | cut -d= -f2)
PG_HOST_PORT=$(grep '^PG_HOST_PORT=' .env | tail -1 | cut -d= -f2)
BACKEND_HOST_PORT=$(grep '^BACKEND_HOST_PORT=' .env 2>/dev/null | tail -1 | cut -d= -f2 || echo "$(( 8081 + WS * 100 ))")
LOCAL_APP_PORT=$(grep '^LOCAL_APP_PORT=' .env | tail -1 | cut -d= -f2)
LOCAL_BACKOFFICE_PORT=$(grep '^LOCAL_BACKOFFICE_PORT=' .env | tail -1 | cut -d= -f2)
SMTP_HOST_PORT=$(grep '^SMTP_HOST_PORT=' .env | tail -1 | cut -d= -f2)
HP="w${WS}-"

echo ""
echo "─────────────────────────────────────────────────────────────────────────────"
echo "Workspace $WS setup complete."
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""

# ---------------------------------------------------------------------------
# Colors & formatting (using $'...' so variables hold actual escape bytes)
# ---------------------------------------------------------------------------
BOLD=$'\033[1m'
DIM=$'\033[2m'
RESET=$'\033[0m'
GREEN=$'\033[0;32m'
CYAN=$'\033[0;36m'
YELLOW=$'\033[0;33m'
MAGENTA=$'\033[0;35m'
BLUE=$'\033[0;34m'
WHITE=$'\033[1;37m'

# Repeat character $1 exactly $2 times
rep() { local i s=""; for ((i=0; i<$2; i++)); do s+="$1"; done; printf '%s' "$s"; }

# ---------------------------------------------------------------------------
# Table dimensions — all tables are 77 display columns wide
#   Header:      ║ + 75 + ║ = 77
#   Services:    │16│58│ = 77   (text areas: 14, 56)
#   Credentials: │18│39│16│ = 77   (text: 16, 37, 14)
#   Demo Users:  │32│42│ = 77   (text: 30, 40)
#   Quick Start: │26│48│ = 77   (text: 24, 46)
# ---------------------------------------------------------------------------
HW=75
SLT=14; SRT=56
C1T=16; C2T=37; C3T=14
DLT=30; DRT=40
QLT=24; QRT=46

H_EQ=$(rep '═' $HW); H_SP=$(rep ' ' $HW)
SL_D=$(rep '─' 16);  SR_D=$(rep '─' 58)
C1_D=$(rep '─' 18);  C2_D=$(rep '─' 39);  C3_D=$(rep '─' 16)
DL_D=$(rep '─' 32);  DR_D=$(rep '─' 42)
QL_D=$(rep '─' 26);  QR_D=$(rep '─' 48)
DIV=$(rep '─' $HW)

# Service row: emoji (2 display cols) + space + name, right-padded to SLT
svc() {
  local emoji="$1" name="$2" url="$3" note="${4:-}"
  local lpad=$(( SLT - 2 - 1 - ${#name} ))
  local rpad=$(( SRT - ${#url} - ${#note} ))
  printf "  ${DIM}│${RESET} %s %s%*s ${DIM}│${RESET} ${WHITE}%s${RESET}${DIM}%s${RESET}%*s ${DIM}│${RESET}\n" \
      "$emoji" "$name" "$lpad" "" "$url" "$note" "$rpad" ""
}

# Demo user row: plain ASCII email (left), emoji roles with width adjustment (right)
demo() {
  local email="$1" roles="$2" emojis="${3:-0}"
  local rpad=$(( DRT - ${#roles} - emojis ))
  printf "  ${DIM}│${RESET} %-${DLT}s ${DIM}│${RESET} %s%*s ${DIM}│${RESET}\n" \
      "$email" "$roles" "$rpad" ""
}

# =========================================================================
# Header
# =========================================================================
TITLE="Buurman Workspace ${WS} — Ready!"
TPAD=$(( HW - 6 - ${#TITLE} ))  # 6 = "  🏠  " prefix (2 spaces + emoji@2cols + 2 spaces)

echo ""
printf "  ${GREEN}${BOLD}╔%s╗${RESET}\n" "$H_EQ"
printf "  ${GREEN}${BOLD}║%s║${RESET}\n" "$H_SP"
printf "  ${GREEN}${BOLD}║  🏠  %s%*s║${RESET}\n" "$TITLE" "$TPAD" ""
printf "  ${GREEN}${BOLD}║%s║${RESET}\n" "$H_SP"
printf "  ${GREEN}${BOLD}╚%s╝${RESET}\n" "$H_EQ"
echo ""

# =========================================================================
# Services
# =========================================================================
printf "  ${CYAN}${BOLD}🌐 Services${RESET}\n"
printf "  ${DIM}┌%s┬%s┐${RESET}\n" "$SL_D" "$SR_D"
svc "💻" "App"        "https://${HP}app.local.buurman.io:${HTTPS_PORT}"
svc "🔧" "Backoffice" "https://${HP}backoffice.local.buurman.io:${HTTPS_PORT}"
svc "⚡" "API"        "https://${HP}api.local.buurman.io:${HTTPS_PORT}"
svc "🔑" "Keycloak"   "https://${HP}keycloak.local.buurman.io:${HTTPS_PORT}"
svc "🔀" "Traefik"    "https://${HP}traefik.local.buurman.io:${HTTPS_PORT}"
svc "📬" "Mailpit"    "https://${HP}mailpit.local.buurman.io:${HTTPS_PORT}" "  (SMTP: ${SMTP_HOST_PORT})"
svc "📦" "AWRust"     "https://${HP}awrust.local.buurman.io:${HTTPS_PORT}"
svc "📈" "Prometheus" "https://${HP}prometheus.local.buurman.io:${HTTPS_PORT}"
svc "📊" "Grafana"    "https://${HP}grafana.local.buurman.io:${HTTPS_PORT}"
svc "🐘" "PostgreSQL" "localhost:${PG_HOST_PORT}" "  (buurman/buurman)"
printf "  ${DIM}└%s┴%s┘${RESET}\n" "$SL_D" "$SR_D"
echo ""

# =========================================================================
# Default Credentials
# =========================================================================
printf "  ${YELLOW}${BOLD}🔐 Default Credentials${RESET}\n"
printf "  ${DIM}┌%s┬%s┬%s┐${RESET}\n" "$C1_D" "$C2_D" "$C3_D"
printf "  ${DIM}│${RESET} ${BOLD}%-${C1T}s${RESET} ${DIM}│${RESET} ${BOLD}%-${C2T}s${RESET} ${DIM}│${RESET} ${BOLD}%-${C3T}s${RESET} ${DIM}│${RESET}\n" "Service" "Username" "Password"
printf "  ${DIM}├%s┼%s┼%s┤${RESET}\n" "$C1_D" "$C2_D" "$C3_D"
printf "  ${DIM}│${RESET} %-${C1T}s ${DIM}│${RESET} %-${C2T}s ${DIM}│${RESET} %-${C3T}s ${DIM}│${RESET}\n" "Keycloak Admin" "admin" "admin"
printf "  ${DIM}│${RESET} %-${C1T}s ${DIM}│${RESET} %-${C2T}s ${DIM}│${RESET} %-${C3T}s ${DIM}│${RESET}\n" "PostgreSQL" "admin" "admin"
printf "  ${DIM}└%s┴%s┴%s┘${RESET}\n" "$C1_D" "$C2_D" "$C3_D"
echo ""

# =========================================================================
# Demo Users
# =========================================================================
printf "  ${MAGENTA}${BOLD}👥 Demo Users${RESET}  ${DIM}(password = email – demo.user@demo.buurman.io == buurman)${RESET}\n"
printf "  ${DIM}┌%s┬%s┐${RESET}\n" "$DL_D" "$DR_D"
printf "  ${DIM}│${RESET} ${BOLD}%-${DLT}s${RESET} ${DIM}│${RESET} ${BOLD}%-${DRT}s${RESET} ${DIM}│${RESET}\n" "Email" "Roles"
printf "  ${DIM}├%s┼%s┤${RESET}\n" "$DL_D" "$DR_D"
demo "demo.user@demo.buurman.io"    "👑 demo-team 📝 team-alpha 👀 team-beta" 3
demo "admin@demo.buurman.io"        "👑 team-alpha" 1
demo "editor@demo.buurman.io"       "📝 team-alpha" 1
demo "viewer@demo.buurman.io"       "👀 team-alpha" 1
demo "admin.team2@demo.buurman.io"  "👑 team-beta" 1
demo "editor.team2@demo.buurman.io" "📝 team-beta" 1
demo "viewer.team2@demo.buurman.io" "👀 team-beta" 1
printf "  ${DIM}└%s┴%s┘${RESET}\n" "$DL_D" "$DR_D"
echo ""

# =========================================================================
# Quick Start
# =========================================================================
printf "  ${BLUE}${BOLD}🚀 Quick Start${RESET}\n"
printf "  ${DIM}┌%s┬%s┐${RESET}\n" "$QL_D" "$QR_D"
printf "  ${DIM}│${RESET} ${GREEN}%-${QLT}s${RESET} ${DIM}│${RESET} %-${QRT}s ${DIM}│${RESET}\n" "make dev"                  "Start infra (Traefik, DB, Keycloak...)"
printf "  ${DIM}│${RESET} ${GREEN}%-${QLT}s${RESET} ${DIM}│${RESET} %-${QRT}s ${DIM}│${RESET}\n" "make backend"              "Start Spring Boot (port ${BACKEND_HOST_PORT})"
printf "  ${DIM}│${RESET} ${GREEN}%-${QLT}s${RESET} ${DIM}│${RESET} %-${QRT}s ${DIM}│${RESET}\n" "make frontend-app"         "Start Vite app (port ${LOCAL_APP_PORT})"
printf "  ${DIM}│${RESET} ${GREEN}%-${QLT}s${RESET} ${DIM}│${RESET} %-${QRT}s ${DIM}│${RESET}\n" "make frontend-backoffice"  "Start Vite backoffice (port ${LOCAL_BACKOFFICE_PORT})"
printf "  ${DIM}│${RESET} ${GREEN}%-${QLT}s${RESET} ${DIM}│${RESET} %-${QRT}s ${DIM}│${RESET}\n" "make hub"                  "Workspace dashboard (http://localhost:3333)"
printf "  ${DIM}├%s┼%s┤${RESET}\n" "$QL_D" "$QR_D"
printf "  ${DIM}│${RESET} ${GREEN}%-${QLT}s${RESET} ${DIM}│${RESET} %-${QRT}s ${DIM}│${RESET}\n" "make test-bdd-run"         "Start system + run BDD tests (one command)"
printf "  ${DIM}│${RESET} ${GREEN}%-${QLT}s${RESET} ${DIM}│${RESET} %-${QRT}s ${DIM}│${RESET}\n" "make test-bdd"             "Run BDD tests (system must be running)"
printf "  ${DIM}│${RESET} ${GREEN}%-${QLT}s${RESET} ${DIM}│${RESET} %-${QRT}s ${DIM}│${RESET}\n" "make test-bdd-smoke"       "Run BDD smoke tests only (fast)"
printf "  ${DIM}└%s┴%s┘${RESET}\n" "$QL_D" "$QR_D"
echo ""

printf "  ${DIM}%s${RESET}\n" "$DIV"
printf "  ${GREEN}${BOLD}✅ All done!${RESET} ${DIM}Happy landlording 🏠${RESET}\n"
printf "  ${DIM}%s${RESET}\n" "$DIV"
echo ""
