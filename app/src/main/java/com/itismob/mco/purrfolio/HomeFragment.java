package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseListFragment {
    private final List<Reminder> all = new ArrayList<>();


    @Override
    protected void onReady() {
        titleTv.setText("Today's Care");
        bannerTv.setVisibility(View.VISIBLE);
        actionBtn.setText("View Care Log");
        actionBtn.setVisibility(View.VISIBLE);
        actionBtn.setOnClickListener(v -> {
            if (Session.petId(requireContext()) == null) {
                Ui.toast(requireContext(), "Add a pet first");
                return;
            }
            startActivity(new Intent(requireContext(), CareLogActivity.class));
        });
    }

    @Override
    protected void load() {
        String first = Repo.myName().split(" ")[0];
        subtitleTv.setText("Hello, " + first + " 🐾");
        Repo.loadAllReminders(list -> {
            if (!isAdded()) return;
            all.clear();
            all.addAll(list);
            render();
        });
    }

    private void render() {
        long today = Ui.startOfDay(System.currentTimeMillis());
        int[] p = Reminder.progress(all, today);
        bannerTv.setText(p[1] == 0 ? "Nothing scheduled today. Add reminders from the Care tab." : p[0] + " of " + p[1] + " done");
        showRows(Reminder.rowsFor(all, today, true, "All"));
    }

    @Override
    protected void onRowClick(Row row) {
        Reminder r = Reminder.find(all, row.id);
        if (r == null) return;
        Ui.confirm(requireContext(), r.title, "Mark as " + r.actionWord() + "?", () -> {
            ReminderScheduler.markGiven(requireContext(), r);
            r.lastDoneAt = System.currentTimeMillis();
            render();
        });
    }
}
