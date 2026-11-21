package com.purrfolio.mco1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/** Run in Android Studio: right-click this file → Run 'LocalDocumentRepositoryTest'. */
public class LocalDocumentRepositoryTest {
    /** Stands in for SharedPreferences: the "saved" text is just a variable. */
    private static final class MemoryStorage implements LocalDocumentRepository.Storage {
        String text;
        @Override public String read() { return text; }
        @Override public void write(String value) { text = value; }
    }

    private MemoryStorage storage;
    private LocalDocumentRepository repository;

    @Before public void setUp() {
        storage = new MemoryStorage();
        repository = new LocalDocumentRepository(storage);
    }

    private static LocalDocument doc(String id, String petId, long addedAt) {
        return new LocalDocument(id, petId, "Title " + id, "Receipt", id + ".pdf",
                "docs/" + petId + "/" + id + ".pdf", addedAt, 1234);
    }

    @Test public void emptyAtFirst() {
        assertTrue(repository.list().isEmpty());
    }

    @Test public void addedDocumentComesBackWithItsFields() {
        repository.add(doc("a", "pet-1", 100));
        List<LocalDocument> all = repository.list();
        assertEquals(1, all.size());
        assertEquals("a", all.get(0).id);
        assertEquals("pet-1", all.get(0).petId);
        assertEquals("docs/pet-1/a.pdf", all.get(0).path);
        assertEquals(1234, all.get(0).sizeBytes);
    }

    @Test public void listsNewestFirst() {
        repository.add(doc("old", "pet-1", 100));
        repository.add(doc("new", "pet-1", 300));
        repository.add(doc("mid", "pet-1", 200));
        List<LocalDocument> all = repository.list();
        assertEquals("new", all.get(0).id);
        assertEquals("mid", all.get(1).id);
        assertEquals("old", all.get(2).id);
    }

    @Test public void forPetOnlyReturnsThatPet() {
        repository.add(doc("a", "pet-1", 100));
        repository.add(doc("b", "pet-2", 200));
        repository.add(doc("c", "pet-1", 300));
        List<LocalDocument> mine = repository.forPet("pet-1");
        assertEquals(2, mine.size());
        assertEquals("c", mine.get(0).id);
        assertEquals("a", mine.get(1).id);
        assertTrue(repository.forPet("nobody").isEmpty());
        assertTrue(repository.forPet(null).isEmpty());
    }

    @Test public void sameIdReplacesInsteadOfDoubling() {
        repository.add(doc("a", "pet-1", 100));
        repository.add(new LocalDocument("a", "pet-1", "Renamed", "Medication", "a.pdf",
                "docs/pet-1/a.pdf", 100, 1234));
        List<LocalDocument> all = repository.list();
        assertEquals(1, all.size());
        assertEquals("Renamed", all.get(0).title);
        assertEquals("Medication", all.get(0).type);
    }

    @Test public void removeReturnsTheRecordSoItsFileCanBeDeleted() {
        repository.add(doc("a", "pet-1", 100));
        repository.add(doc("b", "pet-1", 200));
        LocalDocument removed = repository.remove("a");
        assertNotNull(removed);
        assertEquals("docs/pet-1/a.pdf", removed.path);
        assertEquals(1, repository.list().size());
        assertEquals("b", repository.list().get(0).id);
    }

    @Test public void removingUnknownIdChangesNothing() {
        repository.add(doc("a", "pet-1", 100));
        assertNull(repository.remove("nope"));
        assertNull(repository.remove(null));
        assertEquals(1, repository.list().size());
    }

    @Test public void documentsWithoutAnIdAreNotSaved() {
        repository.add(null);
        repository.add(doc("", "pet-1", 100));
        assertTrue(repository.list().isEmpty());
        assertNull(storage.text);
    }

    @Test public void brokenTextGivesAnEmptyListInsteadOfCrashing() {
        storage.text = "this is not json";
        assertTrue(repository.list().isEmpty());
        storage.text = "{\"not\":\"an array\"}";
        assertTrue(repository.list().isEmpty());
    }

    @Test public void oneBrokenEntryIsSkippedAndTheRestSurvive() {
        storage.text = "[42, null, {\"title\":\"no id\"}, "
                + "{\"id\":\"good\",\"petId\":\"pet-1\",\"addedAt\":5}]";
        List<LocalDocument> all = repository.list();
        assertEquals(1, all.size());
        assertEquals("good", all.get(0).id);
    }

    @Test public void survivesBeingReopened() {
        repository.add(doc("a", "pet-1", 100));
        LocalDocumentRepository reopened = new LocalDocumentRepository(storage);
        assertEquals(1, reopened.list().size());
        assertEquals("a", reopened.list().get(0).id);
    }
}
