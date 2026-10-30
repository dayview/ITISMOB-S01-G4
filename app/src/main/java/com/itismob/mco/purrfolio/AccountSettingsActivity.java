package com.itismob.mco.purrfolio;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountSettingsActivity extends BaseListActivity {
    private static final String[] THEMES = {"Light", "Dark", "System default"};
    private static final int[] MODES = {AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM};

    @Override
    protected void onScreenCreated() {
        titleTv.setText("Account Settings");
    }

    private String themeName() {
        int mode = Session.getThemeMode(this);
        for (int i = 0; i < MODES.length; i++) if (MODES[i] == mode) return THEMES[i];
        return THEMES[2];
    }

    @Override
    protected void load() {
        List<Row> out = new ArrayList<>();
        out.add(new Row("password", "Change Password", "We’ll email you a reset link", "›"));
        out.add(new Row("email", "Email Preferences", null, "›"));
        out.add(new Row("theme", "App Theme — " + themeName(), null, "›"));
        out.add(new Row("language", "Language — English", null, "›"));
        out.add(new Row("data", "Data & Storage", null, "›"));
        out.add(Row.header("Account"));
        Row del = new Row("delete", "Delete Account", null, "›");
        del.danger = true;
        out.add(del);
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        switch (row.id) {
            case "password":
                String email = Repo.auth().getCurrentUser().getEmail();
                Repo.auth().sendPasswordResetEmail(email).addOnSuccessListener(v -> Ui.toast(this, "Reset link sent to " + email)).addOnFailureListener(e -> Ui.toast(this, e.getMessage()));
                break;
            case "email":
                startActivity(new Intent(this, NotificationSettingsActivity.class));
                break;
            case "theme":
                new AlertDialog.Builder(this).setTitle("App Theme").setItems(THEMES, (d, which) -> {
                    Session.setThemeMode(this, MODES[which]);
                    AppCompatDelegate.setDefaultNightMode(MODES[which]);
                }).show();
                break;
            case "language":
                Ui.toast(this, "English is the only language in this version");
                break;
            case "data":
                new AlertDialog.Builder(this).setTitle("Data & Storage").setMessage("Your pets, logs, and documents are stored in your Purrfolio cloud account. " + "A copy is kept on this phone so you can keep logging offline.").setPositiveButton("OK", null).show();
                break;
            case "delete":
                Ui.confirm(this, "Delete account?", "This permanently deletes your login and profile.", this::deleteAccount);
                break;
        }
    }

    private void deleteAccount() {
        FirebaseUser user = Repo.auth().getCurrentUser();
        String uid = user.getUid();
        String email = user.getEmail();
        Repo.db().collection("users").document(uid).delete();

        user.delete().addOnSuccessListener(v -> {
            Session.clearPet(this);
            Intent i = new Intent(this, WelcomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }).addOnFailureListener(e -> {
            Map<String, Object> back = new HashMap<>();
            back.put("fullName", Repo.myName());
            back.put("email", email == null ? "" : email.toLowerCase());
            Repo.db().collection("users").document(uid).set(back);
            Ui.toast(this, "Please log out, log in again, and then retry deleting your account.");
        });
    }
}