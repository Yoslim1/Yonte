# Security Review Process

## Goal

Keep security changes reviewable, traceable, and maintainable.

## Review Order

1. Threat model impact.
2. Trust boundary changes.
3. Key lifecycle impact.
4. Data exposure risk.
5. Performance and operational impact.
6. Tests covering critical paths.

## Documentation Rule

A new security concept should add a focused document or ADR instead of growing an unrelated file.

## Quality Bar

A reviewer should identify:

- what changed.
- why it changed.
- what can fail.
- how rollback works.
- how the behavior is tested.
