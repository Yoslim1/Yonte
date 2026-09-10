# Resource Budgets

## Purpose

Prevent feature growth from degrading battery, memory, storage, latency, network use, or AI cost on a mobile device.

## Rules

- Expensive work must have a measurable reason and a bounded input size or paging/batching strategy.
- Background work respects Android execution limits and avoids unnecessary wakeups.
- Large files, backups, indexes, and AI contexts have explicit size limits and failure behavior.
- Network and external-AI operations use timeouts, cancellation, and bounded retries.
- AI context/token use is minimized to the data required for the requested task.
- Performance fixes are evidence-driven; do not add caches, indexes, or concurrency complexity without a demonstrated need.
- A feature failure caused by resource exhaustion must not corrupt user data.

## Review

Critical paths should define representative scale targets and be profiled/benchmarked before introducing speculative optimization.
