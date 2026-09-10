# Quality and Testing Gates

## Purpose

Define evidence expected before Yonte changes are considered production-ready.

## Test layers

- unit tests for domain rules and deterministic policies.
- integration tests for contracts/repositories/DI boundaries.
- UI tests for critical flows where behavior cannot be proven below UI.
- security tests for authentication, authorization, crypto boundaries, secret handling.
- migration tests for every persistent schema change.
- recovery/fault-injection tests for backup/restore and destructive-capable flows.
- architecture checks for forbidden dependency/type leakage.

## Gate rule

Passing compilation is not equivalent to passing tests, CI, security review, or recovery verification.

## Regression rule

Every confirmed high-impact defect should receive focused regression coverage when technically practical.
