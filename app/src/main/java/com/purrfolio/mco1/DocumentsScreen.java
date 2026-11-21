package com.purrfolio.mco1;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.purrfolio.mco1.databinding.ItemDocumentBinding;
import com.purrfolio.mco1.databinding.ScreenDocumentsBinding;

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

        int count = 0;
        for (PetDocument d : shown) {
            if (!DocumentRepository.inRange(d, range)) continue;
            addRow(d);
            count++;
        }
        binding.documentsEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void addRow(PetDocument d) {
        ItemDocumentBinding item = ItemDocumentBinding.inflate(activity.getLayoutInflater(), binding.documentContainer, false);
        item.documentIcon.setText(d.looksLikeImage() ? "▣" : "▤");
        item.documentTitle.setText(d.title);
        String date = new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(d.addedAt));
        item.documentMeta.setText(date + " · " + d.type);
        BrandType.apply(item.documentIcon, true, 600);
        BrandType.apply(item.documentTitle, false, 700);
        BrandType.apply(item.documentMeta, false, 400);
        item.getRoot().setOnClickListener(v -> DocumentRepository.view(activity, d));
        binding.documentContainer.addView(item.getRoot());
    }
}
