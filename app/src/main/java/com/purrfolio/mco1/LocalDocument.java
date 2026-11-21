package com.purrfolio.mco1;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * One document saved on this phone (5.1 Pet Paperless Inbox). Plain Java, like {@link Pet}.
 *
 * The field names match the team's {@code PetDocument} (id, petId, title, type, fileName, addedAt)
 * so the two can be shown together later. {@link #path} is relative to the app's files folder,
 * for example "docs/&lt;petId&gt;/&lt;id&gt;.pdf". A relative path stays valid if the folder moves.
 */
public final class LocalDocument {
    public final String id;
    public final String petId;
    public final String title;
    public final String type;
    public final String fileName;
    public final String path;
    public final long addedAt;
    public final long sizeBytes;

    public LocalDocument(String id, String petId, String title, String type, String fileName,
                         String path, long addedAt, long sizeBytes) {
        this.id = clean(id);
        this.petId = clean(petId);
        this.title = clean(title);
        this.type = DocumentRules.typeOrDefault(type);
        this.fileName = clean(fileName);
        this.path = clean(path);
        this.addedAt = addedAt;
        this.sizeBytes = Math.max(0, sizeBytes);
    }

    /** Missing or wrong-typed keys become empty values instead of throwing. */
    public static LocalDocument fromJson(JSONObject json) {
        if (json == null) return new LocalDocument("", "", "", null, "", "", 0, 0);
        return new LocalDocument(json.optString("id"), json.optString("petId"), json.optString("title"),
                json.optString("type"), json.optString("fileName"), json.optString("path"),
                json.optLong("addedAt", 0), json.optLong("sizeBytes", 0));
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("petId", petId);
            json.put("title", title);
            json.put("type", type);
            json.put("fileName", fileName);
            json.put("path", path);
            json.put("addedAt", addedAt);
            json.put("sizeBytes", sizeBytes);
        } catch (JSONException impossible) {
            // Keys are fixed strings and values are plain types, so this cannot happen.
        }
        return json;
    }

    public boolean looksLikeImage() {
        return DocumentRules.isImage(fileName);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
