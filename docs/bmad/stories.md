# BMAD Stories

## Gateway

- As an API client, I can submit a valid transaction event so the system records and applies it.
- As an API client, I can resubmit the same event ID and receive the original event without duplicate balance impact.
- As an API client, I can query an event by ID to see the latest lifecycle status.
- As an API client, I can query events by account and receive them ordered by original event timestamp.
- As an operator, I can see failed events when Account Service is unavailable.

## Account Service

- As Gateway, I can submit an authenticated transaction request to Account Service.
- As Account Service, I reject duplicate event IDs before changing balance twice.
- As an API client, I can read account balance.
- As an API client, I can read account details including transaction history.
- As Account Service, I reject unsupported currencies, invalid account IDs, and invalid amounts.

## Resiliency

- As Gateway, I timeout slow Account Service calls quickly.
- As Gateway, I retry transient downstream failures.
- As Gateway, I open the circuit breaker when Account Service is failing.
- As Gateway, I fail fast with 503 when the circuit breaker is open.

## Observability

- As an operator, I can see service health.
- As an operator, I can scrape Prometheus metrics.
- As an operator, I can follow a trace ID from Gateway to Account Service.
- As a reviewer, I can verify behavior through tests and requirements traceability.

## Security

- As Account Service, I reject requests without a valid internal API key.
- As a client, I receive safe validation errors without stack traces.
- As an operator, I can provide secrets through environment variables.

