package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RecordsActivity extends BaseListActivity {
    private static class Rec {
        String title, subtitle, type, docId;
        long ts;
    }

    private static final String[] RANGES = {"All time", "This month", "Last 3 months", "Last year"};
    private final List<Rec> recs = new ArrayList<>();

    @Override
    protected void onScreenCreated() {
        titleTv.setText("Records");
        subtitleTv.setText("Find documents, visits, medications, and more");
        show(searchEt, true);
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        addTabs("All", "Vaccinations", "Medications", "Documents", "Care Logs", "Appointments", "Obeservations");
        show(dateSpinner, true);
        Ui.spinner(dateSpinner, RANGES);
        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { render(); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
    }

    private void add(String title, String subtitle, String type, long ts, String docId) {
        Rec r = new Rec();
        r.title = title;
        r.subtitle = subtitle;
        r.type = type;
        r.ts = ts;
        r.docId = docId;
        recs.add(r);
    }

    @Override
    protected void load() {
        recs.clear();
        String pid = petId();
        int[] left = {5};
        Runnable done = () -> {
            if (--left[0] > 0) return;
            Collections.sort(recs, (a, b) -> Long.compare(b.ts, a.ts));
            render();
        };
        Repo.sub(pid, "logs").get().addOnCompleteListener(t -> {
            if (t.isSuccessful()) for (DocumentSnapshot d : t.getResult()) {
                Long ts = d.getLong("timestamp");
                long time = ts == null ? 0 : ts;
                String type = Repo.str(d, "type");
                String amount = Repo.str(d, "amount");
                String cat = type.equals("Medication") ? "Medications" : type.equals("Vet Visit") ? "Appointments" : "Care Logs";
                add(type, Ui.dateTime(time) + (amount.isEmpty() ? "" : " · " + amount), cat, time, null);
            }
            done.run();
        });
        Repo.sub(pid, "documents").get().addOnCompleteListener(t -> {
            if (t.isSuccessful()) for (DocumentSnapshot d : t.getResult()) {
                Long ts = d.getLong("addedAt");
                String type = Repo.str(d, "type");
                String cat = type.equals("Vaccination") ? "Vaccinations" : type.equals("Medication") ? "Medications" : "Documents";
                add(Repo.str(d, "title"), Ui.date(ts == null ? 0 : ts) + " · " + type, cat, ts == null ? 0 : ts, d.getId());
            }
            done.run();
        });
        Repo.sub(pid, "vaccinations").get().addOnCompleteListener(t -> {
            if (t.isSuccessful()) for (DocumentSnapshot d : t.getResult()) {
                add(Repo.str(d, "name") + "vaccine", Repo.vaccineLine(d), "Vaccinations", Ui.parseDate(Repo.str(d, "givenDate")), null);
            }
            done.run();
        });
        Repo.sub(pid, "sounds").get().addOnCompleteListener(t -> {
            if (t.isSuccessful()) for (DocumentSnapshot d : t.getResult()) {
                Long ts = d.getLong("timestamp");
                add(Repo.str(d, "label"), Ui.dateTime(ts == null ? 0 : ts), "Observations", ts == null ? 0 : ts, null);
            }
            done.run();
        });
        Repo.sub(pid, "reminders").get().addOnCompleteListener(t -> {
            if (t.isSuccessful()) for (DocumentSnapshot d : t.getResult()) {
                String cat = Repo.str(d, "category");
                if (!cat.equals("Vet Appointment") && !cat.equals("Vaccination")) continue;
                Long ts = d.getLong("timeMillis");
                add(Repo.str(d,"title"), Ui.dateTime(ts == null ? 0 : ts), "Appointments", ts == null ? 0 : ts, null);
            }
            done.run();
        });
    }

    @Override
    protected void onFilterChanged() { render(); }

    private long cutoff() {
        Calendar c = Calendar.getInstance();
        switch (dateSpinner.getSelectedItemPosition()) {
            case 1: c.set(Calendar.DAY_OF_MONTH, 1); return Ui.startOfDay(c.getTimeInMillis());
            case 2: c.add(Calendar.MONTH, -3); return c.getTimeInMillis();
            case 3: c.add(Calendar.YEAR, -1); return c.getTimeInMillis();
            default: return Long.MIN_VALUE;
        }
    }

    private void render() {
        String tab = selectedTab();
        String q = Ui.text(searchEt).toLowerCase(Locale.getDefault());
        long cut = cutoff();

        List<Row> out = new ArrayList<>();
        String lastDay = "";
        for (Rec r : recs) {
            if (!tab.equals("All") && !tab.equals(r.type)) continue;
            if (r.ts < cut) continue;
            if (!q.isEmpty() && !(r.title + " " + r.subtitle).toLowerCase(Locale.getDefault()).contains(q)) continue;
            String day = Ui.dayLabel(r.ts);
            if (!day.equals(lastDay)) { out.add(Row.header(day)); lastDay = day; }
            out.add(new Row(r.docId, r.title, r.subtitle, r.type));
        }
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        if (row.id == null) return;
        Intent i = new Intent(this, DocumentDetailActivity.class);
        i.putExtra(Ui.PET_ID, petId());
        i.putExtra(DocumentDetailActivity.DOC_ID, row.id);
        startActivity(i);
    }
}
