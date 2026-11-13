package com.purrfolio.mco1;

import android.os.Bundle;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Everything the user has typed into the Add / Edit pet forms.
 * It lives in MainActivity (not in a view) so typing survives opening the photo picker, rotating the phone,
 * or Android recreating the activity. Values are written into it on every keystroke.
 */
public final class PetDraft {
    public static final int ADD = 0;
    public static final int EDIT = 1;
    public static final String OTHER = "Other";
    public static final List<String> SPECIES_CHOICES = Arrays.asList("Cat", "Dog", "Rabbit", OTHER);
    public static final List<String> SEX_CHOICES = Arrays.asList("Female", "Male", "Unknown");

    public final int mode;
    public final String id;
    public int step = 0;
    public String name = "";
    public String species = "Cat";
    public String otherSpecies = "";
    public String breed = "";
    public String birthday = "";
    public String sex = "";
    public String weight = "";
    public String notes = "";
    public String photoUri = "";

    /** The photo the pet had before this draft started. We never delete that one when the user changes photos. */
    public String originalPhotoUri = "";
    private String originalSnapshot = "";

    public PetDraft(int mode, String id) {
        this.mode = mode;
        this.id = id;
    }

    private String snapshot() {
        return String.join("\u001F", name.trim(), species, otherSpecies.trim(), breed.trim(), birthday,
                sex, PetRules.normalizeWeight(weight), notes.trim(), photoUri);
    }

    public boolean isDirty() { return !snapshot().equals(originalSnapshot); }

    private void markClean() {
        originalSnapshot = snapshot();
        originalPhotoUri = photoUri;
    }

    public String finalSpecies() {
        if (!OTHER.equals(species)) return species;
        String typed = otherSpecies.trim();
        return typed.isEmpty() ? OTHER : typed;
    }

    public Pet toPet() {
        return new Pet(id, name.trim(), finalSpecies(), breed.trim(), birthday, sex,
                PetRules.normalizeWeight(weight), notes.trim(), photoUri);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("mode", mode);
        bundle.putString("id", id);
        bundle.putInt("step", step);
        bundle.putString("name", name);
        bundle.putString("species", species);
        bundle.putString("otherSpecies", otherSpecies);
        bundle.putString("breed", breed);
        bundle.putString("birthday", birthday);
        bundle.putString("sex", sex);
        bundle.putString("weight", weight);
        bundle.putString("notes", notes);
        bundle.putString("photoUri", photoUri);
        bundle.putString("originalPhotoUri", originalPhotoUri);
        bundle.putString("originalSnapshot", originalSnapshot);
        return bundle;
    }

    public static PetDraft forNewPet() {
        PetDraft draft = new PetDraft(ADD, UUID.randomUUID().toString());
        draft.markClean();
        return draft;
    }

    public static PetDraft forEdit(Pet pet) {
        PetDraft draft = new PetDraft(EDIT, pet.id);
        draft.name = pet.name;
        if (Arrays.asList("Cat", "Dog", "Rabbit").contains(pet.species)) {
            draft.species = pet.species;
        } else if (pet.species.equals(OTHER) || pet.species.trim().isEmpty()) {
            draft.species = OTHER;
        } else {
            draft.species = OTHER;
            draft.otherSpecies = pet.species;
        }
        draft.breed = pet.breed;
        draft.birthday = pet.birthday;
        draft.sex = pet.sex;
        draft.weight = pet.weightKg;
        draft.notes = pet.notes;
        draft.photoUri = pet.photoUri;
        draft.markClean();
        return draft;
    }

    /** Rebuilds a draft saved by toBundle(), or returns null when there is nothing (valid) to restore. */
    public static PetDraft fromBundle(Bundle bundle) {
        if (bundle == null) return null;
        String id = bundle.getString("id");
        if (id == null) return null;
        PetDraft d = new PetDraft(bundle.getInt("mode"), id);
        d.step = bundle.getInt("step");
        d.name = orEmpty(bundle.getString("name"));
        String species = bundle.getString("species");
        d.species = species == null ? "Cat" : species;
        d.otherSpecies = orEmpty(bundle.getString("otherSpecies"));
        d.breed = orEmpty(bundle.getString("breed"));
        d.birthday = orEmpty(bundle.getString("birthday"));
        d.sex = orEmpty(bundle.getString("sex"));
        d.weight = orEmpty(bundle.getString("weight"));
        d.notes = orEmpty(bundle.getString("notes"));
        d.photoUri = orEmpty(bundle.getString("photoUri"));
        d.originalPhotoUri = orEmpty(bundle.getString("originalPhotoUri"));
        d.originalSnapshot = orEmpty(bundle.getString("originalSnapshot"));
        return d;
    }

    private static String orEmpty(String value) { return value == null ? "" : value; }
}
