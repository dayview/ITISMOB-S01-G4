package com.purrfolio.mco1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;

/**
 * Checks the Pet Timeline rules. A fixed "now" (Nov 20, 2026, 12:00 in Manila) and a fixed
 * time zone and locale make the results the same on every machine and at every hour.
 * Run in Android Studio: right-click this file → Run 'TimelineBuilderTest'.
 */
public class TimelineBuilderTest {
    private static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Manila");
    private static final Locale LOCALE = Locale.US;
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;
    private static final long NOW = noon();

    private static long noon() {
        Calendar c = Calendar.getInstance(ZONE, LOCALE);
        c.clear();
        c.set(2026, Calendar.NOVEMBER, 20, 12, 0, 0);
        return c.getTimeInMillis();
    }

    private static TimelineBuilder.Result build(List<TimelineEntry> entries, int limit) {
        return TimelineBuilder.build(entries, NOW, ZONE, LOCALE, limit);
    }

    private static TimelineBuilder.Result build(TimelineEntry... entries) {
        return build(Arrays.asList(entries), 50);
    }

    private static List<String> labels(TimelineBuilder.Result result) {
        List<String> labels = new ArrayList<>();
        for (TimelineBuilder.Group group : result.groups) labels.add(group.label);
        return labels;
    }

    /** "Title" for plain rows, "Title/STATUS" for reminder rows, joined with ";". */
    private static String titles(TimelineBuilder.Group group) {
        StringBuilder out = new StringBuilder();
        for (TimelineEntry e : group.entries) {
            out.append(e.title);
            if (e.status != TimelineEntry.Status.NONE) out.append('/').append(e.status);
            out.append(';');
        }
        return out.toString();
    }

    private static int rowCount(TimelineBuilder.Result result) {
        int total = 0;
        for (TimelineBuilder.Group group : result.groups) total += group.entries.size();
        return total;
    }

    @Test public void emptyAndNullInputGiveNoGroups() {
        assertTrue(build(new ArrayList<TimelineEntry>(), 50).groups.isEmpty());
        assertTrue(build(null, 50).groups.isEmpty());
    }

    @Test public void mixedRowsAreGroupedAndOrdered() {
        TimelineBuilder.Result r = build(
                TimelineEntry.log("l1", "Water", "Fresh bowl", NOW - 3 * HOUR),
                TimelineEntry.reminder("r1", "Medication", "Meds", "", NOW + 6 * HOUR, 0, false),
                TimelineEntry.reminder("r2", "Feeding", "Feed", "", NOW - 5 * HOUR, NOW - 4 * HOUR, false),
                TimelineEntry.document("d1", "Vaccination", "Vaccine card", NOW - 3 * DAY));
        assertEquals(Arrays.asList("Coming up", "Today", "Nov 17"), labels(r));
        assertEquals("Meds/UPCOMING;", titles(r.groups.get(0)));
        assertEquals("Water;Feed/DONE;", titles(r.groups.get(1)));
        assertEquals("Vaccine card;", titles(r.groups.get(2)));
    }

    @Test public void reminderDoneEarlyShowsOnlyTheDoneRow() {
        TimelineBuilder.Result r = build(
                TimelineEntry.reminder("x", "Others", "Early", "", NOW + DAY, NOW - HOUR, false));
        assertEquals(Arrays.asList("Today"), labels(r));
        assertEquals("Early/DONE;", titles(r.groups.get(0)));
    }

    @Test public void pastDueReminderThatWasNeverDoneIsMissed() {
        TimelineBuilder.Result r = build(
                TimelineEntry.reminder("m", "Vet Appointment", "Vet", "", NOW - 2 * DAY, 0, false));
        assertEquals("Nov 18", labels(r).get(0));
        assertEquals("Vet/MISSED;", titles(r.groups.get(0)));
    }

