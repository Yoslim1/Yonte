# ADR-006: Backup Recovery Key Separation

## Status

Accepted target architecture; migration not yet implemented.

## Context

Scheduled backup needs unattended access on the current device, while portable recovery must survive device/Keystore replacement.

## Decision

Target backup encryption uses a high-entropy backup master key with two distinct protection paths:

- device-local Keystore wrapping for unattended scheduled backup.
- a versioned portable recovery capsule protected by a user-controlled recovery credential.

Interactive app-unlock credentials and portable backup recovery semantics are not implicitly the same contract.

## Consequences

- Scheduled backups do not prompt every run.
- Device Keystore loss is not automatically equivalent to losing all portable backup recoverability.
- Format/key migration must retain legacy read support until verified.
- Last known good recovery material is never discarded before replacement verification.

## Rejected

- Device-only key as the sole portable recovery path.
- Ambiguous overloads that hide manual-vs-scheduled credential semantics.
