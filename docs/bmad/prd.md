# BMAD PRD: Event Ledger

## Problem

Financial transaction events can arrive more than once, arrive out of order, and be submitted while downstream services are partially unavailable. Event Ledger demonstrates a production-minded approach to accepting those events, protecting account balances from duplicate application, and preserving enough lifecycle state to recover and investigate failures.

## Goals

- Accept transaction events through a public Gateway API.
- Apply transactions through an independently deployable Account Service.
- Prevent duplicate balance changes under normal and concurrent duplicate submissions.
- Preserve source event timestamps and return event history in event time order.
- Degrade safely when Account Service is unavailable.
- Propagate trace context across service boundaries.
- Expose health, metrics, logs, OpenAPI docs, tests, CI, and Docker Compose.

## Non-Goals

- Multi-currency ledger accounting.
- Durable production database operations beyond H2 demo storage.
- Async event broker ingestion.
- Background retry worker or dead-letter queue in the current implementation.
- Full identity provider integration for public clients.

## Personas

- API client: submits transaction events and reads event status.
- Internal service operator: checks health, metrics, logs, and failure status.
- Reviewer: validates architectural decisions, requirements coverage, and test evidence.

## Functional Requirements

- Gateway exposes `POST /events`, `GET /events/{id}`, `GET /events?account={accountId}`, `/health`, and `/metrics`.
- Account Service exposes transaction, balance, account details, `/health`, and `/metrics` endpoints.
- Gateway persists every accepted event before calling Account Service.
- Account Service stores account rows and transaction history.
- Duplicate `eventId` values are idempotent in both services.
- Unsupported currencies, invalid account IDs, unknown event types, missing fields, malformed timestamps, and non-positive amounts are rejected.
- Account Service requires `X-Internal-Api-Key`.
- Gateway forwards `X-Trace-Id` and includes trace IDs in errors and logs.

## Quality Requirements

- Minimum 90% coverage gate for both services.
- Safe errors without stack traces in client responses.
- JSON structured logs with service and trace context.
- Resilience4j timeout, retry, and circuit breaker around Gateway to Account Service calls.
- Docker Compose runs both services independently with separate databases.
- CI runs build, tests, coverage, dependency scanning, CodeQL, and Sonar.

## Acceptance Criteria

- `./run-local-checks.sh` completes successfully.
- Duplicate event submissions return the original event and do not apply balance twice.
- Out-of-order event timestamps are preserved and listed in timestamp order.
- Account Service outage causes Gateway to store the event, mark it failed, and return 503.
- Circuit breaker opens after configured failure conditions and fails fast.
- Trace headers are propagated from Gateway to Account Service.
- README and docs explain architecture, tradeoffs, operations, and production evolution.

