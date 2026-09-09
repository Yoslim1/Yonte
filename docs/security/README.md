# Security Documentation Map

This directory follows a modular security documentation model.

## Principles

- One responsibility per document.
- Short reviewable documents over large encyclopedic files.
- Current implementation, decisions, threats, and future design are separated.
- Every document should have clear ownership and lifecycle.

## Structure

- `principles/` - security principles and invariants.
- `architecture/` - security architecture and boundaries.
- `threats/` - threat models and attack analysis.
- `decisions/` - ADRs for security decisions.
- `operations/` - operational security procedures.
- `reviews/` - audits and findings.

A reviewer should be able to understand one security area without reading the entire security system.
