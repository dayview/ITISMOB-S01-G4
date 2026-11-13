package com.purrfolio.mco1;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses speech recognizer text into an editable care draft. */
public final class CareCapture {
    private CareCapture() {}

    public static final class Draft {
        private final String pet, activity, amount, time, transcript;
        Draft(String pet, String activity, String amount, String time, String transcript) {
            this.pet = pet; this.activity = activity; this.amount = amount;
            this.time = time; this.transcript = transcript;
        }
        public String getPet() { return pet; }
        public String getActivity() { return activity; }
        public String getAmount() { return amount; }
        public String getTime() { return time; }
        public String getTranscript() { return transcript; }
    }

    private static final LinkedHashMap<String, String> ACTIVITIES = new LinkedHashMap<>();
    private static final Pattern AMOUNT = Pattern.compile("\\b\\d+(?:[.,]\\d+)?\\s*(?:cups?|tablespoons?|teaspoons?|tbsp|tsp|ml|milliliters?|grams?|g|kg|pills?|tablets?|doses?|scoops?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME = Pattern.compile("\\b(?:at\\s+)?(?:1[0-2]|0?[1-9]):[0-5]\\d\\s*(?:am|pm)?\\b|\\b(?:at\\s+)?(?:1[0-2]|0?[1-9])\\s*(?:am|pm)\\b", Pattern.CASE_INSENSITIVE);
    static {
        ACTIVITIES.put("fed", "Feeding"); ACTIVITIES.put("feed", "Feeding"); ACTIVITIES.put("feeding", "Feeding");
        ACTIVITIES.put("medication", "Medication"); ACTIVITIES.put("medicine", "Medication"); ACTIVITIES.put("gave medicine", "Medication");
        ACTIVITIES.put("vitamin", "Vitamins"); ACTIVITIES.put("water", "Water"); ACTIVITIES.put("groom", "Grooming");
        ACTIVITIES.put("walk", "Playtime"); ACTIVITIES.put("play", "Playtime"); ACTIVITIES.put("vet", "Vet Visit");
    }

    public static Draft parse(String transcript, List<String> petNames) {
        String lower = transcript.toLowerCase(Locale.ROOT);
        List<String> sorted = new ArrayList<>(petNames);
        sorted.sort(Comparator.comparingInt(String::length).reversed());
        String pet = null, activity = null;
        for (String name : sorted) {
            if (Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(name.toLowerCase(Locale.ROOT)) + "(?![\\p{L}\\p{N}])").matcher(lower).find()) {
                pet = name; break;
            }
        }
        for (Map.Entry<String, String> entry : ACTIVITIES.entrySet()) {
            if (Pattern.compile("\\b" + Pattern.quote(entry.getKey()) + "\\w*\\b").matcher(lower).find()) {
                activity = entry.getValue(); break;
            }
        }
        String amount = match(AMOUNT, transcript);
        String time = match(TIME, transcript);
        if (time != null) time = time.replaceFirst("(?i)^at\\s+", "").trim();
        return new Draft(pet, activity, amount, time, transcript.trim());
    }

    private static String match(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group().trim() : null;
    }
}
