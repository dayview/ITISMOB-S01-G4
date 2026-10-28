package com.itismob.mco.purrfolio;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class PetHealthActivity extends AppCompatActivity {
    private EditText allergiesEt, conditionsEt, medicationsEt, vetNameEt, vetPhoneEt;
    private TextView healthTitleTv;
    private String petId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_pet_health);
        petId = getIntent().getStringExtra(Ui.PET_ID);
        if (petId == null) petId = Session.petId(this);

        allergiesEt = findViewById(R.id.allergiesEt);
        healthTitleTv = findViewById(R.id.healthTitleTv);
        conditionsEt = findViewById(R.id.conditionsEt);
        medicationsEt = findViewById(R.id.medicationsEt);
        vetNameEt = findViewById(R.id.vetNameEt);
        vetPhoneEt = findViewById(R.id.vetPhoneEt);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.saveBtn).setOnClickListener(v -> save());
        findViewById(R.id.vaccineBtn).setOnClickListener(v -> addVaccination());
        Repo.pets().document(petId).get().addOnSuccessListener(d -> {
            Pet p = d.toObject(Pet.class);
            if (p == null) return;
            healthTitleTv.setText(p.name + "'s Health Info");
            allergiesEt.setText(p.allergies);
            conditionsEt.setText(p.conditions);
            medicationsEt.setText(p.medications);
            vetNameEt.setText(p.vetName);
            vetPhoneEt.setText(p.vetPhone);
        });
    }

    private void save() {
        Map<String, Object> m = new HashMap<>();
        m.put("allergies", Ui.text(allergiesEt));
        m.put("conditions", Ui.text(conditionsEt));
        m.put("medications", Ui.text(medicationsEt));
        m.put("vetName", Ui.text(vetNameEt));
        m.put("vetPhone", Ui.text(vetPhoneEt));
        Repo.pets().document(petId).update(m);
        Repo.activity(petId, "Updated health info", "");
        Ui.toast(this, "Saved");
        finish();
    }

    private void addVaccination() {
        EditText nameInput = new EditText(this);
        nameInput.setHint("Vaccine name (e.g. Rabies)");
        new AlertDialog.Builder(this).setTitle("Add vaccination").setView(nameInput).setPositiveButton("Next", (d, w) -> {
            String name = Ui.text(nameInput);
            if (name.isEmpty()) return;
            Calendar given = Calendar.getInstance();
            Ui.toast(this, "Pick the date it was given");
            Ui.pickDate(this, given, () -> askNextDue(name, Ui.ymd(given)));
        }).setNegativeButton("Cancel", null).show();
    }

    private void askNextDue(String name, String givenDate) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);
        DatePickerDialog dlg = new DatePickerDialog(this, (v, y, m, d) -> saveVaccine(name, givenDate, Ui.ymd(y, m, d)), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.setTitle("Next due date");
        dlg.setButton(DialogInterface.BUTTON_NEUTRAL, "No next dose", (di, w) -> saveVaccine(name, givenDate, ""));
        dlg.show();
    }

    private void saveVaccine(String name, String givenDate, String nextDate) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("givenDate", givenDate);
        m.put("nextDate", nextDate);
        m.put("authorName", Repo.myName());
        Repo.sub(petId, "vaccinations").add(m);
        Repo.activity(petId, "Added vaccination", name);
        Ui.toast(this, "Vaccination added");
    }
}
