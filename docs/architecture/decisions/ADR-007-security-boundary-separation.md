# ADR-007: Separate Authentication, Authorization, and Key Wrapping

## Status

Accepted target architecture.

## Context

Current Yonte correctly uses both authentication-gated and non-auth-gated Keystore primitives for different purposes. Future features/AI must not conflate these responsibilities.

## Decision

Treat these as distinct platform responsibilities:

- authentication proves eligibility to establish/open a protected session.
- authorization decides whether an actor may perform a capability on a resource now.
- key wrapping protects key material at rest and does not by itself prove user presence.

## Consequences

- Features consume narrow security contracts instead of raw crypto/key APIs.
- A decryptable local wrapped blob cannot be used as an authorization rule.
- Biometric/PIN/passphrase mechanisms can evolve without changing feature authorization semantics.

## Rejected

- One omnipotent `SecurityManager` API exposing cryptographic details to features.
