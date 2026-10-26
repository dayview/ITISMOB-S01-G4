package com.itismob.mco.purrfolio;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class Session {
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("purrfolio_prefs", Context.MODE_PRIVATE);
    }
    public static void setPet(Context c, String id, String name) {
        prefs(c).edit().putString("pet_id", id).putString("pet_name", name).apply();
    }
    public static String petId(Context c) { return prefs(c).getString("pet_id", null); }
    public static String petName(Context c) { return prefs(c).getString("pet_name", "your pet"); }
    public static void clearPet(Context c) { prefs(c).edit().remove("pet_id").remove("pet_name").apply(); }
    public static int getThemeMode(Context c) {
        return prefs(c).getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
    public static void setThemeMode(Context c, int mode) { prefs(c).edit().putInt("theme", mode).apply(); }
    // keys: care, updates, tips, circle, marketing
    public static boolean notif(Context c, String key) {
        return prefs(c).getBoolean("notif_" + key, !key.equals("marketing"));
    }
    public static void setNotif(Context c, String key, boolean on) { prefs(c).edit().putBoolean("notif_" + key, on).apply(); }
}