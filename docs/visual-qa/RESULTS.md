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

## Phase 1 — Re-check after the Java port — 2026-11-13

Master moved the app from Kotlin to Java (PR #4), so the Phase 1 files were ported and merged on branch `leon/phase1-java-port` (merge commit `9461c0f`, on top of master `12c1cb3`). The pet screens were then re-checked on the Pixel_6a emulator (API 33, 1080 × 2400), debug build, light mode, font scale 1.0×.

### Build

- `./gradlew :app:assembleDebug` with Android Studio's bundled JDK: **passed**. It needs `app/google-services.json` (copy `google-services.json.example` for a local build).
- `./gradlew :app:lintDebug`: **0 errors, 122 warnings**. The 1 error from the Kotlin build (`CareArtwork` `NewApi`) is gone because master's Java `CareArtwork` does not have it. The 2026-11-07 numbers above describe the Kotlin code and no longer apply.

### Functional flows (Java build)

| Check | Result |
| --- | --- |
| My Pets on first launch | Pass. Mochi shows "Selected"; both demo pets are listed. Seeded pets now have ids. |
| Add Pet: empty name | Pass. Red error appears and focus moves to the field. |
| Add Pet: duplicate name | Pass. Hint "You already have a pet named Mochi. You can still save." |
| Add Pet: rotation while typing | Pass. Draft, step and hint survive. |
| Add Pet: Skip this step, then Save pet with weight 200 | Pass. Save is blocked and the flow returns to Step 3 with the weight error. |
| Add Pet: save | Pass. New pet is added and selected; `weightKg: "4.2"` is stored. |
| Pet Profile | Pass. Birthday with age, "Not added yet" for empty weight, care and records links. |
| Edit Pet: dirty Back | Pass. "Discard changes?" appears; Discard keeps the saved name unchanged. |
| Edit Pet: save | Pass. Name updates, and the record keeps its other keys (id, notes, birthday). |
| Photo picker | Pass. Picker opens and cancels cleanly; a picked image is copied to `files/pets/` as a JPEG, survives rotation, and the file is deleted when the form is discarded. |
| Existing pet's photo: choose (Pet Detail photo → "Choose from device") | Pass. Picked image is copied to `files/pets/<petId>-<time>.jpg` and shown. |
| Existing pet's photo: replace | Pass. Exactly one file remains and it has the new name; the old copy is deleted. |
| Existing pet's photo: remove | Pass. File deleted and `photoUri` removed from the stored pet. |
| Switch pets from My Pets | Pass. Tapping Bailey opens Bailey's Pet Detail; going back shows "Selected" on Bailey. |
| Demo pet "Add to my pets" | Pass. Dialog appears; Luna is saved with a real UUID id (not `demo-luna`), becomes Selected, and a toast confirms. |
| Crashes | None in logcat during the pass. |

### Not covered in this re-check

- Dark mode and font scale 1.3× / 2.0×. These were checked on the Kotlin build (section above); the Java port did not change any layout code, but it was not re-captured.
- Add Pet "Other" species field, future-date blocking in the calendar, and the Review step's Change links.
- Changing an existing pet's photo from the Edit Pet form (the Pet Detail photo menu was exercised, and Add Pet's photo path).
- TalkBack, Accessibility Scanner, a 360dp-wide screen, process death mid-flow, and a device without Google Play services.

### Notes

- Ported behaviour differences are API-shape only (`PetPhotoStore.importPhoto`, `PetDraft.isDirty()`, `PetRules.isDuplicateName(name, selfId, List<Pet>)`). See the update note at the top of `docs/phase1/HANDOFF.md`.
- Pet Detail's centre picture is master's fixed artwork (a cat even for Bailey). It does not use the saved pet photo. Not changed by this branch.
- The selected pet still resets to the first pet after a restart (`petIndex` is not saved). Same as before this branch.

## Pet Passport (5.7) — 2026-11-19

