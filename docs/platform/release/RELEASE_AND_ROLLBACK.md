# Release and Rollback Policy

## Purpose

Make application releases safe when code, persistent data, security policy, and update trust evolve at different speeds.

## Rules

- Persistent schema migrations are treated as forward evolution; destructive rollback is not an acceptable recovery strategy for user-owned data.
- Before a schema/security format change, define which older/newer app versions can open the resulting data safely.
- A release that can make older binaries incompatible must have an explicit rollback limitation documented before shipment.
- Update signer/package identity is verified independently from mutable release metadata.
- Release gates include canonical CI, migration/recovery evidence when applicable, and integrity/signature verification.
- Failed/partial upgrades must preserve data and expose a safe blocking/recovery state rather than guessing or wiping.
- Rollback plans distinguish code rollback from data-format rollback; they are not assumed to be equivalent.

## Non-goal

Do not introduce downgrade migrations merely to promise universal rollback. Preserve data first and require a compatible forward fix when safe reverse transformation is not guaranteed.
