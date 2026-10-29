package com.itismob.mco.purrfolio;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceParser {
    private static final Pattern AMOUNT = Pattern.compile("\\b(\\d+(?:\\.\\d+)?(?:/\\d+)?|half|one|two|three|four|five|a|an)\\s*" + "(cups?|tablets?|pills?|ml|mg|grams?|scoops?|minutes?|mins?|hours?|drops?|cans?|packs?|pouch(?:es)?)\\b");
    private static final Pattern TIME = Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b");
    private static boolean has(String s, String... words) {
        for (String w : words) {
            if (Pattern.compile("\\b" + Pattern.quote(w)).matcher(s).find()) return true;
        }
        return false;
    }
    public static String detectType(String s) {
        if (has(s, "water", "drank", "drink")) return "Water";
        if (has(s, "medicine", "medication", "pill", "tablet", "vitamin", "dose")) return "Medication";
        if (has(s, "fed", "feed", "ate", "eat", "breakfast", "dinner", "lunch", "kibble", "food")) return "Feeding";
        if (has(s, "play", "walk")) return "Playtime";
        if (has(s, "vet", "check-up", "checkup")) return "Vet Visit";
        if (has(s, "groom", "bath", "brush", "nail")) return "Grooming";
        return null;
    }

    public static String detectAmount(String s) {
        Matcher m = AMOUNT.matcher(s);
        if (!m.find()) return null;
        String n = m.group(1);
        switch (n) {
            case "half": n = "1/2"; break;
            case "one": case "a": case "an": n = "1"; break;
            case "two": n = "2"; break;
            case "three": n = "3"; break;
            case "four" : n = "4"; break;
            case "five" : n = "5"; break;
            default: break;
        }
        return n + " " + m.group(2);
    }

    public static Long detectTime(String s) {
        if (has(s, "just now", "now")) return System.currentTimeMillis();
        String t = s.replace("a.m.", "am").replace("p.m.", "pm");
        Matcher m = TIME.matcher(t);
        if (!m.find()) return null;

        int hour = Integer.parseInt(m.group(1)) % 12;
        int min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        if (m.group(3).equals("pm")) hour += 12;

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
