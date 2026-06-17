# ADR 0003: Resiliency Strategy

## Status

Accepted

## Context

Gateway must remain responsive when Account Service is slow, unavailable, or returning transient server errors. It must persist events before downstream calls and avoid generic 500 responses.

## Decision

Gateway wraps Account Service REST calls with:

- 1 second connection timeout.
- 2 second read timeout.
- 3 retry attempts.
- 200 ms initial randomized exponential backoff.
- Circuit breaker with 50% failure threshold, 5 minimum calls, 10-call sliding window, 10 second open wait, and 2 half-open calls.

Gateway marks events `FAILED` when Account Service cannot apply them and returns a safe 503 response.

## Consequences

- Gateway fails fast instead of hanging.
- Retry is safe because Account Service is idempotent by `eventId`.
- Failed events are observable and can be retried manually or by a future worker.
- Current implementation does not yet include an automated retry worker or DLQ.

