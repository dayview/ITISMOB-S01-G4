# Phase 1 guide: build functions 2.1 to 2.4 yourself

> **Update 2026-11-13: Java port.** Master migrated the whole app from Kotlin to Java (PR #4) and removed the Kotlin plugin, so the Phase 1 files were ported to Java on branch `leon/phase1-java-port`. File names below now end in `.java`, and `PetModels.kt` became four classes: `Pet.java`, `PetDraft.java`, `PetMigrations.java` and `DemoPets.java`. Behaviour is unchanged. Names that differ from the Kotlin version: `PetPhotoStore.importPhoto()` (was `import()`, a reserved word in Java), `PetDraft.isDirty()` is a method, `PetScreenHost.setBackHandler(Runnable)`, and `PetRules.isDuplicateName` takes a `List<Pet>`. Code snippets and the Kotlin-era verification notes are kept as history; the current QA results are in `docs/visual-qa/RESULTS.md`.

This guide walks you through adding **Add Pet (2.1)**, **View Pet Profile (2.2)**, **Edit Pet Profile (2.3)** and **Manage Multiple Pets (2.4)** step by step, so you understand each piece instead of pasting blindly. Every command here is for **you** to run. Nothing in this package touches your git branches.

## 0. What you received

All files use the **same folder and package as master** (`app/src/main/java/com/purrfolio/mco1/`, package `com.purrfolio.mco1`). No new folders.

| File | New or edited | What it does |
|---|---|---|
| `PetRules.java` | new | Pure logic: validation messages, age text ("2 years old"), date helpers. No Android code. |
| `Pet.java`, `PetDraft.java`, `PetMigrations.java`, `DemoPets.java` | new | `Pet` (with JSON conversion), `PetDraft` (what the user has typed), old-weight migration, and the demo pets. |
| `PetPhotoStore.java` | new | Copies a picked photo into private app storage (fixes rotation, shrinks it). |
| `PetUi.java` | new | Small UI kit: colors, buttons, labeled fields, chips, date field, accessibility helpers. |
| `PetScreenHost.java` | new | The interface that lists what the screens may ask of the app. |
| `PetForm.java` | new | The three field groups and their validation, shared by Add and Edit. |
| `AddPetFlow.java` | new | 2.1: the 4-step Add Pet flow. |
| `EditPetScreen.java` | new | 2.3: one-page edit form. |
| `MyPetsScreen.java` | new | 2.4: pet list, selected pet marked, demo pets in debug builds. |
| `PetProfileScreen.java` | new | 2.2: pet info plus shortcuts to care history, reminders, records, passport, care circle. |
| `MainActivity.java` | edited | Wiring only (about 150 added lines). |
| `ReferenceScreens.java` | edited | One line: adds "Pet profile" to the pet options menu. |
| `AndroidManifest.xml` | edited | Adds the photo-picker back-port entry. |
| `docs/visual-qa/capture_pets.py` | new | Screenshot script for these screens (no hard-coded adb path). |
| `phase1-shared-files.patch` | helper | The three edited files as a patch, if you'd rather apply than type. |

**How well is it checked?** I compiled every new file against the Android SDK (API 34 stubs) with Kotlin 2.0.21 (the project has since moved to Java), compile-checked the new `MainActivity` code in a test harness, and ran 29 checks on `PetRules`. I could **not** run a full Gradle build or an emulator here, so your first real build is the real test. Treat any error as normal and use section 9.

## 1. The idea in six minutes

Five ideas make the whole thing work. Read these before the code.

1. **The draft lives in `MainActivity`, not in a screen.** The old code rebuilt the whole screen after the photo picker closed, so anything typed was lost. Now every keystroke is written into a `PetDraft` object held by `MainActivity`, which is also saved when the phone rotates. Screens can be rebuilt freely.
2. **Screens talk to the app through an interface (`PetScreenHost`).** A screen says `host.savePet(...)` or `host.go("Home")`. It never touches `MainActivity` directly. This keeps screens small and lets you reuse the pattern for 3.1, 5.1, 5.6 and 5.7.
3. **Pets are still stored as JSON.** `Pet` is a plain Java class, and it converts to and from the JSON the app already saves. When Firebase arrives, only that conversion changes, and no screen needs to change.
4. **Photos are copied into private storage.** The Android photo picker gives temporary access only, so a saved link would stop working later. We copy a small JPEG into the app's own folder and save that path. No permission is needed.
5. **Accessibility is built into the helpers.** Buttons are at least 48dp, fields have visible labels above them, errors replace helper text and are announced, chips are announced as radio buttons, and focus rings are visible. Because every screen uses `PetUi`, you get this for free.

Data flow for "Add a pet":

```
User types  ->  PetForm writes into PetDraft  ->  Next / Save  ->  PetRules validates
   -> host.savePet(pet)  ->  MainActivity.commitPet  ->  JSON in SharedPreferences  ->  Pet Detail
```

## 2. Before you type anything

Run these yourself in a terminal at the repo root:

```bash
git switch master            # go to master
git pull                     # get the latest from your team
git switch -c leon/phase1-pets   # create your own branch from it
```

Check that Android Studio builds the untouched project first (see section 4). If it doesn't build now, fix that before adding anything.

Unzip the package at the repo root. Because the zip mirrors your folders, the files drop into the right place automatically.

## 3. Build order (one step at a time)

The order below is chosen so **the project still builds after every step**. Nothing uses the new files until step 8, so the old screens keep working while you add the new ones.

After each step: **Build > Make Project** in Android Studio (or `./gradlew :app:assembleDebug`). It should stay green.

### Step 1: `PetRules.java` (the logic)
Read it top to bottom. Key functions:
- `parseIso` / `toIso`: dates are stored as `yyyy-MM-dd`. `parseIso("2026-02-30")` returns null (not a real date).
- `ageLabel`: turns a birthday into "2 years old", "8 months old", or "3 weeks old".
- `nameError`, `weightError`, `birthdayError`, `notesError`: return a **message** or `null`. A message tells the user how to fix it (for example "Enter your pet's name").
- `isDuplicateName`: used for a gentle hint, never to block saving.

Why separate: it has no Android code, so you can test it quickly. Try changing a rule (say, `NAME_MAX`) and notice nothing else needs to change.

### Step 2: `Pet.java`, `PetDraft.java`, `PetMigrations.java`, `DemoPets.java` (the data)
- `Pet`: one pet. `toJson()` makes a new JSON record. `writeInto(json)` updates an existing one and **keeps keys it doesn't know about**, so teammates' data on the same record isn't lost.
- `PetDraft`: the in-progress form. `isDirty()` compares the current values with the snapshot taken at the start, which powers "Discard changes?". `toBundle()` / `fromBundle()` let it survive rotation.
- `DemoPets`: Luna and Simba. `enabled()` is true only when the app is *debuggable*, which is what Android Studio's Run button installs. A release build never shows them.
- `PetMigrations`: the old form saved weight under a key built from the pet's name. This moves it onto the pet record once.

### Step 3: `PetPhotoStore.java` (photos)
`importPhoto()` reads the picked image, fixes its rotation (EXIF), shrinks it to at most 1024px, saves a JPEG in `filesDir/pets/`, and returns a `file://` link. `deleteIfOurs()` only deletes files inside that folder, so it can never remove a user's gallery photo.

### Step 4: `PetUi.java` (the UI kit)
Skim it, then focus on three things:
- **Colors**: same values as the existing screens, plus `fieldBorder`. The old `outline` color was too faint (about 1.4:1) to show where a field is, and WCAG needs 3:1 for that. `fieldBorder` is about 4.5:1 in light mode and 5.4:1 in dark mode.
- **`textField` / `dateField` / `choiceChips`**: label above, helper text below, and an error that replaces the helper. Chips are used for Species and Sex because there are only 3 to 4 options and it's quicker than a dropdown.
- **`role(view, Button.class)`**: plain Views don't tell TalkBack they're buttons. This helper does.

### Step 5: `PetScreenHost.java`
Just an interface. Read the comments. This is the full list of things the pet screens can ask for.

### Step 6: `PetForm.java`
Three groups (`addPhotoAndName`, `addSpecies`, `addDetails`) and four validators. `apply()` shows **every** error, then moves focus to the first bad field. That's why a TalkBack user hears the problem and doesn't have to hunt for it.

### Step 7: the four screens
Add `AddPetFlow.java`, `EditPetScreen.java`, `MyPetsScreen.java`, `PetProfileScreen.java`.
- `AddPetFlow`: `showStep()` clears and redraws one step. Back goes to the previous step; at step 1 it asks before discarding if you typed anything. "Skip this step" on step 3 jumps to Review. Only the name is required.
- `EditPetScreen`: the same form groups on one page.
- `MyPetsScreen`: each card reads as one TalkBack item, for example "Mochi, Cat, Mixed Breed, 2 years old, selected pet". Selected is shown with a border, a check mark and the word "Selected", never colour alone.
- `PetProfileScreen`: info rows open Edit; link rows open the other features (those screens belong to teammates and are only linked).

Build now. Still green? Good. The new code isn't used yet.

### Step 8: wire into `MainActivity.java`
Apply this as nine small edits. (`phase1-shared-files.patch` is from the Kotlin version and no longer applies; the finished edits are in `MainActivity.java` on `leon/phase1-java-port`.) Each edit and why:

1. **Import** `androidx.activity.result.PickVisualMediaRequest`.
2. **Replace the photo picker** (`OpenDocument` becomes `PickVisualMedia`) and swap `pendingNewPetPhotoUri` for two fields: `petDraft` and `formBackHandler`. `PickVisualMedia` needs no storage permission.
3. **`onCreate`**: restore the draft with `PetDraft.fromBundle(state.getBundle("petDraft"))` (inside the `state != null` check).
4. **`onSaveInstanceState`**: save it with `if (petDraft != null) out.putBundle("petDraft", petDraft.toBundle())`.
5. **Rewrite `editPetPhoto()`** (used only by Pet Detail's round photo). It now launches the new picker and deletes only our own photo copies.
6. **`navigateBack()`**: first line `if (formBackHandler != null) { formBackHandler.run(); return; }`. This lets a form use the system Back button for "previous step". Also add `"Pet Profile"` to the fallback list.
7. **`render()`**: reset `formBackHandler`, discard the draft when you leave a form, and add four routes (`Add Your Pet`, `Edit Pet Profile`, `Pets`, `Pet Profile`). These run **before** the old `PhaseTwoScreens` routing, so the old versions are bypassed. Also pass `null` and `adding -> {}` where the old code passed `pendingNewPetPhotoUri` and `editPetPhoto(adding)`.
8. **`restore()`**: run `PetMigrations.moveLegacyWeight(preferences, pets)`.
9. **Add the new block** (before `restore()`): `petHost`, `savedPets()`, `showAddPet/EditPet/MyPets/PetProfile`, `commitPet`, `discardPetDraft`, `launchPhotoPicker`, `onPetPhotoPicked`.

Things worth understanding in step 9:
- `commitPet`, when **adding**, calls `render("Pet Detail", false)`, which replaces the form in the back stack, so Back doesn't reopen an empty form (the old code did). When **editing**, it calls `navigateBack()` to return to where you came from.
- `onPetPhotoPicked` handles two targets: a form draft, or Pet Detail's round photo.

### Step 9: two small edits
- **`ReferenceScreens.java`**, `petOptions()`: add `"Pet profile"` as the first item and shift the other indexes by one (this is how a user reaches 2.2).
- **`AndroidManifest.xml`**: inside `<application>`, add the `ModuleDependencies` service from the patch. It lets Android 11 to 12 phones with Google Play services use the same photo picker.

### 3b. Applying the patch instead (obsolete)
The patch targeted the Kotlin files. Use the nine edits above, or compare with `MainActivity.java` on `leon/phase1-java-port`.

Original commands, kept for history:
```bash
git apply --check phase1-shared-files.patch   # dry run, changes nothing
git apply phase1-shared-files.patch           # applies the three edits
```
If the check fails, someone has changed those files since master was captured. Do the edits by hand using step 8.

## 4. Build and run

Use **JDK 17 or 21** (JDK 25 is not supported by your Gradle version; Android Studio's bundled JDK works).

```bash
./gradlew :app:assembleDebug :app:lintDebug
```
The repo's last record was 0 lint errors and 24 warnings. Don't add errors. Then press **Run** in Android Studio (a debug build installs, which is what shows the demo pets), or use `./run-pixel6.sh` (set `ANDROID_HOME` if your SDK isn't in `~/Library/Android/sdk`).

## 5. Test it (do all of these)

**Add Pet (2.1)**
- [ ] My Pets > "Add another pet". Tap Next with an empty name: an error appears under the field, focus moves to it.
- [ ] Type a name, choose a photo, come back: the **name is still there**.
- [ ] Pick "Other": a "What kind of pet?" field appears.
- [ ] Step 3: choose a birthday: future dates are greyed out. Clear it. Skip the step.
- [ ] Review shows what you entered; "Change" jumps to the right step and back.
- [ ] Press the system Back at step 3: goes to step 2. At step 1 with typed text: asks to discard.
- [ ] Save: new pet opens in Pet Detail; Back goes to My Pets (not to an empty form).
- [ ] Rotate the phone mid-flow: nothing is lost, same step.

**View, Edit, Switch (2.2 to 2.4)**
- [ ] Pet Detail > ⋯ > "Pet profile" shows the info and "Not added yet" rows. Tapping a row opens Edit.
- [ ] Edit: change name and weight, Save: returns to where you came from, new values show.
- [ ] Edit, change something, Back: "Discard changes?".
- [ ] Rename a pet: its care logs are still attached (they're linked by id, not name).
- [ ] Two pets with the same name: you get a hint but can still save.
- [ ] My Pets: selected pet has the border, check mark and "Selected". Switch pets from Home too.
- [ ] Debug build: "Demo pets" appear; "Add to my pets" saves a real copy.
- [ ] Restart the app: everything is still there, including photos.

**Accessibility (these are the acceptance criteria)**
- [ ] **TalkBack** (Settings > Accessibility): swipe through every screen with your eyes closed. Each control is announced with its purpose; errors are read out; step changes are announced.
- [ ] **Font size**: Settings > Display > Font size, largest, and "Display size", largest. Nothing is cut off; the primary button is reachable by scrolling.
- [ ] **Accessibility Scanner** (Google Play): no touch-target, contrast or label findings.
- [ ] **Light and dark mode** both look right.

## 6. Screenshots for the visual-QA folder
```bash
python3 docs/visual-qa/capture_pets.py dark
python3 docs/visual-qa/capture_pets.py light
python3 docs/visual-qa/capture_pets.py light --font-scale 1.3
python3 docs/visual-qa/capture_pets.py dark  --font-scale 2.0
```
Add a short note to `docs/visual-qa/RESULTS.md` with what you tested. If a screen doesn't open from `preview_screen`, open it by hand first.

## 7. Commit and share

```bash
git status                                  # check only expected files changed
git add app/src/main/java/com/purrfolio/mco1/ app/src/main/AndroidManifest.xml docs/
git commit -m "feat: add multi-step Add Pet, pet profile, edit and My Pets (2.1-2.4)"
git push -u origin leon/phase1-pets         # then open a Pull Request on GitHub
```
Tips so you don't clash with teammates:
- In the PR description, say you edited `MainActivity.java`, `ReferenceScreens.java` and the manifest, so others know to rebase.
- Don't delete the old `petForm()`, `myPets()` and `petCard()` in `PhaseTwoScreens.java` in this PR. They're now unused (`MainActivity` routes around them). Remove them in a **separate** small PR after yours is merged.
- `capture_pets.py` is separate from Zel's `capture.py`, which hard-codes a Mac path. Mention it so they can switch to the `ANDROID_HOME` approach.

## 8. Where each requirement lives

| Function | Where |
|---|---|
| 2.1 Add Pet | `AddPetFlow.java`, `PetForm.java`, `commitPet` |
| 2.2 View Pet Profile | `PetProfileScreen.java` (opened from the ⋯ menu) |
| 2.3 Edit Pet Profile | `EditPetScreen.java`, `PetForm.java` |
| 2.4 Manage Multiple Pets | `MyPetsScreen.java`, `selectPet` in `MainActivity` |

## 9. Troubleshooting

- **"Unresolved reference: R.drawable.pdf_..."**: those drawables come from the existing `res/drawable` folder. If you renamed any, update `PetUi.defaultPortrait` and `DemoPets`.
- **Photo doesn't appear after picking**: check Logcat for `PetPhotoStore`. On very old emulators without Google Play services, the picker falls back to the system file picker; that's expected.
- **Demo pets don't show**: they only show in debuggable builds, never in a release build.
- **Back button closes the app from a form**: `formBackHandler` is reset on every `render()`. Check the form's `build()` calls `host.setBackHandler`.
- **Layout looks different from the wireframe**: this version is intentionally single-column with labels above fields (an accessibility choice). Colors, fonts and the dock are unchanged. Tell the team if they want the wireframe's side-by-side fields back.

## 10. Honest limits

- Not run on a device or emulator by me; first run may need small fixes.
- Pet Detail's wellness rings and insights are still the illustrative sample from the wireframe (not mine to change, and already labelled as examples).
- The photo picker back-port needs Google Play services on Android 11 to 12 devices; otherwise it falls back.
- Sex chips can't be un-selected once chosen ("Unknown" is the neutral option).
