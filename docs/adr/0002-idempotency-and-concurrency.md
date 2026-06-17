# ADR 0002: Idempotency And Concurrency

## Status

Accepted

## Context

Duplicate events and concurrent duplicate submissions must not create duplicate event records, duplicate transactions, or repeated balance changes.

## Decision

Use `eventId` as the idempotency key in both services. Gateway enforces uniqueness on `events.event_id`. Account Service enforces uniqueness on `transactions.event_id`. Service-level logic returns existing records for duplicates, and database uniqueness is the final guard.

Account rows use optimistic locking. Write paths are synchronized in-process for this single-instance demo to make concurrency behavior deterministic during tests.

## Consequences

- Duplicate submissions are safe and simple to reason about.
- Retry from Gateway is safe because Account Service ignores duplicate `eventId` values.
- The in-process lock is not sufficient for horizontally scaled production deployments.
- Production should rely on database conflict handling, atomic updates, row locks or optimistic retry loops, and idempotency-key tables.

