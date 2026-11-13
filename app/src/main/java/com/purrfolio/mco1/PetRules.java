package com.purrfolio.mco1;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Plain Java rules for pet data. No Android view types are used here on purpose:
 * the logic is easy to read, easy to unit test, and safe to reuse once a backend exists.
 */
public final class PetRules {
    private PetRules() {}

    public static final int NAME_MAX = 40;
    public static final int TEXT_MAX = 40;
    public static final int NOTES_MAX = 300;
    public static final double WEIGHT_MAX_KG = 150.0;

    private static final Pattern ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    /** Same shapes Kotlin's toDoubleOrNull accepts, so "4.2f" or "NaN" are not treated as numbers. */
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    /** Parses "yyyy-MM-dd". Returns null for anything that is not a real calendar date. */
    public static Calendar parseIso(String value) {
        if (value == null || !ISO.matcher(value).matches()) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        try {
            Calendar result = Calendar.getInstance();
            result.setTime(format.parse(value));
            return result;
        } catch (java.text.ParseException error) {
            return null;
        }
    }

    /** month0 is the zero-based month that Android's DatePickerDialog gives back. */
    public static String toIso(int year, int month0, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month0 + 1, day);
    }

    /** Shows the stored ISO date in the phone's own date style, e.g. "Sep 26, 2024". */
    public static String displayDate(String iso) {
        Calendar parsed = parseIso(iso);
        if (parsed == null) return "";
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(parsed.getTime());
    }

    private static long dayNumber(Calendar c) {
        Calendar copy = Calendar.getInstance();
        copy.clear();
        copy.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        return copy.getTimeInMillis() / 86_400_000L;
    }

    public static boolean isFuture(Calendar date) { return isFuture(date, Calendar.getInstance()); }

    public static boolean isFuture(Calendar date, Calendar now) { return dayNumber(date) > dayNumber(now); }

    /** "2 years old", "8 months old", "3 weeks old"... or null when the birthday is missing/invalid. */
    public static String ageLabel(String birthdayIso) { return ageLabel(birthdayIso, Calendar.getInstance()); }

    public static String ageLabel(String birthdayIso, Calendar now) {
        Calendar birth = parseIso(birthdayIso);
        if (birth == null || isFuture(birth, now)) return null;
        int months = (now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)) * 12
                + (now.get(Calendar.MONTH) - birth.get(Calendar.MONTH));
        if (now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) months -= 1;
        if (months >= 24) return (months / 12) + " years old";
        if (months >= 12) return "1 year old";
        if (months == 1) return "1 month old";
        if (months > 1) return months + " months old";
        int weeks = (int) ((dayNumber(now) - dayNumber(birth)) / 7);
        if (weeks >= 2) return weeks + " weeks old";
        if (weeks == 1) return "1 week old";
        return "Less than a week old";
    }

    // ---- Validation: each function returns an error message, or null when the value is fine ----

    public static String nameError(String raw) {
        String name = raw.trim();
        if (name.isEmpty()) return "Enter your pet's name";
        if (name.length() > NAME_MAX) return "Use " + NAME_MAX + " characters or fewer";
        return null;
    }

    public static String shortTextError(String raw, String label) {
        return raw.trim().length() > TEXT_MAX ? label + " must be " + TEXT_MAX + " characters or fewer" : null;
    }

    public static String notesError(String raw) {
        return raw.trim().length() > NOTES_MAX ? "Use " + NOTES_MAX + " characters or fewer" : null;
    }

    public static String birthdayError(String iso) { return birthdayError(iso, Calendar.getInstance()); }

    public static String birthdayError(String iso, Calendar now) {
        if (iso.trim().isEmpty()) return null;
        Calendar date = parseIso(iso);
        if (date == null) return "Choose a valid date";
        return isFuture(date, now) ? "Birthday can't be in the future" : null;
    }

    /** Accepts "4.2" or "4,2". Empty is fine because weight is optional. */
    public static String normalizeWeight(String raw) { return raw.trim().replace(',', '.'); }

    public static String weightError(String raw) {
        String text = normalizeWeight(raw);
        if (text.isEmpty()) return null;
        Double value = null;
        if (NUMBER.matcher(text).matches()) {
            try { value = Double.valueOf(text); } catch (NumberFormatException ignored) { }
        }
        return value == null || value <= 0.0 || value > WEIGHT_MAX_KG ? "Enter the weight in kg, like 4.2" : null;
    }

    /** True when another pet already uses this name. Used for a gentle hint, never a block. */
    public static boolean isDuplicateName(String name, String selfId, List<Pet> others) {
        String wanted = name.trim();
        if (wanted.isEmpty()) return false;
        for (Pet other : others)
            if (!other.id.equals(selfId) && other.name.trim().equalsIgnoreCase(wanted)) return true;
        return false;
    }
}
