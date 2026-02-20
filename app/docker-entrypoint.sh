#!/bin/sh
set -e

# Generate runtime config from environment variables
cat > /usr/share/nginx/html/config.js <<EOF
window.__CONFIG__ = {
  VITE_KEYCLOAK_URL: "${VITE_KEYCLOAK_URL:-}",
  VITE_KEYCLOAK_REALM: "${VITE_KEYCLOAK_REALM:-}",
  VITE_KEYCLOAK_CLIENT_ID: "${VITE_KEYCLOAK_CLIENT_ID:-}",
  VITE_GOOGLE_MAPS_API_KEY: "${VITE_GOOGLE_MAPS_API_KEY:-}",
  VITE_ENVIRONMENT: "${VITE_ENVIRONMENT:-local}"
};
EOF

exec "$@"
