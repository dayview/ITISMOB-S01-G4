package com.itismob.mco.purrfolio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessControlActivity extends BaseListActivity {
    public static final String MEMBER_ID = "memberId";
    private static final String[] KEYS = {"canView", "canLog", "canReminders", "canDocs"};
    private static final String[] LABELS = {"View Profile & Records", "Add & Edit Care Logs", "Manage Reminders", "ManageDocuments"};
    private static final String[] HINTS = {"Can view all information", "Can add and edit care entries", "Can add / edit reminders", "Can upload / edit documents"};
    private final Map<String, Boolean> perms = new HashMap<>();
    private String memberId;

    @Override
    protected void onScreenCreated() {
        memberId = getIntent().getStringExtra(MEMBER_ID);
        titleTv.setText("Access Control");
        bannerTv.setText("Access Permissions");
        show(bannerTv, true);
        actionBtn.setText("Save Permissions");
        show(actionBtn, true);
        actionBtn.setOnClickListener(v -> save());
    }

    @Override
    protected void load() {
        Repo.sub(petId(), "circle").document(memberId).get().addOnSuccessListener(d -> {
            if (!d.exists()) { finish(); return; }
            subtitleTv.setText(Repo.str(d, "name") + " · " + Repo.str(d, "role"));
            for (String k : KEYS) perms.put(k, Boolean.TRUE.equals(d.getBoolean(k)));
            render();
        });
    }

    private void render() {
        List<Row> out = new ArrayList<>();
        for (int i = 0; i < KEYS.length; i++) {
            out.add(toggleRow(KEYS[i], LABELS[i], HINTS[i], Boolean.TRUE.equals(perms.get(KEYS[i]))));
        }
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        perms.put(row.id, !Boolean.TRUE.equals(perms.get(row.id)));
        render();
    }

    private void save() {
        Map<String, Object> m = new HashMap<>(perms);
        Repo.sub(petId(), "circle").document(memberId).update(m);
        Repo.activity(petId(), "Changed access", subtitleTv.getText().toString());
        Ui.toast(this, "Permissions saved");
        finish();
    }
}
