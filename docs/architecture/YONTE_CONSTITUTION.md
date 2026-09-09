# YONTE Architecture Constitution

## Purpose

This document defines the non-negotiable architecture rules for Yonte evolution.

The goal is a scalable platform where every feature is independent, secure, testable, and connected through stable contracts.

## Core Principles

1. Feature Sovereignty

Each feature owns its UI, domain logic, data models, and tests.

Examples:

- Notes
- Tasks
- Calendar
- Files
- AI Assistant

A feature must not directly access another feature's internal database or implementation.

## Global Identity

Every user-owned entity has a stable identity.

Examples:

- Note
- Task
- Event
- File
- AI Memory item

Entities are referenced by ID and type, not by internal implementation details.

## Communication Rules

Features communicate through contracts:

- Commands: request an action.
- Events: announce completed changes.
- Queries: request information.

Events should contain references and metadata, not sensitive user content.

## AI Constitution

AI access is capability based.

The user grants explicit capabilities:

- Read
- Create
- Update
- Delete

Sensitive operations require confirmation.

Required confirmations:

- Delete data.
- Bulk changes.
- Sensitive data access.
- Security related actions.

AI must never bypass feature boundaries or access raw databases directly.

## Security Doctrine

Security is a platform capability, not a feature.

Rules:

- Least privilege.
- Default deny.
- No plaintext secrets.
- No sensitive data in logs.
- Audit security decisions.
- Fail safely.

## Versioning

The system must support independent evolution of:

- Application version.
- Database schema version.
- Event contract version.
- Backup format version.
- Security policy version.
- AI policy version.

Breaking changes require migration plans.

## Data Recovery

Backup and restore must protect user data.

Restore flow:

1. Validate backup.
2. Verify integrity.
3. Check compatibility.
4. Restore into safe staging.
5. Validate.
6. Commit.

A failed restore must never corrupt existing user data.

## Agent Rules

AI coding agents must provide impact analysis before architectural changes.

Required review areas:

- Security impact.
- Database impact.
- Migration impact.
- Compatibility impact.
- Test requirements.
- Rollback plan.

No silent architectural debt is allowed.
