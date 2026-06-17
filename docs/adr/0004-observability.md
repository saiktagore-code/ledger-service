# ADR 0004: Observability

## Status

Accepted

## Context

The system must support trace propagation, structured logs, metrics, and health checks.

## Decision

Use:

- `X-Trace-Id` as the cross-service trace propagation header.
- MDC-backed JSON logs through Logback and logstash encoder.
- Micrometer Prometheus registry for metrics.
- Lightweight `/health` and `/metrics` endpoints in both services.
- Springdoc OpenAPI for API documentation.

## Consequences

- Local operation is easy to inspect with curl, logs, and metrics endpoints.
- Trace IDs appear in errors and logs for incident investigation.
- Production can evolve to OpenTelemetry Collector, Jaeger/Tempo, Prometheus, and Grafana without changing the service boundary model.

