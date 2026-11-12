package com.purrfolio.mco1;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CareWrites {
    private CareWrites() { }

    private static DocumentReference pet(String petId) {
        return FirebaseFirestore.getInstance().collection("pets").document(petId);
    }

    public static String authorId() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u == null ? "" : u.getUid();
    }

    public static String authorName() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return "Someone";
        if (u.getDisplayName() != null && !u.getDisplayName().trim().isEmpty()) return u.getDisplayName();
        return u.getEmail() == null ? "Someone" : u.getEmail();
    }

    /* care circle activity log entry */
    public static void activity(String petId, String action, String detail) {
        Map<String, Object> m = new HashMap<>();
        m.put("action", action);
        m.put("detail", detail);
        m.put("authorId", authorId());
        m.put("authorName", authorName());
        m.put("timestamp", System.currentTimeMillis());
        pet(petId).collection("activity").add(m);
    }

    /* a care log entry */
    public static void careLog(String petId, String petName, String type, String amount, String note, long whenMillis) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("amount", amount);
        m.put("note", note);
        m.put("timestamp", whenMillis);
        m.put("finished", true);
        m.put("petName", petName);
        m.put("authorId", authorId());
        m.put("authorName", authorName());
        pet(petId).collection("logs").add(m);
    }
}
