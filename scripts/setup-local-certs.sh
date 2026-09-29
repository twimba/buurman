#!/usr/bin/env bash
# =============================================================================
# Setup Local TLS Certificates
# =============================================================================
# Generates locally-trusted TLS certificates for *.local.buurman.io using mkcert.
# Run once after cloning the repository. Certificates are gitignored.
#
# Prerequisites: mkcert (https://github.com/FiloSottile/mkcert)
#   macOS:   brew install mkcert && mkcert -install
#   Linux:   See https://github.com/FiloSottile/mkcert#installation
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERTS_DIR="$SCRIPT_DIR/../docker/traefik/certs"
CERT_FILE="$CERTS_DIR/local.pem"
KEY_FILE="$CERTS_DIR/local-key.pem"

# Check if mkcert is installed
if ! command -v mkcert &>/dev/null; then
  echo "ERROR: mkcert is not installed."
  echo ""
  echo "Install it:"
  echo "  macOS:  brew install mkcert"
  echo "  Linux:  https://github.com/FiloSottile/mkcert#installation"
  echo ""
  echo "Then install the local CA:"
  echo "  mkcert -install"
  exit 1
fi

# Check if local CA is installed
if ! mkcert -CAROOT &>/dev/null || [ ! -f "$(mkcert -CAROOT)/rootCA.pem" ]; then
  echo "Installing local CA (may prompt for password)..."
  mkcert -install
fi

# Skip if certs already exist
if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
  echo "Certificates already exist at $CERTS_DIR"
  echo "  To regenerate, delete them and re-run this script."
  exit 0
fi

# Generate wildcard certificate
mkdir -p "$CERTS_DIR"
echo "Generating TLS certificates for *.local.buurman.io..."
mkcert \
  -cert-file "$CERT_FILE" \
  -key-file "$KEY_FILE" \
  "*.local.buurman.io" \
  "local.buurman.io"

echo ""
echo "Done! Certificates generated:"
echo "  Certificate: $CERT_FILE"
echo "  Key:         $KEY_FILE"
echo ""
echo "Start the environment with: docker compose up -d"

# =============================================================================
# Documenso e-signature sidecar: self-signed PKCS#12 signing certificate.
# Grounded against https://docs.documenso.com self-hosting docs — Documenso refuses to
# sign documents without a certificate present at startup.
# =============================================================================
DOCUMENSO_CERT_DIR="$SCRIPT_DIR/../docker/documenso"
DOCUMENSO_CERT_PATH="$DOCUMENSO_CERT_DIR/cert.p12"
if [ ! -f "$DOCUMENSO_CERT_PATH" ]; then
  mkdir -p "$DOCUMENSO_CERT_DIR"
  echo "Generating Documenso local signing certificate..."
  openssl genrsa -out /tmp/documenso-private.key 2048
  openssl req -new -x509 -key /tmp/documenso-private.key -out /tmp/documenso-cert.crt -days 365 \
    -subj "/C=NL/ST=NH/L=Amsterdam/O=Buurman Dev/OU=Engineering/CN=Buurman Dev Signing CA"
  openssl pkcs12 -export -out "$DOCUMENSO_CERT_PATH" \
    -inkey /tmp/documenso-private.key -in /tmp/documenso-cert.crt \
    -passout pass:buurman-dev
  rm /tmp/documenso-private.key /tmp/documenso-cert.crt
  echo "Documenso certificate written to $DOCUMENSO_CERT_PATH (passphrase: buurman-dev, local dev only)"
else
  echo "Documenso certificate already exists at $DOCUMENSO_CERT_PATH"
fi
