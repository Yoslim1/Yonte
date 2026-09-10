# Clock and Time Policy

## Purpose

Keep time-dependent behavior deterministic, testable, and correct across timezone/clock changes.

## Rules

- Domain/application logic obtains current time through an injectable clock abstraction when decisions depend on "now".
- Persist canonical instants in a stable form; convert to locale/timezone for presentation at the UI boundary.
- Calendar-local concepts that intentionally depend on timezone store the required timezone/context explicitly.
- Do not scatter direct `System.currentTimeMillis()` / `LocalDateTime.now()` calls through business logic.
- Tests use controlled clocks for expiry, reminders, ordering, lockout, backup naming, and conflict behavior.
- Timezone/device-clock changes must not silently rewrite historical facts.

## Non-goal

Do not introduce clock wrappers into code that has no time-dependent behavior merely for abstraction purity.
