package com.itismob.mco.purrfolio;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemindersActivity extends BaseListActivity {
    private final List<Reminder> all = new ArrayList<>();

    @Override
    protected void onScreenCreated() {
        titleTv.setText("Reminders");
        subtitleTv.setText("Never missa moment of their care");
        addTabs("All", "Medication", "Feeding", "Other");
        show(addBtn, true);
        addBtn.setOnClickListener(v -> {
            String pid = Session.petId(this);
            if (pid == null) { Ui.toast(this, "Add a pet first"); return; }

            Repo.can(pid, "canReminders", ok -> {
                if (ok) startActivity(new Intent(this, AddReminderActivity.class));
                else Ui.toast(this, "You don't have permission to manage reminders");
            });
        });
    }

    @Override
    protected void load() {
        Repo.loadAllReminders(list -> {
            all.clear();
            all.addAll(list);
            render();
        });
    }

    @Override
    protected void onFilterChanged() { render(); }

    private void render() {
        String tab = selectedTab();
        long today = Ui.startOfDay(System.currentTimeMillis());
        long tomorrow = Ui.addDays(today, 1);

        List<Row> out = new ArrayList<>();
        out.add(Row.header(Ui.dayLabel(today)));
        out.addAll(Reminder.rowsFor(all, today, true, tab));
        out.add(Row.header(Ui.dayLabel(tomorrow)));
        out.addAll(Reminder.rowsFor(all, tomorrow, true, tab));

        List<Reminder> later = new ArrayList<>();
        for (Reminder r : all) {
            if (r.matches(tab) && !r.repeatDaily && r.timeMillis >= Ui.addDays(today, 2)) later.add(r);
        }
        Collections.sort(later, (a, b) -> Long.compare(a.timeMillis, b.timeMillis));
        if (!later.isEmpty()) {
            out.add(Row.header("Later"));
            for (Reminder r : later) {
                out.add(new Row(r.key(), r.title, Ui.dateTime(r.timeMillis) + " · " + r.petName, ""));
            }
        }
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        Reminder r = Reminder.find(all, row.id);
        if (r == null) return;
        String[] options = {"Mark as " + r.actionWord(), "Edit / reschedule", "Delete"};
        new AlertDialog.Builder(this).setTitle(r.title).setItems(options, (d, which) -> {
            Repo.can(r.petId, "canReminders", ok -> {
                if (!ok) { Ui.toast(this, "You don't have permission to manage reminders"); return; }
                if (which == 0) {
                    ReminderScheduler.markGiven(this, r);
                    r.lastDoneAt = System.currentTimeMillis();
                    render();
                } else if (which == 1) {
                    Intent i = new Intent(this, AddReminderActivity.class);
                    i.putExtra(Ui.PET_ID, r.petId);
                    i.putExtra(AddReminderActivity.REMINDER_ID, r.id);
                    startActivity(i);
                } else {
                    Ui.confirm(this, "Delete reminder?", r.title, () -> {
                        ReminderScheduler.cancel(this, r);
                        Repo.sub(r.petId, "reminders").document(r.id).delete();
                        Repo.activity(r.petId, "Delete reminder", r.title);
                        all.remove(r);
                        render();
                    });
                }
            });
        }).show();
    }
}
