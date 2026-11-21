package com.purrfolio.mco1;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReminderRepository {
    private ReminderRepository() { }

    public interface RemindersCallback { void onResult(List<Reminder> reminders); }
    public interface PetsCallback { void onResult(List<PetRef> pets); }
    private static CollectionReference col(String petId) {
        return FirebaseFirestore.getInstance().collection("pets").document(petId).collection("reminders");
    }

    private static void readInto(List<Reminder> out, Iterable<? extends DocumentSnapshot> docs, PetRef pet) {
        for (DocumentSnapshot d : docs) {
            Reminder r = d.toObject(Reminder.class);
            if (r == null) continue;
            r.petId = pet.id;
            r.petName = pet.name;
            out.add(r);
        }
    }

    /* creates the reminder or updates it. the new id is written into r.id. */
    public static void save(Reminder r) {
        DocumentReference doc = (r.id == null || r.id.isEmpty()) ? col(r.petId).document() : col(r.petId).document(r.id);
        r.id = doc.getId();
        doc.set(r.toMap(), SetOptions.merge());
    }

    public static void delete(Reminder r) {
        if (r.id != null && !r.id.isEmpty()) col(r.petId).document(r.id).delete();
    }

    /* stamps "given/done" */
    public static void stampDone(Reminder r, long whenMillis) {
        r.lastDoneAt = whenMillis;
        if (r.id != null && !r.id.isEmpty()) col(r.petId).document(r.id).update("lastDoneAt", whenMillis);
    }

    /* live list of the reminders of every pet in pets. call the returned runnable to stop listening */
    public static Runnable listen(List<PetRef> pets, RemindersCallback callback) {
        Map<String, List<Reminder>> byPet = new HashMap<>();
        List<ListenerRegistration> registrations = new ArrayList<>();
        for (PetRef pet : pets) {
            registrations.add(col(pet.id).addSnapshotListener((snap, error) -> {
                if (snap == null) return;
                List<Reminder> list = new ArrayList<>();
                readInto(list, snap.getDocuments(), pet);
                byPet.put(pet.id, list);
                List<Reminder> all = new ArrayList<>();
                for (List<Reminder> part : byPet.values()) all.addAll(part);
                callback.onResult(all);
            }));
        }
        return () -> { for (ListenerRegistration r : registrations) r.remove(); };
    }

    /* one-time read of every pet's reminders */
    public static void loadOnce(List<PetRef> pets, RemindersCallback callback) {
        List<Reminder> all = new ArrayList<>();
        if (pets.isEmpty()) { callback.onResult(all); return; }
        int[] left = {pets.size()};
        for (PetRef pet : pets) {
            col(pet.id).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) readInto(all, task.getResult().getDocuments(), pet);
                left[0]--;
                if (left[0] == 0) callback.onResult(all);
            });
        }
    }

    /* the pets the signed-in user belongs to the owner/member */
    public static void loadMyPets(PetsCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onResult(new ArrayList<>()); return; }
        FirebaseFirestore.getInstance().collection("pets").whereArrayContains("members", user.getUid()).get().addOnCompleteListener(task -> {
            List<PetRef> list = new ArrayList<>();
            if (task.isSuccessful()) {
                for (DocumentSnapshot d : task.getResult().getDocuments()) {
                    String name = d.getString("name");
                    list.add(new PetRef(d.getId(), name == null ? "Pet" : name));
                }
            }
            Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
            callback.onResult(list);
        });
    }
}
