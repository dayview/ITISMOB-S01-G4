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

    private static JSONObject log(String kind, String audioFile) throws Exception {
        JSONObject log = new JSONObject().put("id", "log1").put("petId", "mochi").put("kind", kind)
                .put("text", "1/2 cup").put("occurredAt", NOW);
        if (audioFile != null) log.put("audioFile", audioFile);
        return log;
    }
}
