package com.purrfolio.mco1

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pure Kotlin rules for pet data. No Android types are used here on purpose:
 * the logic is easy to read, easy to unit test, and safe to reuse once a backend exists.
 */
object PetRules {
    const val NAME_MAX = 40
    const val TEXT_MAX = 40
    const val NOTES_MAX = 300
    const val WEIGHT_MAX_KG = 150.0

    private val isoPattern = Regex("""\d{4}-\d{2}-\d{2}""")

    /** Parses "yyyy-MM-dd". Returns null for anything that is not a real calendar date. */
    fun parseIso(value: String): Calendar? {
        if (!isoPattern.matches(value)) return null
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.isLenient = false
        val date = runCatching { format.parse(value) }.getOrNull() ?: return null
        return Calendar.getInstance().apply { time = date }
    }

    /** month0 is the zero-based month that Android's DatePickerDialog gives back. */
    fun toIso(year: Int, month0: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month0 + 1, day)

    /** Shows the stored ISO date in the phone's own date style, e.g. "Sep 26, 2024". */
    fun displayDate(iso: String): String {
        val parsed = parseIso(iso) ?: return ""
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(parsed.time)
    }

    private fun dayNumber(c: Calendar): Long {
        val copy = Calendar.getInstance()
        copy.clear()
        copy.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        return copy.timeInMillis / 86_400_000L
    }

    fun isFuture(date: Calendar, now: Calendar = Calendar.getInstance()): Boolean =
        dayNumber(date) > dayNumber(now)

    /** "2 years old", "8 months old", "3 weeks old"... or null when the birthday is missing/invalid. */
    fun ageLabel(birthdayIso: String, now: Calendar = Calendar.getInstance()): String? {
        val birth = parseIso(birthdayIso) ?: return null
        if (isFuture(birth, now)) return null
        var months = (now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)) * 12 +
                (now.get(Calendar.MONTH) - birth.get(Calendar.MONTH))
        if (now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) months -= 1
        return when {
            months >= 24 -> "${months / 12} years old"
            months >= 12 -> "1 year old"
            months == 1 -> "1 month old"
            months > 1 -> "$months months old"
            else -> {
                val weeks = ((dayNumber(now) - dayNumber(birth)) / 7).toInt()
                when {
                    weeks >= 2 -> "$weeks weeks old"
                    weeks == 1 -> "1 week old"
                    else -> "Less than a week old"
                }
            }
        }
    }

    // ---- Validation: each function returns an error message, or null when the value is fine ----

    fun nameError(raw: String): String? {
        val name = raw.trim()
        return when {
            name.isEmpty() -> "Enter your pet's name"
            name.length > NAME_MAX -> "Use $NAME_MAX characters or fewer"
            else -> null
        }
    }

    fun shortTextError(raw: String, label: String): String? =
        if (raw.trim().length > TEXT_MAX) "$label must be $TEXT_MAX characters or fewer" else null

    fun notesError(raw: String): String? =
        if (raw.trim().length > NOTES_MAX) "Use $NOTES_MAX characters or fewer" else null

    fun birthdayError(iso: String, now: Calendar = Calendar.getInstance()): String? {
        if (iso.isBlank()) return null
        val date = parseIso(iso) ?: return "Choose a valid date"
        return if (isFuture(date, now)) "Birthday can't be in the future" else null
    }

    /** Accepts "4.2" or "4,2". Empty is fine because weight is optional. */
    fun normalizeWeight(raw: String): String = raw.trim().replace(',', '.')

    fun weightError(raw: String): String? {
        val text = normalizeWeight(raw)
        if (text.isEmpty()) return null
        val value = text.toDoubleOrNull()
        return if (value == null || value <= 0.0 || value > WEIGHT_MAX_KG)
            "Enter the weight in kg, like 4.2" else null
    }

    /** True when another pet already uses this name. Used for a gentle hint, never a block. */
    fun isDuplicateName(name: String, selfId: String, others: List<Pair<String, String>>): Boolean {
        val wanted = name.trim()
        if (wanted.isEmpty()) return false
        return others.any { (id, other) -> id != selfId && other.trim().equals(wanted, ignoreCase = true) }
    }
}
