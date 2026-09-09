# Backup Security Contract

## Status
Accepted baseline for Backup foundation.

## Purpose
Define the security boundary for portable and scheduled backups before adding future features.

## Rules

### 1. Backup is a capability, not two unrelated features

The system has one backup contract with different execution modes:

- Portable backup: user initiated.
- Scheduled backup: background initiated.

Both must share the same security and data integrity rules.

### 2. Secret ownership

Backup keys and passphrases are sensitive capabilities.

Rules:

- No permanent ownership by UI layers.
- No passing secrets through unrelated layers.
- No long-lived mutable storage of raw secrets.
- Secrets must have an explicit lifecycle: create, use, clear.

### 3. Portable backup

Flow:

1. User chooses destination.
2. User provides backup passphrase.
3. Backup operation derives encryption material.
4. Data is encrypted and written.
5. Temporary secret material is cleared.

### 4. Scheduled backup

Scheduled backup must not depend on UI state.

It requires:

- Explicit policy.
- Controlled key lifecycle.
- Background-safe execution.
- Privacy-safe diagnostics.

### 5. Future compatibility

The backup format must remain versioned and extensible.

New domains must be added through contracts, not by creating independent backup systems.

## Non-goals

This document does not define:

- AI permissions.
- Sync protocol.
- Cloud backup.
- New user-facing features.

Those will build on this contract later.
