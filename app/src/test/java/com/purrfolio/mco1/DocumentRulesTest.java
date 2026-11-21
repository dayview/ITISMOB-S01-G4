package com.purrfolio.mco1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

/** Run in Android Studio: right-click this file → Run 'DocumentRulesTest'. */
public class DocumentRulesTest {
    @Test public void typesFallBackToOther() {
        assertEquals("Vaccination", DocumentRules.typeOrDefault("vaccination"));
        assertEquals("Receipt", DocumentRules.typeOrDefault("  Receipt "));
        assertEquals("Other", DocumentRules.typeOrDefault("Invoice"));
        assertEquals("Other", DocumentRules.typeOrDefault(null));
    }

    @Test public void extensionsAreLowerCaseAndSafe() {
        assertEquals("pdf", DocumentRules.extensionOf("Card.PDF"));
        assertEquals("gz", DocumentRules.extensionOf("a.tar.gz"));
        assertEquals("", DocumentRules.extensionOf("noextension"));
        assertEquals("", DocumentRules.extensionOf("trailing."));
        assertEquals("", DocumentRules.extensionOf(null));
    }

    @Test public void onlyPdfAndImagesAreAllowed() {
        assertTrue(DocumentRules.isAllowedFile("a.pdf"));
        assertTrue(DocumentRules.isAllowedFile("a.JPEG"));
        assertTrue(DocumentRules.isAllowedFile("a.webp"));
        assertFalse(DocumentRules.isAllowedFile("a.exe"));
        assertFalse(DocumentRules.isAllowedFile("a.docx"));
        assertFalse(DocumentRules.isAllowedFile("a"));
        assertTrue(DocumentRules.isImage("a.png"));
        assertFalse(DocumentRules.isImage("a.pdf"));
    }

    @Test public void fileErrorsSayHowToFixThem() {
        assertNull(DocumentRules.fileError("a.pdf", 1));
        assertNull(DocumentRules.fileError("a.pdf", DocumentRules.MAX_BYTES));
        assertEquals("Purrfolio can save PDF, JPG, PNG or WEBP files.", DocumentRules.fileError("a.zip", 10));
        assertEquals("This file is empty. Choose another one.", DocumentRules.fileError("a.pdf", 0));
        assertEquals("This file is larger than 10 MB. Choose a smaller one.",
                DocumentRules.fileError("a.pdf", DocumentRules.MAX_BYTES + 1));
    }

    @Test public void titleIsRequiredAndLimited() {
        assertNull(DocumentRules.titleError("Rabies card"));
        assertNotNull(DocumentRules.titleError(""));
        assertNotNull(DocumentRules.titleError("   "));
        assertNotNull(DocumentRules.titleError(null));
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < DocumentRules.TITLE_MAX; i++) longTitle.append('a');
        assertNull(DocumentRules.titleError(longTitle.toString()));
        assertNotNull(DocumentRules.titleError(longTitle + "a"));
    }

    @Test public void defaultTitleComesFromTheFileName() {
        assertEquals("vet receipt nov", DocumentRules.defaultTitle("vet_receipt-nov.pdf"));
        assertEquals("My scan", DocumentRules.defaultTitle("My   scan.JPG"));
        assertEquals("a.b", DocumentRules.defaultTitle("a.b.pdf"));
        assertEquals("Document", DocumentRules.defaultTitle(".pdf"));
        assertEquals("Document", DocumentRules.defaultTitle(null));
        assertEquals("Document", DocumentRules.defaultTitle("___.pdf"));
    }

    @Test public void defaultTitleIsCutToTheLimit() {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 100; i++) name.append('x');
        assertEquals(DocumentRules.TITLE_MAX, DocumentRules.defaultTitle(name + ".pdf").length());
    }

    @Test public void sizeLabels() {
        assertEquals("0 B", DocumentRules.sizeLabel(0));
        assertEquals("512 B", DocumentRules.sizeLabel(512));
        assertEquals("340 KB", DocumentRules.sizeLabel(340 * 1024));
        assertEquals("1.0 MB", DocumentRules.sizeLabel(1024 * 1024));
        assertEquals("1.5 MB", DocumentRules.sizeLabel((long) (1.5 * 1024 * 1024)));
    }

    @Test public void relativePathStaysInsideDocs() {
        assertEquals("docs/pet-1/doc-1.pdf", DocumentRules.relativePath("pet-1", "doc-1", "Card.PDF"));
        assertEquals("docs/_/_", DocumentRules.relativePath("", null, "noext"));
        String sneaky = DocumentRules.relativePath("../../etc", "a/b", "x.pdf");
        assertFalse(sneaky.contains(".."));
        assertEquals(3, sneaky.split("/").length);
        assertTrue(sneaky.startsWith("docs/"));
    }

    @Test public void documentSurvivesAJsonRoundTrip() {
        LocalDocument original = new LocalDocument("d1", "p1", " Rabies card ", "vaccination", "rabies.pdf",
                "docs/p1/d1.pdf", 1_700_000_000_000L, 2048);
        LocalDocument copy = LocalDocument.fromJson(original.toJson());
        assertEquals("d1", copy.id);
        assertEquals("p1", copy.petId);
        assertEquals("Rabies card", copy.title);
        assertEquals("Vaccination", copy.type);
        assertEquals("rabies.pdf", copy.fileName);
        assertEquals("docs/p1/d1.pdf", copy.path);
        assertEquals(1_700_000_000_000L, copy.addedAt);
        assertEquals(2048, copy.sizeBytes);
        assertFalse(copy.looksLikeImage());
    }

    @Test public void brokenJsonBecomesEmptyValues() {
        LocalDocument fromNull = LocalDocument.fromJson(null);
        assertEquals("", fromNull.id);
        assertEquals("Other", fromNull.type);
        LocalDocument partial = LocalDocument.fromJson(new JSONObject());
        assertEquals("", partial.title);
        assertEquals(0, partial.addedAt);
        assertEquals(0, partial.sizeBytes);
        LocalDocument negative = new LocalDocument("a", "b", "c", "Receipt", "photo.jpg", "docs/b/a.jpg", 5, -10);
        assertEquals(0, negative.sizeBytes);
        assertTrue(negative.looksLikeImage());
    }
}
