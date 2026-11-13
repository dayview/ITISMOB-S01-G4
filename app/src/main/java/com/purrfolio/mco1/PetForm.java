package com.purrfolio.mco1;

import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * The three groups of fields shared by the multi-step "Add pet" flow and the one-page "Edit pet" screen.
 * Every field writes into the PetDraft as the user types, so nothing is lost when a screen is rebuilt.
 */
public class PetForm {
    private final PetUi ui;
    private final PetDraft draft;
    private final java.util.List<Pet> existingPets;
    private final Runnable choosePhoto;
    private final Runnable refresh;

    private PetUi.LabeledField nameField, otherField, breedField, birthdayField, weightField, notesField;

    public PetForm(PetUi ui, PetDraft draft, java.util.List<Pet> existingPets, Runnable choosePhoto, Runnable refresh) {
        this.ui = ui;
        this.draft = draft;
        this.existingPets = existingPets;
        this.choosePhoto = choosePhoto;
        this.refresh = refresh;
    }

    private void add(LinearLayout into, View view) { add(into, view, 18); }

    private void add(LinearLayout into, View view, int bottomDp) { into.addView(view, ui.lpBottom(bottomDp)); }

    // ---- photo and name group ----

    public void addPhotoAndName(LinearLayout into) {
        boolean hasPhoto = !draft.photoUri.trim().isEmpty();
        LinearLayout hero = ui.column();
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(ui.portrait(draft.photoUri, draft.finalSpecies(), 140,
                hasPhoto ? "Your pet's photo" : "No photo yet"), new LinearLayout.LayoutParams(ui.dp(140), ui.dp(140)));
        LinearLayout buttons = ui.row();
        buttons.setGravity(Gravity.CENTER);
        buttons.addView(ui.button(hasPhoto ? "Change photo" : "Add photo", false, choosePhoto),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (hasPhoto) {
            buttons.addView(ui.textButton("Remove", "Remove photo", () -> {
                if (!draft.photoUri.equals(draft.originalPhotoUri))
                    PetPhotoStore.deleteIfOurs(ui.a, draft.photoUri);
                draft.photoUri = "";
                refresh.run();
            }));
        }
        hero.addView(buttons, ui.lpTop(12));
        add(into, hero, 24);

        nameField = ui.textField("Pet name", false, draft.name, null,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false, PetRules.NAME_MAX,
                value -> { draft.name = value; updateDuplicateHint(); });
        add(into, nameField.root);
        updateDuplicateHint();
    }

    private void updateDuplicateHint() {
        boolean duplicate = PetRules.isDuplicateName(draft.name, draft.id, existingPets);
        if (nameField != null) nameField.setHelper(duplicate
                ? "You already have a pet named " + draft.name.trim() + ". You can still save." : null);
    }

    // ---- species, breed, sex group ----

    public void addSpecies(LinearLayout into) {
        PetUi.LabeledField other = ui.textField("What kind of pet?", false, draft.otherSpecies,
                "For example: Hamster or Parrot", PetUi.DEFAULT_TEXT_INPUT, false, PetRules.TEXT_MAX,
                value -> draft.otherSpecies = value);
        other.root.setVisibility(PetDraft.OTHER.equals(draft.species) ? View.VISIBLE : View.GONE);
        otherField = other;

        add(into, ui.choiceChips("Species", false, PetDraft.SPECIES_CHOICES, draft.species, choice -> {
            draft.species = choice;
            other.root.setVisibility(PetDraft.OTHER.equals(choice) ? View.VISIBLE : View.GONE);
            other.showError(null);
        }));
        add(into, other.root);

        breedField = ui.textField("Breed", true, draft.breed, "For example: Mixed breed or Siamese",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false, PetRules.TEXT_MAX,
                value -> draft.breed = value);
        add(into, breedField.root);

        add(into, ui.choiceChips("Sex", true, PetDraft.SEX_CHOICES, draft.sex, choice -> draft.sex = choice));
    }

    // ---- birthday, weight, notes group ----

    public void addDetails(LinearLayout into) {
        birthdayField = ui.dateField("Birthday", true, draft.birthday, "Used to show your pet's age.",
                value -> draft.birthday = value);
        add(into, birthdayField.root);

        weightField = ui.textField("Weight in kg", true, draft.weight, "For example: 4.2",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, false, 6,
                value -> draft.weight = value);
        add(into, weightField.root);

        notesField = ui.textField("Notes", true, draft.notes, "Anything helpful, like favorite treats or health notes.",
                PetUi.DEFAULT_TEXT_INPUT, true, PetRules.NOTES_MAX, value -> draft.notes = value);
        add(into, notesField.root);
    }

    // ---- error validation: show every error, then move focus to the first one ----

    private boolean apply(PetUi.LabeledField[] fields, String[] errors) {
        PetUi.LabeledField firstBad = null;
        boolean anyBad = false;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != null) fields[i].showError(errors[i]);
            if (errors[i] != null) {
                anyBad = true;
                if (firstBad == null) firstBad = fields[i];
            }
        }
        if (firstBad != null) firstBad.focus();
        return !anyBad;
    }

    private String otherSpeciesError() {
        return PetDraft.OTHER.equals(draft.species) ? PetRules.shortTextError(draft.otherSpecies, "Pet type") : null;
    }

    public boolean validatePhotoAndName() {
        return apply(new PetUi.LabeledField[]{nameField}, new String[]{PetRules.nameError(draft.name)});
    }

    public boolean validateSpecies() {
        return apply(new PetUi.LabeledField[]{otherField, breedField},
                new String[]{otherSpeciesError(), PetRules.shortTextError(draft.breed, "Breed")});
    }

    public boolean validateDetails() {
        return apply(new PetUi.LabeledField[]{birthdayField, weightField, notesField},
                new String[]{PetRules.birthdayError(draft.birthday), PetRules.weightError(draft.weight),
                        PetRules.notesError(draft.notes)});
    }

    /** Used by the one-page Edit screen, where every field is on screen at once. */
    public boolean validateAll() {
        return apply(new PetUi.LabeledField[]{nameField, otherField, breedField, birthdayField, weightField, notesField},
                new String[]{PetRules.nameError(draft.name), otherSpeciesError(),
                        PetRules.shortTextError(draft.breed, "Breed"), PetRules.birthdayError(draft.birthday),
                        PetRules.weightError(draft.weight), PetRules.notesError(draft.notes)});
    }
}
