package com.itismob.mco.purrfolio;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PassportActivity extends BaseListActivity {
    private Pet pet;
    private final List<DocumentSnapshot> vaccines = new ArrayList<>();

    @Override
    protected void onScreenCreated() {
        titleTv.setText("Pet Passport");
        subtitleTv.setText("Share important info with your vet or cargiver anytime");
        show(bannerTv, true);
        actionBtn.setText("Share passport");
        show(actionBtn, true);
        actionBtn.setOnClickListener(v -> {
            if (pet != null) Ui.share(this, pet.name + " - Pet Passport", passportText());
        });

        action2Btn.setText("View QR Code");
        show(action2Btn, true);
        action2Btn.setOnClickListener(v -> {
            Intent i = new Intent(this, PetQrActivity.class);
            i.putExtra(Ui.PET_ID, petId());
            startActivity(i);
        });
    }

    @Override
    protected void load() {
        Repo.pets().document(petId()).get().addOnSuccessListener(d -> {
            pet = d.toObject(Pet.class);
            if (pet == null) { finish(); return; }
            Repo.loadVaccinations(pet.id, list -> {
                vaccines.clear();
                vaccines.addAll(list);
                render();
            });
        });
    }

    private void render() {
        bannerTv.setText(pet.name + " · " + pet.species + " · " + Ui.age(pet.birthday));
        List<Row> out = new ArrayList<>();
        out.add(new Row("vaccines", "Vaccinations", vaccines.size() + (vaccines.size() == 1 ? " records" : " records"), "›"));
        out.add(new Row("meds", "Medications", Repo.orNone(pet.medications), "›"));
        out.add(new Row("allergies", "Allergies", Repo.orNone(pet.allergies), "›"));
        out.add(new Row("conditions", "Conditions", Repo.orNone(pet.conditions), "›"));
        out.add(new Row("vet", "Veterinarian", Repo.orNone((pet.vetName + " " + pet.vetPhone).trim()), "›"));
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        if (row.id.equals("vaccines")) {
            StringBuilder sb = new StringBuilder();
            for (DocumentSnapshot d : vaccines) sb.append("• ").append(Repo.vaccineLine(d)).append("\n");
            new AlertDialog.Builder(this).setTitle("Vaccinations").setMessage(sb.length() == 0 ? "No vaccinations recorded yet." : sb.toString().trim()).setPositiveButton("OK", null).show();
        } else {
            Intent i = new Intent(this, PetHealthActivity.class);
            i.putExtra(Ui.PET_ID, petId());
            startActivity(i);
        }
    }

    private String passportText() {
        StringBuilder sb = new StringBuilder();
        sb.append("🐾 Pet Passport - ").append(pet.name).append("\n");
        sb.append(pet.species).append(pet.breed.isEmpty() ? "" : " (" + pet.breed + ")").append(" • ").append(Ui.age(pet.birthday)).append("\n\n");
        sb.append("Vaccinations:\n");
        if (vaccines.isEmpty()) sb.append("None recorded\n");
        for (DocumentSnapshot d : vaccines) sb.append("• ").append(Repo.vaccineLine(d)).append("\n");
        sb.append("\nMedications: ").append(Repo.orNone(pet.medications));
        sb.append("\nAllergies: ").append(Repo.orNone(pet.allergies));
        sb.append("\nConditions: ").append(Repo.orNone(pet.conditions));
        sb.append("\nVeterinarian: ").append(Repo.orNone((pet.vetName + " " + pet.vetPhone).trim()));
        sb.append("\n\nShared from Purrfolio. Information entered by the owner; not medical advice.");
        return sb.toString();
    }
}
