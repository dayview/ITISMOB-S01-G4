package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Arrays;
import java.util.List;

/**
 * F2.1 Add Pet
 * Photo + name -> species/breed/sex -> extra details -> Review
 */
public class AddPetFlow {
    private final Activity a;
    private final PetDraft draft;
    private final PetScreenHost host;
    private final PetUi ui;
    private final PetForm form;

    private final List<String> titles = Arrays.asList("Meet your pet", "About your pet", "Extra details", "Review");
    private final List<String> subtitles = Arrays.asList(
            "Add a photo and a name. You can change everything later.",
            "This helps Purrfolio show the right details.",
            "All of these are optional. Add what you know.",
            "Check everything, then save. You can edit it any time.");

    private ScrollView scroll;
    private LinearLayout body, progress, actions;
    private TextView stepLabel;

    public AddPetFlow(Activity a, PetDraft draft, List<Pet> existingPets, PetScreenHost host) {
        this.a = a;
        this.draft = draft;
        this.host = host;
        this.ui = new PetUi(a);
        this.form = new PetForm(ui, draft, existingPets, host::choosePhoto, () -> showStep(false));
    }

    public View build() {
        LinearLayout root = ui.screenRoot();

        LinearLayout top = ui.row();
        top.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), 0);
        top.addView(ui.iconButton("Back", "Back", this::goBack));
        TextView title = ui.heading("Add your pet", 22f);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(new View(a), new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));
        root.addView(top, ui.lp());

        stepLabel = ui.text("", 14f, ui.sub, 700);
        stepLabel.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(6));
        root.addView(stepLabel, ui.lp());

        progress = ui.row();
        progress.setPadding(ui.dp(20), 0, ui.dp(20), ui.dp(4));
        progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(progress, ui.lp());

        PetUi.ScrollBody scrollBody = ui.scrollBody();
        scroll = scrollBody.scroll;
        body = scrollBody.body;
        root.addView(scroll);

        actions = ui.column();
        actions.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(12));
        root.addView(actions, ui.lp());

        host.setBackHandler(this::goBack);
        showStep(false);
        return root;
    }

    // ---- step navigation ----

    private void goToStep(int step) {
        draft.step = step;
        showStep(true);
    }

    private void goBack() {
        if (draft.step > 0) goToStep(draft.step - 1); else leave();
    }

    private void leave() {
        if (!draft.isDirty()) {
            host.exitForm();
            return;
        }
        new AlertDialog.Builder(a)
                .setTitle("Discard this pet?")
                .setMessage("What you entered won't be saved.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard", (dialog, which) -> host.exitForm())
                .show();
    }

    private void next() {
        switch (draft.step) {
            case 0: if (form.validatePhotoAndName()) goToStep(1); break;
            case 1: if (form.validateSpecies()) goToStep(2); break;
            case 2: if (form.validateDetails()) goToStep(3); break;
            default: save();
        }
    }

    private void save() {
        // "Skip this step" jumps to Review without validating, so recheck every step before saving.
        int badStep = -1;
        if (PetRules.nameError(draft.name) != null) {
            badStep = 0;
        } else if ((PetDraft.OTHER.equals(draft.species)
                && PetRules.shortTextError(draft.otherSpecies, "Pet type") != null)
                || PetRules.shortTextError(draft.breed, "Breed") != null) {
            badStep = 1;
        } else if (PetRules.birthdayError(draft.birthday) != null || PetRules.weightError(draft.weight) != null
                || PetRules.notesError(draft.notes) != null) {
            badStep = 2;
        }
        if (badStep >= 0) {
            goToStep(badStep);
            if (badStep == 0) form.validatePhotoAndName();
            else if (badStep == 1) form.validateSpecies();
            else form.validateDetails();
            return;
        }
        host.savePet(draft.toPet(), true);
    }

    // ---- drawing one step ----

    private void showStep(boolean announce) {
        body.removeAllViews();
        actions.removeAllViews();
        int last = titles.size() - 1;
        stepLabel.setText("Step " + (draft.step + 1) + " of " + titles.size());
        paintProgress();

        TextView title = ui.heading(titles.get(draft.step), 28f);
        body.addView(title, ui.lpBottom(6));
        body.addView(ui.text(subtitles.get(draft.step), 15f, ui.sub), ui.lpBottom(22));

        switch (draft.step) {
            case 0: form.addPhotoAndName(body); break;
            case 1: form.addSpecies(body); break;
            case 2: form.addDetails(body); break;
            default: addReview();
        }

        String primaryLabel = draft.step <= 1 ? "Next" : draft.step == 2 ? "Review" : "Save pet";
        actions.addView(ui.button(primaryLabel, this::next), ui.lp());
        if (draft.step == 2) {
            actions.addView(ui.textButton("Skip this step", () -> goToStep(last)), ui.lpTop(4));
        }

        scroll.scrollTo(0, 0);
        if (announce) ui.announce(title, "Step " + (draft.step + 1) + " of " + titles.size() + ", " + titles.get(draft.step));
    }

    private void paintProgress() {
        progress.removeAllViews();
        for (int index = 0; index < titles.size(); index++) {
            View segment = new View(a);
            segment.setBackground(ui.rounded(index <= draft.step ? ui.primary : ui.outline, 4));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ui.dp(6), 1f);
            if (index > 0) params.leftMargin = ui.dp(6);
            progress.addView(segment, params);
        }
    }

    // ---- review step ----

    private void addReview() {
        Pet pet = draft.toPet();
        String none = "Not added";

        LinearLayout nameCard = summaryCard("Photo and name", 0);
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(ui.dp(72), ui.dp(72));
        photoParams.bottomMargin = ui.dp(12);
        nameCard.addView(ui.portrait(pet.photoUri, pet.species, 72,
                pet.photoUri.trim().isEmpty() ? "No photo yet" : "Your pet's photo"), photoParams);
        summaryRow(nameCard, "Name", pet.name, true);

        LinearLayout aboutCard = summaryCard("About", 1);
        summaryRow(aboutCard, "Species", pet.species, true);
        summaryRow(aboutCard, "Breed", orNone(pet.breed, none), !pet.breed.trim().isEmpty());
        summaryRow(aboutCard, "Sex", orNone(pet.sex, none), !pet.sex.trim().isEmpty());

        String birthday = none;
        if (!pet.birthday.trim().isEmpty()) {
            String age = PetRules.ageLabel(pet.birthday);
            birthday = PetRules.displayDate(pet.birthday) + (age == null ? "" : " (" + age + ")");
        }
        LinearLayout detailCard = summaryCard("Details", 2);
        summaryRow(detailCard, "Birthday", birthday, !pet.birthday.trim().isEmpty());
        summaryRow(detailCard, "Weight", pet.weightKg.trim().isEmpty() ? none : pet.weightKg + " kg",
                !pet.weightKg.trim().isEmpty());
        summaryRow(detailCard, "Notes", orNone(pet.notes, none), !pet.notes.trim().isEmpty());
    }

    private static String orNone(String value, String none) { return value.trim().isEmpty() ? none : value; }

    private LinearLayout summaryCard(String title, int stepIndex) {
        LinearLayout card = ui.column();
        card.setBackground(ui.rounded(ui.surface, 20, ui.outline));
        card.setPadding(ui.dp(16), ui.dp(8), ui.dp(8), ui.dp(14));
        LinearLayout header = ui.row();
        TextView heading = ui.text(title, 17f, ui.primary, 700);
        ui.markHeading(heading);
        header.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(ui.textButton("Change", "Change " + title, () -> goToStep(stepIndex)));
        card.addView(header, ui.lp());
        body.addView(card, ui.lpBottom(14));
        return card;
    }

    private void summaryRow(LinearLayout card, String label, String value, boolean filled) {
        LinearLayout line = ui.column();
        line.setContentDescription(label + ": " + value);
        line.addView(ui.text(label, 13f, ui.sub, 600), ui.lp());
        line.addView(ui.text(value, 16f, filled ? ui.ink : ui.sub, 600), ui.lpTop(2));
        card.addView(line, ui.lpBottom(10));
    }
}
