package com.purrfolio.mco1;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Remembers the list of {@link LocalDocument}s between app launches (5.1 Pet Paperless Inbox).
 *
 * The list is one JSON array saved as one string, the same way pets, logs and reminders are kept.
 * Reading and writing that string goes through {@link Storage}, so the logic here can be tested
 * on the JVM with a plain in-memory Storage. {@link #inPreferences} is the real one.
 *
 * This class only keeps the records. The files themselves are handled by {@link DocumentFileStore}.
 */
public final class LocalDocumentRepository {
    /** Where the JSON text lives. Two methods, so a test can replace it with a plain variable. */
    public interface Storage {
        /** The saved text, or null when nothing was saved yet. */
        String read();

        void write(String text);
    }

    public static final String PREFERENCES_KEY = "documents";

    private final Storage storage;

    public LocalDocumentRepository(Storage storage) {
        this.storage = storage;
    }

    /** The real storage: one string in SharedPreferences under {@link #PREFERENCES_KEY}. */
    public static LocalDocumentRepository inPreferences(SharedPreferences preferences) {
        return new LocalDocumentRepository(new Storage() {
            @Override public String read() {
                return preferences.getString(PREFERENCES_KEY, null);
            }

            @Override public void write(String text) {
                preferences.edit().putString(PREFERENCES_KEY, text).apply();
            }
        });
    }

    /** Every saved document, newest first. Broken data gives an empty list instead of a crash. */
    public List<LocalDocument> list() {
        List<LocalDocument> documents = new ArrayList<>();
        String text = storage.read();
        if (text == null || text.trim().isEmpty()) return documents;
        try {
            JSONArray array = new JSONArray(text);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                LocalDocument document = LocalDocument.fromJson(item);
                // A record without an id can never be found or removed again, so it is skipped.
                if (!document.id.isEmpty()) documents.add(document);
            }
        } catch (JSONException broken) {
            return new ArrayList<>();
        }
        Collections.sort(documents, (a, b) -> Long.compare(b.addedAt, a.addedAt));
        return documents;
    }

    /** The documents of one pet, newest first. */
    public List<LocalDocument> forPet(String petId) {
        List<LocalDocument> mine = new ArrayList<>();
        if (petId == null) return mine;
        for (LocalDocument document : list()) if (document.petId.equals(petId)) mine.add(document);
        return mine;
    }

    /** Adds the document. A document with the same id replaces the old one instead of doubling it. */
    public void add(LocalDocument document) {
        if (document == null || document.id.isEmpty()) return;
        List<LocalDocument> documents = list();
        for (int i = documents.size() - 1; i >= 0; i--) {
            if (documents.get(i).id.equals(document.id)) documents.remove(i);
        }
        documents.add(document);
        saveAll(documents);
    }

    /** Removes the record and returns it, or null when there was no such id. */
    public LocalDocument remove(String id) {
        if (id == null) return null;
        List<LocalDocument> documents = list();
        LocalDocument removed = null;
        for (int i = documents.size() - 1; i >= 0; i--) {
            if (documents.get(i).id.equals(id)) removed = documents.remove(i);
        }
        if (removed != null) saveAll(documents);
        return removed;
    }

    private void saveAll(List<LocalDocument> documents) {
        JSONArray array = new JSONArray();
        for (LocalDocument document : documents) array.put(document.toJson());
        storage.write(array.toString());
    }
}
