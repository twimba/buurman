#!/bin/sh
set -e

# Render prometheus.yml from the template without envsubst (the prom/prometheus base image has no
# gettext, and a copied dynamically-linked envsubst fails to load its musl/libintl deps). busybox
# sed is always present. We use @@TOKEN@@ placeholders (no braces) so busybox BRE never confuses
# them with intervals and the relabel '${1}' is left untouched.
: "${BACKEND_METRICS_TARGET:=backend:8082}"
: "${POSTGRES_EXPORTER_TARGET:=postgres-exporter:9187}"
: "${HETZNER_BEARER_TOKEN:=}"

sed \
  -e "s|@@BACKEND_METRICS_TARGET@@|${BACKEND_METRICS_TARGET}|g" \
  -e "s|@@POSTGRES_EXPORTER_TARGET@@|${POSTGRES_EXPORTER_TARGET}|g" \
  -e "s|@@HETZNER_BEARER_TOKEN@@|${HETZNER_BEARER_TOKEN}|g" \
  /etc/prometheus/prometheus.yml.template > /etc/prometheus/prometheus.yml

# CMD is just prometheus flags (no program name), so exec the binary explicitly.
exec /bin/prometheus "$@"