Branch `leon/5-7-pet-passport` (base master `0404862`; merged into master as PR #6). Checked on the Pixel_6a emulator (API 33, 1080 × 2400), debug build.

### Build

- `./gradlew :app:assembleDebug`: **passed**.
- `./gradlew :app:lintDebug`: **0 errors, 122 warnings**. Same as the Phase 1 baseline; none of the warnings are in the files this branch touches.

### Functional flows

| Check | Result |
| --- | --- |
| Edit Pet: new "Health and emergency" fields | Pass. Saved values show again when Edit is reopened. |
| Storage round trip | Pass. `medicalInfo`, `vetClinic`, `vetPhone`, `emergencyContactName`, `emergencyContactRelation`, `emergencyContactPhone` and `microchipId` are written to `purrfolio_demo.xml` (read with `run-as`). |
| Clear a field | Pass. The key is removed from the stored JSON, and the row shows only what is left (e.g. the phone). |
| Bad phone number | Pass. Red border, specific message replaces the helper text, focus stays on the field. |
| Edit Pet: dirty Back | Pass. "Discard changes?" appears; Discard leaves without saving (no `weightKg` stored). |
| Rotation on Edit | Pass. Typed weight `4.2` survives portrait → landscape → portrait. |
| Passport with no details (Bailey) | Pass. "No passport details yet" card, "Add passport details" button, rows read "Not added yet". |
| Passport with details | Pass. Every saved value is shown; weight now appears with " kg". |
| Call | Pass. Opens the dialer with the number filled in. It does not place the call. |
| Share passport | Pass. Confirm dialog, then the system share sheet with the text preview. Nothing was sent. |
| Share with nothing to share | Pass. "Nothing to share yet" dialog with Cancel and "Add details". |
| Entry point: Pet Detail ⋯ menu → "Pet Passport" | Pass. |
| Entry point: Pet Profile "Pet passport" link | Pass. Back returns to Pet Profile. |
| Crashes | None. `logcat -b crash` and `FATAL EXCEPTION` counts were 0. |

### Accessibility (measured, not heard)

| Check | Result |
| --- | --- |
| Tap targets (`uiautomator dump`) | Pass. Rows 61dp tall, Call 52 × 48dp, Share 52dp. |
| Labels | Each row is one stop with a specific sentence ("Veterinarian, Paws Clinic. Double tap to edit."). Call buttons say who they call. |
| Contrast | Pass. 9 colour pairs checked; lowest is 4.77:1 (row label on card, light mode). |
| Dark mode | Pass visually. |
| Font scale 2.0× | Passport text wraps cleanly and Share is reachable. |

### Known issue (not changed here)

- At font scale 2.0× the bottom dock's labels are clipped on every screen. Cause: fixed heights in the shared dock styles in `app/src/main/res/values/styles.xml` (`PurrfolioDockCell` 66dp, `PurrfolioDockLabel` 21dp). Needs a team decision because it is a shared file. An earlier theory that it came from the 78dp root in `floating_bottom_nav.xml` was wrong; a WRAP_CONTENT test on the Passport screen changed nothing and was reverted.

### Not covered

- TalkBack and Accessibility Scanner were not run. The labels above come from the code and the UI dump, not from listening.
- The "No dialer app" and "No share app" fallback dialogs.
- The Emergency and Sitter screens' links to Passport (they use the same route name but were not clicked).
- The Edit "Health and emergency" section at large font and in dark mode; font scale 1.3×.
- A 360dp-wide screen, and Android 11–12 without Google Play services.

### Notes

- Old `ReferenceScreens.passport()` is now unreachable (the pet route in `MainActivity.render()` returns first). It was removed in a separate cleanup PR.
- Test data used on the emulator (pet "MochiX" with fake vet and contact numbers) is not part of the repo.

## Pet Timeline (5.6) — 2026-11-21

Branch `leon/5-6-pet-timeline` (base master `84e47b1`, then master `4feb0f3` merged in). Checked on the Pixel_6a emulator (API 33, 1080 × 2400), debug build, signed out of Firebase.

### Build and tests

- `./gradlew :app:assembleDebug`: **passed**, also after merging master.
- `./gradlew :app:lintDebug`: **0 errors, 186 warnings**. Master has the same 186. The branch's own base (`84e47b1`) had 179 and so did the branch before the merge, so the timeline adds none. No warning mentions `TimelineScreen`, `TimelineBuilder`, `TimelineEntry` or `PetUi`.
- `./gradlew :app:testDebugUnitTest`: **passed**. New `TimelineBuilderTest` has 13 tests (fixed time zone, locale and "now"); the two test classes that came from master pass too (9 and 17 tests). The tests were not mutation-checked.

### Functional flows

| Check | Result |
| --- | --- |
| Care logs of the selected pet | Pass. Rows grouped Today / Yesterday / date; another pet's log does not appear. |
| Old logs with only a pet name (no `petId`) | Pass. Matched through `entryPetId`. |
| Log with no time | Pass. Shown last under "Earlier". |
| Filter chips (All / Care / Reminders / Documents) | Pass. Chips are 2 per line because "Reminders" broke mid-word at 4 per line. |
| Filter with no rows | Pass. "Nothing to show for … Try another filter." |
| Pet with nothing at all | Pass. "Nothing on the timeline yet" card; "Log care" opens Quick Log. |
| Signed out | Pass. Banner "Sign in to see reminders and documents…" on All, Reminders and Documents; hidden on Care. |
| Show earlier | Pass with 120 seeded logs: 50 rows + "Show earlier (70 more)", then 100 + "(20 more)", then all 120 and no button. Changing the filter resets to 50. |
| Entry: Home summary card → timeline → Back | Pass. Returns to Home (system Back). |
| Entry: dock "Care" cell on Pet Detail → timeline → Back | Pass. Returns to Pet Detail (system Back). Care is highlighted. |
| Crashes | None. `FATAL EXCEPTION` count was 0 in each run. |

Reminder rows (Upcoming / Done / Missed) and document rows were checked with a temporary sample list that is not in the repo, because the signed-out emulator cannot load real ones.

### Accessibility (measured from the UI dump, not heard)

| Check | Result |
| --- | --- |
| Tap targets | Pass. Chips and Back are 48dp; rows are at least 56dp tall. |
| Labels | Each row is one stop with one sentence, e.g. "Care log: Feeding, 2:41 PM. Half a cup of kibble". Reminder rows add the status ("Reminder: Booster shot, missed, 5:36 PM"). |
| Rail | Pass. The line and dot views do not appear in the accessibility tree. |
| Chips | Announced as radio buttons, with checked state. |
| Status | Written as a word (Upcoming, Done, Missed), not only a colour. |
| Dark mode | Pass visually (status colours, banner, rail, chips). Contrast ratios were **not measured**. |
| Font scale 2.0× | Pass. Rows, chips and banner wrap; nothing clipped in the timeline itself. |
| 360dp-wide screen | Pass. No clipping and no horizontal scroll. |

### Known issue (not changed here)

- At font scale 2.0× the bottom dock's labels are clipped, as on every other screen. Same cause as in the Pet Passport section above.

### Not covered

- TalkBack audio and swipe order, Accessibility Scanner, and a measured contrast ratio. Whether the date headings are announced as headings is set in code but not seen in the dump.
- Reminders and documents from real Firestore data (signed in), and the observer being removed when leaving the screen with a live listener.
- Whether local pet ids match the Firestore `pets/{petId}` documents and whether the rules allow reads (question for Rick in `docs/phase1/HANDOFF.md`).
- Font scale 1.3×, landscape, a pet with many reminders, and Android 11–12 without Google Play services.

### Notes

- The Home summary card and the dock's "Care" cell already routed to "Pet Timeline". They now open the real screen instead of the old sample, because `MainActivity.render()` handles the route first.
- Old `ReferenceScreens.timeline()` and `timelineEntry()` are now unreachable; to be removed in a separate cleanup PR (check that `careRow` is still used elsewhere first).
- Test data used on the emulator (up to 120 logs for Mochi) is not part of the repo and was cleared afterwards.
