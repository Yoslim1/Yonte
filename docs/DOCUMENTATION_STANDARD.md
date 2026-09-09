# Documentation Engineering Standard

## Purpose

Keep Yonte architecture documentation reviewable, modular, and maintainable.

## Rules

- One document owns one responsibility or decision.
- Prefer short linked documents over encyclopedic files.
- Index files navigate; they do not duplicate detailed policy.
- Separate current implementation facts, target design, decisions, and review findings.
- Do not copy the same rule into many files; define one authoritative source and link to it.
- ADRs capture durable decisions and rejected alternatives, not operational checklists.
- File/folder names must reveal responsibility without opening the document.
- A document that begins accumulating unrelated sections must be split before more content is added.

## Recommended shape

Use only sections that add value: Purpose, Scope, Rules/Decision, Non-goals, Verification/Testing, Related documents.

## Review test

A reviewer should be able to answer `what does this file own?` in one sentence and review it without reading the entire repository.

## Non-goal

More files are not automatically better. Split by responsibility, not by arbitrary line count.
