# Architecture Migration Phases

## Purpose

Define the safe order for moving from current Yonte to the target platform.

## Phase 0 — Baseline health

Restore green CI and fix confirmed P0 correctness/security defects before broad refactors.

## Phase 1 — Governance and enforcement

Finish architecture/security/data/backup specifications, ADRs, documentation standards, and strengthen automated boundary checks.

## Phase 2 — Domain/persistence boundary hardening

Remove Room/persistence entity leakage from feature presentation; introduce feature-owned domain contracts and explicit mapping.

## Phase 3 — Identity and relationships

Introduce stable global entity references, revisions/provenance, and relation model without forcing all feature content into one global table.

## Phase 4 — Authorization and consent

Implement capability/resource/risk-based authorization before AI cross-feature access.

## Phase 5 — Commands, queries, events

Introduce typed integration contracts and transactional outbox only where atomic event delivery is required.

## Phase 6 — Backup/recovery hardening

Resolve credential semantics, staged restore, verified generations, recovery-key lifecycle, and fault-injection tests.

## Phase 7 — AI platform integration

Add knowledge/action gateways, capability registry, provider boundary, memory/context governance, provenance, and derived-data invalidation.

## Rule

Do not skip a prerequisite phase merely because a later feature is more visible to users.
