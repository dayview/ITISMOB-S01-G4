package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class DocumentRepository {
    private DocumentRepository() { }
    public interface DocumentsCallback { void onResult(List<PetDocument> documents); }
    public interface BitmapCallback { void onResult(Bitmap bitmap); }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /* live list of a pet's documents, newest first. call the returned runnable to stop listening */
    public static Runnable listen(String petId, DocumentsCallback callback) {
        ListenerRegistration registration = FirebaseFirestore.getInstance().collection("pets").document(petId).collection("documents").orderBy("addedAt", Query.Direction.DESCENDING).addSnapshotListener((snap, error) -> {
            if (snap == null) return;
            List<PetDocument> list = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                PetDocument doc = d.toObject(PetDocument.class);
                if (doc == null) continue;
                doc.petId = petId;
                list.add(doc);
            }
            callback.onResult(list);
        });
        return registration::remove;
    }

    /* tab: all, vaccination, medications, receipts or other. query searches the title and file name. */
    public static List<PetDocument> filter(List<PetDocument> docs, String tab, String query) {
        List<PetDocument> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (PetDocument d : docs) {
            boolean tabOk = tab.equals("All") || d.tabLabel().equals(tab);
            boolean queryOk = q.isEmpty() || d.title.toLowerCase().contains(q) || d.fileName.toLowerCase().contains(q);
            if (tabOk && queryOk) out.add(d);
        }
        return out;
    }

    /* range: all time, this month, last 3 months, last year */
    public static boolean inRange(PetDocument d, int rangeIndex) {
        Calendar cutOff = Calendar.getInstance();
        switch (rangeIndex) {
            case 1: cutOff.set(Calendar.DAY_OF_MONTH, 1); cutOff.set(Calendar.HOUR_OF_DAY, 0); cutOff.set(Calendar.MINUTE, 0); break;
            case 2: cutOff.add(Calendar.MONTH, -3); break;
            case 3: cutOff.add(Calendar.YEAR, -1); break;
            default: return true;
        }
        return d.addedAt >= cutOff.getTimeInMillis();
    }

    /* opens the file in whatever app can show it */
    public static void open(Context context, PetDocument d) {
        if (d.url == null || d.url.trim().isEmpty()) {
            Toast.makeText(context, "This document has no file attached.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(d.url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No app can open this file.", Toast.LENGTH_SHORT).show();
        }
    }

    /* downloads an image on a background thread and returns it on the main thread */
    public static void loadBitMap(String url, BitmapCallback callback) {
        new Thread(() -> {
            Bitmap bitmap = null;
            try (InputStream in = new URL(url).openStream()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                bitmap = BitmapFactory.decodeStream(in, null, options);
            } catch (Exception ignored) { }
            Bitmap result = bitmap;
            MAIN.post(() -> callback.onResult(result));
        }).start();
    }

    /* view full screen: images appear in a dialog, everything else opens in another app */
    public static void view(Activity activity, PetDocument d) {
        if (!d.looksLikeImage()) { open(activity, d); return; }
        Toast.makeText(activity, "Loading...", Toast.LENGTH_SHORT).show();
        loadBitMap(d.url, bitmap -> {
            if (bitmap == null || activity.isFinishing()) { open(activity, d); return; }
            ImageView image = new ImageView(activity);
            image.setImageBitmap(bitmap);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            new AlertDialog.Builder(activity).setTitle(d.title).setView(image).setPositiveButton("Close", null).setNeutralButton("Open in another app", (dialog, which) -> open(activity, d)).show();
        });
    }
}
