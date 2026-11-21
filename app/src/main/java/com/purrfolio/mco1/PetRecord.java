package com.purrfolio.mco1;

import java.util.Locale;
import org.json.JSONObject;

/**
 * One searchable item in a pet's records (5.4 Search &amp; Filter Records, 5.5 Share / Export).
 *
 * Records are built from the app's other data (care logs on the phone; documents and reminders from
 * Firestore), so search and export never need to know where an entry came from.
 */
public final class PetRecord {
    /** Record types offered by the search filters, in the order the chips show them. */
    public enum Type {
        MEDICATIONS("Medications"),
        VACCINATIONS("Vaccinations"),
        APPOINTMENTS("Appointments"),
        DOCUMENTS("Documents"),
        CARE_LOGS("Care Logs"),
        OBSERVATIONS("Observations");

        public final String label;
        Type(String label) { this.label = label; }

        /** The type whose chip label is {@code label}, or null for "All" or an unknown label. */
        public static Type fromLabel(String label) {
            for (Type type : values()) if (type.label.equals(label)) return type;
            return null;
        }
    }

    /** Where the record came from, so tapping a search result can open the original entry. */
    public static final String SOURCE_CARE_LOG = "careLog";
    public static final String SOURCE_DOCUMENT = "document";
    public static final String SOURCE_REMINDER = "reminder";

    /** Used when an entry has no time; such records only appear when no date filter is set. */
    public static final long UNKNOWN_TIME = 0L;

    public final String id;
    public final String petId;
    public final Type type;
    public final String title;
    public final String detail;
    public final long timeMillis;
    public final String source;

    public PetRecord(String id, String petId, Type type, String title, String detail, long timeMillis, String source) {
        this.id = id == null ? "" : id;
        this.petId = petId == null ? "" : petId;
        this.type = type;
        this.title = title == null ? "" : title;
        this.detail = detail == null ? "" : detail;
        this.timeMillis = timeMillis;
        this.source = source == null ? "" : source;
    }

    public boolean hasTime() { return timeMillis > UNKNOWN_TIME; }

    /**
     * Turns a saved care log (see MainActivity.newLog) into a record. The care log's activity decides
     * the type: medication and vitamins are Medications, vet visits are Appointments, recorded sounds are
     * Observations, and everything else (feeding, water, grooming, …) is a Care Log.
     */
    public static PetRecord fromCareLog(JSONObject log) {
        String kind = log.optString("kind");
        return new PetRecord(log.optString("id"), log.optString("petId"), typeOfCareLog(kind, log.optString("audioFile")),
                kind, log.optString("text"), log.optLong("occurredAt", UNKNOWN_TIME), SOURCE_CARE_LOG);
    }

    /**
     * Turns a document from Pet Records (Firestore pets/{petId}/documents) into a record. Vaccination
     * cards count as Vaccinations; prescriptions, receipts and everything else are Documents.
     */
    public static PetRecord fromDocument(PetDocument document) {
        String type = document.type == null || document.type.trim().isEmpty() ? "Other" : document.type.trim();
        String title = document.title == null || document.title.trim().isEmpty() ? document.fileName : document.title;
        StringBuilder detail = new StringBuilder(type).append(" document");
        if (document.fileName != null && !document.fileName.isEmpty() && !document.fileName.equals(title))
            detail.append(" · ").append(document.fileName);
        if (document.authorName != null && !document.authorName.isEmpty())
            detail.append(" · Added by ").append(document.authorName);
        Type recordType = "Vaccination".equalsIgnoreCase(type) ? Type.VACCINATIONS : Type.DOCUMENTS;
        return new PetRecord(document.id, document.petId, recordType, title, detail.toString(),
                Math.max(document.addedAt, UNKNOWN_TIME), SOURCE_DOCUMENT);
    }

    /**
     * Turns a reminder (Firestore pets/{petId}/reminders) into a record dated at its scheduled time.
     * Medication, vaccination and vet reminders keep those types; feeding and other reminders are Care Logs.
     */
    public static PetRecord fromReminder(Reminder reminder) {
        StringBuilder detail = new StringBuilder(reminder.category == null ? Reminder.OTHERS : reminder.category)
                .append(" reminder");
        if (reminder.repeatDaily) detail.append(" · Repeats daily");
        if (reminder.detail != null && !reminder.detail.trim().isEmpty()) detail.append(" · ").append(reminder.detail.trim());
        return new PetRecord(reminder.id, reminder.petId, typeOfReminder(reminder.category), reminder.title,
                detail.toString(), Math.max(reminder.timeMillis, UNKNOWN_TIME), SOURCE_REMINDER);
    }

    static Type typeOfReminder(String category) {
        if (Reminder.MEDICATION.equals(category)) return Type.MEDICATIONS;
        if (Reminder.VACCINATION.equals(category)) return Type.VACCINATIONS;
        if (Reminder.VET.equals(category)) return Type.APPOINTMENTS;
        return Type.CARE_LOGS;
    }

    static Type typeOfCareLog(String kind, String audioFile) {
        if (audioFile != null && !audioFile.isEmpty()) return Type.OBSERVATIONS;
        switch (kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT)) {
            case "medication":
            case "vitamins":
                return Type.MEDICATIONS;
            case "vaccination":
            case "vaccine":
                return Type.VACCINATIONS;
            case "vet visit":
            case "vet appointment":
            case "appointment":
                return Type.APPOINTMENTS;
            case "observation":
                return Type.OBSERVATIONS;
            default:
                return Type.CARE_LOGS;
        }
    }
}
