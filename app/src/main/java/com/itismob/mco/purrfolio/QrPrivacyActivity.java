package com.itismob.mco.purrfolio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QrPrivacyActivity extends BaseListActivity {
    private static final String[] KEYS = {"name", "photo", "species", "owner", "medical", "vet"};
    private static final String[] LABELS = {"Pet Name", "Photo", "Species & Breed", "Owner Contact Number", "Medical Information", "Veterinarian"};
    private static final String[] HINTS = {"", "Link to the photo", "", "Your phone number from Personal Information", "Allergies, medications, conditions", "Clinic name and phone"};
    private static final boolean[] DEFAULTS = {true, true, true, false, true, true};
    private final Map<String, Boolean> state = new HashMap<>();

    @Override
    protected void onScreenCreated() {
        titleTv.setText("QR Information Privacy");
        subtitleTv.setText("Choose what’s shown when the QR is scanned");
        bannerTv.setText("Information to Share");
        show(bannerTv, true);
        actionBtn.setText("Save Changes");
        show(actionBtn, true);
        actionBtn.setOnClickListener(v -> {
            Repo.pets().document(petId()).update("qrShare", state);
            Repo.activity(petId(), "Changed QR privacy", "");
            Ui.toast(this, "Saved");
            finish();
        });
    }

    @Override
    protected void load() {
        Repo.pets().document(petId()).get().addOnSuccessListener(d -> {
            Pet p = d.toObject(Pet.class);
            if (p == null) { finish(); return; }
            for (int i = 0; i < KEYS.length; i++) state.put(KEYS[i], p.share(KEYS[i], DEFAULTS[i]));
            render();
        });
    }

    private void render() {
        List<Row> out = new ArrayList<>();
        for (int i = 0; i < KEYS.length; i++) {
            out.add(toggleRow(KEYS[i], LABELS[i], HINTS[i], Boolean.TRUE.equals(state.get(KEYS[i]))));
        }
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        state.put(row.id, !Boolean.TRUE.equals(state.get(row.id)));
        render();
    }
}
