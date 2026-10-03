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

# Skip if certs already exist — but NOT via an early `exit`: this script also generates the
# unrelated Documenso signing certificate below, and an early exit here would (did) skip that
# block entirely for anyone who already had the TLS certs from a previous run.
if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
  echo "TLS certificates already exist at $CERTS_DIR"
  echo "  To regenerate, delete them and re-run this script."
else
  # Generate wildcard certificate
  mkdir -p "$CERTS_DIR"
  echo "Generating TLS certificates for *.local.buurman.io..."
  mkcert \
    -cert-file "$CERT_FILE" \
    -key-file "$KEY_FILE" \
    "*.local.buurman.io" \
    "local.buurman.io"

  echo ""
  echo "Done! TLS certificates generated:"
  echo "  Certificate: $CERT_FILE"
  echo "  Key:         $KEY_FILE"
fi

# =============================================================================
# Documenso e-signature sidecar: self-signed PKCS#12 signing certificate.
# Grounded against https://docs.documenso.com self-hosting docs — Documenso refuses to
# sign documents without a certificate present at startup.
# =============================================================================
DOCUMENSO_CERT_DIR="$SCRIPT_DIR/../docker/documenso"
DOCUMENSO_CERT_PATH="$DOCUMENSO_CERT_DIR/cert.p12"
# docker-compose silently creates an empty directory at a bind-mount source path that doesn't
# exist yet — if `documenso` ever starts before this script runs, that's exactly what happens
# here. An empty directory isn't a file, so the check below would try to generate into it and
# openssl would fail (can't write a file where a directory already exists) — clear it first.
if [ -d "$DOCUMENSO_CERT_PATH" ]; then
  echo "Removing empty placeholder directory at $DOCUMENSO_CERT_PATH (docker-compose artifact, not a real certificate)"
  rmdir "$DOCUMENSO_CERT_PATH"
fi
if [ ! -f "$DOCUMENSO_CERT_PATH" ]; then
  mkdir -p "$DOCUMENSO_CERT_DIR"
  echo "Generating Documenso local signing certificate..."
  # A private key must never land on a predictable path: a fixed /tmp name is pre-creatable and
  # readable by any local user. mktemp -d gives a 0700 directory with an unguessable name, and the
  # trap removes it on any exit path (including the set -e failure of an openssl step).
  DOCUMENSO_TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/documenso-signing-XXXXXX")"
  trap 'rm -rf "$DOCUMENSO_TMP_DIR"' EXIT
  openssl genrsa -out "$DOCUMENSO_TMP_DIR/private.key" 2048
  openssl req -new -x509 -key "$DOCUMENSO_TMP_DIR/private.key" \
    -out "$DOCUMENSO_TMP_DIR/cert.crt" -days 365 \
    -subj "/C=NL/ST=NH/L=Amsterdam/O=Buurman Dev/OU=Engineering/CN=Buurman Dev Signing CA"
  openssl pkcs12 -export -out "$DOCUMENSO_CERT_PATH" \
    -inkey "$DOCUMENSO_TMP_DIR/private.key" -in "$DOCUMENSO_TMP_DIR/cert.crt" \
    -passout pass:buurman-dev
  rm -rf "$DOCUMENSO_TMP_DIR"
  trap - EXIT
  echo "Documenso certificate written to $DOCUMENSO_CERT_PATH (passphrase: buurman-dev, local dev only)"
else
  echo "Documenso certificate already exists at $DOCUMENSO_CERT_PATH"
fi

echo "Start the environment with: docker compose up -d"
