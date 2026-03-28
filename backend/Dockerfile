# === Dev stage: builds from source at startup (docker-compose) ===
# Build context is the repo root (not ./backend) so openapi/ is accessible.
FROM maven:3-eclipse-temurin-25-alpine AS dev

WORKDIR /app

RUN apk add --no-cache docker-cli wget

# Copy all POMs first for dependency caching
COPY backend/pom.xml .
COPY backend/common/pom.xml common/
COPY backend/jooq/pom.xml jooq/
COPY backend/buurman-core/pom.xml buurman-core/
COPY backend/buurman-notifications/pom.xml buurman-notifications/
COPY backend/buurman-backoffice/pom.xml buurman-backoffice/
COPY backend/buurman-demo-data/pom.xml buurman-demo-data/
COPY backend/buurman-takeout/pom.xml buurman-takeout/
COPY backend/buurman-booklets/pom.xml buurman-booklets/
COPY backend/buurman-app/pom.xml buurman-app/
COPY backend/.mvn .mvn
COPY backend/lombok.config .
RUN mvn dependency:go-offline -B -fn -Dmaven.build.cache.enabled=false

# Copy OpenAPI specs (referenced by buurman-core and buurman-backoffice pom.xml)
COPY openapi/app.yaml openapi/backoffice.yaml ../openapi/

# Copy source
COPY backend/common/src common/src
COPY backend/jooq/src jooq/src
COPY backend/buurman-core/src buurman-core/src
COPY backend/buurman-notifications/src buurman-notifications/src
COPY backend/buurman-backoffice/src buurman-backoffice/src
COPY backend/buurman-demo-data/src buurman-demo-data/src
COPY backend/buurman-takeout/src buurman-takeout/src
COPY backend/buurman-booklets/src buurman-booklets/src
COPY backend/buurman-app/src buurman-app/src

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Build at startup so the Docker socket is available for Testcontainers/JOOQ codegen
ENV DOCKER_HOST=unix:///var/run/docker.sock
ENTRYPOINT ["sh", "-c", "mvn clean package -DskipTests -B -Dmaven.build.cache.enabled=false && java -jar buurman-app/target/buurman-app-*.jar"]

# === Prod stage: packages a pre-built JAR (CI) ===
FROM eclipse-temurin:25-jre-alpine AS prod

WORKDIR /app

RUN apk add --no-cache wget ca-certificates \
    && wget -qO /tmp/isrg-root-x1.pem https://letsencrypt.org/certs/isrgrootx1.pem \
    && keytool -importcert -noprompt -trustcacerts \
        -alias isrg-root-x1 \
        -file /tmp/isrg-root-x1.pem \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit 2>/dev/null || true \
    && rm /tmp/isrg-root-x1.pem

COPY backend/buurman-app/target/buurman-app-*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
