package com.itismob.mco.purrfolio;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PetRecordsActivity extends BaseListActivity {
    private static final String[] TYPES = {"Vaccination", "Medication", "Receipt", "Other"};
    private final List<DocumentSnapshot> docs = new ArrayList<>();

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) askType(uri);
            });

    @Override
    protected void onScreenCreated() {
        titleTv.setText("Pet Records");
        subtitleTv.setText("Keep " + Session.petName(this) + "’s documents in one place");
        addTabs("All", "Vaccinations", "Medications", "Receipts", "Other");
        show(addBtn, true);
        addBtn.setOnClickListener(v -> Repo.can(petId(), "canDocs", ok -> {
            if (ok) picker.launch(new String[]{"image/*", "application/pdf"});
            else Ui.toast(this, "You don’t have permission to add documents");
        }));
    }

    @Override
    protected void load() {
        Repo.loadDocs(petId(), list -> {
            docs.clear();
            docs.addAll(list);
            render();
        });
    }

    @Override
    protected void onFilterChanged() { render(); }

    private void render() {
        String tab = selectedTab();
        List<Row> out = new ArrayList<>();
        for (DocumentSnapshot d : docs) {
            String type = Repo.str(d, "type");
            boolean match = tab.equals("All") || (tab.equals("Vaccinations") && type.equals("Vaccination")) || (tab.equals("Medications") && type.equals("Medication")) || (tab.equals("Receipts") && type.equals("Receipt")) || (tab.equals("Other") && type.equals("Other"));
            if (match) out.add(Repo.docRow(d));
        }
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        Intent i = new Intent(this, DocumentDetailActivity.class);
        i.putExtra(Ui.PET_ID, petId());
        i.putExtra(DocumentDetailActivity.DOC_ID, row.id);
        startActivity(i);
    }

    private void askType(Uri uri) {
        new AlertDialog.Builder(this).setTitle("What kind of document is this?").setItems(TYPES, (d, which) -> askTitle(uri, TYPES[which])).show();
    }

    private void askTitle(Uri uri, String type) {
        String fileName = Ui.fileName(this, uri);
        EditText input = new EditText(this);
        input.setText(fileName);
        new AlertDialog.Builder(this).setTitle("Document name").setView(input).setPositiveButton("Upload", (d, w) -> {
                    String title = Ui.text(input);
                    upload(uri, type, title.isEmpty() ? fileName : title, fileName);
                }).setNegativeButton("Cancel", null).show();
    }

    private void upload(Uri uri, String type, String title, String fileName) {
        String pid = petId();
        Context app = getApplicationContext();
        StorageReference ref = FirebaseStorage.getInstance().getReference("pets/" + pid + "/docs/" + System.currentTimeMillis() + "_" + fileName);

        Ui.toast(app, "Uploading…");
        ref.putFile(uri).addOnSuccessListener(t ->
                ref.getDownloadUrl().addOnSuccessListener(url -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("title", title);
                    m.put("type", type);
                    m.put("url", url.toString());
                    m.put("fileName", fileName);
                    m.put("addedAt", System.currentTimeMillis());
                    m.put("authorName", Repo.myName());
                    Repo.sub(pid, "documents").add(m);
                    Repo.activity(pid, "Added document", title);
                    Ui.toast(app, "Document saved");
                    if (!isFinishing()) load();
                })).addOnFailureListener(e -> Ui.toast(app, "Upload failed: " + e.getMessage()));
    }
}
