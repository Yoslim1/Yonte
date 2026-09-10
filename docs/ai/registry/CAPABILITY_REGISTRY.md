# AI Capability Registry

## Purpose

Allow AI and orchestration layers to discover available feature capabilities without hardcoding every future feature into the AI implementation.

## Registry entry

A feature can advertise safe typed capabilities such as knowledge sources, commands/actions, supported entity types, and required permission scopes.

## Rules

- Registry exposes contracts/metadata, not implementation objects or DAOs.
- Availability does not grant authorization.
- Capability IDs are stable and versioned when contract semantics change.
- Feature removal/disablement is reflected without crashing AI orchestration.

## Non-goal

This is not an unrestricted plugin/service locator.
