package com.itismob.mco.purrfolio;

import com.google.firebase.firestore.DocumentId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Reminder {
    @DocumentId public String id;
    public String petId = "", petName = "", title = "", detail = "", category = "Others";
    public long timeMillis, lastDoneAt;
    public boolean repeatDaily;
    // Time this reminder happens on the given day (S-O-D millis), or -1 if it does not
    public long occurrenceOn(long dayStart) {
        long baseDay = Ui.startOfDay(timeMillis);
        if (dayStart == baseDay) return timeMillis;
        if (repeatDaily && dayStart > baseDay) {
            Calendar t = Calendar.getInstance();
            t.setTimeInMillis(timeMillis);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(dayStart);
            c.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
            return c.getTimeInMillis();
        }
        return -1;
    }
    public boolean doneOn(long dayStart) {
        return lastDoneAt >= dayStart && lastDoneAt < Ui.addDays(dayStart, 1);
    }
    // Next time the alarm should ring after "now", or -1
    public long nextAfter(long now) {
        if (!repeatDaily) return timeMillis > now ? timeMillis : -1;
        long day = Ui.startOfDay(now);
        for (int i = 0; i < 2; i++) {
            long o = occurrenceOn(Ui.addDays(day, i));
            if (o > now) return o;
        }
        return -1;
    }
    public String actionWord() {
        return ("Medication".equals(category) || "Feeding".equals(category) || "Vaccination".equals(category)) ? "Given" : "Done";
    }
    public boolean matches(String tab) {
        boolean med = "Medication".equals(category) || "Vaccination".equals(category);
        switch (tab) {
            case "Medication": return med;
            case "Feeding": return "Feeding".equals(category);
            case "Others": return !med && !"Feeding".equals(category);
            default: return true;
        }
    }
    public String key() { return petId + "/" + id; }
    public static Reminder find(List<Reminder> list, String key) {
        for (Reminder r : list) if (r.key().equals(key)) return r;
        return null;
    }
    // {done, total} for the given day
    public static int[] progress(List<Reminder> all, long day) {
        int done = 0, total = 0;
        for (Reminder r : all) {
            if (r.occurrenceOn(day) < 0) continue;
            total++;
            if (r.doneOn(day)) done++;
        }
        return new int[]{done, total};
    }
    public static List<Row> rowsFor(List<Reminder> all, long day, boolean showPet, String tab) {
        List<Reminder> hit = new ArrayList<>();
        for (Reminder r : all) if (r.matches(tab) && r.occurrenceOn(day) >= 0) hit.add(r);
        Collections.sort(hit, (a, b) -> Long.compare(a.occurrenceOn(day), b.occurrenceOn(day)));
        List<Row> rows = new ArrayList<>();
        for (Reminder r : hit) {
            String sub = Ui.time(r.occurrenceOn(day)) + (r.detail.isEmpty() ? "" : " · " + r.detail) + (showPet ? " · " + r.petName : "");
            rows.add(new Row(r.key(), r.title, sub, r.doneOn(day) ? "✓" : ""));
        }
        return rows;
    }
}

