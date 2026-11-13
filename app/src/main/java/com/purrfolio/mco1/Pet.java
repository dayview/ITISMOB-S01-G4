package com.purrfolio.mco1;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A pet as a plain Java object. The app still stores pets as JSON in SharedPreferences (MainActivity owns that),
 * so Pet converts to and from JSONObject. When a backend arrives, only this conversion changes.
 */
public final class Pet {
    public final String id;
    public final String name;
    public final String species;
    public final String breed;
    /** ISO yyyy-MM-dd, or empty when unknown. */
    public final String birthday;
    public final String sex;
    /** Kilograms as typed, e.g. "4.2". Empty when unknown. */
    public final String weightKg;
    public final String notes;
    /** A file:// URI inside the app's private storage (see PetPhotoStore), or a legacy content:// URI. */
    public final String photoUri;

    public Pet(String id, String name, String species, String breed, String birthday,
               String sex, String weightKg, String notes, String photoUri) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthday = birthday;
        this.sex = sex;
        this.weightKg = weightKg;
        this.notes = notes;
        this.photoUri = photoUri;
    }

    /** Short form used by the demo pets: no photo. */
    public Pet(String id, String name, String species, String breed, String birthday,
               String sex, String weightKg, String notes) {
        this(id, name, species, breed, birthday, sex, weightKg, notes, "");
    }

    /** Copy of this pet with a different id. */
    public Pet withId(String newId) {
        return new Pet(newId, name, species, breed, birthday, sex, weightKg, notes, photoUri);
    }

    /** Creates a brand-new JSON record for this pet. */
    public JSONObject toJson() { return writeInto(new JSONObject()); }

    /**
     * Copies this pet's fields into target and returns it. Keys this class doesn't know about are left
     * untouched, so other teammates' data on the same record is never lost.
     * Optional fields that are blank are removed instead of saved as "".
     */
    public JSONObject writeInto(JSONObject target) {
        put(target, "id", id);
        put(target, "name", name);
        put(target, "species", species);
        putOrRemove(target, "breed", breed);
        putOrRemove(target, "birthday", birthday);
        putOrRemove(target, "sex", sex);
        putOrRemove(target, "weightKg", weightKg);
        putOrRemove(target, "notes", notes);
        putOrRemove(target, "photoUri", photoUri);
        return target;
    }

    public static Pet fromJson(JSONObject json) {
        String species = json.optString("species", "Cat");
        return new Pet(json.optString("id"), json.optString("name"),
                species.trim().isEmpty() ? "Cat" : species,
                json.optString("breed"), json.optString("birthday"), json.optString("sex"),
                json.optString("weightKg"), json.optString("notes"), json.optString("photoUri"));
    }

    static void put(JSONObject target, String key, Object value) {
        try { target.put(key, value); } catch (JSONException error) { throw new IllegalStateException(error); }
    }

    private static void putOrRemove(JSONObject target, String key, String value) {
        if (value.trim().isEmpty()) target.remove(key); else put(target, key, value);
    }
}
