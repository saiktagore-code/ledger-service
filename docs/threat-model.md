# Threat Model

## Scope

This model covers Gateway, Account Service, REST communication between services, local H2 storage, logs, metrics, and CI/security scanning.

## Assets

- Transaction event payloads.
- Account balances and transaction history.
- Internal API key.
- Trace IDs and operational logs.
- Build and CI pipeline integrity.

## Trust Boundaries

- Public client to Gateway.
- Gateway to Account Service.
- Services to their own databases.
- CI system to dependency and static analysis tooling.

## STRIDE Review

| Threat | Risk | Mitigation |
| --- | --- | --- |
| Spoofing Gateway to Account Service | Unauthorized transaction application | Account Service requires `X-Internal-Api-Key`; secret supplied by environment. |
| Tampering with request payloads | Invalid balance changes | Bean validation, event type/currency/account/amount validation, safe errors. |
| Repudiation | Hard to trace failures or duplicate submissions | Trace IDs in requests, logs, and errors; persisted event lifecycle status. |
| Information disclosure | Stack traces or secrets exposed to clients | Safe error responses; no stack traces in API responses; secrets are not hardcoded. |
| Denial of service | Large requests or slow downstream calls | Request size filter, connection/read timeouts, retry limits, circuit breaker. |
| Elevation of privilege | Public caller reaches internal transaction endpoint | Internal API key requirement on Account Service. |

## Security Controls

- Environment-based secret management for local and Compose runs.
- Validation at service boundaries.
- Internal service authentication.
- Request size limits.
- Safe Problem Details-style errors.
- CodeQL static analysis in CI.
- Sonar quality scanning.

## Residual Risks

- Public Gateway authentication is not implemented.
- Rate limiting is not implemented.
- H2 is not appropriate for production durability.
- Internal API key should move to Vault, AWS Secrets Manager, or Kubernetes secret integration in production.
- TLS/mTLS should protect service-to-service traffic outside local development.
