package com.purrfolio.mco1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Plain Java rules that merge care logs, reminders and documents into the sections the
 * Pet Timeline shows. No Android types and no Firebase, so it can be checked with plain
 * Java and the same input always gives the same output.
 *
 * Order: "Coming up" first (soonest first), then past days, newest first. Rows with no
 * known time go last under "Earlier".
 */
public final class TimelineBuilder {
    private TimelineBuilder() {}

    public static final String COMING_UP = "Coming up";
    public static final String EARLIER = "Earlier";
    public static final int DEFAULT_LIMIT = 50;
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    /** One titled block of rows, for example "Today" or "Nov 18". */
    public static final class Group {
        public final String label;
        public final List<TimelineEntry> entries;

        Group(String label, List<TimelineEntry> entries) {
            this.label = label;
            this.entries = Collections.unmodifiableList(entries);
        }
    }

    public static final class Result {
        public final List<Group> groups;
        /** Past rows left out because of the limit, for a "Show earlier" button. */
        public final int hiddenCount;

        Result(List<Group> groups, int hiddenCount) {
            this.groups = Collections.unmodifiableList(groups);
            this.hiddenCount = hiddenCount;
        }
    }

    /**
     * @param limit maximum number of past rows to return. Zero or less means no limit.
     *              "Coming up" rows are never cut.
     */
    public static Result build(List<TimelineEntry> input, long nowMillis, TimeZone zone, Locale locale, int limit) {
        List<TimelineEntry> upcoming = new ArrayList<>();
        List<TimelineEntry> history = new ArrayList<>();

        if (input != null) {
            for (TimelineEntry entry : input) {
                if (entry == null) continue;
                if (entry.kind == TimelineEntry.Kind.REMINDER) addReminder(entry, nowMillis, upcoming, history);
                else history.add(entry);
            }
        }

        Collections.sort(upcoming, new Comparator<TimelineEntry>() {
            @Override public int compare(TimelineEntry a, TimelineEntry b) {
                int byTime = Long.compare(a.timeMillis, b.timeMillis);
                return byTime != 0 ? byTime : a.title.compareToIgnoreCase(b.title);
            }
        });
        Collections.sort(history, new Comparator<TimelineEntry>() {
            @Override public int compare(TimelineEntry a, TimelineEntry b) {
                boolean aKnown = a.timeMillis > 0, bKnown = b.timeMillis > 0;
                if (aKnown != bKnown) return aKnown ? -1 : 1;
                int byTime = Long.compare(b.timeMillis, a.timeMillis);
                return byTime != 0 ? byTime : a.title.compareToIgnoreCase(b.title);
            }
        });

        int hidden = 0;
        if (limit > 0 && history.size() > limit) {
            hidden = history.size() - limit;
            history = new ArrayList<>(history.subList(0, limit));
        }

        List<Group> groups = new ArrayList<>();
        if (!upcoming.isEmpty()) groups.add(new Group(COMING_UP, upcoming));

        long today = startOfDay(nowMillis, zone);
        long yesterday = addDays(today, -1, zone);
        String currentLabel = null;
        List<TimelineEntry> current = null;
        for (TimelineEntry entry : history) {
            String label = entry.timeMillis > 0
                    ? dayLabel(startOfDay(entry.timeMillis, zone), today, yesterday, nowMillis, zone, locale)
                    : EARLIER;
            if (!label.equals(currentLabel)) {
                if (current != null) groups.add(new Group(currentLabel, current));
                currentLabel = label;
                current = new ArrayList<>();
            }
            current.add(entry);
        }
        if (current != null) groups.add(new Group(currentLabel, current));

        return new Result(groups, hidden);
    }

    /** Turns one stored reminder into its Done row and/or its Coming up or Missed row. */
    private static void addReminder(TimelineEntry r, long now, List<TimelineEntry> upcoming, List<TimelineEntry> history) {
        long due = r.timeMillis;
        boolean done = r.lastDoneMillis > 0;
        if (done) history.add(r.derived(r.lastDoneMillis, TimelineEntry.Status.DONE));
        if (due <= 0) return;

        if (r.repeatDaily) {
            // A daily reminder is always coming up again: show its next occurrence after now.
            long next = due;
            if (next <= now) next = due + ((now - due) / DAY_MILLIS + 1) * DAY_MILLIS;
            upcoming.add(r.derived(next, TimelineEntry.Status.UPCOMING));
        } else if (!done) {
            if (due > now) upcoming.add(r.derived(due, TimelineEntry.Status.UPCOMING));
            else history.add(r.derived(due, TimelineEntry.Status.MISSED));
        }
    }

    private static String dayLabel(long dayStart, long today, long yesterday, long now, TimeZone zone, Locale locale) {
        if (dayStart == today) return "Today";
        if (dayStart == yesterday) return "Yesterday";
        Calendar nowCal = Calendar.getInstance(zone, locale);
        nowCal.setTimeInMillis(now);
        Calendar dayCal = Calendar.getInstance(zone, locale);
        dayCal.setTimeInMillis(dayStart);
        String pattern = dayCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) ? "MMM d" : "MMM d, yyyy";
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        format.setTimeZone(zone);
        return format.format(new Date(dayStart));
    }

    static long startOfDay(long millis, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static long addDays(long dayStart, int days, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(dayStart);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTimeInMillis();
    }
}
