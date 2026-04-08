FROM keycloak/keycloak:26.6.0

COPY themes /opt/keycloak/themes
COPY buurman-realm.json /opt/keycloak/data/import/
COPY buurman-backoffice-realm.json /opt/keycloak/data/import/

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/localhost/9000 && echo -e 'GET /health/ready HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3 && grep -q '\"status\": \"UP\"' <&3"]

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--import-realm", "--health-enabled=true", "--metrics-enabled=true"]
