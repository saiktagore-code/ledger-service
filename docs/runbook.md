# Event Ledger Runbook

## Local Startup

```bash
export ACCOUNT_SERVICE_API_KEY=supersecret
docker compose up --build
```

Gateway runs on `http://localhost:8080`.

Account Service runs on `http://localhost:8081`.

## Local Validation

```bash
./run-local-checks.sh
```

Expected result:

- Gateway build succeeds.
- Account Service build succeeds.
- Tests pass.
- JaCoCo coverage gates pass.

## Health Checks

```bash
curl http://localhost:8080/health
curl http://localhost:8081/health
```

Healthy responses include service status and database-backed counts.

## Metrics

```bash
curl http://localhost:8080/metrics
curl http://localhost:8081/metrics
```

Important metrics:

- `events_received_total`
- `events_duplicate_total`
- `events_failed_total`
- `account_transactions_total`
- `account_service_calls_total`
- `account_service_failures_total`

## Common Failures

### Gateway Returns 503 On `POST /events`

Likely cause: Account Service is unavailable, circuit breaker is open, or internal service auth is failing.

Check:

```bash
curl http://localhost:8081/health
docker compose logs account-service
docker compose logs gateway-service
```

Expected behavior: Gateway stores the event, marks it `FAILED`, and returns a safe 503 with trace ID.

### Account Service Returns 401

Likely cause: missing or mismatched `X-Internal-Api-Key`.

Check environment:

```bash
echo "$ACCOUNT_SERVICE_API_KEY"
```

In Docker Compose, Gateway `ACCOUNT_SERVICE_API_KEY` must match Account Service `INTERNAL_API_KEY`.

### Validation Error

Check required fields:

- `eventId`
- `accountId`
- `type`
- `amount`
- `currency`
- `eventTimestamp`

Account IDs must match `acct-[A-Za-z0-9_-]+`, currency must be `USD`, and amount must be positive.

### Duplicate Event

Expected behavior: duplicate submissions return the original event and do not change balance twice.

Check:

```bash
curl http://localhost:8080/events/{eventId}
curl -H "X-Internal-Api-Key: supersecret" http://localhost:8081/accounts/{accountId}
```

## Operational Notes

- `FAILED` events are not automatically retried in this implementation.
- Gateway event reads continue to work even if Account Service is down.
- H2 is local/demo storage. Production should use PostgreSQL and managed migrations.

