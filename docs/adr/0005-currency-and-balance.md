# ADR 0005: Currency And Balance

## Status

Accepted

## Context

The assignment allows either USD-only rejection of other currencies or extensible multi-currency support. Balance must be `SUM(CREDIT) - SUM(DEBIT)`, and transaction history should remain the source of truth.

## Decision

Support USD only. Reject all non-USD payloads during validation.

Account Service stores all applied transactions and maintains a materialized account balance for efficient reads. Negative balances are allowed; overdraft rules are outside the demo boundary.

## Consequences

- The implementation avoids pretending that multi-currency ledger accounting is simple.
- Validation remains clear and deterministic.
- Production multi-currency support would require currency-specific balances, FX policy, precision choices, and settlement rules.

