# ADR 0001: Service Boundaries

## Status

Accepted

## Context

The assignment requires two independently deployable services that do not share databases, caches, memory, or application state. Gateway is public-facing and Account Service is internal.

## Decision

Separate the system into:

- Event Gateway API: event intake, validation, idempotency, lifecycle persistence, resiliency, event queries.
- Account Service: authenticated transaction application, account state, transaction history, balance reads.

Communication is synchronous REST with `X-Trace-Id` propagation and `X-Internal-Api-Key` authentication.

## Consequences

- Each service has a clear ownership boundary and can be run, tested, and deployed independently.
- Gateway can continue serving event reads when Account Service is unavailable.
- Account Service remains protected from direct unauthenticated public access.
- Cross-service consistency is eventual with respect to Gateway event status and Account Service balance application.

