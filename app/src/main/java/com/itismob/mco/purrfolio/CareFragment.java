package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CareFragment extends BaseListFragment {
    private Pet pet;

    @Override
    protected void onReady() {
        titleTv.setText("Care Circle");
        actionBtn.setText("+ Invite Caregiver");
        actionBtn.setOnClickListener(v -> askEmail());
    }

    private boolean isOwner() { return pet != null && Repo.uid().equals(pet.ownerId); }

    @Override
    protected void load() {
        String pid = Session.petId(requireContext());
        actionBtn.setVisibility(View.GONE);
        if (pid == null) {
            subtitleTv.setText("Add a pet to start a Care Circle");
            showRows(shortcuts(new ArrayList<>()));
            return;
        }
        Repo.pets().document(pid).get().addOnSuccessListener(d -> {
            if (!isAdded()) return;
            pet = d.toObject(Pet.class);
            if (pet == null) return;
            subtitleTv.setText("People who care for " + pet.name);
            actionBtn.setVisibility(isOwner() ? View.VISIBLE : View.GONE);
            Repo.sub(pid, "circle").get().addOnSuccessListener(snap -> {
                if (!isAdded()) return;
                List<Row> out = new ArrayList<>();
                out.add(Row.header("Care Circle (" + snap.size() + ")"));
                for (DocumentSnapshot m : snap) {
                    boolean me = m.getId().equals(Repo.uid());
                    String role = Repo.str(m, "role");
                    out.add(new Row("m:" + m.getId(), me ? "You" : Repo.str(m, "name"), role + " · " + access(m, role), isOwner() && !me ? "›" : ""));
                }
                showRows(shortcuts(out));
            });
        });
    }

    private String access(DocumentSnapshot m, String role) {
        if (role.equals("Owner")) return "Full access";
        boolean view = Boolean.TRUE.equals(m.getBoolean("canView"));
        boolean log = Boolean.TRUE.equals(m.getBoolean("canLog"));
        if (view && log) return "Care + Records";
        return log ? "Today's Care only" : "Limited";
    }

    private List<Row> shortcuts(List<Row> list) {
        list.add(Row.header("Shortcuts"));
        list.add(new Row("reminders", "Reminders", "Medication, feeding, vet visits", "›"));
        list.add(new Row("calendar", "Care Calendar", "See scheduled care by day", "›"));
        list.add(new Row("carelog", "Care Log", "View, edit, or delete entries", "›"));
        list.add(new Row("activity", "Activity Log", "Who did what and when", "›"));
        list.add(new Row("sound", "Sound Event Journal", "Record unusual sounds", "›"));
        list.add(new Row("emergency", "Emergency Mode", "Essential info at a glance", "›"));
        list.add(new Row("offline", "Offline & Sync", "Entries waiting to sync", "›"));
        return list;
    }

    private void open(Class<?> cls) {
        String pid = Session.petId(requireContext());
        if (pid == null) { Ui.toast(requireContext(), "Add or select a pet first"); return; }
        Intent i = new Intent(requireContext(), cls);
        i.putExtra(Ui.PET_ID, pid);
        startActivity(i);
    }

    @Override
    protected void onRowClick(Row row) {
        if (row.id.startsWith("m:")) {
            String uid = row.id.substring(2);
            if (!isOwner() || uid.equals(Repo.uid())) return;
            Intent i = new Intent(requireContext(), AccessControlActivity.class);
            i.putExtra(Ui.PET_ID, pet.id);
            i.putExtra(AccessControlActivity.MEMBER_ID, uid);
            startActivity(i);
            return;
        }
        switch (row.id) {
            case "reminders": open(RemindersActivity.class); break;
            case "calendar": open(CareCalendarActivity.class); break;
            case "carelog": open(CareLogActivity.class); break;
            case "activity": open(ActivityLogActivity.class); break;
            case "sound": open(SoundJournalActivity.class); break;
            case "emergency": open(EmergencyActivity.class); break;
            case "offline": open(OfflineSyncActivity.class); break;
        }
    }

    @Override
    protected void onRowLongClick(Row row) {
        if (!row.id.startsWith("m:") || !isOwner()) return;
        String uid = row.id.substring(2);
        if (uid.equals(Repo.uid())) return;
        Ui.confirm(requireContext(), "Remove from Care Circle?", row.title, () -> {
            WriteBatch b = Repo.db().batch();
            b.update(Repo.pets().document(pet.id), "members", FieldValue.arrayRemove(uid));
            b.delete(Repo.sub(pet.id, "circle").document(uid));
            b.commit();
            Repo.activity(pet.id, "Remove caregiver", row.title);
            load();
        });
    }

    private void askEmail() {
        EditText input = new EditText(requireContext());
        input.setHint("Caregiver's Purrfolio email");
        new AlertDialog.Builder(requireContext()).setTitle("Invite caregiver").setView(input).setPositiveButton("Next", (d, w) -> {
            String email = Ui.text(input).toLowerCase();
            if (!email.isEmpty()) askRole(email);
        }).setNegativeButton("Cancel", null).show();
    }

    private void askRole(String email) {
        String[] roles = {"Family (care + records)", "Sitter (today's care only)"};
        new AlertDialog.Builder(requireContext()).setTitle("Role").setItems(roles, (d, which) -> invite(email, which == 0 ? "Family" : "Sitter")).show();
    }

    private void invite(String email, String role) {
        Repo.db().collection("users").whereEqualTo("email", email).limit(1).get().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            if (snap.isEmpty()) {
                Ui.toast(requireContext(), "No Purrfolio account with that email yet");
                return;
            }
            DocumentSnapshot u = snap.getDocuments().get(0);
            boolean family = role.equals("Family");
            Map<String, Object> m = new HashMap<>();
            m.put("uid", u.getId());
            m.put("name", Repo.str(u, "fullName"));
            m.put("email", email);
            m.put("role", role);
            m.put("canView", family);
            m.put("canLog", true);
            m.put("canReminders", family);
            m.put("canDocs", family);

            WriteBatch b = Repo.db().batch();
            b.update(Repo.pets().document(pet.id), "members", FieldValue.arrayUnion(u.getId()));
            b.set(Repo.sub(pet.id, "circle").document(u.getId()), m);
            b.commit();
            Repo.activity(pet.id, "Invited caregiver", Repo.str(u, "fullName") + " (" + role + ")");
            Ui.toast(requireContext(), "Added to the Care Circle");
            load();
        }).addOnFailureListener(e -> Ui.toast(requireContext(), "Could not look up that email"));
    }
}
