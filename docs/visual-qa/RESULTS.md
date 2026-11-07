# Visual verification — 2026-10-28

## Build and functional checks

- `./gradlew :app:assembleDebug :app:lintDebug` with JDK 21: **passed**.
- Final lint: **0 errors, 24 warnings**. Remaining warnings concern static Canvas allocations, programmatic custom-view constructors, existing unused resources/RTL alignment, and available dependency updates.
- Install on Pixel 6 emulator: passed.
- Actual visible-control smoke checks: Home center button → Log Care → Water opens the existing form with Water selected; pet options → Passport → Share Passport opens the existing integration placeholder. No test care records or public links were created.
- Backend, package name, persistence schema, local profile editing and care-entry saving remain in the existing Activity.

## Screenshot review

All seven dark and all seven light screens were captured on the same emulator: 1080 × 2400, density 420. Real Android clock, cutout/status area, and gesture region are retained. Contact sheets are in `dark-contact-sheet.png` and `light-contact-sheet.png`; full-size captures are in `dark/` and `light/`.

The supplied reference composites were used for visual review in the conversation. They were not available as local image files, so no numeric pixel-difference score or direct automated color sampling is claimed.

| Screen | Inspected and refined |
| --- | --- |
| Onboarding | Wordmark image proportions, transparent cat composition, heart/sparkles, subtitle, action pills, social marks and dots. Removed excess gap between cat and primary button. |
| Home | Two-line greeting, avatar ring, seven weekday rings, selected capsule contrast, care summary, four icon rows and dock. |
| Overview | Actual Canvas tracks/arcs with round caps; revised inner-ring spacing, lavender-to-blue arc, photo crop, face badges, four metrics and Insights. |
| Log Care | Top sheet geometry, handle/close, two-column circular grid, illustrated icons and expanded voice panel. Smaller-width circle sizing adapts to available width. |
| Timeline | Seven capsules, continuous colored rail and markers, four today activities and tomorrow vet card; navigation remains above system gestures. |
| Sitter | Mint header dot, reconstructed cat/dog art, descriptive copy, visible lavender switch, inset URL and four information rows. Sharing explicitly remains a preview. |
| Passport | Selected pet identity, seven separated information rows, edit/share controls and dock. Missing clinical/contact data are identified instead of inventing records. |

Light-mode review checked the same geometry and replaced low-contrast pastel metric text with deeper green/blue values.

## Fidelity limits

This is a reference-based reconstruction, **not a claim of pixel identity**. The original supplied local cat was an incomplete square crop and the wordmark had an opaque background. Transparent sibling assets were reconstructed using the built-in image tool; the pet face, fur, lettering edges and sitter illustration differ from the composite references. Original assets remain bundled unchanged. See `ASSETS.md` for paths and prompts.

Native Android aspect ratio and system insets produce different vertical breathing room than the reference phones. Icons are custom approximations, not extracted originals. The reference-only care schedule and wellness insights remain illustrative; unavailable clinical records and unsupported sitter/public-sharing operations are deliberately not presented as real data or successful integrations.

These limitations matter for strict visual acceptance: the original full-resolution photo/illustration/transparent logo artwork would be needed to eliminate asset differences. The captured UI is directly reviewable alongside the supplied references.

## Compact viewport / system navigation

Supplemental captures in `compact/` use 1080 × 2400 at density 480 (360dp wide). Home was inspected with both gesture and three-button system navigation; the dock stays above the system controls. The Log Care check exposed a compressed voice panel; the scroll child now measures its content height and the voice panel retains its full height, permitting vertical scrolling on short screens. Emulator density and navigation mode were restored after the checks.

## Phase 1 — Pet screens (2.1 Add, 2.2 Profile, 2.3 Edit, 2.4 My Pets) — 2026-11-07

Checked on the API 33 emulator (1080 × 2400), debug build, branch `leon/phase1`.

### Build and lint

- `./gradlew assembleDebug lint` with JDK 21: build compiles; lint reports **1 error, 119 warnings**.
- The 1 error is `CareArtwork.kt:26` (`NewApi`, `TypedArray.use {}` needs API 31, minSdk is 24). It is **not part of Phase 1**: the file is unchanged by this branch and the same error exists on `master`. It needs a fix from the file's owner or a separate PR.
- Warnings in the new Pet files: 5 (`PetPhotoStore.kt:7` ExifInterface; `AddPetFlow.kt:132` `SetTextI18n` ×4). The rest are in files this branch does not own.
- The 24-warning baseline recorded above is out of date and cannot be compared with the current count.

### Functional flows

| Function | Result |
| --- | --- |
| 2.1 Add Pet | Pass. Name required, state survives rotation, "Other" shows its field, future birthdays disabled, Review is correct, Back and the discard dialog work. "Skip this step" can no longer save invalid data (weight 0). |
| 2.2 View Profile | Pass. Empty fields read "Not added yet"; shortcut links open their screens. |
| 2.3 Edit Pet | Pass. Clean Back leaves, dirty Back asks "Discard changes?", rename keeps the same id, validation errors show. |
| 2.4 Manage Pets | Pass. Switching pets works and persists across a force-stop; demo pets can be added as real pets. |
| Photos | Pass. Picker opens, photo is copied to private storage; Discard, replace and remove leave no orphan files. |

### Font scale and dark mode

| Screen | Font 2.0× (light) | Dark mode (font 1.3×) |
| --- | --- | --- |
| My Pets | Pass: text wraps, nothing cut off | Pass |
| Add Pet (steps 1, 2, Review) | Pass: titles wrap, Next / Save pet stay pinned and tappable | Pass (step 1) |
| Pet Profile | Pass: rows wrap, scrolls to Edit profile | Pass |
| Edit Pet | Pass: all fields reachable, Save / Cancel visible | Pass, including the red weight error |

### Not covered

- **TalkBack walkthrough** and **Accessibility Scanner**: not run yet (need a person at the device).
- Font scale 1.3× in light mode, a 360dp-wide screen, process death mid-flow, and an image without Google Play services.
- Add Pet step 3 and Review in dark mode.

### Problems seen in other screens (not changed)

- Pet Detail (rings) at font 2.0×: metric values ("Good", "3/4") and the Insights text are clipped.
- Bottom dock at font 2.0×: label descenders are clipped.
- The selected pet resets to the first pet after the app restarts (`petIndex` is not saved). This behavior existed before this branch.
