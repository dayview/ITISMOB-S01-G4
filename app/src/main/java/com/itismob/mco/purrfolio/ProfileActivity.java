package com.itismob.mco.purrfolio;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BaseListActivity {
    @Override
    protected void onScreenCreated() {
        titleTv.setText("Profile");
        show(bannerTv, true);
    }

    @Override
    protected void load() {
        String email = Repo.auth().getCurrentUser() == null ? "" : Repo.auth().getCurrentUser().getEmail();
        bannerTv.setText(Repo.myName() + "\n" + email);

        Repo.db().collection("users").document(Repo.uid()).get().addOnCompleteListener(t -> {
            int done = 2;   // name and email always exist
            if (t.isSuccessful() && t.getResult() != null) {
                if (!Repo.str(t.getResult(), "phone").isEmpty()) done++;
                if (!Repo.str(t.getResult(), "dob").isEmpty()) done++;
                if (!Repo.str(t.getResult(), "country").isEmpty()) done++;
            }
            List<Row> out = new ArrayList<>();
            out.add(new Row("complete", "Account Complete", done + " of 5 completed", ""));
            out.add(new Row("personal", "Personal Information", null, "›"));
            out.add(new Row("account", "Account Settings", null, "›"));
            out.add(new Row("notifications", "Notifications", null, "›"));
            out.add(new Row("privacy", "Privacy & Security", null, "›"));
            out.add(new Row("linked", "Linked Accounts", null, "›"));
            Row logout = new Row("logout", "Log Out", null, "");
            logout.danger = true;
            out.add(logout);
            showRows(out);
        });
    }

    @Override
    protected void onRowClick(Row row) {
        switch (row.id) {
            case "personal": startActivity(new Intent(this, PersonalInfoActivity.class)); break;
            case "account": startActivity(new Intent(this, AccountSettingsActivity.class)); break;
            case "notifications": startActivity(new Intent(this, NotificationSettingsActivity.class)); break;
            case "privacy":
                new AlertDialog.Builder(this).setTitle("Privacy & Security").setMessage("• Only people in a pet’s Care Circle can see that pet’s records.\n" + "• You decide what a Pet ID QR code reveals (Pet ID QR → Customize Info).\n" + "• The Activity Log shows who changed what.").setPositiveButton("OK", null).show();
                break;
            case "linked": Ui.toast(this, "No linked accounts yet"); break;
            case "logout": Ui.confirm(this, "Log out?", "Reminders on this phone will stop until you log in again.", this::logout); break;
        }
    }

    private void logout() {
        ReminderScheduler.cancelAll(this, () -> {
            Repo.auth().signOut();
            Session.clearPet(this);
            Intent i = new Intent(this, WelcomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }
}
