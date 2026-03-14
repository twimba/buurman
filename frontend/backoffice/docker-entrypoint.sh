#!/bin/sh
set -e

# Generate runtime config from environment variables
cat > /usr/share/nginx/html/config.js <<EOF
window.__CONFIG__ = {
  VITE_BACKOFFICE_KEYCLOAK_URL: "${VITE_BACKOFFICE_KEYCLOAK_URL:-}",
  VITE_BACKOFFICE_KEYCLOAK_REALM: "${VITE_BACKOFFICE_KEYCLOAK_REALM:-}",
  VITE_BACKOFFICE_KEYCLOAK_CLIENT_ID: "${VITE_BACKOFFICE_KEYCLOAK_CLIENT_ID:-}",
  VITE_ENVIRONMENT: "${VITE_ENVIRONMENT:-local}"
};
EOF

exec "$@"
