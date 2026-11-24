package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;

import com.purrfolio.mco1.databinding.ItemDocumentBinding;
import com.purrfolio.mco1.databinding.ScreenDocumentsBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class DocumentsScreen {
    private static final String[] TABS = {"All", "Vaccinations", "Medications", "Receipts", "Other"};
    private static final String[] RANGES = {"All time", "This month", "Last 3 months", "Last year"};

    private final Activity activity;
    private final ScreenDocumentsBinding binding;
    private final PetRef current;
    private final TextView[] chips;
    private final Runnable observer = this::render;
    private String filter = "All";

    public DocumentsScreen(Activity activity, ScreenDocumentsBinding binding, PetRef current) {
        this.activity = activity;
        this.binding = binding;
        this.current = current;
        this.chips = new TextView[]{binding.docFilterAll, binding.docFilterVaccinations,
                binding.docFilterMedications, binding.docFilterReceipts, binding.docFilterOther};
    }

    public void bind() {
        BrandType.apply(binding.documentsEyebrow, false, 700);
        BrandType.apply(binding.documentsTitle, true, 600);
        BrandType.apply(binding.documentsSubtitle, false, 400);
        BrandType.apply(binding.documentsEmpty, false, 400);
        for (int i = 0; i < chips.length; i++) {
            String tab = TABS[i];
            BrandType.apply(chips[i], false, 700);
            chips[i].setOnClickListener(v -> { filter = tab; render(); });
        }

        binding.documentSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        binding.documentDateSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, RANGES));
        binding.documentDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { render(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        BackendHub hub = BackendHub.get();
        hub.watchDocuments(current.id);
        hub.observe(observer);
        binding.getRoot().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { }
            @Override public void onViewDetachedFromWindow(View v) { hub.unobserve(observer); }
        });
        render();
    }

    private void render() {
        for (int i = 0; i < chips.length; i++) {
            boolean selected = TABS[i].equals(filter);
            chips[i].setBackgroundResource(selected ? R.drawable.legacy_button : R.drawable.legacy_chip);
            chips[i].setTextColor(ContextCompat.getColor(activity, selected ? R.color.on_primary : R.color.text_primary));
        }

        binding.documentContainer.removeAllViews();
        String query = binding.documentSearch.getText().toString();
        int range = binding.documentDateSpinner.getSelectedItemPosition();
        List<PetDocument> shown = DocumentRepository.filter(BackendHub.get().documents(), filter, query);
        // Documents saved on this phone (5.1). Same chips, search and date range as the online ones.
        List<LocalDocument> local = LocalDocumentRepository.filter(
                localRepository().forPet(current.id),
                filter, query, range, System.currentTimeMillis());

        int count = 0;
        int nextLocal = 0;
        for (PetDocument d : shown) {
            if (!DocumentRepository.inRange(d, range)) continue;
            // Both lists are newest first, so phone documents newer than this one go above it.
            while (nextLocal < local.size() && local.get(nextLocal).addedAt >= d.addedAt) {
                addLocalRow(local.get(nextLocal++));
                count++;
            }
            addRow(d);
            count++;
        }
        while (nextLocal < local.size()) {
            addLocalRow(local.get(nextLocal++));
            count++;
        }
        binding.documentsEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private LocalDocumentRepository localRepository() {
        // Same preferences file MainActivity uses for pets and reminders.
        return LocalDocumentRepository.inPreferences(activity.getSharedPreferences("purrfolio_demo", Context.MODE_PRIVATE));
    }

    private void confirmDelete(LocalDocument d) {
        new AlertDialog.Builder(activity)
                .setTitle("Delete \"" + d.title + "\"?")
                .setMessage("This removes the saved file from this phone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteLocal(d))
                .show();
    }

    /** Record first, then the file: a failed file delete leaves an unseen leftover, never a row that points at nothing. */
    private void deleteLocal(LocalDocument d) {
        LocalDocument removed = localRepository().remove(d.id);
        if (removed != null) DocumentFileStore.delete(activity, removed.path);
        render();
    }

    /** Opens a saved copy in whatever app can show it. The viewer gets a temporary read-only link, not the file path. */
    private void openLocal(LocalDocument d) {
        File file = DocumentFileStore.fileFor(activity, d.path);
        if (file == null || !file.isFile()) {
            Toast.makeText(activity, "This file is no longer on this phone.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            Intent view = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, DocumentRules.mimeTypeFor(file.getName()))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(view);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            Toast.makeText(activity, "No app can open this file.", Toast.LENGTH_SHORT).show();
        }
    }

    /** Draws a Photo icon for images and a Document icon for everything else, the same icons used elsewhere in the app. */
    private void addFileIcon(ItemDocumentBinding item, boolean isImage) {
        int tint = ContextCompat.getColor(activity, R.color.primary);
        item.documentIcon.addView(new DetailIcon(activity, isImage ? "Photo" : "Document", tint),
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /** Draws the chevron in the row's slot, the same icon the Pet Timeline uses. */
    private void addChevron(ItemDocumentBinding item) {
        int tint = ContextCompat.getColor(activity, R.color.primary);
        item.documentChevron.addView(new CareArtwork(activity, "Chevron", tint),
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /** A document saved on this phone (5.1). */
    private void addLocalRow(LocalDocument d) {
        ItemDocumentBinding item = ItemDocumentBinding.inflate(activity.getLayoutInflater(), binding.documentContainer, false);
        addFileIcon(item, d.looksLikeImage());
        item.documentTitle.setText(d.title);
        String date = new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(d.addedAt));
        item.documentMeta.setText(date + " · " + d.type + " · On this phone");
        addChevron(item);
        BrandType.apply(item.documentTitle, false, 700);
        BrandType.apply(item.documentMeta, false, 400);
        item.getRoot().setOnClickListener(v -> openLocal(d));
        // Long-press for sighted users, and a named action so TalkBack users can reach it too.
        item.getRoot().setOnLongClickListener(v -> { confirmDelete(d); return true; });
        ViewCompat.addAccessibilityAction(item.getRoot(), "Delete", (view, arguments) -> {
            confirmDelete(d);
            return true;
        });
        binding.documentContainer.addView(item.getRoot());
    }

    private void addRow(PetDocument d) {
        ItemDocumentBinding item = ItemDocumentBinding.inflate(activity.getLayoutInflater(), binding.documentContainer, false);
        addFileIcon(item, d.looksLikeImage());
        item.documentTitle.setText(d.title);
        String date = new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(d.addedAt));
        item.documentMeta.setText(date + " · " + d.type);
        addChevron(item);
        BrandType.apply(item.documentTitle, false, 700);
        BrandType.apply(item.documentMeta, false, 400);
        item.getRoot().setOnClickListener(v -> DocumentRepository.view(activity, d));
        binding.documentContainer.addView(item.getRoot());
    }
}
