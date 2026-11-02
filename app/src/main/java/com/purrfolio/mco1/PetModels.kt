package com.purrfolio.mco1

import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Bundle
import org.json.JSONObject
import java.util.UUID

/**
 * A pet as plain Kotlin. The app still stores pets as JSON in SharedPreferences (MainActivity owns that),
 * so [Pet] converts to and from JSONObject. When a backend arrives, only this conversion changes.
 */
data class Pet(
    val id: String,
    val name: String,
    val species: String = "Cat",
    val breed: String = "",

    /** ISO yyyy-MM-dd, or empty when unknown. */
    val birthday: String = "",

    val sex: String = "",

    /** Kilograms as typed, e.g., "4.2". Empty when unknown. */
    val weightKg: String = "",
    val notes: String = "",

    /** A file:// URI inside the app's private storage (see PetPhotoStore), or a legacy content:// URI. */
    val photoUri: String = ""
) {
    /** Creates a brand-new JSON record for this pet. */
    fun toJson(): JSONObject = writeInto(JSONObject())

    /**
     * Copies this pet's fields into [target] and returns it. Keys this class doesn't know about are left
     * untouched, so other teammates' data on the same record is never lost.
     * Optional fields that are blank are removed instead of saved as "".
     */
    fun writeInto(target: JSONObject): JSONObject {
        target.put("id", id)
        target.put("name", name)
        target.put("species", species)
        putOrRemove(target, "breed", breed)
        putOrRemove(target, "birthday", birthday)
        putOrRemove(target, "sex", sex)
        putOrRemove(target, "weightKg", weightKg)
        putOrRemove(target, "notes", notes)
        putOrRemove(target, "photoUri", photoUri)
        return target
    }

    companion object {
        fun fromJson(json: JSONObject): Pet = Pet(
            id = json.optString("id"),
            name = json.optString("name"),
            species = json.optString("species", "Cat").ifBlank { "Cat" },
            breed = json.optString("breed"),
            birthday = json.optString("birthday"),
            sex = json.optString("sex"),
            weightKg = json.optString("weightKg"),
            notes = json.optString("notes"),
            photoUri = json.optString("photoUri")
        )

        private fun putOrRemove(target: JSONObject, key: String, value: String) {
            if (value.isBlank()) target.remove(key) else target.put(key, value)
        }
    }
}

/** One-time upgrade of data saved by the older screens. Safe to call on every launch. */
object PetMigrations {
    /** The old form saved weight as SharedPreferences "pet-ui-weight:<name>". Move it onto the pet record. */
    fun moveLegacyWeight(prefs: SharedPreferences, pets: List<JSONObject>): Boolean {
        var changed = false
        val editor = prefs.edit()
        pets.forEach { pet ->
            val key = "pet-ui-weight:" + pet.optString("name")
            val legacy = prefs.getString(key, null)
            if (legacy != null) {
                if (!pet.has("weightKg") && PetRules.weightError(legacy) == null && legacy.isNotBlank()) {
                    pet.put("weightKg", PetRules.normalizeWeight(legacy))
                    changed = true
                }
                editor.remove(key)
            }
        }
        editor.apply()
        return changed
    }
}

/**
 * Sample pets for development. Only shown in debug builds (what Android Studio's Run button installs),
 * never in a release build, and they are never saved unless the tester taps "Add to my pets".
 */
object DemoPets {
    class Demo(val pet: Pet, val portraitRes: Int)

    fun enabled(activity: Activity): Boolean =
        activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    fun all(): List<Demo> = listOf(
        Demo(Pet("demo-luna", "Luna", "Rabbit", "Holland Lop",
            "2025-09-01", "Female", "1.8", "Sample pet for testing."),
            R.drawable.pdf_luna),
        Demo(Pet("demo-simba", "Simba", "Cat", "Orange Tabby", "2022-06-15", "Male", "5.1", "Sample pet for testing."),
            R.drawable.pdf_simba)
    )
}

