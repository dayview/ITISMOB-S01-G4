# Phase 2 validation

## Reference preparation

The six-page PDF was rendered at 400 DPI before implementation. Its embedded artwork yielded 28 separately cropped phone wireframes, each inspected at its original aspect ratio. The [reference inventory](REFERENCE_INVENTORY.md) records the source mapping and measurements. The [gallery](index.html) pairs each crop with Pixel 6 captures at matching width without stretching either image.

## Android checks

- `:app:assembleDebug` and `:app:lintDebug` passed. Android lint reports 0 errors and 39 nonblocking warnings, chiefly existing custom drawing allocations and deprecated system bar APIs.
- The final APK was installed on `emulator-5554`, a Pixel 6 AVD at 1080 × 2400 and 420 dpi.
- All 24 Phase 2 destinations and the seven preserved Phase 1 destinations were launched and captured in both dark and light mode: 31 screens per theme. Comparisons are in `comparisons/dark/` and `comparisons/light/`.
- Status bar, camera cutout and gesture area remained unobstructed in the captured views. Content longer than the Pixel 6 viewport scrolls above the native dock.
- On-device UI checks passed for Home → Log Care, Passport → QR Privacy, Profile → Personal Information, the pet species dropdown, calendar month/date controls, records filtering, Access Control → Activity Log, Sound Journal → written log, and distinct Sitter Mode/Care Circle routes. Results are in `smoke-results.txt`.

## Deliberate frontend limits

The PDF contains illustrative caregiver names, documents, recordings, reminders and cloud states. They are marked as sample or demo content where presented. The QR graphic is explicitly non-scannable. Authentication providers, reset email, speech/audio recording, document upload/export, public sharing and cloud sync have preview actions rather than fabricated success states. Existing local pet, care-log and reminder persistence remains available.

The PDF phone frames are taller than the Pixel 6 screen, so some lower content is reached by scrolling. Some supplied illustrations include dark rectangular backgrounds, which remain visible on light mode. No source artwork was recolored or regenerated.
