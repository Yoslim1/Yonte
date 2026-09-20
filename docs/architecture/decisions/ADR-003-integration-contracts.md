# ADR-003: Contract-Based Feature Integration

## Status

Accepted target architecture.

## Context

Features require strong cooperation without importing each other's implementations.

## Decision

Use typed contracts with explicit interaction semantics:

- command for requested state change.
- query for requested information.
- event for notification of an already completed change.

App/DI composition connects implementations to contracts.

## Consequences

- Feature-to-feature implementation dependencies remain forbidden.
- Required synchronous validation stays in command/query flow rather than event chains.
- Events are suitable for fan-out and eventual follow-up, not every interaction.

## Rejected

- Direct repository/DAO imports between features.
- A generic service locator.
- Event bus for all communication.
