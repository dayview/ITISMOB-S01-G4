package com.itismob.mco.purrfolio;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.util.ArrayList;
import java.util.List;

public class MoreFragment extends BaseListFragment {

    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) return;
                new AlertDialog.Builder(requireContext()).setTitle("Scanned Pet ID").setMessage(result.getContents()).setPositiveButton("OK", null).show();
            });

    @Override
    protected void onReady() {
        titleTv.setText("More");
        subtitleTv.setText("Records, identification, and settings");
    }

    @Override
    protected void load() {
        List<Row> out = new ArrayList<>();
        out.add(Row.header("For " + Session.petName(requireContext())));
        out.add(new Row("records", "Pet Records", "Vaccination cards, prescriptions, receipts", "›"));
        out.add(new Row("timeline", "Records & Timeline", "Search and filter everything", "›"));
        out.add(new Row("passport", "Pet Passport", "Shareable health summary", "›"));
        out.add(new Row("qr", "Pet ID QR", "For collars, vet visits, emergencies", "›"));
        out.add(new Row("emergency", "Emergency Mode", "Essential info at a glance", "›"));
        out.add(Row.header("Tools"));
        out.add(new Row("scan", "Scan a Pet ID QR", "Read the info an owner chose to share", "›"));
        out.add(Row.header("Account"));
        out.add(new Row("profile", "Profile", null, "›"));
        out.add(new Row("notifications", "Notifications", null, "›"));
        out.add(new Row("settings", "Account Settings", null, "›"));
        showRows(out);
    }

    private void open(Class<?> cls, boolean needsPet) {
        String pid = Session.petId(requireContext());
        if (needsPet && pid == null) {
            Ui.toast(requireContext(), "Add or select a pet first");
            return;
        }
        Intent i = new Intent(requireContext(), cls);
        if (pid != null) i.putExtra(Ui.PET_ID, pid);
        startActivity(i);
    }

    @Override
    protected void onRowClick(Row row) {
        switch (row.id) {
            case "records": open(PetRecordsActivity.class, true); break;
            case "timeline": open(RecordsActivity.class, true); break;
            case "passport": open(PassportActivity.class, true); break;
            case "qr": open(PetQrActivity.class, true); break;
            case "emergency": open(EmergencyActivity.class, true); break;
            case "profile": open(ProfileActivity.class, false); break;
            case "notifications": open(NotificationSettingsActivity.class, false); break;
            case "settings": open(AccountSettingsActivity.class, false); break;
            case "scan":
                ScanOptions options = new ScanOptions();
                options.setPrompt("Scan a Pet ID QR code");
                options.setBeepEnabled(false);
                options.setOrientationLocked(false);
                scanner.launch(options);
                break;
        }
    }
}