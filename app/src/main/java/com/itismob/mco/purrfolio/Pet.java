package com.itismob.mco.purrfolio;

import com.google.firebase.firestore.DocumentId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pet {
    @DocumentId public String id;
    public String ownerId = "";
    public String name = "", species = "", breed = "", birthday = "", weight = "", gender = "", about = "", photoUrl = "";
    public String allergies = "", conditions = "", medications = "", vetName = "", vetPhone = "";
    public List<String> members = new ArrayList<>();
    public Map<String, Boolean> qrShare = new HashMap<>();
    public String summary() { return Ui.age(birthday) + " · " + species; }
    public String details() {
        return summary() + (weight.isEmpty() ? "" : " · " + weight + " kg");
    }

    // QR privacy switches: name, photo, species, owner, medical, vet
    public boolean share(String key, boolean defaultValue) {
        Boolean v = qrShare == null ? null : qrShare.get(key);
        return v == null ? defaultValue : v;
    }
}

