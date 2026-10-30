package com.itismob.mco.purrfolio;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ReminderScheduler {
    public static final String ACTION_GIVEN = "com.itismob.mco.purrfolio.ACTION_GIVEN";
    private static String nz(String s) { return s == null ? "" : s; }
    public static Intent intentFor(Context c, Reminder r) {
        Intent i = new Intent(c, ReminderReceiver.class);
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
        r.id = i.getStringExtra("id");
        r.petId = nz(i.getStringExtra("petId"));
        r.petName = nz(i.getStringExtra("petName"));
        r.title = nz(i.getStringExtra("title"));
        r.detail = nz(i.getStringExtra("detail"));
        r.category = nz(i.getStringExtra("category"));
        r.timeMillis = i.getLongExtra("timeMillis", 0);
        r.repeatDaily = i.getBooleanExtra("repeat", false);
        return r;
    }

    private static PendingIntent alarmIntent(Context c, Reminder r) {
        return PendingIntent.getBroadcast(c, r.id.hashCode(), intentFor(c, r), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c, Reminder r) {
        if (r.id == null) return;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = alarmIntent(c, r);
        long next = r.nextAfter(System.currentTimeMillis());
        if (next < 0) { am.cancel(pi); return; }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
    }

    public static void cancel(Context c, Reminder r) {
        if (r.id == null) return;
        c.getSystemService(AlarmManager.class).cancel(alarmIntent(c, r));
    }

    public static void syncAll(Context c) {
        if (Repo.uid() == null) return;
        Context app = c.getApplicationContext();
        Repo.loadAllReminders(list -> { for (Reminder r : list) schedule(app, r); });
    }

    public static void cancelAll(Context c, Runnable done) {
        Context app = c.getApplicationContext();
        Repo.loadAllReminders(list -> {
            for (Reminder r : list) cancel(app, r);
            done.run();
        });
    }

    public static void markGiven(Context c, Reminder r) {
        long now = System.currentTimeMillis();
        Repo.sub(r.petId, "reminders").document(r.id).update("lastDoneAt", now);

        String type = "Other";
        if (r.category.equals("Medication") || r.category.equals("Vaccination")) type = "Medication";
        else if (r.category.equals("Feeding")) type = "Feeding";
        Repo.addLog(r.petId, r.petName, type, r.detail, r.title + " (from reminder)", now, true);
    }
}
