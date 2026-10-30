package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class EmergencyActivity extends BaseListActivity {
    private Pet pet;
    private String ownerName = "", ownerPhone = "";
    private final List<String> circleNames = new ArrayList<>();

    @Override
    protected void onScreenCreated() {
        int red = ContextCompat.getColor(this, R.color.purr_danger);
        titleTv.setText("Emergency Mode");
        titleTv.setTextColor(red);
        subtitleTv.setText("Quick access when it matters");

        show(bannerTv, true);
        bannerTv.setBackgroundColor(red);
        bannerTv.setTextColor(Color.WHITE);

        actionBtn.setText("Share Emergency Info");
        show(actionBtn, true);
        actionBtn.setBackgroundTintList(ColorStateList.valueOf(red));
        actionBtn.setTextColor(Color.WHITE);
        actionBtn.setOnClickListener(v -> {
            if (pet != null) Ui.share(this, "EMERGENCY — " + pet.name, emergencyText());
        });
    }

    @Override
    protected void load() {
        Repo.pets().document(petId()).get().addOnSuccessListener(d -> {
            pet = d.toObject(Pet.class);
            if (pet == null) { finish(); return; }
            Repo.db().collection("users").document(pet.ownerId).get().addOnCompleteListener(t -> {
                if (t.isSuccessful() && t.getResult() != null) {
                    ownerName = Repo.str(t.getResult(), "fullName");
                    ownerPhone = Repo.str(t.getResult(), "phone");
                }
                Repo.sub(pet.id, "circle").get().addOnCompleteListener(c -> {
                    circleNames.clear();
                    if (c.isSuccessful()) {
                        for (DocumentSnapshot m : c.getResult()) {
                            if (!"Owner".equals(m.getString("role"))) circleNames.add(Repo.str(m, "name"));
                        }
                    }
                    render();
                });
            });
        });
    }

    private void render() {
        bannerTv.setText(pet.name + " — " + pet.species + ", " + Ui.age(pet.birthday) + "\nEMERGENCY INFO");
        List<Row> out = new ArrayList<>();
        out.add(new Row("medical", "Medical Information", "Allergies, medications, conditions", "›"));
        out.add(new Row("vet", "Veterinarian",
                pet.vetName.isEmpty() ? "Not saved yet" : pet.vetName + " · tap to call", "›"));
        out.add(new Row("owner", "Emergency Contact",
                (ownerName.isEmpty() ? "Owner" : ownerName + " (Owner)") + (ownerPhone.isEmpty() ? " · no phone saved" : " · tap to call"), "›"));
        out.add(new Row("circle", "Care Circle", circleNames.isEmpty() ? "No one else yet" : String.join(", ", circleNames), ""));
        out.add(new Row("passport", "Pet Passport", "View and share full summary", "›"));
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        switch (row.id) {
            case "medical":
                new AlertDialog.Builder(this).setTitle("Medical Information").setMessage(medicalText())
                        .setPositiveButton("OK", null).show();
                break;
            case "vet": dial(pet.vetPhone); break;
            case "owner": dial(ownerPhone); break;
            case "passport":
                Intent i = new Intent(this, PassportActivity.class);
                i.putExtra(Ui.PET_ID, pet.id);
                startActivity(i);
                break;
        }
    }

    private void dial(String phone) {
        if (phone == null || phone.isEmpty()) { Ui.toast(this, "No phone number saved"); return; }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
    }

    private String medicalText() {
        return "Allergies: " + Repo.orNone(pet.allergies)
                + "\nMedications: " + Repo.orNone(pet.medications)
                + "\nConditions: " + Repo.orNone(pet.conditions);
    }

    private String emergencyText() {
        return "🚨 EMERGENCY — " + pet.name + " (" + pet.species + ", " + Ui.age(pet.birthday) + ")\n\n"
                + medicalText()
                + "\nVet: " + Repo.orNone((pet.vetName + " " + pet.vetPhone).trim())
                + "\nOwner: " + Repo.orNone((ownerName + " " + ownerPhone).trim());
    }
}
