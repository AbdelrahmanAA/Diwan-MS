# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

Monorepo of Spring Boot 3.2.5 / Java 17 Maven services under a root aggregator/parent pom (`com.diwan:diwan-parent`, modules: common, gateway, users, transactions, medical, smarthome, logging). Versions, Java level and Spring Cloud BOM are managed there. Unit tests run with `mvn test`; integration tests (`*IT`, real MySQL/Kafka via Testcontainers, needs Docker) run only in `mvn verify` (`-DskipITs` skips them). Every DB service has a `SchemaMigrationIT` that applies all Flyway scripts to an empty MySQL 8.4 and lets Hibernate validate the result, so an entity change without a migration fails the build (use `clean` when checking: stale `target/classes` can hide a removed script).

| Module | Port (local) | Role |
|---|---|---|
| `diwan-common` | — | Shared library + Spring Boot auto-configuration (see below) |
| `diwan-gateway` | 8111 (published as 8087 in docker/Jenkins) | Spring Cloud Gateway (reactive), DB-driven routes, JWT validation, request logging |
| `diwan-users` | 8083 | Register/login, issues JWTs, publishes `UserInvalidatedEvent` to Kafka |
| `diwan-transactions` | 8085 | Financial transactions |
| `diwan-medical` | 8082 | Medical records |
| `diwan-smarthome` | 8086 | Smart devices, Alexa endpoint, MQTT command publishing |
| `diwan-logging` | 8084 | Consumes Kafka `request-logs`, stores/serves request logs |
| `diwan-portfolio` | 8087 | New, untracked, deliberately NOT in the parent pom yet (its pom is a copy of medical's, including the `diwan-medical` artifactId). Builds standalone |

## Commands

Maven is vendored at `tools/apache-maven-3.9.9/bin/mvn(.cmd)` (no wrapper). Run from the repo root:

```
tools/apache-maven-3.9.9/bin/mvn -DskipTests package              # all modules, no tests
tools/apache-maven-3.9.9/bin/mvn verify                           # unit + integration tests (Docker required)
tools/apache-maven-3.9.9/bin/mvn -pl diwan-users -am test         # unit tests of one module
tools/apache-maven-3.9.9/bin/mvn -pl diwan-users -am package      # one module
tools/apache-maven-3.9.9/bin/mvn -pl diwan-users spring-boot:run  # run one (set SPRING_PROFILES_ACTIVE=DEV)
```

- `start-backend.bat` runs the built jars locally with `SPRING_PROFILES_ACTIVE=DEV`.
- **Docker:** every service listens on `SERVER_PORT=8080` inside its container (locally the `server.port` default differs per service) and exposes actuator on `MANAGEMENT_PORT=8081` (never published). Dockerfiles use the **repo root as build context** (`docker build -f diwan-users/Dockerfile -t diwan-users .`), run unit tests during the build (`--build-arg SKIP_TESTS=true` skips them), and produce a non-root image with a liveness `HEALTHCHECK`; the JVM heap is 70% of the container memory limit (`JAVA_OPTS` overrides). `docker-compose.yml` brings up MySQL, Redis, Zookeeper, Kafka (bound to 127.0.0.1) and all services with memory limits (`GATEWAY_MEMORY`/`SERVICE_MEMORY`); only the gateway publishes a port (8087). It requires `JWT_SECRET`, `ALEXA_LAMBDA_SHARED_SECRET` and `MQTT_BROKER_URL` in `.env`.
- **Run everything locally in Docker:** copy `.env.example` to `.env` (gitignored; needs `JWT_SECRET`, `ALEXA_LAMBDA_SHARED_SECRET`, `MQTT_BROKER_URL=tcp://mosquitto:1883`), then `docker compose --profile mqtt up -d --build` (the `mqtt` profile adds a local Mosquitto broker for the smart-home device commands). The gateway is the only published port: `http://localhost:8087`. To restart services without touching MySQL/Redis/Kafka add `--no-deps <service...>`.
- **API tests:** `postman/Diwan-API.postman_collection.json` (every API through the gateway, with assertions and security checks) + environment `postman/Diwan.postman_environment.json` (set `lambdaSecret` to `ALEXA_LAMBDA_SHARED_SECRET` from `.env`; `*.local.postman_environment.json` is gitignored for the real value). Headless: `docker run --rm -v "$PWD/postman:/etc/newman" postman/newman:alpine run Diwan-API.postman_collection.json -e Diwan.local.postman_environment.json --env-var baseUrl=http://host.docker.internal:8087`. A run uses the whole login/register rate-limit budget (5 per IP), so wait about a minute between runs. It creates and deletes its own users and records.
- **Observability:** `docker compose --profile observability up -d` adds Prometheus (scrapes `<service>:8081/actuator/prometheus`, alert rules in `observability/alerts.yml`) and Grafana (dashboard `observability/grafana/dashboards`), both on 127.0.0.1. Every request carries `X-Request-Id` (created by the gateway, put in every service's log lines as `[requestId]`, returned to the client, stored in `request_logs.request_id`). The gateway's filters treat its own management port as trusted (no JWT, not logged).
- **`Jenkinsfile`:** builds and deploys only the services whose files changed since the last successful build (changes to the root `pom.xml`, `diwan-common/` or `.dockerignore` rebuild all; `DEPLOY_ALL` forces it). Stages: images (tagged with the 8-char commit SHA, unit tests inside the build) -> Testcontainers ITs in a maven container (`INTEGRATION_TESTS` = warn/enforce/off) -> Trivy scan (`SCAN_SEVERITY`) -> deploy (services in parallel, gateway last) with a readiness wait on the management port and automatic rollback to the previous image. Containers run on the `nginx-proxy` network with memory limits and `--restart unless-stopped`; only the gateway publishes 8087->8080. Requires Jenkins credentials `diwan-jwt-secret`, `diwan-alexa-secret`, `diwan-mqtt-broker-url` (Secret text) and `diwan-db` (username/password). `diwan-portfolio` is not part of the parent pom and is not deployed.

## Configuration

- Each service has `application.properties` plus `application-DEV.properties` / `application-SIT.properties`; the base file sets `spring.profiles.active=SIT` (git branch `SIT` is the working branch). DB URL lives in the profile files; SIT reads `DB_USERNAME`/`DB_PASSWORD` from the environment; everything else is env-overridable (`SERVER_PORT`, `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_HOST`, `JPA_DDL_AUTO`, `*_SERVICE_URL`, ...).
- `JWT_SECRET` (plus `ALEXA_LAMBDA_SHARED_SECRET` and `MQTT_BROKER_URL` for smarthome) have **no default outside the DEV profile** — SIT/prod startup fails if unset. DEV files carry a dev-only fallback. All services must share the same `JWT_SECRET`.
- Schema is owned by Flyway (`db/migration/V1__baseline.sql` + later `V<n>__*.sql`); Hibernate only validates (`JPA_DDL_AUTO=validate`). Existing databases are auto-baselined at v1. Any entity change needs a new migration (see `MIGRATIONS.md`). Each service uses its own MySQL database (`diwan_users`, `diwan_transactions`, `diwan_medical`, `diwan_logging`, `diwan_smarthome`, `diwan_gateway`).

## Architecture

**`diwan-common`** is the only shared code. It is auto-configured (`META-INF/spring/...AutoConfiguration.imports`), so a service gets behaviour by adding the dependency plus properties, not by copying classes:
- `JwtService` (jjwt 0.12, issue/parse/jti/TTL) and `GatewaySigner` beans whenever `diwan.jwt.secret` is set (startup fails if it is shorter than 32 chars).
- `GatewayTrustFilter` when `diwan.trust.url-patterns` is set (also `diwan.trust.public-paths`, `diwan.trust.required-role`). Servlet services only.
- `GlobalExceptionHandler` (`{"success":false,"message":...}`, Spring MVC exceptions keep their 4xx status). Opt out with `diwan.web.error-handler=false` (smarthome keeps its own `{"error":...}` contract).
- A stateless/permit-all Spring Security chain for servlet services that have Spring Security (auth is the gateway's job). Users only defines the BCrypt `PasswordEncoder`.
- `UserInvalidatedEvent` (Kafka topic `user.invalidated`), used by users (producer) and transactions/medical (consumers, via `spring.json.value.default.type`).

**Request flow:** client → gateway → service.
- `JwtAuthFilter` (gateway) first strips every client-supplied identity header (also on public paths), validates the JWT + Redis logout blacklist, enforces ADMIN on `diwan.gateway.admin-paths` (`/admin`, `/api/logs`, `/actuator`; no token carries a role yet, so these are closed), then forwards `X-User-Id/Email/Role` plus `X-Gateway-Timestamp` and an HMAC `X-Gateway-Signature` (`GatewaySigner`, key derived from `JWT_SECRET`). Downstream services register `GatewayTrustFilter` and reject anything without a valid, fresh (±2 min) signature, so controllers can read `X-User-Id`/`X-User-Role` safely. Public paths: login, register, `/api/features`, `/actuator/health`, and `/api/smart-home/alexa` (authenticates itself with the lambda secret + user JWT). The retired `X-Gateway-Validated` header is no longer sent.
- Self-service rule: users may delete/deactivate only their own account unless role is ADMIN.
- **Routes are data, not config.** `RouteDataInitializer` (a `CommandLineRunner`) upserts `service_routes` (name → target URL + path prefix, e.g. `/api/users`, `/api/transactions`, `/api/medical`, `/api/smart-home`, `/api/logs`) and `app_features` (dashboard cards served by `FeaturesController`, cached via `FeaturesCacheService`) on every startup. `DynamicRouteConfig`/`RouteService`/`RouteManagementController` build and refresh gateway routes from that table. To add a service: add a `seedOrUpdate` line (and feature card if user-facing) plus a `*_SERVICE_URL` env var.
- **Route behaviour (`RouteDefinitionFactory`):** every service route gets, outermost first: Redis token-bucket rate limit (per user, per IP when anonymous) -> circuit breaker (trips on connection errors, timeouts, 502/503/504; fallback `/fallback/<service>` returns 503/504 JSON) -> Retry (GET only, connection errors and 502/503). The response timeout is the row's `timeout_ms` (default 5000). `login`/`register` also get a separate strict per-IP route (5 attempts, then ~1 per 12 s). All knobs are `diwan.gateway.*` (see `GatewayProperties` and the gateway `application.properties`). Rows that fail `RouteValidator` (e.g. a prefix ending in `/**`) are logged and skipped instead of crashing the gateway.
- **Route reloads:** the gateway caches routes; `RouteService` publishes a `RefreshRoutesEvent` after every change and `RouteRefresher` polls `service_routes` (default 30 s) so other instances and manual DB edits are picked up without a restart. `RouteDataInitializer` still overwrites the five built-in routes from the `*_SERVICE_URL` variables on every start.
- **Client IP:** only the last `diwan.gateway.trusted-proxies` X-Forwarded-For entries (those appended by nginx) are trusted; used for rate limiting and request logs.
- **Request logs:** `LoggingWebFilter` -> `RequestLogPublisher` (bounded queue, short Kafka timeouts, drops new events when full, never blocks requests; metrics `gateway.request.log.dropped/failed`). No query strings, headers or bodies are logged.
- **Async logging:** gateway `LoggingWebFilter` publishes each request as JSON to Kafka topic `request-logs`; `diwan-logging`'s `RequestLogConsumer` persists it.
- **Transactional outbox (users):** `deleteUser`/`deactivateUser` write the event to the `outbox_event` table in the same DB transaction; `OutboxRelay` (scheduled) publishes pending rows to Kafka in order and marks them published, so a Kafka outage cannot lose an event. Delivery is at-least-once; events carry `eventId`/`version`/`occurredAt`.
- **Kafka failure handling (`KafkaReliabilityAutoConfiguration`):** every listener retries with exponential backoff (`diwan.kafka.retry.*`) and then publishes the record to `<topic>.DLT`. Consumers of JSON events use `ErrorHandlingDeserializer`, so poison messages go straight to the DLT instead of blocking the partition. Do not catch-and-swallow exceptions inside `@KafkaListener` methods. Topic names and consumer group ids come from properties (`diwan.kafka.topics.*`, `spring.kafka.consumer.group-id`). Event schema rule: only add optional fields.
- **User invalidation:** `diwan-users` produces `UserInvalidatedEvent` (from `diwan-common`) via Kafka; `UserInvalidatedConsumer` in transactions/medical consumes it.
- Gateway circuit breaker (Resilience4j) falls back to `FallbackController`.
- Smart home: `AlexaSmartHomeController` is called by an Alexa Lambda (`ALEXA_LAMBDA_SHARED_SECRET`); device commands go out over MQTT (`MqttCommandPublisher`, `MQTT_BROKER_URL`).

Service DTOs and entities are per-service; anything needed by more than one service belongs in `diwan-common`.
