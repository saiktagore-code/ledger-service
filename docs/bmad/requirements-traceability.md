# Requirements Traceability Matrix

| Requirement | Evidence |
| --- | --- |
| Two independently deployable microservices | `gateway-service`, `account-service`, service-specific POMs and Dockerfiles |
| No shared database/cache/memory/application state | Separate service packages, separate H2 configs, Docker Compose services |
| Synchronous REST communication | `gateway-service/src/main/java/com/eventledger/gateway/service/AccountServiceClient.java` |
| Gateway accepts transaction events | `EventController#createEvent` |
| Gateway validates requests | `TransactionEventRequest`, `GlobalExceptionHandler`, controller tests |
| Gateway generates/propagates trace IDs | `EventController`, `AccountServiceClient`, `GatewayIntegrationTest` |
| Gateway enforces idempotency | `EventService`, `EventRepository`, `EventServiceTest` |
| Gateway persists events and lifecycle | `EventEntity`, `EventStatus`, `EventService` |
| Gateway handles Account Service failure | `EventService`, `AccountServiceClient`, `AccountServiceFailureTest` |
| Gateway health and metrics | `HealthController`, `HealthControllerTest` |
| Account Service maintains accounts | `AccountEntity`, `AccountRepository`, `AccountService` |
| Account Service stores transactions | `TransactionEntity`, `TransactionRepository`, `AccountService` |
| Balance calculation | `AccountService`, `AccountServiceTest` |
| Account Service idempotency | `TransactionRepository#findByEventId`, `AccountServiceTest` |
| Duplicate event IDs do not double-apply | Gateway and Account Service service tests |
| Concurrent duplicate safety | DB uniqueness, synchronized write paths, service tests |
| Out-of-order event preservation | `eventTimestamp`, repository ordering, README domain rules |
| USD-only currency validation | DTO validation and controller/service tests |
| Event lifecycle statuses | `EventStatus`, README lifecycle docs |
| Partial failure recovery | Gateway failed status persistence, failure tests |
| RFC7807-style validation errors | `ApiError`, `GlobalExceptionHandler`, exception tests |
| End-to-end trace propagation | `GatewayIntegrationTest`, `AccountServiceClientTest` |
| JSON structured logging | `logback-spring.xml` in both services |
| Health checks | `/health` controllers and Docker Compose health checks |
| Metrics | `/metrics` controllers, Micrometer Prometheus registry |
| Resilience4j timeout/retry/circuit breaker | `application.yml`, `RestTemplateConfig`, `ResilienceConfig`, tests |
| Graceful degradation on Account Service unavailable | Gateway failure handling tests and README |
| Internal service authentication | `SecurityConfig`, `FilterConfigTest`, Account controller tests |
| Secrets from environment variables | `application.yml`, `docker-compose.yml`, README |
| Request size limits | `RequestSizeFilter`, `FilterConfigTest` |
| Database indexes and uniqueness | JPA entity annotations and repositories |
| OpenAPI/Swagger | `springdoc-openapi-starter-webmvc-ui` dependency |
| Unit tests | `src/test/java` in both services |
| Integration/resiliency tests | `GatewayIntegrationTest`, service/controller tests |
| Coverage gate | JaCoCo config in each POM and `run-local-checks.sh` |
| CI/CD | `.github/workflows` |
| CodeQL | `.github/workflows/codeql-analysis.yml` |
| SonarQube/SonarCloud | `.github/workflows/sonar.yml` |
| Docker Compose | `docker-compose.yml` |
| README documentation | `README.md` |
| Architecture diagrams | README Mermaid diagram and lifecycle diagram |
| Production evolution | README and ADRs |
