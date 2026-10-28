package com.purrfolio.mco1

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object AppTheme {
    private const val PREFERENCES = "purrfolio_demo"
    private const val KEY = "app_theme"
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    fun selected(context: Context): String =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun label(context: Context): String = when (selected(context)) {
        LIGHT -> "Light Mode"
        DARK -> "Dark Mode"
        else -> "System Default"
    }

    fun applySaved(context: Context) {
        AppCompatDelegate.setDefaultNightMode(mode(selected(context)))
    }

    fun select(context: Context, choice: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY, choice).apply()
        AppCompatDelegate.setDefaultNightMode(mode(choice))
    }

    private fun mode(choice: String): Int = when (choice) {
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
