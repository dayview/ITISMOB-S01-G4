package com.itismob.mco.purrfolio;

import android.content.Intent;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PetDetailActivity extends BaseListActivity {
    private Pet pet;
    private final List<Reminder> reminders = new ArrayList<>();

    @Override
    protected void onScreenCreated() {
        addTabs("Overview", "Records", "Health", "Notes");
        addBtn.setText("💉");
        show(addBtn, true);
        addBtn.setOnClickListener(v -> openScreen(PetFormActivity.class));
        actionBtn.setText("Open Pet Passport");
        show(actionBtn, true);
        actionBtn.setOnClickListener(v -> openScreen(PassportActivity.class));
    }

    private void openScreen(Class<?> cls) {
        Intent i = new Intent(this, cls);
        i.putExtra(Ui.PET_ID, petId());
        startActivity(i);
    }

    @Override
    protected void load() {
        Repo.pets().document(petId()).get().addOnSuccessListener(d -> {
            pet = d.toObject(Pet.class);
            if (pet == null) { finish(); return; }
            Session.setPet(this, pet.id, pet.name);
            titleTv.setText(pet.name);
            subtitleTv.setText(pet.details());
            onFilterChanged();
        });
    }

    @Override
    protected void onFilterChanged() {
        if (pet == null) return;
        show(bannerTv, false);
        switch (selectedTab()) {
            case "Records": loadRecords(); break;
            case "Health": loadHealth(); break;
            case "Notes": loadNotes(); break;
            default: loadOverview();
        }
    }

    private void loadOverview() {
        Repo.loadReminders(pet, list -> {
            reminders.clear();
            reminders.addAll(list);
            renderOverview();
        });
    }

    private void renderOverview() {
        long today = Ui.startOfDay(System.currentTimeMillis());
        int[] p = Reminder.progress(reminders, today);
        bannerTv.setText("Today's Care · " + p[0] + " of " + p[1] + " done");
        show(bannerTv, true);

        List<Row> out = new ArrayList<>(Reminder.rowsFor(reminders, today, false, "All"));
        out.add(Row.header("Upcoming"));
        out.addAll(Reminder.rowsFor(reminders, Ui.addDays(today, 1), false, "All"));
        showRows(out);
    }

    private void loadRecords() {
        Repo.loadDocs(pet.id, docs -> {
            List<Row> out = new ArrayList<>();
            for (DocumentSnapshot d : docs) out.add(Repo.docRow(d));
            showRows(out);
        });
    }

    private void loadHealth() {
        Repo.loadVaccinations(pet.id, vaccines -> {
            List<Row> out = new ArrayList<>();
            out.add(Row.header("Health info"));
            out.add(new Row("allergies", "Allergies", Repo.orNone(pet.allergies), "›"));
            out.add(new Row("conditions", "Conditions", Repo.orNone(pet.conditions), "›"));
            out.add(new Row("meds", "Medications", Repo.orNone(pet.medications), "›"));
            out.add(new Row("vet", "Veterinarian", Repo.orNone((pet.vetName + " " + pet.vetPhone).trim()), "›"));
            out.add(Row.header("Vaccinations"));
            for (DocumentSnapshot d : vaccines) out.add(new Row("v:" + d.getId(), Repo.vaccineLine(d), null, null));
            showRows(out);
        });
    }

    private void loadNotes() {
        List<Row> out = new ArrayList<>();
        out.add(new Row("about", "About " + pet.name, pet.about.isEmpty() ? "No notes yet. Tap to add." : pet.about, "›"));
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        switch (selectedTab()) {
            case "Records":
                Intent i = new Intent(this, DocumentDetailActivity.class);
                i.putExtra(Ui.PET_ID, pet.id);
                i.putExtra(DocumentDetailActivity.DOC_ID, row.id);
                startActivity(i);
                break;
            case "Health":
                if (!row.id.startsWith("v:")) openScreen(PetHealthActivity.class);
                break;
            case "Notes":
                openScreen(PetFormActivity.class);
                break;
            default:
                Reminder r = Reminder.find(reminders, row.id);
                if (r == null) return;
                Ui.confirm(this, r.title, "Mark as " + r.actionWord() + "?", () -> {
                    ReminderScheduler.markGiven(this, r);
                    r.lastDoneAt = System.currentTimeMillis();
                    renderOverview();
                });
        }
    }
}

