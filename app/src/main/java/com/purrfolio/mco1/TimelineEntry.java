package com.purrfolio.mco1;

/**
 * One row of the Pet Timeline. Plain Java, no Android types, like {@link Pet}.
 *
 * The three factory methods build the raw rows that come from the app's data
 * (a care log, a reminder, a document). {@link TimelineBuilder} then turns those
 * into the rows the screen shows: it may add a "Done" row for a reminder, and it sets
 * {@link #status} and the displayed {@link #timeMillis}.
 */
public final class TimelineEntry {
    public enum Kind { LOG, REMINDER, DOCUMENT }

    /** NONE for logs and documents. The other values only apply to reminder rows. */
    public enum Status { NONE, UPCOMING, DONE, MISSED }

    public final Kind kind;
    public final String id;
    /** Care category, reminder category or document type. */
    public final String category;
    public final String title;
    public final String detail;
    /** Time the row is shown at. For raw reminders this is the due time. 0 means unknown. */
    public final long timeMillis;
    public final Status status;

    /** Raw reminder inputs. Only {@link TimelineBuilder} reads these. */
    final long lastDoneMillis;
    final boolean repeatDaily;

    private TimelineEntry(Kind kind, String id, String category, String title, String detail,
                          long timeMillis, Status status, long lastDoneMillis, boolean repeatDaily) {
        this.kind = kind;
        this.id = clean(id);
        this.category = clean(category);
        this.title = clean(title);
        this.detail = clean(detail);
        this.timeMillis = timeMillis;
        this.status = status;
        this.lastDoneMillis = lastDoneMillis;
        this.repeatDaily = repeatDaily;
    }

    /** A care log. The title is the care category, the detail is the saved text. */
    public static TimelineEntry log(String id, String category, String text, long occurredAtMillis) {
        return new TimelineEntry(Kind.LOG, id, category, category, text, occurredAtMillis, Status.NONE, 0, false);
    }

    /** A reminder, as stored. A done or upcoming row is derived from it by the builder. */
    public static TimelineEntry reminder(String id, String category, String title, String detail,
                                         long dueMillis, long lastDoneMillis, boolean repeatDaily) {
        return new TimelineEntry(Kind.REMINDER, id, category, title, detail, dueMillis, Status.NONE,
                lastDoneMillis, repeatDaily);
    }

    /** A saved document. The category is its type (Vaccination, Receipt, ...). */
    public static TimelineEntry document(String id, String type, String title, long addedAtMillis) {
        return new TimelineEntry(Kind.DOCUMENT, id, type, title, "", addedAtMillis, Status.NONE, 0, false);
    }

    /** Same row at another time and with another status. Used by the builder. */
    TimelineEntry derived(long newTimeMillis, Status newStatus) {
        return new TimelineEntry(kind, id, category, title, detail, newTimeMillis, newStatus, lastDoneMillis, repeatDaily);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
