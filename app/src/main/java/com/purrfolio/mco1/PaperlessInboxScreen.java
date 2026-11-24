package com.purrfolio.mco1;

import android.app.Activity;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * F5.1 Pet Paperless Inbox: add a document to a pet. Choose pet and type, pick a file, check the
 * preview, then Save. This class only draws and checks. {@link DocumentRules} decides what is allowed
 * and the Host does the saving, so nothing here touches files or storage.
 */
public class PaperlessInboxScreen {
    public interface Host {
        /** Opens the system file picker. The host answers by calling {@link #onFilePicked}. */
        void pickDocument();

        /** Copies the file and remembers the record. Returns null when saved, otherwise a message to show. */
        String save(String petId, String title, String type, Uri source, DocumentFileStore.Picked picked);

        /** Leaves this screen (Back, Cancel, or after a successful save). */
        void close();
    }

    private final Activity a;
    private final List<PetRef> pets;
    private final Host host;
    private final PetUi ui;

    private String petId;
    private String type = DocumentRules.DEFAULT_TYPE;
    private String title = "";
    /** True once the user typed in the title, so picking another file no longer replaces it. */
    private boolean titleEdited;
    private Uri source;
    private DocumentFileStore.Picked picked;

    private LinearLayout previewHolder;
    private PetUi.LabeledField titleField;
    private TextView fileError;
    private TextView saveError;

    public PaperlessInboxScreen(Activity a, List<PetRef> pets, String selectedPetId, Host host) {
        this.a = a;
        this.pets = pets;
        this.host = host;
        this.ui = new PetUi(a);
        this.petId = containsPet(selectedPetId) ? selectedPetId : (pets.isEmpty() ? "" : pets.get(0).id);
    }

    public View build() {
        LinearLayout root = ui.screenRoot();

        LinearLayout top = ui.row();
        top.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        top.addView(ui.iconButton("Back", "Back", host::close));
        TextView heading = ui.heading("Add document", 22f);
        heading.setGravity(Gravity.CENTER);
        top.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(new View(a), new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));
        root.addView(top, ui.lp());

        PetUi.ScrollBody scrollBody = ui.scrollBody();
        LinearLayout body = scrollBody.body;
        root.addView(scrollBody.scroll);

        List<String> labels = petLabels();
        if (!pets.isEmpty()) {
            body.addView(ui.choiceChips("Pet", false, labels, labels.get(indexOfPet(petId)), 2,
                    choice -> petId = pets.get(labels.indexOf(choice)).id), ui.lpBottom(16));
        }
        body.addView(ui.choiceChips("Type", false, DocumentRules.TYPES, type, 2, choice -> type = choice),
                ui.lpBottom(16));

        body.addView(ui.text("File", 14f, ui.ink, 700), ui.lpBottom(6));
        previewHolder = ui.column();
        body.addView(previewHolder, ui.lp());
        fileError = ui.text("", 13f, ui.coral, 700);
        fileError.setVisibility(View.GONE);
        fileError.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(fileError, ui.lpTop(6));
        drawPreview();

        titleField = ui.textField("Title", false, title, "Shown in the list. You can change it.",
                PetUi.DEFAULT_TEXT_INPUT, false, DocumentRules.TITLE_MAX, value -> {
                    title = value;
                    titleEdited = true;
                });
        body.addView(titleField.root, ui.lpTop(16));

        LinearLayout actions = ui.column();
        actions.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(12));
        saveError = ui.text("", 13f, ui.coral, 700);
        saveError.setVisibility(View.GONE);
        saveError.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        actions.addView(saveError, ui.lpBottom(8));
        actions.addView(ui.button("Save document", this::save), ui.lp());
        actions.addView(ui.textButton("Cancel", host::close), ui.lpTop(4));
        root.addView(actions, ui.lp());
        return root;
    }

    /** Called by the host with the file the user chose. */
    public void onFilePicked(Uri uri) {
        if (uri == null) return;
        source = uri;
        picked = DocumentFileStore.describe(a, uri);
        // An unknown size (-1) is checked while copying, so only the type is judged here.
        String problem = DocumentRules.fileError(picked.fileName, picked.sizeBytes < 0 ? 1 : picked.sizeBytes);
        showFileError(problem);
        // Suggest a title from the file name, unless the user wrote one. An emptied title counts as not written.
        if ((!titleEdited || title.trim().isEmpty()) && titleField != null) {
            titleField.input.setText(DocumentRules.defaultTitle(picked.fileName));
            titleEdited = false;   // setText just ran the listener above, which marked it as typed
        }
        drawPreview();
    }

    private void drawPreview() {
        previewHolder.removeAllViews();
        if (picked == null) {
            previewHolder.addView(ui.button("Choose file", false, host::pickDocument), ui.lp());
            return;
        }
        LinearLayout card = ui.column();
        card.setBackground(ui.rounded(ui.surface, 20, ui.outline));
        card.setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(14));
        if (DocumentRules.isImage(picked.fileName)) {
            ImageView image = new ImageView(a);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription("Preview of " + picked.fileName);
            try { image.setImageURI(source); } catch (RuntimeException ignored) { }
            card.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(160)));
        }
        card.addView(ui.text(picked.fileName, 16f, ui.ink, 700), ui.lpTop(8));
        String size = picked.sizeBytes < 0 ? "Size unknown" : DocumentRules.sizeLabel(picked.sizeBytes);
        card.addView(ui.text(size, 14f, ui.sub), ui.lpTop(2));
        card.addView(ui.textButton("Choose a different file", host::pickDocument), ui.lpTop(4));
        previewHolder.addView(card, ui.lp());
    }

    private void save() {
        saveError.setVisibility(View.GONE);
        String titleProblem = DocumentRules.titleError(title);
        titleField.showError(titleProblem);
        if (picked == null) {
            showFileError("Choose a file to save.");
        } else if (picked.sizeBytes >= 0) {
            showFileError(DocumentRules.fileError(picked.fileName, picked.sizeBytes));
        }
        if (titleProblem != null) { titleField.focus(); return; }
        if (picked == null || fileError.getVisibility() == View.VISIBLE) return;
        if (petId.isEmpty()) { showSaveError("Add a pet first."); return; }

        String failure = host.save(petId, title.trim(), type, source, picked);
        if (failure == null) host.close();
        else showSaveError(failure);
    }

    private void showFileError(String message) {
        fileError.setText(message == null ? "" : message);
        fileError.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    private void showSaveError(String message) {
        saveError.setText(message);
        saveError.setVisibility(View.VISIBLE);
    }

    private boolean containsPet(String id) {
        if (id == null) return false;
        for (PetRef pet : pets) if (pet.id.equals(id)) return true;
        return false;
    }

    private int indexOfPet(String id) {
        for (int i = 0; i < pets.size(); i++) if (pets.get(i).id.equals(id)) return i;
        return 0;
    }

    /** Pet names, with "(2)" added to repeated names so every chip is different. */
    private List<String> petLabels() {
        List<String> labels = new ArrayList<>();
        for (PetRef pet : pets) {
            String label = pet.name;
            int n = 2;
            while (labels.contains(label)) label = pet.name + " (" + n++ + ")";
            labels.add(label);
        }
        return labels;
    }
}
