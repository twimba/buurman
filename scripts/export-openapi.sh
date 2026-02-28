#!/bin/bash
# Export OpenAPI specs from the running backend server.
# Usage: bash scripts/export-openapi.sh [base_url]
# Default base URL: https://api.local.buurman.io
set -euo pipefail

BASE_URL="${1:-https://api.local.buurman.io}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/../openapi"

mkdir -p "$OUTPUT_DIR"

echo "Fetching app API spec from $BASE_URL/api-docs/openapi/app..."
curl -sk "$BASE_URL/api-docs/openapi/app" | jq '.' > "$OUTPUT_DIR/app.json"

echo "Fetching backoffice API spec from $BASE_URL/api-docs/openapi/backoffice..."
curl -sk "$BASE_URL/api-docs/openapi/backoffice" | jq '.' > "$OUTPUT_DIR/backoffice.json"

echo "Exported to:"
echo "  $OUTPUT_DIR/app.json"
echo "  $OUTPUT_DIR/backoffice.json"
