FROM grafana/grafana:12.4.1

USER root
RUN apk add --no-cache gettext
USER grafana

ENV PROMETHEUS_URL=https://prometheus.buurman.io

COPY --chown=grafana:root provisioning /etc/grafana/provisioning
COPY dashboards /var/lib/grafana/dashboards
COPY docker-entrypoint.sh /docker-entrypoint.sh

EXPOSE 3000

HEALTHCHECK --interval=15s --timeout=5s --start-period=15s --retries=3 \
  CMD ["wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/api/health"]

ENTRYPOINT ["/docker-entrypoint.sh"]
