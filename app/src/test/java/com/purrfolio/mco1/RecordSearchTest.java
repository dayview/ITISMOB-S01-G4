package com.purrfolio.mco1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;

/** Run in Android Studio: right-click this file → Run 'RecordSearchTest'. */
public class RecordSearchTest {
    /** Fixed "today" so the date tests don't depend on when they run: Nov 15, 2026, 10:00. */
    private static final long NOW = at(2026, Calendar.NOVEMBER, 15, 10);

    private static long at(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, day, hour, 0, 0);
        return c.getTimeInMillis();
    }

    private static PetRecord record(String id, String pet, PetRecord.Type type, String title, String detail, long time) {
        return new PetRecord(id, pet, type, title, detail, time, PetRecord.SOURCE_CARE_LOG);
    }

    private final List<PetRecord> records = Arrays.asList(
            record("feed", "mochi", PetRecord.Type.CARE_LOGS, "Feeding", "1/2 cup kibble", at(2026, Calendar.NOVEMBER, 14, 8)),
            record("meds", "mochi", PetRecord.Type.MEDICATIONS, "Medication", "Deworming tablet", at(2026, Calendar.NOVEMBER, 2, 9)),
            record("vet", "mochi", PetRecord.Type.APPOINTMENTS, "Vet Visit", "Annual check-up", at(2026, Calendar.SEPTEMBER, 1, 15)),
            record("old", "mochi", PetRecord.Type.MEDICATIONS, "Vitamins", "Fish oil", at(2025, Calendar.JANUARY, 5, 9)),
            record("cough", "mochi", PetRecord.Type.OBSERVATIONS, "Observation", "Coughing after dinner", at(2026, Calendar.NOVEMBER, 15, 7)),
            record("bailey", "bailey", PetRecord.Type.CARE_LOGS, "Feeding", "1 cup kibble", at(2026, Calendar.NOVEMBER, 15, 8)),
            record("undated", "mochi", PetRecord.Type.CARE_LOGS, "Grooming", "Brushed", PetRecord.UNKNOWN_TIME));

    private static List<String> ids(List<PetRecord> list) {
        List<String> ids = new ArrayList<>();
        for (PetRecord r : list) ids.add(r.id);
        return ids;
    }

    @Test public void noFiltersShowsOnlyThatPetNewestFirstWithUndatedLast() {
        List<PetRecord> result = RecordSearch.filter(records, "mochi", null, RecordSearch.DateRange.ALL, "");
        assertEquals(Arrays.asList("cough", "feed", "meds", "vet", "old", "undated"), ids(result));
    }

    @Test public void emptyPetIdSearchesEveryPet() {
        assertEquals(7, RecordSearch.filter(records, "", null, RecordSearch.DateRange.ALL, null).size());
    }

    @Test public void typeFilter() {
        List<PetRecord> result = RecordSearch.filter(records, "mochi", PetRecord.Type.MEDICATIONS, RecordSearch.DateRange.ALL, "");
        assertEquals(Arrays.asList("meds", "old"), ids(result));
    }

    @Test public void keywordIgnoresCaseAndNeedsEveryWord() {
        assertEquals(Arrays.asList("feed"), ids(RecordSearch.filter(records, "mochi", null, RecordSearch.DateRange.ALL, "KIBBLE")));
        assertEquals(Arrays.asList("cough"), ids(RecordSearch.filter(records, "mochi", null, RecordSearch.DateRange.ALL, "dinner coughing")));
        assertTrue(RecordSearch.filter(records, "mochi", null, RecordSearch.DateRange.ALL, "kibble dinner").isEmpty());
    }

    @Test public void keywordAlsoMatchesTheTypeName() {
        assertEquals(Arrays.asList("vet"), ids(RecordSearch.filter(records, "mochi", null, RecordSearch.DateRange.ALL, "appointments")));
    }

    @Test public void thisMonthStartsOnTheFirstAndSkipsUndated() {
        List<PetRecord> result = RecordSearch.filter(records, "mochi", null, RecordSearch.DateRange.forLabel("This Month", NOW), "");
        assertEquals(Arrays.asList("cough", "feed", "meds"), ids(result));
    }

    @Test public void lastThreeMonthsAndLastYear() {
        assertEquals(Arrays.asList("cough", "feed", "meds", "vet"),
                ids(RecordSearch.filter(records, "mochi", null, RecordSearch.DateRange.forLabel("Last 3 Months", NOW), "")));
        assertEquals(Arrays.asList("cough", "feed", "meds", "vet"),
                ids(RecordSearch.filter(records, "mochi", null, RecordSearch.DateRange.forLabel("Last Year", NOW), "")));
    }

    @Test public void customRangeIncludesBothWholeDaysInEitherOrder() {
        RecordSearch.DateRange range = RecordSearch.DateRange.custom(at(2026, Calendar.NOVEMBER, 14, 23), at(2026, Calendar.NOVEMBER, 2, 0));
        assertEquals(Arrays.asList("feed", "meds"), ids(RecordSearch.filter(records, "mochi", null, range, "")));
    }

    @Test public void filtersCombine() {
        List<PetRecord> result = RecordSearch.filter(records, "mochi", PetRecord.Type.MEDICATIONS,
                RecordSearch.DateRange.forLabel("This Month", NOW), "tablet");
        assertEquals(Arrays.asList("meds"), ids(result));
    }

    @Test public void careLogsGetTheRightType() throws Exception {
        assertEquals(PetRecord.Type.MEDICATIONS, PetRecord.fromCareLog(log("Vitamins", null)).type);
        assertEquals(PetRecord.Type.MEDICATIONS, PetRecord.fromCareLog(log("Medication", null)).type);
        assertEquals(PetRecord.Type.APPOINTMENTS, PetRecord.fromCareLog(log("Vet Visit", null)).type);
        assertEquals(PetRecord.Type.OBSERVATIONS, PetRecord.fromCareLog(log("Observation", "sound_1.m4a")).type);
        assertEquals(PetRecord.Type.CARE_LOGS, PetRecord.fromCareLog(log("Feeding", null)).type);
        assertEquals(PetRecord.Type.CARE_LOGS, PetRecord.fromCareLog(log("Other", null)).type);
    }

    @Test public void careLogFieldsAreCopied() throws Exception {
        PetRecord r = PetRecord.fromCareLog(log("Feeding", null));
        assertEquals("log1", r.id);
        assertEquals("mochi", r.petId);
        assertEquals("Feeding", r.title);
        assertEquals("1/2 cup", r.detail);
        assertEquals(NOW, r.timeMillis);
        assertEquals(PetRecord.SOURCE_CARE_LOG, r.source);
    }

    @Test public void chipLabelsMapToTypes() {
        assertEquals(PetRecord.Type.CARE_LOGS, PetRecord.Type.fromLabel("Care Logs"));
        assertEquals(null, PetRecord.Type.fromLabel("All"));
    }

    @Test public void documentsBecomeDocumentsExceptVaccinationCards() {
        PetDocument receipt = document("d1", "Receipt", "Vet receipt", "receipt.pdf");
        PetRecord r = PetRecord.fromDocument(receipt);
        assertEquals(PetRecord.Type.DOCUMENTS, r.type);
        assertEquals("d1", r.id);
        assertEquals("mochi", r.petId);
        assertEquals("Vet receipt", r.title);
        assertEquals("Receipt document · receipt.pdf · Added by Theo", r.detail);
        assertEquals(NOW, r.timeMillis);
        assertEquals(PetRecord.SOURCE_DOCUMENT, r.source);
        assertEquals(PetRecord.Type.VACCINATIONS, PetRecord.fromDocument(document("d2", "Vaccination", "Rabies card", "")).type);
    }

    @Test public void untitledDocumentUsesItsFileName() {
        assertEquals("scan.jpg", PetRecord.fromDocument(document("d3", "Other", "", "scan.jpg")).title);
    }

    @Test public void remindersGetTheRightType() {
        assertEquals(PetRecord.Type.MEDICATIONS, PetRecord.fromReminder(reminder(Reminder.MEDICATION, false)).type);
        assertEquals(PetRecord.Type.VACCINATIONS, PetRecord.fromReminder(reminder(Reminder.VACCINATION, false)).type);
        assertEquals(PetRecord.Type.APPOINTMENTS, PetRecord.fromReminder(reminder(Reminder.VET, false)).type);
        assertEquals(PetRecord.Type.CARE_LOGS, PetRecord.fromReminder(reminder(Reminder.FEEDING, false)).type);
        assertEquals(PetRecord.Type.CARE_LOGS, PetRecord.fromReminder(reminder(Reminder.OTHERS, false)).type);
    }

    @Test public void reminderFieldsAreCopied() {
        PetRecord r = PetRecord.fromReminder(reminder(Reminder.MEDICATION, true));
        assertEquals("r1", r.id);
        assertEquals("mochi", r.petId);
        assertEquals("Heartworm pill", r.title);
        assertEquals("Medication reminder · Repeats daily · With food", r.detail);
        assertEquals(NOW - 1, r.timeMillis);
        assertEquals(PetRecord.SOURCE_REMINDER, r.source);
    }

    @Test public void documentsAndRemindersAreSearchedWithCareLogs() {
        List<PetRecord> all = new ArrayList<>(records);
        all.addAll(RecordSearch.fromDocuments(Arrays.asList(document("d2", "Vaccination", "Rabies card", ""))));
        all.addAll(RecordSearch.fromReminders(Arrays.asList(reminder(Reminder.VACCINATION, false))));
        assertEquals(Arrays.asList("d2", "r1"),
                ids(RecordSearch.filter(all, "mochi", PetRecord.Type.VACCINATIONS, RecordSearch.DateRange.ALL, "")));
        assertEquals(Arrays.asList("d2"), ids(RecordSearch.filter(all, "mochi", null, RecordSearch.DateRange.ALL, "rabies")));
    }

    private static PetDocument document(String id, String type, String title, String fileName) {
        PetDocument d = new PetDocument();
        d.id = id;
        d.petId = "mochi";
        d.type = type;
        d.title = title;
        d.fileName = fileName;
        d.addedAt = NOW;
        d.authorName = "Theo";
        return d;
    }

    private static Reminder reminder(String category, boolean daily) {
        Reminder r = new Reminder();
        r.id = "r1";
        r.petId = "mochi";
        r.title = "Heartworm pill";
        r.detail = "With food";
        r.category = category;
        r.timeMillis = NOW - 1; // just before the document, so the order is predictable
        r.repeatDaily = daily;
        return r;
    }

    private static JSONObject log(String kind, String audioFile) throws Exception {
        JSONObject log = new JSONObject().put("id", "log1").put("petId", "mochi").put("kind", kind)
                .put("text", "1/2 cup").put("occurredAt", NOW);
        if (audioFile != null) log.put("audioFile", audioFile);
        return log;
    }
}
