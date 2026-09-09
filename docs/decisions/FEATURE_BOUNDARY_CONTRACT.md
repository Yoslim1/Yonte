# Feature Boundary Contract

## Decision

Yonte will use isolated feature domains connected through stable contracts.

## Goal

A feature can evolve internally without causing unnecessary changes in other features.

Examples:

- Notes can change UI, storage implementation, or internal logic.
- AI Assistant can evolve its reasoning pipeline.
- Tasks can evolve scheduling logic.

The public communication points remain stable.

## Rules

1. Feature-to-feature direct dependency is prohibited.
2. Shared business contracts belong to core contracts.
3. UI belongs to its feature module.
4. Data ownership remains with the feature that owns the domain.
5. Cross-feature operations use explicit interfaces/events.

## Example

AI does not directly access Notes database tables.

AI requests a capability:

AI -> Permission Gateway -> Notes Contract -> Notes Data

This keeps authorization, auditing, and future changes controlled.

## Security Principle

No feature receives broad access by default.
Access is granted by explicit capability permissions.

## Future Expansion

This supports future domains:

- Tasks
- Habits
- Finance
- AI Agents
- Integrations

without creating a tightly coupled system.
