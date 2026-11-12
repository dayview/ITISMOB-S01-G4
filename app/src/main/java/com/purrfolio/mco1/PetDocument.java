package com.purrfolio.mco1;

import com.google.firebase.firestore.DocumentId;

public class PetDocument {
    @DocumentId public String id = "";
    public String petId = "";
    public String title = "";
    public String type = "Other";
    public String url = "";
    public String fileName = "";
    public long addedAt;
    public String authorName = "";

    /* label used by the filter chips on the documents screen */
    public String tabLabel() {
        switch (type) {
            case "Vaccination": return "Vaccinations";
            case "Medication": return "Medications";
            case "Receipt": return "Receipts";
            default: return "Other";
        }
    }

    public boolean looksLikeImage() {
        String name = fileName.isEmpty() ? url : fileName;
        int q = name.indexOf('?');
        if (q >= 0) name = name.substring(0,q);
        name = name.toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }
}
