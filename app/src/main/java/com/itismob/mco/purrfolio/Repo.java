package com.itismob.mco.purrfolio;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Repo {
    public interface PetsCallback { void onLoaded(List<Pet> pets); }
    public interface RemindersCallback { void onLoaded(List<Reminder> reminders); }
    public interface DocsCallback { void onLoaded(List<DocumentSnapshot> docs); }
    public interface Check { void result(boolean allowed); }
    public static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    public static FirebaseAuth auth() { return FirebaseAuth.getInstance(); }
    public static String uid() {
        FirebaseUser u = auth().getCurrentUser();
        return u == null ? null : u.getUid();
    }
    public static String myName() {
        FirebaseUser u = auth().getCurrentUser();
        if (u == null) return "Someone";
        if (u.getDisplayName() != null && !u.getDisplayName().isEmpty()) return u.getDisplayName();
        return u.getEmail() == null ? "Someone" : u.getEmail();
    }
    public static CollectionReference pets() { return db().collection("pets"); }
    public static CollectionReference sub(String petId, String name) { return pets().document(petId).collection(name); }
    public static String str(DocumentSnapshot d, String field) {
        String s = d.getString(field);
        return s == null ? "" : s;
    }
    public static String orNone(String s) { return s == null || s.isEmpty() ? "None recorded" : s; }

    // pets
    public static void loadMyPets(PetsCallback cb) {
        if (uid() == null) { cb.onLoaded(new ArrayList<>()); return; }
        pets().whereArrayContains("members", uid()).get().addOnCompleteListener(task -> {
            List<Pet> list = new ArrayList<>();
            if (task.isSuccessful()) {
                for (DocumentSnapshot d : task.getResult()) {
                    Pet p = d.toObject(Pet.class);
                    if (p != null) list.add(p);
                }
            }
            Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
            cb.onLoaded(list);
        });
    }

    // reminders
    public static void loadReminders(Pet p, RemindersCallback cb) {
        sub(p.id, "reminders").get().addOnCompleteListener(task -> {
            List<Reminder> out = new ArrayList<>();
            if (task.isSuccessful()) {
                for (DocumentSnapshot d : task.getResult()) {
                    Reminder r = d.toObject(Reminder.class);
                    if (r != null) { r.petId = p.id; r.petName = p.name; out.add(r); }
                }
            }
            cb.onLoaded(out);
        });
    }
    public static void loadAllReminders(RemindersCallback cb) {
        loadMyPets(pets -> {
            List<Reminder> all = new ArrayList<>();
            if (pets.isEmpty()) { cb.onLoaded(all); return; }
            int[] left = {pets.size()};
            for (Pet p : pets) {
                loadReminders(p, list -> {
                    all.addAll(list);
                    if (--left[0] == 0) cb.onLoaded(all);
                });
            }
        });
    }

    // documents and vaccinations
    private static void loadOrdered(String petId, String name, String field, DocsCallback cb) {
        sub(petId, name).orderBy(field, Query.Direction.DESCENDING).get().addOnCompleteListener(t -> cb.onLoaded(t.isSuccessful() ? t.getResult().getDocuments() : new ArrayList<DocumentSnapshot>()));
    }
    public static void loadDocs(String petId, DocsCallback cb) { loadOrdered(petId, "documents", "addedAt", cb); }
    public static void loadVaccinations(String petId, DocsCallback cb) { loadOrdered(petId, "vaccinations", "givenDate", cb); }
    public static Row docRow(DocumentSnapshot d) {
        Long ts = d.getLong("addedAt");
        return new Row(d.getId(), str(d, "title"), Ui.date(ts == null ? 0 : ts) + " · " + str(d, "type"), "›");
    }
    public static String vaccineLine(DocumentSnapshot d) {
        String next = str(d, "nextDate");
        return str(d, "name") + " - given " + str(d, "givenDate") + (next.isEmpty() ? "" : " · next " + next);
    }

    // permissions (Care circle)
    // field: canView, canLog, canReminders, canDocs. the owner is always allowed on this matter
    public static void can(String petId, String field, Check cb) {
        sub(petId, "circle").document(uid()).get().addOnSuccessListener(d -> {
            if (!d.exists()) { cb.result(true); return; }
            cb.result("Owner".equals(d.getString("role")) || Boolean.TRUE.equals(d.getBoolean(field)));
        }).addOnFailureListener(e -> cb.result(true));
    }

    // writes
    public static void addLog(String petId, String petName, String type, String amount, String note, long ts, boolean finished) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("amount", amount);
        m.put("note", note);
        m.put("timestamp", ts);
        m.put("finished", finished);
        m.put("petName", petName);
        m.put("authorId", uid());
        m.put("authorName", myName());
        sub(petId, "logs").add(m);
        activity(petId, "Logged " + type, (amount.isEmpty() ? "" : amount + " · ") + Ui.time(ts));
    }

    // care circle activity log
    public static void activity(String petId, String action, String detail) {
        Map<String, Object> m = new HashMap<>();
        m.put("action", action);
        m.put("detail", detail);
        m.put("authorId", uid());
        m.put("authorName", myName());
        m.put("timestamp", System.currentTimeMillis());
        sub(petId, "activity").add(m);
    }

    // petId QR
    public static String qrPayload(Pet p, String ownerPhone) {
        StringBuilder sb = new StringBuilder("PURRFOLIO PET ID\n");
        if (p.share("name", true)) sb.append("Name: ").append(p.name).append('\n');
        if (p.share("species", true)) {
            sb.append("Species: ").append(p.species).append(p.breed.isEmpty() ? "" : " (" + p.breed + ")").append('\n');
        }
        if (p.share("photo", true) && !p.photoUrl.isEmpty()) sb.append("Photo: ").append(p.photoUrl).append('\n');
        if (p.share("medical", true)) {
            sb.append("Allergies: ").append(orNone(p.allergies)).append('\n');
            sb.append("Medications: ").append(orNone(p.medications)).append('\n');
            sb.append("Conditions: ").append(orNone(p.conditions)).append('\n');
        }
        if (p.share("vet", true) && !p.vetName.isEmpty()) {
            sb.append("Vet: ").append(p.vetName).append(p.vetPhone.isEmpty() ? "" : " " + p.vetPhone).append('\n');
        }
        if (p.share("owner", false) && ownerPhone != null && !ownerPhone.isEmpty()) {
            sb.append("Owner contact: ").append(ownerPhone).append('\n');
        }
        return sb.toString().trim();
    }

}
