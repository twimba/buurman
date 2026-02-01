# Use Maven image with JDK for development
FROM maven:3.9.12-eclipse-temurin-21-alpine

WORKDIR /app

# Install Docker CLI for Testcontainers (needed for jOOQ code generation)
RUN apk add --no-cache docker-cli wget

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Build and run - Maven build happens at startup when socket is available
# Running as root to access Docker socket (dev environment)
ENV DOCKER_HOST=unix:///var/run/docker.sock
ENTRYPOINT ["sh", "-c", "mvn clean package -DskipTests -B && java -jar target/buurman-backend-*.jar"]
