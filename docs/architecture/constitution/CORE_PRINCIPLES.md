# Core Architecture Principles

## Purpose

Define the platform-level engineering axioms used to evaluate every architectural change.

## Principles

1. High connectivity with low coupling.
2. Strong cohesion and explicit ownership.
3. One-way, acyclic dependency direction.
4. Stable contracts over implementation sharing.
5. Local-first and private-by-default behavior.
6. Security and data integrity outrank convenience.
7. Wrong architecture should be difficult to express in code.
8. Prefer evolutionary migration over rewrites.
9. No silent technical debt.
10. Observability must not leak protected content.

## Decision test

A proposed change should reduce coupling, clarify ownership, preserve security/data guarantees, improve testability, or enable a required capability. If it only adds abstraction without one of those benefits, reject it.

## Non-goals

- Microservices inside the Android app.
- Abstraction for its own sake.
- Central God modules or service locators.
