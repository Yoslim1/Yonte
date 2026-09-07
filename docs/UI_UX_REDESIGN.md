# Yonte redesign — implementation handoff (2026-09-07)

This working-tree implementation targets the existing Android Kotlin/Compose application. It is not a web rewrite or a claim of device-tested production readiness. No new dependencies, database schema, unlock behavior or cryptographic parameters were introduced. Nothing was committed or published.

## Integration base

Integrated onto upstream `142b3ac` on 2026-09-07 (original audit baseline: `1a5cd9`). Upstream key/salt zeroing, PIN hash cleanup, permanent backup/database failure classification and invalidated-biometric handling are preserved. Backup framing validation retains the stricter fixed salt/IV lengths and payload bounds from this change. Unchanged editor saves acknowledge completion without rewriting timestamps. The baseline audit is historical; findings must be interpreted against this integrated diff.

## Identity and interaction

- A charcoal/mineral dark default, limestone light theme and restrained bronze/gold actions replace the previous ivory/indigo direction. A small geometric papyrus mark provides Egyptian identity without decorative hieroglyphs in controls.
- Arabic is the initial in-app language. Arabic/English and light/dark preferences persist locally. Compose layout direction changes immediately; the settings drawer opens from the right in Arabic and the left in English.
- The three-line button opens settings only. The modal drawer retains the notes composition underneath. Home supports swipe opening/closing; gestures are disabled while editing to avoid stealing text selection. Back closes settings or its current subsection before leaving the main screen.
- Active notes, archive and trash live on the notes screen, with explicit restore/unarchive actions. Search and tag empty states offer clearing filters instead of misleading first-note copy. Grid headings span the full grid width. Header, filters and results share one lazy scroll container in list/grid modes so enlarged text does not reserve a fixed header above the results.
- Editing retains its session in the ViewModel through Activity recreation, never saving plaintext note contents into a Bundle. A consumed ACTION_SEND text extra is cleared after capture so rotation cannot replay and replace that retained draft. Toolbar insertion respects selection/cursor. Selection-only moves do not schedule writes. Existing notes can intentionally be cleared, blank new drafts complete without creating a record, and save failures keep the editor available for retry. While Done/Back waits for the immediate save, both fields are read-only and toolbar/close buttons are disabled; repeated Back is consumed. Callback guards reject queued edits so they cannot cancel the pending close save.
- Checklist toolbar actions remain Markdown text insertion; this implementation does not promise interactive checkbox editing or rich-text rendering.

## Data and safety

Backup work moves to IO, has a single busy guard, propagates cancellation and clears owned key/passphrase buffers. The settings ViewModel belongs to a composition-owned ViewModelStore cleared on disposal; work is cancelled on Activity destruction rather than retained across recreation. Import warns that matching note IDs are overwritten; existing merge policy is unchanged.

Import and export enforce a 16 MiB encoded JSON file limit, including scheduled export. Valid cryptographic framing remains unchanged. Header lengths are checked before KDF/allocation. Optional `is_archived` and `is_trashed` fields retain collection membership in new backups; old backups default missing fields to false because their original state cannot be recovered. Older apps may ignore these added fields. The export cap is checked before writing the destination, but payload assembly still uses memory proportional to stored notes.

Save and restore update FTS inside the database transaction; the existing best-effort FTS fallback remains. The visible UI uses reactive normalized matching against observed notes, so results respond to edits and collection mutations. This trades index efficiency for coherent collection-wide results and needs large-library profiling.

## Baseline audit disposition

See `BASELINE_AUDIT_2026-09-06.md` for baseline evidence. This table is the implementer's disposition, pending independent final-diff review.

| Finding | Disposition |
| --- | --- |
| B01 unbounded backup input | Header and file bounds implemented; version validation remains as existing payload checks. |
| B02 backup collection flags | Optional flags implemented in manual/scheduled exports and import; legacy defaults covered by added test. |
| B03 restore overwrites | Warning implemented; collision preview/conflict policy remains outstanding. |
| B04 Main-thread work | Interactive backup moved to IO; PIN derivation still outstanding. |
| B05 FTS restore | Save/restore index writes added; old index repair and swallowed FTS failures remain outstanding. |
| B06 stale search | UI now observes all notes and recomputes normalized collection/query matches. |
| B07 clearing notes | Existing blank edits persist; empty new drafts acknowledge completion. |
| B08 automatic backup key lifecycle | Outstanding; enabling and disabling/key-cache refresh need dedicated fix. |
| B09 PIN double confirmation | Outstanding. |
| B10 settings lifecycle | Owned store and disposal cleanup implemented; cancellation on recreation is deliberate. |
| B11 biometric setup failure | Outstanding. |
| B12 error handling | Import/restore covered; update installer launch failures remain outstanding. |
| B13 startup failure handling | Outstanding. |
| B14 update independent trust anchor | Outstanding; no security assurance claim. |

Arabic priority is implemented in notes/settings and layout direction. Existing onboarding copy and Android/system-generated strings are not a complete bilingual localization migration. Note timestamps explicitly use the selected Arabic/English locale; they display an absolute date/time rather than system-locale relative text.

## Verification evidence and remaining acceptance checks

- `python3 tools/check_architecture.py`: passed locally, no forbidden source/Gradle dependency edges detected.
- `git diff --check`: passed locally.
- Attempted `./gradlew :feature:notes:testDebugUnitTest :core:backup:testDebugUnitTest :feature:settings:testDebugUnitTest --no-daemon`. Failed before compilation while downloading `gradle-8.11.1-bin.zip`: `java.net.SocketException: Network is unreachable`.
- Added tests are not reported as executed. The committed-range changelog gate cannot verify this uncommitted patch; a dated entry is included for a future authorized commit.
- Canonical GitHub Actions, Kotlin compilation, lint, APK creation, emulator/device gestures, TalkBack focus, Arabic/English typography and enlarged font sizes remain unverified.
- Required device checks: right/left swipe and Back hierarchy, drawer scrim, text selection, rotate during typing, save error retry, typing/toolbar taps immediately after Done followed by repeated Back, blank note reopening, collection restore, current/legacy backup round trips and cancellation during backup. Process death can still lose a debounce window of unsaved changes; no plaintext saved-state workaround was introduced.
