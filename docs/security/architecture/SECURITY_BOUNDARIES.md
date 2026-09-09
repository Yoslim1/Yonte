# Security Boundaries

## Purpose

Define where security responsibilities live and prevent accidental coupling.

## Core Boundaries

### Authentication Boundary

Responsible for proving user presence or knowledge.

Examples:

- passphrase verification.
- PIN verification.
- biometric authentication.

Authentication does not automatically grant every capability.

### Key Management Boundary

Responsible for key creation, wrapping, storage, rotation, and lifecycle.

Keys must have explicit purpose.

### Authorization Boundary

Responsible for deciding whether an actor or feature may perform an action.

Future AI features must use this boundary.

### Data Protection Boundary

Responsible for encrypted storage and recovery guarantees.

## Rule

No module should silently combine authentication, authorization, and encryption responsibilities.
