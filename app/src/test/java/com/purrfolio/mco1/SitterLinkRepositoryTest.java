package com.purrfolio.mco1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.json.JSONObject;
import org.junit.Test;

public class SitterLinkRepositoryTest {
    @Test public void snapshotContainsOnlyApprovedCareFields() throws Exception {
        JSONObject pet = new JSONObject().put("name", " Mochi ").put("notes", "Feed twice daily")
                .put("photoUri", "file:///private/photo.jpg").put("ownerEmail", "private@example.com")
                .put("medicalInfo", "A".repeat(600));
        Map<String, Object> shared = SitterLinkRepository.sharedPet(pet);
        assertEquals(9, shared.size());
        assertEquals("Mochi", shared.get("name"));
        assertEquals("Feed twice daily", shared.get("notes"));
        assertEquals(500, ((String) shared.get("medicalInfo")).length());
        assertFalse(shared.containsKey("photoUri"));
        assertFalse(shared.containsKey("ownerEmail"));
        assertTrue(shared.containsKey("emergencyContactPhone"));
    }
}
