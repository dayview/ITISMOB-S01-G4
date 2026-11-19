package com.purrfolio.mco1;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.widget.Toast;

public final class CalendarExport {
    private CalendarExport() { }

    public static Intent intentFor(Reminder r, long startMillis) {
        Intent i = new Intent(Intent.ACTION_INSERT);
        i.setData(CalendarContract.Events.CONTENT_URI);
        i.putExtra(CalendarContract.Events.TITLE, r.title + " · " + r.petName);
        i.putExtra(CalendarContract.Events.DESCRIPTION, r.detail);
        i.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis);
        i.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 30 * 60 * 1000L);
        if (r.repeatDaily) i.putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY");
        return i;
    }

    public static void add(Context context, Reminder r, long startMillis) {
        try {
            context.startActivity(intentFor(r, startMillis));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No calendar app found on this phone.", Toast.LENGTH_SHORT).show();
        }
    }
}
