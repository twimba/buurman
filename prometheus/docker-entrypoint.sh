#!/bin/sh
set -e

envsubst < /etc/prometheus/prometheus.yml.template > /etc/prometheus/prometheus.yml

exec "$@"
