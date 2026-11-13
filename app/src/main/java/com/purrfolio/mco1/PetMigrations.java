package com.purrfolio.mco1;

import android.content.SharedPreferences;
import java.util.List;
import org.json.JSONObject;

/** One-time upgrade of data saved by the older screens. Safe to call on every launch. */
public final class PetMigrations {
    private PetMigrations() {}

    /** The old form saved weight as SharedPreferences "pet-ui-weight:name". Move it onto the pet record. */
    public static boolean moveLegacyWeight(SharedPreferences prefs, List<JSONObject> pets) {
        boolean changed = false;
        SharedPreferences.Editor editor = prefs.edit();
        for (JSONObject pet : pets) {
            String key = "pet-ui-weight:" + pet.optString("name");
            String legacy = prefs.getString(key, null);
            if (legacy == null) continue;
            if (!pet.has("weightKg") && PetRules.weightError(legacy) == null && !legacy.trim().isEmpty()) {
                Pet.put(pet, "weightKg", PetRules.normalizeWeight(legacy));
                changed = true;
            }
            editor.remove(key);
        }
        editor.apply();
        return changed;
    }
}
