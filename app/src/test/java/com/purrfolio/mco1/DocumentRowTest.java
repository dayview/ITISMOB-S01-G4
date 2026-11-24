package com.purrfolio.mco1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Run in Android Studio: right-click this file → Run 'DocumentRowTest'. */
public class DocumentRowTest {
    private static PetDocument online(String id, long addedAt) {
        PetDocument d = new PetDocument();
        d.id = id;
        d.title = "Online " + id;
        d.type = "Receipt";
        d.addedAt = addedAt;
        return d;
    }

    private static LocalDocument local(String id, long addedAt) {
        return new LocalDocument(id, "pet-1", "Phone " + id, "Vaccination", id + ".pdf",
                "docs/pet-1/" + id + ".pdf", addedAt, 10);
    }

    /** The row's own id, whichever kind it is. */
    private static List<String> ids(List<DocumentRow> rows) {
        List<String> out = new ArrayList<>();
        for (DocumentRow row : rows) out.add(row.isLocal() ? row.local.id : row.online.id);
        return out;
    }

    @Test public void mixesBothKindsNewestFirst() {
        List<DocumentRow> rows = DocumentRow.merge(
                Arrays.asList(online("o300", 300), online("o100", 100)),
                Arrays.asList(local("l200", 200)));
        assertEquals(Arrays.asList("o300", "l200", "o100"), ids(rows));
    }

    @Test public void phoneDocumentComesFirstWhenTheTimesAreEqual() {
        List<DocumentRow> rows = DocumentRow.merge(
                Arrays.asList(online("o", 100)),
                Arrays.asList(local("l", 100)));
        assertEquals(Arrays.asList("l", "o"), ids(rows));
    }

    @Test public void equalTimesKeepTheOrderOfTheirOwnList() {
        List<DocumentRow> rows = DocumentRow.merge(
                Arrays.asList(online("o1", 100), online("o2", 100)),
                Arrays.asList(local("l1", 100), local("l2", 100)));
        assertEquals(Arrays.asList("l1", "l2", "o1", "o2"), ids(rows));
    }

    @Test public void listsThatAreNotSortedStillComeOutNewestFirst() {
        List<DocumentRow> rows = DocumentRow.merge(
                Arrays.asList(online("o100", 100), online("o400", 400)),
                Arrays.asList(local("l50", 50), local("l300", 300)));
        assertEquals(Arrays.asList("o400", "l300", "o100", "l50"), ids(rows));
    }

    @Test public void onlyOnlineDocuments() {
        List<DocumentRow> rows = DocumentRow.merge(
                Arrays.asList(online("a", 1), online("b", 2)), Collections.<LocalDocument>emptyList());
        assertEquals(Arrays.asList("b", "a"), ids(rows));
    }

    @Test public void onlyPhoneDocuments() {
        List<DocumentRow> rows = DocumentRow.merge(
                Collections.<PetDocument>emptyList(), Arrays.asList(local("a", 1), local("b", 2)));
        assertEquals(Arrays.asList("b", "a"), ids(rows));
    }

    @Test public void nothingInGivesNothingOut() {
        assertTrue(DocumentRow.merge(Collections.<PetDocument>emptyList(), Collections.<LocalDocument>emptyList()).isEmpty());
    }

    @Test public void nullListsAndNullItemsAreSkipped() {
        assertTrue(DocumentRow.merge(null, null).isEmpty());
        List<DocumentRow> rows = DocumentRow.merge(
                Arrays.asList(online("o", 5), null), Arrays.asList((LocalDocument) null, local("l", 7)));
        assertEquals(Arrays.asList("l", "o"), ids(rows));
    }

    @Test public void aRowTellsWhereItCameFromAndShowsItsFields() {
        DocumentRow phone = DocumentRow.ofLocal(local("p", 20));
        assertTrue(phone.isLocal());
        assertEquals("Phone p", phone.title());
        assertEquals("Vaccination", phone.type());
        assertEquals(20, phone.addedAt());
        assertFalse(phone.looksLikeImage());

        PetDocument photo = online("img", 30);
        photo.fileName = "scan.JPG";
        DocumentRow cloud = DocumentRow.ofOnline(photo);
        assertFalse(cloud.isLocal());
        assertEquals("Online img", cloud.title());
        assertEquals("Receipt", cloud.type());
        assertEquals(30, cloud.addedAt());
        assertTrue(cloud.looksLikeImage());
    }
}
