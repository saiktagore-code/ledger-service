# BMAD Epics

## Epic 1: Event Gateway Intake

Accept, validate, persist, and expose financial transaction events through a public API.

Key outcomes:

- `POST /events`
- `GET /events/{id}`
- `GET /events?account={accountId}`
- Event lifecycle status
- Duplicate event handling

## Epic 2: Account Ledger Processing

Maintain account state and transaction history with idempotent transaction application.

Key outcomes:

- `POST /accounts/{accountId}/transactions`
- `GET /accounts/{accountId}/balance`
- `GET /accounts/{accountId}`
- Balance derived from credit/debit transaction semantics
- Duplicate transaction protection

## Epic 3: Resiliency And Failure Handling

Keep Gateway responsive and data-safe when Account Service is slow or unavailable.

Key outcomes:

- Timeouts
- Retry with exponential randomized backoff
- Circuit breaker
- Failed event persistence
- Safe 503 responses

## Epic 4: Observability And Operations

Make the system diagnosable through logs, metrics, health checks, trace propagation, and docs.

Key outcomes:

- JSON logs
- `X-Trace-Id`
- `/health`
- `/metrics`
- OpenAPI
- Runbook

## Epic 5: Security And Delivery Readiness

Harden internal service access and provide repeatable build/deploy workflows.

Key outcomes:

- Internal API key authentication
- Request validation and request size limits
- Docker Compose
- GitHub Actions
- Dependency scanning
- CodeQL and Sonar setup

