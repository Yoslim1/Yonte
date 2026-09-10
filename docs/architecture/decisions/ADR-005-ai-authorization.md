# ADR-005: AI Capability Authorization and Confirmation

## Status

Accepted target architecture.

## Context

AI is intended to be deeply connected to Yonte but must remain under explicit user control.

## Decision

AI access is capability-based and resource-scoped. The user grants standing permissions in AI settings. Authorization returns allow, deny, or confirmation-required according to capability, resource, sensitivity, and operation risk.

Permission and confirmation are separate concepts.

## Consequences

- Read/create/update/delete are independent capabilities.
- Delete, permanent delete, destructive bulk actions, sensitive external disclosure, and security/recovery changes require just-in-time confirmation.
- A clear user request may authorize a low-risk create without redundant confirmation when standing permission allows it.
- AI accesses feature data/actions through governed gateways/contracts, never DAOs/Room tables.

## Rejected

- AI superuser mode by default.
- One global AI access toggle as the complete permission model.
