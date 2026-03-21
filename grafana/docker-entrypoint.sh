#!/bin/sh
set -e

envsubst < /etc/grafana/provisioning/datasources/prometheus.yml.template \
         > /etc/grafana/provisioning/datasources/prometheus.yml

exec /run.sh "$@"
