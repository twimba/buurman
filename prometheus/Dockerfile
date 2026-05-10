FROM alpine:3.21 AS gettext
RUN apk add --no-cache gettext

FROM prom/prometheus:v3.11.3

USER root
COPY --from=gettext /usr/bin/envsubst /usr/local/bin/envsubst
USER nobody

COPY prometheus.yml.template /etc/prometheus/prometheus.yml.template
COPY docker-entrypoint.sh /docker-entrypoint.sh

EXPOSE 9090

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD ["wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["--config.file=/etc/prometheus/prometheus.yml", \
     "--storage.tsdb.path=/prometheus", \
     "--storage.tsdb.retention.time=30d", \
     "--web.enable-lifecycle"]
