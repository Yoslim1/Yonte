# Documentation Engineering Standard

## Purpose

Keep Yonte architecture documentation reviewable, cohesive, maintainable, and proportionate to the problem being documented.

## Core rule

Optimize for **cohesion and reviewability**, not maximum file count.

A document may own one responsibility or a small cluster of closely related responsibilities when they:

- belong to the same architectural boundary or domain.
- serve the same reviewer/audience.
- normally change for the same reason or in the same change set.
- share the same authority and lifecycle.
- remain small enough to understand and review as one unit.

Two or three small related concerns may therefore live in one document. Do not create micro-files merely to satisfy a one-topic rule.

## Split a document when

Split when one or more of these becomes true:

- sections have independent reasons to change.
- different owners or review gates apply.
- policy, current implementation facts, historical decisions, and audit findings would become ambiguous if mixed.
- a security/data/backup concern needs its own authority or lifecycle.
- the file becomes difficult to navigate, review, or safely modify without understanding unrelated material.
- one section can evolve substantially without the others.

There is no fixed line-count threshold; complexity and coupling matter more than raw size.

## Repository rules

- Prefer cohesive documents over encyclopedic files and over excessive fragmentation.
- Index files navigate a real collection and may contain short orientation summaries; they must not duplicate authoritative policy.
- Do not copy the same rule into many files. Define one authoritative source and link to it.
- ADRs capture durable decisions, trade-offs, and rejected alternatives; operational procedures belong with operations/policies.
- File/folder names must reveal their architectural area without requiring repository archaeology.
- Do not split a document if the split would increase navigation cost without improving ownership, review, or change isolation.
- Do not merge documents merely to reduce file count when they have different authority, owners, or change reasons.

## Recommended shape

Use only sections that add value. Typical sections are Purpose, Scope, Rules/Decision, Non-goals, Verification/Testing, and Related Documents.

Small related topics may share one document under clear headings when the grouping remains cohesive.

## Review test

A reviewer should be able to answer:

1. What coherent area does this file own?
2. Why do these sections belong together?
3. Can a change be reviewed without loading unrelated parts of the repository?
4. Would splitting or merging materially improve change isolation or clarity?

If the grouping has no clear answer, reorganize it.

## Non-goals

- More files are not automatically better.
- Fewer files are not automatically better.
- Documentation structure must not be driven by arbitrary line counts.

Choose the smallest number of cohesive, authoritative documents that keeps changes easy to understand and review.
