package com.itismob.mco.purrfolio;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CareCalendarActivity extends BaseListActivity {
    private final List<Reminder> all = new ArrayList<>();
    private long selected = Ui.startOfDay(System.currentTimeMillis());

    @Override
    protected void onScreenCreated() {
        titleTv.setText("Care Calendar");
        subtitleTv.setText("See all scheduled care at a glance");
        show(calendarView, true);
        bannerTv.setText("Tap an item below to add it to your device calendar");
        show(bannerTv, true);
        calendarView.setOnDateChangeListener((view, y, m, d) -> {
            Calendar c = Calendar.getInstance();
            c.set(y, m, d, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            selected = c.getTimeInMillis();
            render();
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

    private void render() {
        List<Row> out = new ArrayList<>();
        out.add(Row.header(Ui.dayLabel(selected)));
        out.addAll(Reminder.rowsFor(all, selected, true, "All"));
        showRows(out);
    }

    @Override
    protected void onRowClick(Row row) {
        Reminder r = Reminder.find(all, row.id);
        if (r == null) return;
        long start = r.occurrenceOn(selected);
        Intent i = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE, r.title + " · " + r.petName).putExtra(CalendarContract.Events.DESCRIPTION, r.detail).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 30 * 60 * 1000L);
        if (r.repeatDaily) i.putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY");
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "No calendar app found on this device");
        }
    }
}
