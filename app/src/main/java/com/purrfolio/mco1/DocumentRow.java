package com.purrfolio.mco1;

import java.util.ArrayList;
import java.util.List;

/**
 * One row in a document list: either an online {@link PetDocument} or a {@link LocalDocument} saved on
 * this phone. A screen can show both kinds in one newest-first list with {@link #merge}, so the merge is
 * written once and shared (the Documents list now, the Records screen later).
 *
 * No Android types, so DocumentRowTest can run on the JVM.
 */
public final class DocumentRow {
    /** The online document, or null when this row is a phone document. */
    public final PetDocument online;
    /** The phone document, or null when this row is an online document. */
    public final LocalDocument local;

    private DocumentRow(PetDocument online, LocalDocument local) {
        this.online = online;
        this.local = local;
    }

    public static DocumentRow ofOnline(PetDocument document) {
        return new DocumentRow(document, null);
    }

    public static DocumentRow ofLocal(LocalDocument document) {
        return new DocumentRow(null, document);
    }

    public boolean isLocal() { return local != null; }
    public String title() { return local != null ? local.title : online.title; }
    public String type() { return local != null ? local.type : online.type; }
    public long addedAt() { return local != null ? local.addedAt : online.addedAt; }
    public boolean looksLikeImage() { return local != null ? local.looksLikeImage() : online.looksLikeImage(); }

    /**
     * Puts both lists into one list, newest first. The lists do not have to be sorted already. When a phone
     * document and an online document have the same time, the phone document comes first. A null list counts
     * as empty and null items are skipped.
     */
    public static List<DocumentRow> merge(List<PetDocument> online, List<LocalDocument> local) {
        List<DocumentRow> rows = new ArrayList<>();
        // Phone documents go in first. List.sort is stable (equal items keep their order), so they stay
        // ahead of online documents with the same time.
        if (local != null) {
            for (LocalDocument document : local) {
                if (document != null) rows.add(ofLocal(document));
            }
        }
        if (online != null) {
            for (PetDocument document : online) {
                if (document != null) rows.add(ofOnline(document));
            }
        }
        rows.sort((a, b) -> Long.compare(b.addedAt(), a.addedAt()));
        return rows;
    }
}
