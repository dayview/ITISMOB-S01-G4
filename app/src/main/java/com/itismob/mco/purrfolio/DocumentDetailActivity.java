package com.itismob.mco.purrfolio;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public class DocumentDetailActivity extends BaseListActivity {

    public static final String DOC_ID = "docId";
    private String docId, title = "", url = "", fileName = "document";

    @Override
    protected void onScreenCreated() {
        docId = getIntent().getStringExtra(DOC_ID);
        show(bannerTv, true);
    }

    @Override
    protected void load() {
        Repo.sub(petId(), "documents").document(docId).get().addOnSuccessListener(d -> {
            if (!d.exists()) { finish(); return; }
            title = Repo.str(d, "title");
            url = Repo.str(d, "url");
            fileName = Repo.str(d, "fileName");
            Long ts = d.getLong("addedAt");
            titleTv.setText(title);
            subtitleTv.setText("Added " + Ui.date(ts == null ? 0 : ts));
            bannerTv.setText(Repo.str(d, "type") + " · " + fileName);

            List<Row> out = new ArrayList<>();
            out.add(new Row("view", "View Full Screen", null, "›"));
            out.add(new Row("download", "Download", null, "›"));
            out.add(new Row("share", "Share", null, "›"));
            out.add(new Row("rename", "Rename", null, "›"));
            Row del = new Row("delete", "Delete", null, "›");
            del.danger = true;
            out.add(del);
            showRows(out);
        });
    }

    @Override
    protected void onRowClick(Row row) {
        switch (row.id) {
            case "view":
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (ActivityNotFoundException e) {
                    Ui.toast(this, "No app can open this file");
                }
                break;
            case "download":
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url))
                        .setTitle(title)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName);
                getSystemService(DownloadManager.class).enqueue(req);
                Ui.toast(this, "Downloading…");
                break;
            case "share":
                Ui.share(this, title, title + "\n" + url);
                break;
            case "rename":
                Repo.can(petId(), "canDocs", ok -> { if (ok) rename(); else denied(); });
                break;
            case "delete":
                Repo.can(petId(), "canDocs", ok -> { if (ok) confirmDelete(); else denied(); });
                break;
        }
    }

    private void denied() { Ui.toast(this, "You don’t have permission to change documents"); }

    private void rename() {
        EditText input = new EditText(this);
        input.setText(title);
        new AlertDialog.Builder(this).setTitle("Rename document").setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String t = Ui.text(input);
                    if (t.isEmpty()) return;
                    Repo.sub(petId(), "documents").document(docId).update("title", t);
                    Repo.activity(petId(), "Renamed document", t);
                    load();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void confirmDelete() {
        Ui.confirm(this, "Delete document?", title, () -> {
            Repo.sub(petId(), "documents").document(docId).delete();
            Repo.activity(petId(), "Deleted document", title);
            finish();
        });
    }
}