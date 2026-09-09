# Feature Boundary Map

## Purpose

Define ownership boundaries before code migration.

## Current Feature Areas

- notes
- onboarding
- settings

## Core Responsibilities

Core is reserved for shared capabilities:

- database infrastructure
- security infrastructure
- backup and recovery
- design system
- navigation
- update mechanisms

## Boundary Rules

Features must own their business rules and UI flows.

Features must not directly access another feature's internal storage.

Cross-feature communication should use stable contracts:

- commands
- queries
- events
- entity references

## Future Domains

Expected future isolated domains:

- tasks
- calendar
- files
- AI assistant

Each domain should have its own internal implementation while using shared platform contracts.
