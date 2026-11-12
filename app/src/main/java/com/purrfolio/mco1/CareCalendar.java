package com.purrfolio.mco1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CareCalendar {
    private CareCalendar() { }

    public static final class Item {
        public final Reminder reminder;
        public final long timeMillis;
        public final boolean done;

        Item(Reminder reminder, long timeMillis, boolean done) {
            this.reminder = reminder;
            this.timeMillis = timeMillis;
            this.done = done;
        }

        public String detailLine() {
            return (reminder.detail.isEmpty() ? "" : reminder.detail + " · ") + reminder.petName;
        }
    }

    public static List<Item> itemsOn(List<Reminder> all, long dayStart, String tab) {
        List<Item> out = new ArrayList<>();
        for (Reminder r : all) {
            if (!r.matches(tab)) continue;
            long time = r.occurrenceOn(dayStart);
            if (time >= 0) out.add(new Item(r, time, r.doneOn(dayStart)));
        }
        Collections.sort(out, (a, b) -> Long.compare(a.timeMillis, b.timeMillis));
        return out;
    }

    public static List<String> categoriesOn(List<Reminder> all, long dayStart) {
        List<String> out = new ArrayList<>();
        for (Reminder r : all) {
            if (r.occurrenceOn(dayStart) >= 0 && !out.contains(r.category)) out.add(r.category);
        }
        return out;
    }

    public static String glyph(Reminder r) {
        switch (r.category) {
            case Reminder.MEDICATION:
            case Reminder.VACCINATION: return "✚";
            case Reminder.FEEDING: return "◉";
            case Reminder.VET: return "✱";
            default: return "✦";
        }
    }

    public static int colorRes(String category) {
        switch (category) {
            case Reminder.MEDICATION:
            case Reminder.VACCINATION: return R.color.pink;
            case Reminder.FEEDING: return R.color.green;
            case Reminder.VET: return R.color.primary;
            default: return R.color.amber;
        }
    }
}
