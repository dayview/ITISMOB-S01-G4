package com.purrfolio.mco1;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

/**
 * Search &amp; Filter Records (5.4): narrows a pet's records by type, date range and keyword.
 * Plain Java with no Android views, so the rules can be unit tested.
 */
public final class RecordSearch {
    private RecordSearch() {}

    /** Date filter: records from {@code from} (inclusive) up to {@code to} (exclusive), in milliseconds. */
    public static final class DateRange {
        public static final DateRange ALL = new DateRange(Long.MIN_VALUE, Long.MAX_VALUE);

        public final long from;
        public final long to;

        public DateRange(long from, long to) { this.from = from; this.to = to; }

        public boolean isAll() { return from == Long.MIN_VALUE && to == Long.MAX_VALUE; }

        public boolean contains(PetRecord record) {
            if (isAll()) return true;
            // An entry without a time can't be placed in a range, so only "All" shows it.
            return record.hasTime() && record.timeMillis >= from && record.timeMillis < to;
        }

        /** From the 1st of this month up to the end of today. */
        public static DateRange thisMonth(long now) {
            Calendar start = startOfDay(now);
            start.set(Calendar.DAY_OF_MONTH, 1);
            return new DateRange(start.getTimeInMillis(), endOfDay(now));
        }

        /** The last {@code months} months, counted back from today, up to the end of today. */
        public static DateRange lastMonths(int months, long now) {
            Calendar start = startOfDay(now);
            start.add(Calendar.MONTH, -months);
            return new DateRange(start.getTimeInMillis(), endOfDay(now));
        }

        /** Every record dated on or between the two picked days (in either order). */
        public static DateRange custom(long firstDay, long lastDay) {
            long start = Math.min(firstDay, lastDay), end = Math.max(firstDay, lastDay);
            return new DateRange(startOfDay(start).getTimeInMillis(), endOfDay(end));
        }

        /** The range behind a date chip: "All", "This Month", "Last 3 Months" or "Last Year". */
        public static DateRange forLabel(String label, long now) {
            switch (label) {
                case "This Month": return thisMonth(now);
                case "Last 3 Months": return lastMonths(3, now);
                case "Last Year": return lastMonths(12, now);
                default: return ALL;
            }
        }

        private static Calendar startOfDay(long time) {
            Calendar day = Calendar.getInstance();
            day.setTimeInMillis(time);
            day.set(Calendar.HOUR_OF_DAY, 0);
            day.set(Calendar.MINUTE, 0);
            day.set(Calendar.SECOND, 0);
            day.set(Calendar.MILLISECOND, 0);
            return day;
        }

        private static long endOfDay(long time) {
            Calendar next = startOfDay(time);
            next.add(Calendar.DAY_OF_MONTH, 1);
            return next.getTimeInMillis();
        }
    }

    /** Turns the saved care logs into records. */
    public static List<PetRecord> fromCareLogs(List<JSONObject> logs) {
        List<PetRecord> records = new ArrayList<>();
        for (JSONObject log : logs) records.add(PetRecord.fromCareLog(log));
        return records;
    }

    /**
     * The records that match every filter, newest first (entries without a time go last).
     *
     * @param petId   only this pet's records; null or empty for every pet
     * @param type    only this type; null for all types
     * @param range   only records in this date range; use {@link DateRange#ALL} for any date
     * @param keyword words that must all appear in the title, details or type (any order, any case); blank for none
     */
    public static List<PetRecord> filter(List<PetRecord> records, String petId, PetRecord.Type type,
                                         DateRange range, String keyword) {
        String[] words = keyword == null || keyword.trim().isEmpty() ? new String[0]
                : keyword.trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<PetRecord> matches = new ArrayList<>();
        for (PetRecord record : records) {
            if (petId != null && !petId.isEmpty() && !petId.equals(record.petId)) continue;
            if (type != null && record.type != type) continue;
            if (range != null && !range.contains(record)) continue;
            if (!containsAll(record, words)) continue;
            matches.add(record);
        }
        Collections.sort(matches, NEWEST_FIRST);
        return matches;
    }

    private static boolean containsAll(PetRecord record, String[] words) {
        if (words.length == 0) return true;
        String text = (record.title + " " + record.detail + " " + record.type.label).toLowerCase(Locale.ROOT);
        for (String word : words) if (!text.contains(word)) return false;
        return true;
    }

    private static final Comparator<PetRecord> NEWEST_FIRST = (left, right) -> {
        if (left.hasTime() != right.hasTime()) return left.hasTime() ? -1 : 1;
        return Long.compare(right.timeMillis, left.timeMillis);
    };
}
