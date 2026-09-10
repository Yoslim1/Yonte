# Accessibility and Localization Platform Rules

## Purpose

Make accessibility, Arabic/English support, and RTL behavior platform contracts rather than feature-by-feature afterthoughts.

## Rules

- Text is localizable; user-visible copy is not scattered as ungoverned literals where resource strategy applies.
- Layouts support RTL without semantic inversion bugs.
- Touch targets, semantics/content descriptions, focus behavior, and contrast meet the chosen accessibility baseline.
- Dynamic text size must not destroy critical action visibility.
- Date/time/number formatting follows locale while stored canonical values remain locale-independent.
- Feature-specific culture/voice may vary; core navigation/security/destructive terminology remains understandable and consistent.
