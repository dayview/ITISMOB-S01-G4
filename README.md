# PURRFOLIO — MCO1 Wireframe Frontend (Android Studio)

Standalone Kotlin / Android Views application inspired by **the actual supplied 6-page MCO1 wireframe document**, including User Management, Pet Management, Shared Care, Care Logging, Records, Reminders, Calendar, Pet ID, Emergency, Offline & Sync, and Security & Privacy.

## Open
Open this folder in Android Studio, let Android Gradle Plugin sync, then Run. It uses applicationId `com.purrfolio.mco1`, minimum SDK 24. **Requires Android SDK + Gradle.** The emulator follows system dark/light mode.

To start the visible Pixel 6 emulator, build this project, install it, and launch PURRFOLIO from Terminal:

```sh
./run-pixel6.sh
```

The emulator command alone only opens Android. If a headless Pixel 6 is already running, `run-pixel6.sh` stops it before opening the visible emulator. Set `ANDROID_HOME` if your SDK is outside `~/Library/Android/sdk`.
Keep that Terminal window open while using the emulator; press Ctrl+C when finished. If Pixel 6 is already visible, the script simply rebuilds and relaunches the app.

## Current status
This is a **frontend prototype** with local editable pet profiles, care logs and reminders. It includes the supplied image-derived PURRFOLIO wordmark, photo accents, theme colors and more wireframe-based destinations. Features requiring cloud accounts, notification permissions, calendars, QR encoding/scanning, media capture, voice, and Firebase are **not** implemented. No network operation or emergency response is falsely claimed.

## Rubric alignment
- UX: pet-first sections and related records together.
- Navigation: persistent five-tab bottom navigation, with pet-switch control.
- Titles: descriptive headings and supporting captions in each view.
- Messaging: short, warm microcopy with accurate frontend-only status.
- Project brief: reflects approved MCO1 functional categories.
- Graphics: supplied PURRFOLIO wordmark, pet photography, lavender on charcoal, rounded surfaces.

## Remaining polish / limitations
Some dynamic screens are deliberately simplified from the source reference rather than pixel-exact screenshots. Original screenshots were embedded as small composite images in the Word file; they were not used as full-screen fake image UI. Screens contain actual Android widgets. Run it in Android Studio and refine densities on your device before grading. Authentication, sharing, recording, emergency services and network access need backend implementation.


## Visual-fidelity revision
- Added screen-safe native `WindowInsets` padding for the status bar, camera cutout and gesture navigation on Android 15+.
- Introduced a consistently placed back control and contextual titles for secondary screens.
- Added date strip and personalized heading to home and polished floating-style bottom navigation.
- Kept supplied PURRFOLIO logo imagery and both theme palettes.
- Includes `CODEX_VISUAL_FIDELITY_PROMPT.md` with the exact requested Codex prompt.
- This package is still a frontend prototype. It has NOT been built on an Android SDK or compared screen-by-screen on an emulator; therefore it is not guaranteed pixel-exact. Fredoka and Nunito font assets are not included; Android uses its rounded/system fallbacks.

## Seven-screen reference reconstruction

The seven reference presentations live in `ReferenceScreens.kt`, with Canvas icons and wellness arcs in `CareArtwork.kt`. Existing local profiles, care-log persistence, and forms remain in `MainActivity.kt`. Home's care schedule and wellness insights are illustrative reference data; clinical records and public sitter sharing are not fabricated.

Navigation: welcome → existing account/login flows → Home; pet avatar → overview; center plus → Log Care; Care dock tab → timeline; overview options → Passport or Sitter Mode. More also links to Sitter Mode.

Build with JDK 17 or 21 (the project's Gradle 8.9/Kotlin 2.0.21 combination does not support JDK 25):

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

Debug-only screenshot entry points accept `--es preview_screen 'Pet Passport'` (or Welcome, Home, Pet Detail, Care, Pet Timeline, Care Circle). They do not alter stored profiles or logs. Release builds ignore this extra.

See `docs/visual-qa/` for screen checklists, actual emulator captures, navigation smoke checks, asset provenance, and remaining fidelity limitations. Reference screens use system light/dark mode with identical geometry and real Android system bars.
