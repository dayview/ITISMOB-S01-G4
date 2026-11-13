# CLAUDE.md — Purrfolio (ITISMOB-S01-G4)

> **Update 2026-11-13: Java port.** Master migrated the whole app from Kotlin to Java (PR #4) and removed the Kotlin plugin, so the Phase 1 files were ported to Java on branch `leon/phase1-java-port`. File names below now end in `.java`, and `PetModels.kt` became four classes: `Pet.java`, `PetDraft.java`, `PetMigrations.java` and `DemoPets.java`. Behaviour is unchanged. Names that differ from the Kotlin version: `PetPhotoStore.importPhoto()` (was `import()`, a reserved word in Java), `PetDraft.isDirty()` is a method, `PetScreenHost.setBackHandler(Runnable)`, and `PetRules.isDuplicateName` takes a `List<Pet>`. Code snippets and the Kotlin-era verification notes are kept as history; the current QA results are in `docs/visual-qa/RESULTS.md`.

Guidance for Claude Code / Opus when working in this repo. Read fully before editing.
Owner of this workstream: **Leon** (functions tagged `(LEON)` in the MCO1 proposal).

## 1. Mission and hard scope

Purrfolio is an Android pet-care organizer (MCO1 proposal: "Purrfolio: Your Pet's Life in Pawgress").
**Our job here is FRONTEND ONLY.** Screens, navigation, forms, states, accessibility, local on-device data.

Do NOT add: Firebase, network calls, auth, push notifications, Facebook/Instagram APIs, calendar writes,
QR scanning, or any new runtime permission. Where a feature needs a backend, build the UI and show
honest preview copy (the repo already does this, e.g. "Frontend preview"). Never claim something works that does not.

My functions: 2.1 Add Pet, 2.2 View Pet Profile, 2.3 Edit Pet Profile, 2.4 Manage Multiple Pets,
3.1 Care Circle, 5.1 Pet Paperless Inbox, 5.6 Unified Pet Timeline, 5.7 Pet Passport.

**Order of work (dependency order):** Phase 1 = 2.1–2.4 → Phase 2 = 5.7 → Phase 3 = 5.6 → Phase 4 = 5.1 → Phase 5 = 3.1.
Start with Phase 1 only. Stop and report after each phase.

## 2. Current codebase facts (verified from repo, Oct 2026)

- Java (ported from Kotlin, see the note above), **Android Views built programmatically** (no Compose, almost no XML layouts for new screens). `viewBinding = true`.
- `applicationId com.purrfolio.mco1`, minSdk 24, compileSdk/targetSdk 35. Only deps: `androidx.core:core:1.13.0`, `androidx.appcompat:appcompat:1.7.0`.
- `MainActivity.java` (about 645 lines): owns data (`pets/logs/reminders` as `MutableList<JSONObject>` persisted in SharedPreferences `purrfolio_demo`) and routing via `render("Screen Name")` + `screenHistory` back stack.
- `ReferenceScreens.java`: 7 reference screens (Home, Pet Detail, Care, Pet Timeline, Sitter Mode, Pet Passport, Welcome).
- `PhaseTwoScreens.java` (about 943 lines): everything else, incl. `petForm(edit)`, `myPets()`, `petCard()`, `careCircle()`, `records()`.
  Code style is dense one-liners with semicolons. **Do not copy that style into new code**; write readable, multi-line Java.
- Theme tokens are duplicated hex values per file (light/dark pairs). Reuse the same hex values; do not invent new colors.
- Fonts: Fredoka (headings) + Nunito (UI) via `BrandType`. Wordmark must stay an image.
- Visual-QA tooling: `docs/visual-qa/` (capture.py, contact-sheet.py, CHECKLIST.md, RESULTS.md). Debug-only
  `--es preview_screen '<Screen>'` intent extra jumps to a screen (read `MainActivity.java` ~line 91; verify it accepts non-reference screens before relying on it).

## 3. Parallel-work rules (5 teammates, one repo)

- Branch per person/feature: `leon/2-1-add-pet`, `leon/2-4-my-pets`, etc. PR into `master`. Never push to `master` directly.
- **File ownership:** new files for my screens, **in the same flat folder and package as master**
  (`app/src/main/java/com/purrfolio/mco1/`, package `com.purrfolio.mco1`). No sub-packages or new folders, so every
  teammate's imports and the existing structure stay exactly as they are. Prefix by area so ownership is obvious:
  - Phase 1 (done, see §7): `PetRules.java`, `Pet.java`, `PetDraft.java`, `PetMigrations.java`, `DemoPets.java`, `PetPhotoStore.java`, `PetUi.java`, `PetScreenHost.java`, `PetForm.java`,
    `AddPetFlow.java`, `EditPetScreen.java`, `MyPetsScreen.java`, `PetProfileScreen.java`
  - Later phases follow the same pattern: `PassportScreen.java`, `TimelineScreen.java`, `PaperlessInboxScreen.java`, `CareCircleScreen.java`
    (each one screen class + a callback interface, like `PetScreenHost`).
- Touching `MainActivity.java` / `PhaseTwoScreens.java` / `ReferenceScreens.java`: only to (a) delegate to my new file, or (b) delete the code I moved. One-line routing changes. Announce in the PR description so teammates can rebase.
- **Moving code out of shared files is a coordination step:** do it in its own small PR *first*, merge quickly, then everyone rebases. Do not mix refactor and feature in one PR.
- Never reformat files you do not own (no auto-format sweeps).
- Another teammate's functions (4.x Zel, 6.x/7.x Erick, 1.x/5.4/5.5/8.x Theo, 5.2/5.3/9–11 Pao M.) are off limits.

## 4. "Easy to plug in" integration principles

Prefer boring and replaceable over clever.

1. **Platform/androidx first.** Add a new dependency only if (a) it removes real complexity, (b) it needs no new permission, (c) it's a one-line Gradle change. Justify every dependency in the PR.
2. **Introduce a data seam, not a framework.** Add a plain Java `Pet` class and a small `PetRepository` interface with a local implementation wrapping the existing JSON/SharedPreferences. UI talks to the interface only. When Firebase arrives, only the implementation changes. No DI library, no Room, no Compose migration.
3. **Keep the Views stack.** Do not migrate to Compose or Material3 theming now (changing the theme parent affects every teammate's screens).
4. **One screen = one class/file, one entry function, callbacks passed in** (`onSaved`, `onBack`, `onNavigate`). No screen reads another screen's internals.
5. User-facing text stays in the screen files for now (the repo has no `strings.xml` yet). When the team adds one, move strings in a single dedicated PR.

## 5. UX / accessibility standards (research summary — treat as acceptance criteria)

Sources: developer.android.com accessibility guides, Material Design text-field guidance, WCAG 2.2, NN/g form guidance.

**Touch & focus**
- Every tappable element has a touch target **≥ 48dp × 48dp** (Android guidance; WCAG 2.2 SC 2.5.8 AA minimum is 24px, so 48dp comfortably passes). Visual size may be smaller if padding/minWidth/minHeight make up the 48dp.
- Focus indicator must be visible, ≥2px, ≥3:1 against its surroundings (WCAG 2.4.13 / 1.4.11). Check keyboard/D-pad and TalkBack focus on every control.

**Contrast**
- Text < 18pt (or < 14pt bold): **≥ 4.5:1**; larger text ≥ 3:1. UI component boundaries and meaningful icons: **≥ 3:1** (WCAG 1.4.11).
- Measured on the current palette: all text pairs pass in light and dark. **The `outline` color fails 3:1 against `bg`/`surface` (light ≈1.3–1.4:1, dark ≈1.4–1.5:1).** Any input field border that is the *only* cue showing where to tap must use a ≥3:1 color (e.g. `primary` or `sub`). Decorative card borders may stay.

**Labels & semantics**
- `contentDescription` states purpose/result, not visual detail; **don't include the control type** ("Back", not "Back button"); each item in a list must have a unique description (include the pet name); decorative views use `importantForAccessibility="no"`.
- Group row-like cards into one TalkBack stop with a single description, e.g. "Mochi, Cat, 2 years old, selected".
- Don't convey state by color/stroke alone: selected pet must also expose text/icon + `stateDescription`.
- Use real roles: clickable `TextView`/`FrameLayout` need `ViewCompat.setAccessibilityDelegate` or `AccessibilityNodeInfoCompat.setRoleDescription`/class name `Button` so TalkBack says "button".
- Text sizes in `sp`; heights via `minHeight`/wrap-content, **not fixed dp**, so layouts survive Android font scale 1.3× and 2.0×.

**Forms (NN/g + Material)**
- Single column; labels **above** fields and always visible. Never use placeholder as the only label. Short paired fields (Weight + Sex) may share a row.
- Mark *optional* fields with "(optional)" instead of asterisks on required ones; group required first.
- Ask for the minimum. Pet name is the only truly required field.
- Validate on submit and on blur (not while typing the first time). Error text is specific and tells how to fix ("Enter your pet's name"), **replaces helper text** (don't stack under it), and is announced to TalkBack; move focus to the first invalid field.
- Don't disable the Save button to "block" invalid input; show errors on tap instead.
- Dates: use a picker constrained by rules (birthday: `maxDate = today`), store ISO `yyyy-MM-dd`, display locale format. For distant dates Material/others recommend typed input over a calendar; pets are rarely >25 years old so a `DatePickerDialog` with year-first selection is acceptable. Birthday must be optional (rescues, unknown ages) — consider "Approximate age" later; ask team first.
- Destructive or lossy actions need confirmation + undo where possible; back from a dirty form asks "Discard changes?".

**States (every screen)**: loading (if any), empty (explain + one clear action), content, error/validation, offline-safe (frontend only → local data always available). Empty states must not look like broken screens.

**System/UX heuristics to review against (NN/g 10):** visibility of system status, match with real world (plain pet-owner language), user control & freedom (cancel/undo), consistency (reuse existing components), error prevention, recognition over recall.

## 6. Research findings that decide implementation choices

| Need | Choice | Why / caveat |
|---|---|---|
| Pet photo picking | `ActivityResultContracts.PickVisualMedia(ImageOnly)` | No storage permission, privacy-friendly, in androidx.activity. On Android 4.4–12 uses Google Play services back-port when the manifest `ModuleDependencies` service entry is added; devices without Play services fall back to `GET_CONTENT`. **Replace current `OpenDocument` + `takePersistableUriPermission`.** Picker URIs are not guaranteed persistent → **copy the chosen image into app-private storage (`filesDir/pets/<petId>.jpg`) and store that path.** Verify on emulator. |
| Birthday | Framework `DatePickerDialog` (zero deps) | Avoids pulling in Material Components. `maxDate = today`. The old form already used a picker but allowed no clearing and had no date limit. |
| Form validation UI | Existing `EditText` + `setError` or a small `FieldView` wrapper in `ui/` | Material Components' `TextInputLayout` is nicer but needs the theme change we are avoiding. Revisit only if team agrees. |
| Later: document capture (5.1) | ML Kit Document Scanner (`play-services-mlkit-document-scanner`) with fallback to `OpenDocument` | Scanner needs no camera permission, runs on-device, returns JPEG/PDF, needs Google Play services and API 21+. It is a dependency → decide in Phase 4, keep `OpenDocument` as the no-dependency default. |
| Later: Passport export/share (5.7) | `android.graphics.pdf.PdfDocument` + `FileProvider` + `Intent.ACTION_SEND` | All framework/androidx. FileProvider needs a manifest entry (shared file → coordinate). |
| Later: Care Circle invite (3.1) | `ACTION_SEND` text invite via Sharesheet; roles chosen in UI | No backend → show invite as "Pending (preview)". Don't fake acceptance. |
| Later: Timeline (5.6) | `RecyclerView` (androidx) over a merged, sorted list of log/reminder/document items | Add `androidx.recyclerview` only when the data is real; use sticky date headers; each row one TalkBack stop. |

## 7. Phase 1 — functions 2.1–2.4 (reference implementation DONE)

**Status:** Leon is hand-building Phase 1 from the reference implementation in `PHASE1_GUIDE.md` / the delivered files. Do not rewrite it. When asked to continue, review it, fix defects, and use it as the pattern for later phases (screen class + callback interface + draft owned by MainActivity + shared `PetUi` kit). Compile-checked against Android SDK stubs only; **not yet run on an emulator or built with Gradle**, so the first real build/run may surface fixes.

Decisions made: Add Pet is a real 4-step flow (Photo+name → Species/breed/sex → Details → Review). Demo pets (Luna, Simba) are kept for development but only appear in debuggable builds, and are saved only if the tester taps "Add to my pets". A new "Pet Profile" screen implements 2.2 (reachable from the ⋯ menu on Pet Detail).

### Original gap analysis (for reference)

Goal: Add / View / Edit / Switch pets are complete, accessible, and data-correct on-device. Keep wireframe look (see `docs/phase2/references/09-add-your-pet.png`, `10-pet-overview-existing.png`, `11-edit-pet-profile.png`, `12-my-pets.png`) — improve behavior and accessibility, do not redesign.

Known issues found in the current code (fix these):

1. **Birthday:** the old field opened a date picker but allowed future dates, couldn't be cleared, and showed a misleading `YYYY-MM-DD` hint. → picker with `maxDate = today`, optional, "Clear", stored ISO.
2. **Weight stored under SharedPreferences key `pet-ui-weight:<name>`** — breaks on rename/duplicate names and is outside the pet record. → move into the pet JSON (`weightKg`, keyed by `petId`), migrate the old key once.
3. **Pet ages hard-coded** in `myPets()` ("2 years old" / "3 years old"). → compute from birthday; show "Age not set" when absent.
4. **Demo pets (Luna, Simba)** appear in My Pets when fewer than 4 real pets. They're labelled Demo but can mislead. → ask team; default plan: hide them once the user has ≥1 real pet.
5. **Add Pet shows a 4-step indicator** (`steps(4)`) but the flow has one screen. → either wire a real multi-step flow (photo → basics → details → done) or drop the indicator. Ask Leon before choosing; wireframe shows steps.
6. **`petCard()` is not accessible:** no `contentDescription`, no role, selected state is a stroke only. → one TalkBack stop "Mochi, Cat, 2 years old, selected"; add check icon for selected.
7. **Touch targets under 48dp:** header back/extra buttons are laid out at 40–44dp, "Skip" is a bare `TextView` 42dp wide. → ≥48dp via padding.
8. **Fixed-height rows** (`add(view, heightDp)`) risk clipping at large font scale. → switch the form/cards to `minHeight`.
9. **Validation is minimal:** only blank name. → trim, max length (e.g., 40), duplicate-name hint (non-blocking), focus + announce first error; helper text replaced by error text.
10. **No discard-changes protection** on Add/Edit when pressing Back. → confirm if dirty.
11. **Photo:** see §6 (PickVisualMedia + copy to `filesDir`); handle "remove photo" by deleting the copied file; handle missing/deleted file with the default portrait.
12. **Pet Detail (2.2):** metrics/wellness values are illustrative. Label them "Sample" or replace with real fields (name, species, breed, age, sex, weight, notes) and show an empty state for records not yet added; keep links to Records/Timeline/Passport.
13. **Input borders:** outline color fails 3:1 (see §5). Fix for form fields only.
14. **My Pets (2.4):** switching pets must update Home, Pet Detail, Care History, Records consistently (state keyed by `petId`, never by name). Add a unit-free manual test list (§8).

Deliverables for Phase 1:
- `Pet.java` (mapper to/from existing `JSONObject`), `PetRepository` + local impl, `PetForm.java`, `PetScreens.java` (+ `ui/` helpers).
- Updated `strings.xml`.
- Extended `docs/visual-qa/` captures for: Add Your Pet, Edit Pet Profile, My Pets, Pet Detail — **light + dark**, plus 1.3× and 2.0× font scale, plus one TalkBack walkthrough note.
- PR description listing every checklist item below.

## 8. Definition of done (per phase)

Build & lint
- `./gradlew :app:assembleDebug :app:lintDebug` with **JDK 17 or 21** (JDK 25 is unsupported). Lint: 0 errors; do not add new warnings. Turn the `ContentDescription` lint to error for files I own.

Manual accessibility (record results in `docs/visual-qa/RESULTS.md`)
- [ ] TalkBack: every control reachable, announced with purpose, no duplicate/unlabeled stops, logical order, errors announced.
- [ ] Android Accessibility Scanner: no touch-target, contrast, or label findings on my screens.
- [ ] Font scale 1.3× and 2.0×: no clipped/overlapping text, primary action still reachable (scrolls).
- [ ] Display size "largest": layouts still usable.
- [ ] Light and dark mode: identical geometry, contrast pass (§5).
- [ ] 360dp-wide and a tall/short emulator; gesture and 3-button navigation; status bar/cutout insets respected (existing `WindowInsetsCompat` owner stays the single owner — no duplicate padding).
- [ ] Rotation / process death: form input and selected pet survive (`onSaveInstanceState` exists; extend for form fields).

Functional smoke (Phase 1)
- [ ] Add pet with only a name → saved, appears in My Pets, becomes selected, Pet Detail shows empty states.
- [ ] Add pet with photo + birthday + weight → values persist after app restart.
- [ ] Rename a pet → logs/records still attach to it (keyed by `petId`).
- [ ] Edit → Back with changes shows discard prompt; without changes goes straight back.
- [ ] Two pets with the same name behave independently.
- [ ] Switch pets from Home, My Pets, and the pet chooser → every screen reflects the switch.
- [ ] Invalid input shows specific error, focuses field, TalkBack reads it.

## 9. Visual QA tooling notes

- `docs/visual-qa/capture.py` hard-codes `adb='/Users/zel/Library/Android/sdk/platform-tools/adb'` and only captures 7 reference screens. Make the adb path come from `ANDROID_HOME` (or `adb` on PATH) and accept extra screen names. Coordinate with Zel (owner of that script) — small PR.
- `run-pixel6.sh` assumes macOS SDK path (`~/Library/Android/sdk`); set `ANDROID_HOME` on Windows/Linux.
- Screenshots go in `docs/visual-qa/{light,dark}/`; keep file names numbered like existing ones.

## 10. Working style for Claude

- Plan first, list files you'll touch, then make small commits (one concern each: `refactor:`, `feat:`, `fix:`, `a11y:`, `docs:`).
- Read before you edit; keep the existing look and wireframe fidelity; don't "improve" other people's screens.
- Don't silently change shared behavior (navigation, theme, data keys). If a change affects teammates, say so in the PR.
- Prefer deleting complexity to adding it. If a choice is between a dependency and ~30 lines of plain Java, choose the plain Java.
- If something in this file conflicts with the wireframe/proposal, stop and ask Leon.
- Be honest in UI copy and docs: unimplemented = labelled preview, never faked.

## 11. Later phases — pointers (do not start yet)

- **5.7 Pet Passport:** show real pet fields; "Not provided" rows become editable inside Edit Pet Profile (allergies, vet, microchip, emergency contact — add to `Pet`); export via PdfDocument; privacy note that sharing is user-initiated.
- **5.6 Timeline:** merge care logs + reminders + documents; filter chips by type; date headers; empty state with "Log care" action; one TalkBack stop per entry ("Fed Miso, 7:30 AM, by You").
- **5.1 Paperless Inbox:** choose pet + document type, preview before saving, copy file to `filesDir/docs/<petId>/`, list with thumbnails/icons; scanner decision per §6.
- **3.1 Care Circle:** invite sheet (name/email + role), roles list, pending/accepted states labelled preview, owner-only controls hidden for caregivers; link to teammate-owned 11.1/11.2 screens, don't build them.
