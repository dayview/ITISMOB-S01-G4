package com.purrfolio.mco1;

import android.util.Base64;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/** Publishes a small, revocable pet-care snapshot for a seven-day sitter link. */
public final class SitterLinkRepository {
    public interface Callback<T> {
        void success(T value);
        void error(String message);
    }
    public static final class Link {
        public final String token;
        public final long expiresAt;
        public final String petId;
        Link(String token, String petId, long expiresAt) {
            this.token = token; this.petId = petId; this.expiresAt = expiresAt;
        }
        public String url() { return "https://purrfolio-b1e6a.web.app/s/" + token; }
    }
    private static final long WEEK_MS = 7L * 24 * 60 * 60 * 1000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void load(String petId, Callback<Link> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.error("Sign in to manage sitter links."); return; }
        db.collection("sitterLinks").whereEqualTo("ownerUid", user.getUid()).get(Source.SERVER)
                .addOnSuccessListener(result -> callback.success(activeForPet(result, petId)))
                .addOnFailureListener(error -> callback.error(message(error, "load")));
    }

    static Link activeForPet(QuerySnapshot result, String petId) {
        Link newest = null;
        long now = System.currentTimeMillis();
        for (DocumentSnapshot document : result.getDocuments()) {
            if (!petId.equals(document.getString("petId")) || !Boolean.TRUE.equals(document.getBoolean("active"))) continue;
            Long expiry = document.getLong("expiresAt");
            if (expiry != null && expiry > now && (newest == null || expiry > newest.expiresAt))
                newest = new Link(document.getId(), petId, expiry);
        }
        return newest;
    }

    public void create(JSONObject pet, Callback<Link> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.error("Sign in to create a sitter link."); return; }
        String petId = pet.optString("id");
        if (petId.isEmpty()) { callback.error("Save this pet before creating a link."); return; }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        long expiry = System.currentTimeMillis() + WEEK_MS;
        Map<String, Object> fields = new HashMap<>();
        fields.put("ownerUid", user.getUid());
        fields.put("petId", petId);
        fields.put("active", true);
        fields.put("createdAt", System.currentTimeMillis());
        fields.put("expiresAt", expiry);
        fields.put("pet", sharedPet(pet));
        db.collection("sitterLinks").document(token).set(fields)
                .addOnSuccessListener(ignored -> callback.success(new Link(token, petId, expiry)))
                .addOnFailureListener(error -> callback.error(message(error, "create")));
    }

    public void revoke(Link link, Callback<Void> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.error("Sign in to revoke the link."); return; }
        db.collection("sitterLinks").whereEqualTo("ownerUid", user.getUid()).get(Source.SERVER)
                .addOnSuccessListener(result -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : result.getDocuments())
                        if (link.petId.equals(document.getString("petId"))
                                && Boolean.TRUE.equals(document.getBoolean("active")))
                            batch.update(document.getReference(), "active", false);
                    batch.commit().addOnSuccessListener(ignored -> callback.success(null))
                            .addOnFailureListener(error -> callback.error(message(error, "turn off")));
                })
                .addOnFailureListener(error -> callback.error(message(error, "turn off")));
    }

    private static String message(Exception error, String action) {
        if (error instanceof FirebaseFirestoreException &&
                ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED)
            return "Sitter links are not enabled on the server yet.";
        return "Could not " + action + " the sitter link. Check your connection and try again.";
    }

    static Map<String, Object> sharedPet(JSONObject pet) {
        Map<String, Object> data = new HashMap<>();
        for (String key : new String[]{"name", "species", "breed", "notes", "medicalInfo",
                "vetClinic", "vetPhone", "emergencyContactName", "emergencyContactPhone"}) {
            String value = pet.optString(key, "").trim();
            data.put(key, value.length() > 500 ? value.substring(0, 500) : value);
        }
        return data;
    }
}
