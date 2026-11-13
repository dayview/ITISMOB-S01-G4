package com.purrfolio.mco1;

import java.util.Locale;
import org.json.JSONObject;

/**
 * One searchable item in a pet's records (5.4 Search &amp; Filter Records, 5.5 Share / Export).
 *
 * Records are built from the app's other data (care logs today; documents and reminders once they are
 * stored in Firestore), so search and export never need to know where an entry came from.
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