    @Test public void dailyReminderAlwaysHasANextOccurrenceAndAlsoShowsDone() {
        long due = NOW - 5 * DAY - 2 * HOUR;
        TimelineBuilder.Result r = build(
                TimelineEntry.reminder("d", "Medication", "Daily", "", due, NOW - 2 * HOUR, true));
        assertEquals("Coming up", labels(r).get(0));
        long next = r.groups.get(0).entries.get(0).timeMillis;
        assertTrue(next > NOW && next <= NOW + DAY);
        assertEquals("Daily/DONE;", titles(r.groups.get(1)));
    }

    @Test public void dailyReminderDueExactlyNowRollsToTomorrow() {
        TimelineBuilder.Result r = build(TimelineEntry.reminder("e", "Feeding", "Exact", "", NOW, 0, true));
        assertEquals(NOW + DAY, r.groups.get(0).entries.get(0).timeMillis);
    }

    @Test public void yesterdayAndOtherYearLabels() {
        TimelineBuilder.Result r = build(
                TimelineEntry.log("y", "Walk", "", NOW - DAY),
                TimelineEntry.log("o", "Walk", "", NOW - 400 * DAY));
        assertEquals("Yesterday", labels(r).get(0));
        assertTrue(labels(r).get(1).matches("[A-Z][a-z]{2} \\d{1,2}, 2025"));
    }

    @Test public void unknownTimeGoesLastUnderEarlier() {
        TimelineBuilder.Result r = build(
                TimelineEntry.log("u", "Walk", "legacy", 0),
                TimelineEntry.log("k", "Walk", "new", NOW - HOUR));
        assertEquals(Arrays.asList("Today", "Earlier"), labels(r));
    }

    @Test public void limitTrimsPastRowsAndReportsTheRest() {
        List<TimelineEntry> many = new ArrayList<>();
        for (int i = 0; i < 60; i++) many.add(TimelineEntry.log("n" + i, "Walk", "", NOW - i * HOUR));
        TimelineBuilder.Result limited = build(many, 50);
        assertEquals(50, rowCount(limited));
        assertEquals(10, limited.hiddenCount);
        TimelineBuilder.Result unlimited = build(many, 0);
        assertEquals(60, rowCount(unlimited));
        assertEquals(0, unlimited.hiddenCount);
    }

    @Test public void limitNeverCutsComingUp() {
        List<TimelineEntry> mix = new ArrayList<>();
        for (int i = 0; i < 60; i++) mix.add(TimelineEntry.log("n" + i, "Walk", "", NOW - i * HOUR));
        for (int i = 0; i < 5; i++) {
            mix.add(TimelineEntry.reminder("f" + i, "Others", "F" + i, "", NOW + (i + 1) * HOUR, 0, false));
        }
        TimelineBuilder.Result r = build(mix, 10);
        assertEquals("Coming up", r.groups.get(0).label);
        assertEquals(5, r.groups.get(0).entries.size());
    }

    @Test public void midnightSplitsTodayFromYesterday() {
        long midnight = TimelineBuilder.startOfDay(NOW, ZONE);
        TimelineBuilder.Result r = build(
                TimelineEntry.log("a", "W", "", midnight),
                TimelineEntry.log("b", "W", "", midnight - 1));
        assertEquals(Arrays.asList("Today", "Yesterday"), labels(r));
    }

    @Test public void nullEntriesAndNullTextAreSafe() {
        List<TimelineEntry> odd = new ArrayList<>();
        odd.add(null);
        odd.add(TimelineEntry.log(null, null, null, NOW));
        assertEquals(1, build(odd, 50).groups.size());
    }

    @Test public void sameTimeRowsAreOrderedByTitle() {
        TimelineBuilder.Result r = build(
                TimelineEntry.log("1", "Walk", "", NOW - HOUR),
                TimelineEntry.log("2", "Bath", "", NOW - HOUR));
        assertEquals("Bath;Walk;", titles(r.groups.get(0)));
    }
}
