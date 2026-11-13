# HANDOFF: Purrfolio Phase 1 (functions 2.1 to 2.4)

> **Update 2026-11-13: Java port.** Master migrated the whole app from Kotlin to Java (PR #4) and removed the Kotlin plugin, so the Phase 1 files were ported to Java on branch `leon/phase1-java-port`. File names below now end in `.java`, and `PetModels.kt` became four classes: `Pet.java`, `PetDraft.java`, `PetMigrations.java` and `DemoPets.java`. Behaviour is unchanged. Names that differ from the Kotlin version: `PetPhotoStore.importPhoto()` (was `import()`, a reserved word in Java), `PetDraft.isDirty()` is a method, `PetScreenHost.setBackHandler(Runnable)`, and `PetRules.isDuplicateName` takes a `List<Pet>`. Code snippets and the Kotlin-era verification notes are kept as history; the current QA results are in `docs/visual-qa/RESULTS.md`.

Read this first, then `CLAUDE.md`, then `docs/PHASE1_GUIDE.md`. Written 2026-10-31 at the end of a planning and code-drafting session in Claude.ai. Nothing from that chat carries over except what is in these files.

## 1. How to work with Leon

- **Leon is writing the code himself to learn.** Default to explaining, reviewing, and answering questions. Do not edit or create files unless he asks. Show proposed changes as snippets first.
- Keep the default permission mode so every edit needs approval.
- If something here conflicts with the wireframes or the MCO1 proposal, stop and ask Leon.

## 2. Project in one paragraph

Purrfolio (repo `dayview/ITISMOB-S01-G4`) is an Android pet-care organizer for the ITISMOB course (MCO1 proposal: "Purrfolio: Your Pet's Life in Pawgress"). Java (ported from Kotlin, see the note above), **Views built programmatically** (no Compose), minSdk 24, compileSdk/targetSdk 35, only `core` and `appcompat` as dependencies. Pets, logs and reminders are `JSONObject` lists in SharedPreferences (`purrfolio_demo`), owned by `MainActivity`. Navigation is `MainActivity.render("Screen Name")` with a string back stack.

**Scope: frontend only.** No Firebase, network calls, auth, new permissions, or fake "it works" claims. Unfinished features are labelled previews.

## 3. Leon's functions and order

2.1 Add Pet, 2.2 View Pet Profile, 2.3 Edit Pet Profile, 2.4 Manage Multiple Pets, 3.1 Care Circle, 5.1 Pet Paperless Inbox, 5.6 Unified Pet Timeline, 5.7 Pet Passport.

Order of work: **Phase 1 = 2.1 to 2.4**, then 5.7, then 5.6, then 5.1, then 3.1. Other teammates' functions are off limits.

## 4. Decisions already made (do not reopen without asking)

| Decision | Detail |
|---|---|
| Code layout | Same flat folder and package as master: `app/src/main/java/com/purrfolio/mco1/`. No sub-packages. |
| Add Pet | Real 4-step flow: Photo + name, Species/breed/sex, Optional details, Review. Only the name is required. |
| Demo pets | Luna and Simba stay for development, shown only in debuggable builds; saved only if the tester taps "Add to my pets". |
| View Pet Profile (2.2) | New `PetProfileScreen`, reached from the ⋯ menu on Pet Detail ("Pet profile" is the first item). |
| Photos | `PickVisualMedia` picker, copied into `filesDir/pets/` as a JPEG, stored as a `file://` URI string. |
| Data seam | `Pet` class converts to and from the existing JSON. Screens use the `PetScreenHost` interface and never touch `MainActivity` directly. |
| Draft state | `PetDraft` lives in `MainActivity` and is saved in the instance-state Bundle, so typing survives the photo picker and rotation. |
| Forms | Single column, labels above, errors replace helper text, optional fields marked "(optional)". Chips for Species and Sex. |
| Accessibility bar | 48dp touch targets, 4.5:1 text contrast, 3:1 for input borders (`fieldBorder`), TalkBack roles and labels, visible focus rings. |
| Dependencies | None added. ML Kit Document Scanner is only a candidate for 5.1, to be decided in that phase. |
| Strings | The repo has no `strings.xml`. Text stays in the screen files for now. |

## 5. What exists

Delivered as `purrfolio-phase1.zip` (unzips at the repo root):

- 13 new files in `app/src/main/java/com/purrfolio/mco1/`: `PetRules.java`, `Pet.java`, `PetDraft.java`, `PetMigrations.java`, `DemoPets.java`, `PetPhotoStore.java`, `PetUi.java`, `PetScreenHost.java`, `PetForm.java`, `AddPetFlow.java`, `EditPetScreen.java`, `MyPetsScreen.java`, `PetProfileScreen.java`.
- (Kotlin-era `phase1-shared-files.patch`, no longer applies; the edits are now committed on the branch) small edits to `MainActivity.java` (about 150 added lines, wiring only), `ReferenceScreens.java` (`petOptions()` adds "Pet profile"), and `AndroidManifest.xml` (photo-picker back-port service entry).
- `docs/visual-qa/capture_pets.py`, `docs/PHASE1_GUIDE.md`, `CLAUDE.md`.

Leon has been (or is about to be) adding these by hand on his own branch (suggested name `leon/phase1-pets`, created from an up-to-date `master`). **Check the actual state with `git status` and the audit prompt in section 8; do not assume it is applied.**

## 6. How well verified (be honest about this)

**Checked:**
- Kotlin version: all 10 files compiled against the Android SDK (API 34 stubs) with Kotlin 2.0.21. Java port: builds with `./gradlew :app:assembleDebug` and passed the emulator QA in `docs/visual-qa/RESULTS.md`.
- The new `MainActivity` block compiles in a test harness with stubbed members.
- The patch applies cleanly to a fresh clone of master.
- `PetRules` passes 29 unit-style checks (ages, validation, dates, duplicates).

**Not checked:**
- No full Gradle/AGP build, no lint run, no emulator or device run, no TalkBack pass.
- The small edits inside `render()`, `onCreate`, `onSaveInstanceState`, `navigateBack()` and `restore()` were reviewed by eye only.
- The real `androidx.activity` `PickVisualMedia` API was used from memory of its signature (`PickVisualMediaRequest(PickVisualMedia.ImageOnly)`), not compiled against the real library.

Expect the first real build to surface small errors. Fix them as they appear.

## 7. Known risks and things to watch

1. **Photo picker** falls back to the system picker on devices without Google Play services (Android 11 to 12). Test on an emulator image without Play Store too.
2. **Back stack:** after *adding* a pet, `commitPet` calls `render("Pet Detail", recordHistory = false)` so Back does not reopen the form; after *editing* it calls `navigateBack()`. Verify both from a cold start and after rotation.
3. **Draft cleanup:** `render()` discards the draft (and a newly copied photo) when leaving a form screen. Check no orphan files remain in `filesDir/pets/`.
4. **Old code is now bypassed, not deleted:** `petForm()`, `myPets()`, `petCard()` in `PhaseTwoScreens.java` are unused. Remove them in a separate PR after Leon's PR merges.
5. **Wireframe difference:** the forms are single-column with labels above. Weight and Sex are no longer side by side as in the wireframe. The team may want to decide.
6. **Sex chips** cannot be deselected once chosen ("Unknown" is the neutral choice).
7. **Pet Detail** wellness rings and insights are still the wireframe's illustrative sample, already labelled as examples. Not changed.
8. Lint baseline from `docs/visual-qa/RESULTS.md`: 0 errors, 24 warnings. Do not add errors.

## 8. Next steps

1. Confirm what is applied: run the audit prompt below.
2. Build (`./gradlew :app:assembleDebug :app:lintDebug`, JDK 17 or 21) and fix any errors.
3. Run the test checklist in `docs/PHASE1_GUIDE.md` section 5 (functional + TalkBack + font size + light/dark).
4. Capture screenshots with `capture_pets.py` (light, dark, font scale 1.3 and 2.0) and add notes to `docs/visual-qa/RESULTS.md`.
5. Open a PR. In the description, list the three shared files edited so teammates can rebase.
6. Later, in separate PRs: delete the dead code (risk 4), and ask Zel to stop hard-coding the adb path in `docs/visual-qa/capture.py`.
7. Then Phase 2 (5.7 Pet Passport). Follow the same pattern: one screen class, one callback interface, draft or state held by `MainActivity`, shared `PetUi` kit. Pointers are in `CLAUDE.md` section 11.

**Audit prompt for the start of a session:**

> Read docs/HANDOFF.md, CLAUDE.md and docs/PHASE1_GUIDE.md. Do not edit any files. Compare them with the current branch and tell me: (1) which Phase 1 files exist, (2) which of the 9 MainActivity edits in step 8 of the guide are done, (3) what is left, in order.

## 9. Open questions for the team (not blockers)

- Keep the single-column form layout, or restore the wireframe's side-by-side Weight and Sex?
- Add a `strings.xml` (one dedicated PR) or keep strings inline?
- For 5.1, adopt the ML Kit Document Scanner (adds a Play services dependency) or keep the plain file picker?
