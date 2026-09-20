# AI Capability Model

## Purpose

Define user-controlled standing permissions for AI access to each feature/domain.

## Capability shape

A permission is scoped by actor, feature/resource, operation, and optional sensitivity/context constraints.

Examples:

- `notes.read`
- `notes.create`
- `notes.update`
- `notes.delete`
- `tasks.read`
- `tasks.create`
- `calendar.create`

## Rules

- Default deny for capabilities not granted.
- Permissions are configured in AI settings and are visible/revocable by the user.
- Read, create, update, delete, execute, and sensitive-read are distinct capabilities.
- Permission does not equal confirmation; high-risk operations may still require just-in-time approval.
- AI never gains permissions merely because a feature is installed/available.

## Verification

Authorization matrix tests cover allow/deny/confirmation-required decisions.
