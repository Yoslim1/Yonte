# Arabic Localization Audit

Status: ACTIVE
Last reviewed: 2026-09-02

This document records the Arabic localization and RTL inventory requested by the repository hardening review. It is an audit, not a translation project. No user-facing strings or UI behavior were changed as part of this inventory.

## Findings

| Area | Current evidence | Follow-up |
|---|---|---|
| User-facing strings | The application currently keeps many Arabic and English labels inline in Compose routes and `MainActivity`, including onboarding, unlock, settings, notes, and error messages. | Create a separate localization task to move stable copy into Android string resources while preserving the approved onboarding wording. |
| Arabic resources | The repository scan did not identify a `values-ar/strings.xml` resource set; current language handling is primarily inline through `isArabic()` and device locale checks. | Add a dedicated resource inventory and decide whether locale selection belongs in scope before translating. |
| RTL | `MainActivity` checks `resources.configuration.layoutDirection`; notes also check `Locale.getDefault().language`. Compose layouts rely on the active configuration. | Unify locale detection and verify mixed Arabic/English content, navigation icons, text alignment, and editor behavior on API 26+. |
| Plurals | No dedicated plural resource inventory was found in the scanned source paths. | Review counters and time-related messages for plural rules before extracting strings. |
| Dates and numbers | No broad date/number formatting layer was identified by the scan. | Audit note timestamps, update metadata, and backup status for Arabic locale formatting. |
| Accessibility labels | Several decorative icons intentionally use `contentDescription = null`; action icons use a mixture of Arabic/English inline descriptions. | Review every actionable icon for localized TalkBack labels and verify that decorative icons remain hidden from accessibility services. |
| Error messages | Unlock and backup errors are frequently selected inline by language, while some system exception text is surfaced directly. | Localize stable errors and replace raw exception text with safe, user-oriented messages where appropriate. |

## Scope boundary

This audit does not translate the application, change RTL layout behavior, or alter database/security code. These items should be handled as a separate feature task after the current hardening changes have independent verification.
