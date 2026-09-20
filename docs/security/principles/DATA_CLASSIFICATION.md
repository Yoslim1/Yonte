# Security Data Classification

## Purpose

Define handling classes used by authorization, AI, logging, backup, and diagnostics.

## Classes

### PRIVATE

Normal user-created content and metadata. Protected by encrypted storage and normal access policy.

### SENSITIVE

Financial, identity, private files, or explicitly protected content. Sensitive reads may require just-in-time confirmation and stricter external-processing policy.

### CRITICAL

Encryption keys, recovery material, passphrases, PINs, authentication secrets, and privileged security configuration.

CRITICAL material is never exposed to AI as ordinary knowledge content and never written to logs/events.

## Rules

- Classification follows the data, not the screen displaying it.
- Derived data cannot be assigned a weaker class merely because it is a summary/index.
- Audit records reference entity IDs and policy decisions, not protected content.

## Non-goals

This file does not define business-domain semantics or storage schemas.
