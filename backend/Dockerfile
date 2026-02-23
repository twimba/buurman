# === Dev stage: builds from source at startup (docker-compose) ===
FROM maven:3-eclipse-temurin-25-alpine AS dev

WORKDIR /app

RUN apk add --no-cache docker-cli wget

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Build at startup so the Docker socket is available for Testcontainers/JOOQ codegen
ENV DOCKER_HOST=unix:///var/run/docker.sock
ENTRYPOINT ["sh", "-c", "mvn clean package -DskipTests -B && java -jar target/buurman-backend-*.jar"]

# === Prod stage: packages a pre-built JAR (CI) ===
FROM eclipse-temurin:21-jre-alpine AS prod

WORKDIR /app

RUN apk add --no-cache wget ca-certificates \
    && wget -qO /tmp/isrg-root-x1.pem https://letsencrypt.org/certs/isrgrootx1.pem \
    && keytool -importcert -noprompt -trustcacerts \
        -alias isrg-root-x1 \
        -file /tmp/isrg-root-x1.pem \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit 2>/dev/null || true \
    && rm /tmp/isrg-root-x1.pem

COPY target/buurman-backend-*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
