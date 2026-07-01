FROM prom/prometheus:v3.13.0

# Render config at startup with busybox sed (see docker-entrypoint.sh) — no envsubst/gettext needed.
USER root
COPY prometheus.yml.template /etc/prometheus/prometheus.yml.template
COPY docker-entrypoint.sh /docker-entrypoint.sh
# Make the entrypoint executable and let the runtime (nobody) user write the rendered config.
RUN chmod +x /docker-entrypoint.sh && chown -R nobody /etc/prometheus
USER nobody

EXPOSE 9090

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD ["wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["--config.file=/etc/prometheus/prometheus.yml", \
     "--storage.tsdb.path=/prometheus", \
     "--storage.tsdb.retention.time=30d", \
     "--web.enable-lifecycle"]