/**
 * Everything the user has typed into the Add / Edit pet forms.
 * It lives in MainActivity (not in a view) so typing survives opening the photo picker, rotating the phone,
 * or Android recreating the activity. Values are written into it on every keystroke.
 */
class PetDraft(val mode: Int, val id: String) {
    var step = 0
    var name = ""
    var species = "Cat"
    var otherSpecies = ""
    var breed = ""
    var birthday = ""
    var sex = ""
    var weight = ""
    var notes = ""
    var photoUri = ""

    /** The photo the pet had before this draft started. We never delete that one when the user changes photos. */
    var originalPhotoUri = ""
    private var originalSnapshot = ""

    private fun snapshot(): String = listOf(
        name.trim(), species, otherSpecies.trim(), breed.trim(),
        birthday,
        sex, PetRules.normalizeWeight(weight), notes.trim(), photoUri
    ).joinToString("\u001F")

    val isDirty: Boolean get() = snapshot() != originalSnapshot

    private fun markClean() {
        originalSnapshot = snapshot()
        originalPhotoUri = photoUri
    }

    fun finalSpecies(): String = if (species == OTHER) otherSpecies.trim().ifBlank { OTHER } else species

    fun toPet(): Pet = Pet(
        id = id,
        name = name.trim(),
        species = finalSpecies(),
        breed = breed.trim(),
        birthday = birthday,
        sex = sex,
        weightKg = PetRules.normalizeWeight(weight),
        notes = notes.trim(),
        photoUri = photoUri
    )

    fun toBundle(): Bundle = Bundle().apply {
        putInt("mode", mode); putString("id", id); putInt("step", step)
        putString("name", name); putString("species", species); putString("otherSpecies", otherSpecies)
        putString("breed", breed); putString("birthday", birthday);
        putString("sex", sex)
        putString("weight", weight); putString("notes", notes); putString("photoUri", photoUri)
        putString("originalPhotoUri", originalPhotoUri); putString("originalSnapshot", originalSnapshot)
    }

    companion object {
        const val ADD = 0
        const val EDIT = 1
        const val OTHER = "Other"
        val SPECIES_CHOICES = listOf("Cat", "Dog", "Rabbit", OTHER)
        val SEX_CHOICES = listOf("Female", "Male", "Unknown")

        fun forNewPet(): PetDraft = PetDraft(ADD, UUID.randomUUID().toString()).also { it.markClean() }

        fun forEdit(pet: Pet): PetDraft = PetDraft(EDIT, pet.id).also { draft ->
            draft.name = pet.name
            when {
                pet.species in listOf("Cat", "Dog", "Rabbit") -> draft.species = pet.species
                pet.species == OTHER || pet.species.isBlank() -> draft.species = OTHER
                else -> { draft.species = OTHER; draft.otherSpecies = pet.species }
            }
            draft.breed = pet.breed
            draft.birthday = pet.birthday
            draft.sex = pet.sex
            draft.weight = pet.weightKg
            draft.notes = pet.notes
            draft.photoUri = pet.photoUri
            draft.markClean()
        }

        fun fromBundle(bundle: Bundle?): PetDraft? {
            if (bundle == null) return null
            val id = bundle.getString("id") ?: return null
            return PetDraft(bundle.getInt("mode"), id).also { d ->
                d.step = bundle.getInt("step")
                d.name = bundle.getString("name").orEmpty()
                d.species = bundle.getString("species") ?: "Cat"
                d.otherSpecies = bundle.getString("otherSpecies").orEmpty()
                d.breed = bundle.getString("breed").orEmpty()
                d.birthday = bundle.getString("birthday").orEmpty()
                d.sex = bundle.getString("sex").orEmpty()
                d.weight = bundle.getString("weight").orEmpty()
                d.notes = bundle.getString("notes").orEmpty()
                d.photoUri = bundle.getString("photoUri").orEmpty()
                d.originalPhotoUri = bundle.getString("originalPhotoUri").orEmpty()
                d.originalSnapshot = bundle.getString("originalSnapshot").orEmpty()
            }
        }
    }
}