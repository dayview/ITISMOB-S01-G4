package com.purrfolio.mco1;

import com.google.firebase.firestore.DocumentId;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reminder {
    public static final String MEDICATION = "Medication";
    public static final String FEEDING = "Feeding";
    public static final String VACCINATION = "Vaccination";
    public static final String VET = "Vet Appointment";
    public static final String OTHERS = "Others";
    public static final String[] CATEGORIES = {MEDICATION, FEEDING, VACCINATION, VET, OTHERS};

    @DocumentId public String id = "";
    public String petId = "", petName = "", title = "", detail = "", category = OTHERS;
    public long timeMillis, lastDoneAt;
    public boolean repeatDaily;

    /* time this reminder happens on the given day, or -1 if it does not */
    public long occurrenceOn(long dayStart) {
        long baseDay = CareTime.startOfDay(timeMillis);
        if (dayStart == baseDay) return timeMillis;
        if (repeatDaily && dayStart > baseDay) {
            Calendar first = Calendar.getInstance();
            first.setTimeInMillis(timeMillis);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(dayStart);
            c.set(Calendar.HOUR_OF_DAY, first.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, first.get(Calendar.MINUTE));
            return c.getTimeInMillis();
        }
        return -1;
    }

    /* true if someone pressed given/done on that day */
    public boolean doneOn(long dayStart) {
        return lastDoneAt >= dayStart && lastDoneAt < CareTime.addDays(dayStart, 1);
    }

    /* the next time the alarm should ring after "now", or -1 if there is none */
    public long nextAfter(long now) {
        if (!repeatDaily) return timeMillis > now ? timeMillis : -1;
        long today = CareTime.startOfDay(now);
        for (int i = 0; i < 2; i++) {
            long o = occurrenceOn(CareTime.addDays(today, i));
            if (o > now) return o;
        }
        return -1;
    }

    /* button label on the notification: given for care you give, done for tasks */
    public String actionWord() {
        return (MEDICATION.equals(category) || FEEDING.equals(category) || VACCINATION.equals(category)) ? "Given" : "Done";
    }

    /* filter chips on the reminders screen: all, medication, feeding, others */
    public boolean matches(String tab) {
        boolean med = MEDICATION.equals(category) || VACCINATION.equals(category);
        switch (tab) {
            case "Medication": return med;
            case "Feeding": return FEEDING.equals(category);
            case "Others": return !med && !FEEDING.equals(category);
            default: return true;
        }
    }

    public String key() { return petId + "/" + id; }

    /* the fields written to firestore */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("detail", detail);
        m.put("category", category);
        m.put("timeMillis", timeMillis);
        m.put("repeatDaily", repeatDaily);
        m.put("lastDoneAt", lastDoneAt);
        m.put("petId", petId);
        m.put("petName", petName);
        return m;
    }

    public static Reminder find(List<Reminder> list, String key) {
        for (Reminder r : list) if (r.key().equals(key)) return r;
        return null;
    }
}
