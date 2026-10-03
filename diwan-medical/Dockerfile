# Build context is the repository root (the parent pom lives there):
#   docker build -f diwan-medical/Dockerfile -t diwan-medical .
#
# The builder stage is IDENTICAL in all six service Dockerfiles on purpose: it builds every module once, and
# Docker's layer cache then reuses that result for the other five images (same context + same instructions).
# Build the first image on its own, then the rest can run in parallel (see the Jenkinsfile). Keep this stage
# byte-for-byte the same in every Dockerfile, otherwise each image runs its own Maven build again.
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY . .
# Unit tests run here so a failing test fails the image build; compose passes SKIP_TESTS=true for quick local builds
ARG SKIP_TESTS=false
RUN mvn -B -DskipTests=${SKIP_TESTS} -DskipITs=true verify

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S diwan && adduser -S diwan -G diwan
WORKDIR /app
COPY --from=builder --chown=diwan:diwan /build/diwan-medical/target/diwan-medical-*.jar app.jar
# Heap follows the container memory limit (docker run --memory); override JAVA_OPTS to tune
ENV SERVER_PORT=8080 \
    MANAGEMENT_PORT=8081 \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
USER diwan
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
