# === Dev stage: builds from source at startup (docker-compose) ===
FROM maven:3-eclipse-temurin-25-alpine AS dev

WORKDIR /app

RUN apk add --no-cache docker-cli wget

# Copy all POMs first for dependency caching
COPY pom.xml .
COPY common/pom.xml common/
COPY jooq/pom.xml jooq/
COPY buurman-core/pom.xml buurman-core/
COPY buurman-notifications/pom.xml buurman-notifications/
COPY buurman-backoffice/pom.xml buurman-backoffice/
COPY buurman-demo-data/pom.xml buurman-demo-data/
COPY buurman-takeout/pom.xml buurman-takeout/
COPY buurman-booklets/pom.xml buurman-booklets/
COPY buurman-app/pom.xml buurman-app/
COPY .mvn .mvn
COPY lombok.config .
RUN mvn dependency:go-offline -B -fn -Dmaven.build.cache.enabled=false

# Copy source
COPY common/src common/src
COPY jooq/src jooq/src
COPY buurman-core/src buurman-core/src
COPY buurman-notifications/src buurman-notifications/src
COPY buurman-backoffice/src buurman-backoffice/src
COPY buurman-demo-data/src buurman-demo-data/src
COPY buurman-takeout/src buurman-takeout/src
COPY buurman-booklets/src buurman-booklets/src
COPY buurman-app/src buurman-app/src

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

COPY buurman-app/target/buurman-app-*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
