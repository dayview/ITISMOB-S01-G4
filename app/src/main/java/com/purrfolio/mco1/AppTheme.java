package com.purrfolio.mco1;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

public final class AppTheme {
    public static final String SYSTEM = "system", LIGHT = "light", DARK = "dark";
    private AppTheme() {}

    public static String selected(Context context) {
        String choice = context.getSharedPreferences("purrfolio_demo", Context.MODE_PRIVATE)
                .getString("app_theme", SYSTEM);
        return choice == null ? SYSTEM : choice;
    }

    public static String label(Context context) {
        switch (selected(context)) {
            case LIGHT: return "Light Mode";
            case DARK: return "Dark Mode";
            default: return "System Default";
        }
    }

    public static void applySaved(Context context) {
        AppCompatDelegate.setDefaultNightMode(mode(selected(context)));
    }

    public static void select(Context context, String choice) {
        context.getSharedPreferences("purrfolio_demo", Context.MODE_PRIVATE).edit()
                .putString("app_theme", choice).apply();
        AppCompatDelegate.setDefaultNightMode(mode(choice));
    }

    private static int mode(String choice) {
        switch (choice) {
            case LIGHT: return AppCompatDelegate.MODE_NIGHT_NO;
            case DARK: return AppCompatDelegate.MODE_NIGHT_YES;
            default: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}
