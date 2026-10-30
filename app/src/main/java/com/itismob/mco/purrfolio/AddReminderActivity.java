package com.itismob.mco.purrfolio;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddReminderActivity extends AppCompatActivity {
    public static final String REMINDER_ID = "reminderId";
    private static final String[] CATEGORIES = {"Medication", "Feeding", "Vaccination", "Vet Appointment", "Others"};
    private Spinner petSp, categorySp;
    private EditText titleEt, detailEt;
    private TextView headTv, dateTv, timeTv;
    private MaterialSwitch repeatSw;
    private final List<Pet> pets = new ArrayList<>();
    private final Calendar cal = Calendar.getInstance();
    private String editPetId, reminderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_add_reminder);

        this.petSp = findViewById(R.id.petSp);
        this.categorySp = findViewById(R.id.categorySp);
        this.titleEt = findViewById(R.id.titleEt);
        this.detailEt = findViewById(R.id.detailEt);
        this.headTv = findViewById(R.id.headTv);
        this.dateTv = findViewById(R.id.dateTv);
        this.timeTv = findViewById(R.id.timeTv);
        this.repeatSw = findViewById(R.id.repeatSw);
        this.editPetId = getIntent().getStringExtra(Ui.PET_ID);
        this.reminderId = getIntent().getStringExtra(REMINDER_ID);

        Ui.spinner(categorySp, CATEGORIES);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        refreshWhen();

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        dateTv.setOnClickListener(v -> Ui.pickDate(this, cal, this::refreshWhen));
        timeTv.setOnClickListener(v -> Ui.pickTime(this, cal, this::refreshWhen));
        findViewById(R.id.saveBtn).setOnClickListener(v -> save());
        if (reminderId != null) headTv.setText("Edit Reminder");
        Repo.loadMyPets(list -> {
            pets.clear();
            pets.addAll(list);
            String[] names = new String[list.size()];
            for (int i = 0; i < names.length; i++) names[i] = list.get(i).name;
            Ui.spinner(petSp, names);

            String want = editPetId != null ? editPetId : Session.petId(this);
            for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(want)) petSp.setSelection(i);
            if (reminderId != null) {
                petSp.setEnabled(false);
                loadExisting();
            }
        });
    }

    private void refreshWhen() {
        dateTv.setText("Date - " + Ui.date(cal.getTimeInMillis()));
        timeTv.setText("Time - " + Ui.time(cal.getTimeInMillis()));
    }

    private void loadExisting() {
        Repo.sub(editPetId, "reminders").document(reminderId).get().addOnSuccessListener(d -> {
            Reminder r = d.toObject(Reminder.class);
            if (r == null) return;
            titleEt.setText(r.title);
            detailEt.setText(r.detail);
            categorySp.setSelection(Math.max(0, Arrays.asList(CATEGORIES).indexOf(r.category)));
            repeatSw.setChecked(r.repeatDaily);
            cal.setTimeInMillis(r.timeMillis);
            refreshWhen();
        });
    }

    private void save() {
        if (pets.isEmpty()) { Ui.toast(this, "Add a pet first"); return; }
        String title = Ui.text(titleEt);
        if (title.isEmpty()) { titleEt.setError("Give this reminder a name"); return; }
        Pet pet = pets.get(petSp.getSelectedItemPosition());
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND, 0);

        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("detail", Ui.text(detailEt));
        m.put("category", categorySp.getSelectedItem().toString());
        m.put("timeMillis", cal.getTimeInMillis());
        m.put("repeatDaily", repeatSw.isChecked());
        m.put("petId", pet.id);
        m.put("petName", pet.name);
        if (reminderId == null) m.put("lastDoneAt", 0L);

        DocumentReference ref = reminderId == null ? Repo.sub(pet.id, "reminders").document() : Repo.sub(pet.id, "reminders").document(reminderId);
        ref.set(m, SetOptions.merge());
        Reminder r = new Reminder();
        r.id = ref.getId();
        r.petId = pet.id;
        r.petName = pet.name;
        r.title = title;
        r.detail = Ui.text(detailEt);
        r.category = categorySp.getSelectedItem().toString();
        r.timeMillis = cal.getTimeInMillis();
        r.repeatDaily = repeatSw.isChecked();
        ReminderScheduler.schedule(this, r);
        Repo.activity(pet.id, reminderId == null ? "Added reminder" : "Updated reminder", title + " · " + Ui.dateTime(cal.getTimeInMillis()));
        finish();
    }
}
