package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/**
 * F2.3
 * Edit Pet Profile: every field on one scrolling page -> "Save changes" at the bottom.
 * Leaving with unsaved changes asks for confirmation.
 */
public class EditPetScreen {
    private final Activity a;
    private final PetDraft draft;
    private final PetScreenHost host;
    private final PetUi ui;
    private final PetForm form;
    private LinearLayout body;

    public EditPetScreen(Activity a, PetDraft draft, List<Pet> existingPets, PetScreenHost host) {
        this.a = a;
        this.draft = draft;
        this.host = host;
        this.ui = new PetUi(a);
        this.form = new PetForm(ui, draft, existingPets, host::choosePhoto, this::renderBody);
    }

    public View build() {
        LinearLayout root = ui.screenRoot();

        LinearLayout top = ui.row();
        top.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        top.addView(ui.iconButton("Back", "Back", this::leave));
        TextView title = ui.heading("Edit pet profile", 22f);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(new View(a), new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));
        root.addView(top, ui.lp());

        PetUi.ScrollBody scrollBody = ui.scrollBody();
        body = scrollBody.body;
        root.addView(scrollBody.scroll);

        LinearLayout actions = ui.column();
        actions.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(12));
        actions.addView(ui.button("Save changes", this::save), ui.lp());
        actions.addView(ui.textButton("Cancel", this::leave), ui.lpTop(4));
        root.addView(actions, ui.lp());

        host.setBackHandler(this::leave);
        renderBody();
        return root;
    }

    private void renderBody() {
        body.removeAllViews();
        form.addPhotoAndName(body);
        form.addSpecies(body);
        form.addDetails(body);
    }

    private void save() {
        if (form.validateAll()) host.savePet(draft.toPet(), false);
    }

    private void leave() {
        if (!draft.isDirty()) {
            host.exitForm();
            return;
        }
        new AlertDialog.Builder(a)
                .setTitle("Discard changes?")
                .setMessage("Your changes to " + (draft.name.trim().isEmpty() ? "this pet" : draft.name) + " won't be saved.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard", (dialog, which) -> host.exitForm())
                .show();
    }
}
