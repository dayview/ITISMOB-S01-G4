# PURRFOLIO — MCO1 Wireframe Frontend (Android Studio)

Standalone Java / Android Views application inspired by **the actual supplied 6-page MCO1 wireframe document**, including User Management, Pet Management, Shared Care, Care Logging, Records, Reminders, Calendar, Pet ID, Emergency, Offline & Sync, and Security & Privacy.

## Open
Open this folder in Android Studio, let Android Gradle Plugin sync, then Run. It uses applicationId `com.purrfolio.mco1`, minimum SDK 24. **Requires Android SDK + Gradle.** The emulator follows system dark/light mode.

To start the visible Pixel 6 emulator, build this project, install it, and launch PURRFOLIO from Terminal:

```sh
./run-pixel6.sh
```

The emulator command alone only opens Android. If a headless Pixel 6 is already running, `run-pixel6.sh` stops it before opening the visible emulator. Set `ANDROID_HOME` if your SDK is outside `~/Library/Android/sdk`.
Keep that Terminal window open while using the emulator; press Ctrl+C when finished. If Pixel 6 is already visible, the script simply rebuilds and relaunches the app.

## Current status
The Care Logging module uses Firebase Authentication and Firestore for Quick Log, confirmed voice capture, per-pet history, and edit/delete operations. Sound observations save metadata to Firestore while the audio stays in private storage on the recording device, following the selected no-Blaze setup. Other modules remain frontend prototypes with local pet profiles and reminders. Calendar export, public sharing, QR services, notifications, and emergency services still require their separate integrations.

Place this project's Firebase Android configuration at `app/google-services.json` before building. See [Care API setup and testing](docs/care-api/README.md) for account requirements, schema, security rules, emulator commands, and the local-audio limitation.

## Rubric alignment
- UX: pet-first sections and related records together.
- Navigation: persistent five-tab bottom navigation, with pet-switch control.
- Titles: descriptive headings and supporting captions in each view.
- Messaging: short, warm microcopy with accurate frontend-only status.
- Project brief: reflects approved MCO1 functional categories.
- Graphics: supplied PURRFOLIO wordmark, pet photography, lavender on charcoal, rounded surfaces.

## Remaining polish / limitations
Some dynamic screens are deliberately simplified from the source reference rather than pixel-exact screenshots. Original screenshots were embedded as small composite images in the Word file; they were not used as full-screen fake image UI. Screens contain actual Android widgets. The care module signs into existing Firebase accounts; the other account-management flows, sharing, and emergency services still need their separate backend work.


## Visual-fidelity revision
- Added screen-safe native `WindowInsets` padding for the status bar, camera cutout and gesture navigation on Android 15+.
- Introduced a consistently placed back control and contextual titles for secondary screens.
- Added date strip and personalized heading to home and polished floating-style bottom navigation.
- Kept supplied PURRFOLIO logo imagery and both theme palettes.
- Includes `CODEX_VISUAL_FIDELITY_PROMPT.md` with the exact requested Codex prompt.
- This package is still a frontend prototype. It has NOT been built on an Android SDK or compared screen-by-screen on an emulator; therefore it is not guaranteed pixel-exact. Fredoka and Nunito font assets are not included; Android uses its rounded/system fallbacks.

## Seven-screen reference reconstruction

The reference presentations live in `ReferenceScreens.java`, with Canvas icons and wellness arcs in `CareArtwork.java` and `PetRings.java`. Local profiles, care-log persistence, and forms remain in `MainActivity.java`. Secondary screens live in `PhaseTwoScreens.java`. Home's care schedule and wellness insights are illustrative reference data; clinical records and public sitter sharing are not fabricated.

Navigation: welcome → existing account/login flows → Home; pet avatar → overview; center plus → Log Care; Care dock tab → timeline; overview options → Passport or Sitter Mode. More also links to Sitter Mode.

Build with JDK 17 or 21:

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

Debug-only screenshot entry points accept `--es preview_screen 'Pet Passport'` (or Welcome, Home, Pet Detail, Care, Pet Timeline, Care Circle). They do not alter stored profiles or logs. Release builds ignore this extra.

See `docs/visual-qa/` for screen checklists, actual emulator captures, navigation smoke checks, asset provenance, and remaining fidelity limitations. Reference screens use system light/dark mode with identical geometry and real Android system bars.
