package com.purrfolio.mco1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public final class ReminderScheduler {
    private ReminderScheduler() { }
    public static final String ACTION_GIVEN = "com.purrfolio.mco1.ACTION_GIVEN";
    private static String nz(String s) { return s == null ? "" : s; }
    public static Intent intentFor(Context context, Reminder r) {
        Intent i = new Intent(context, ReminderScheduler.class);
        i.putExtra("id", r.id);
        i.putExtra("petId", r.petId);
        i.putExtra("petName", r.petName);
        i.putExtra("title", r.title);
        i.putExtra("detail", r.detail);
        i.putExtra("category", r.category);
        i.putExtra("timeMillis", r.timeMillis);
        i.putExtra("repeat", r.repeatDaily);
        return i;
    }

    public static Reminder fromIntent(Intent i) {
        Reminder r = new Reminder();
        r.id = nz(i.getStringExtra("id"));
        r.petId = nz(i.getStringExtra("petId"));
        r.petName = nz(i.getStringExtra("petName"));
        r.title = nz(i.getStringExtra("title"));
        r.detail = nz(i.getStringExtra("detail"));
        String category = i.getStringExtra("category");
        r.category = category == null ? Reminder.OTHERS : category;
        r.timeMillis = i.getLongExtra("timeMillis", 0l);
        r.repeatDaily = i.getBooleanExtra("repeat", false);
        return r;
    }

    private static PendingIntent alarmIntent(Context context, Reminder r) {
        return PendingIntent.getBroadcast(context, r.id.hashCode(), intentFor(context, r), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /* sets the alarm for the next time r should ring */
    public static void schedule(Context context, Reminder r) {
        if (r.id == null || r.id.isEmpty()) return;
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        PendingIntent pending = alarmIntent(context, r);
        long next = r.nextAfter(System.currentTimeMillis());
        if (next < 0) alarms.cancel(pending);
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending);
    }

    public static void cancel(Context context, Reminder r) {
        if (r.id == null || r.id.isEmpty()) return;
        context.getSystemService(AlarmManager.class).cancel(alarmIntent(context, r));
    }

    public static void scheduleAll(Context context, List<Reminder> reminders) {
        for (Reminder r : reminders) schedule(context, r);
    }
}
