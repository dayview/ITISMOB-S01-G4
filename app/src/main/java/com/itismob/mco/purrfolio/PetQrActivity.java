package com.itismob.mco.purrfolio;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PetQrActivity extends BaseListActivity {
    private String payload = "", petName = "Pet";
    private Bitmap qrBitmap;

    @Override
    protected void onScreenCreated() {
        titleTv.setText("PET ID QR");
        subtitleTv.setText("A safer tomorrow for your furbaby");
        show(bannerTv, true);
        show(imageIv, true);

        List<Row> out = new ArrayList<>();
        out.add(new Row("customize", "Customize Info", "Choose what is shown when someone scans", "›"));
        out.add(new Row("share", "Share Pet ID", "Send the notification as text", "›"));
        out.add(new Row("save", "Save QR to Photos", "Print it or add it to a collar tag", "›"));
        showRows(out);
    }

    @Override
    protected void load() {
        Repo.pets().document(petId()).get().addOnSuccessListener(d -> {
            Pet p = d.toObject(Pet.class);
            if (p == null) { finish(); return; }
            petName = p.name;
            bannerTv.setText(p.name + " - " + p.species + ", " + Ui.age(p.birthday));
            if (p.share("owner", false) && !p.ownerId.isEmpty()) {
                Repo.db().collection("users").document(p.ownerId).get().addOnSuccessListener(u -> build(p, u.getString("phone"))).addOnFailureListener(e -> build(p, null));
            } else {
                build(p, null);
            }
        });
    }

    private void build(Pet p, String ownerPhone) {
        payload = Repo.qrPayload(p, ownerPhone);
        try {
            qrBitmap = new BarcodeEncoder().encodeBitmap(payload, BarcodeFormat.QR_CODE, 700, 700);
            imageIv.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            Ui.toast(this, "Could not create the QR code");
        }
    }

    @Override
    protected void onRowClick(Row row) {
        switch (row.id) {
            case "customize":
                Intent i = new Intent(this, QrPrivacyActivity.class);
                i.putExtra(Ui.PET_ID, petId());
                startActivity(i);
                break;
            case "share":
                if (!payload.isEmpty()) Ui.share(this, petName + " - PET ID", payload);
                break;
            case "save":
                saveToPhotos();
                break;
        }
    }

    private void saveToPhotos() {
        if (qrBitmap == null) return;
        if (Build.VERSION.SDK_INT < 29) {
            Ui.toast(this, "Saving to Photos needs Android 10 or newer. Use Share instead.");
            return;
        }
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "purrfolio_qr_" + System.currentTimeMillis() + ".png");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Purrfolio");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            Ui.toast(this, "Saved to Pictures/Purrfolio");
        } catch (IOException | NullPointerException e) {
            Ui.toast(this, "Could not save the image");
        }
    }
}
