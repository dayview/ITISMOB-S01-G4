package com.purrfolio.mco1;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ReminderManager {
    private ReminderManager() { }
    private static String stamp(long ms) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.US).format(new Date(ms));
    }

    public static String validate(String title, long whenMillis, boolean repeatDaily) {
        if (title == null || title.trim().isEmpty()) return "Give the reminder a name.";
        if (whenMillis <= 0) return "Pick a date and time.";
        if (!repeatDaily && whenMillis <= System.currentTimeMillis()) return "Pick a time in the future.";
        return null;
    }

    public static Reminder create(Context context, PetRef pet, String title, String detail, String category, long whenMillis, boolean repeatDaily) {
        Reminder r = new Reminder();
        r.petId = pet.id;
        r.petName = pet.name;
        r.title = title.trim();
        r.detail = detail.trim();
        r.category = category;
        r.timeMillis = whenMillis;
        r.repeatDaily = repeatDaily;
        ReminderRepository.save(r);
        ReminderScheduler.schedule(context, r);
        CareWrites.activity(pet.id, "Added reminder", r.title + " · " + stamp(whenMillis));
        return r;
    }

    public static void update(Context context, Reminder r) {
        ReminderRepository.save(r);
        ReminderScheduler.schedule(context, r);
        CareWrites.activity(r.petId, "Updated reminder", r.title + " · " + stamp(r.timeMillis));
    }

    public static void delete(Context context, Reminder r) {
        ReminderScheduler.cancel(context, r);
        NotificationHelper.cancel(context, r);
        ReminderRepository.delete(r);
        CareWrites.activity(r.petId, "Deleted reminder", r.title);
    }

    public static void markGiven(Context context, Reminder r) {
        if (r.id == null || r.id.isEmpty() || r.petId.isEmpty()) return;
        long now = System.currentTimeMillis();
        ReminderRepository.stampDone(r, now);

        String type = "Other";
        if (Reminder.MEDICATION.equals(r.category) || Reminder.VACCINATION.equals(r.category)) type = "Medication";
        else if (Reminder.FEEDING.equals(r.category)) type = "Feeding";
        CareWrites.careLog(r.petId, r.petName, type, r.detail, r.title + " (from reminder)", now);
        CareWrites.activity(r.petId, "Completed reminder", r.title + " · " + stamp(now));
    }

    public static void syncAlarms(Context context, Runnable onDone) {
        ReminderRepository.loadMyPets(pets ->
                ReminderRepository.loadOnce(pets, reminders -> {
                    ReminderScheduler.scheduleAll(context, reminders);
                    if (onDone != null) onDone.run();
                }));
    }

    public static void cancelAlarms(Context context, Runnable onDone) {
        ReminderRepository.loadMyPets(pets ->
                ReminderRepository.loadOnce(pets, reminders -> {
                    for (Reminder r : reminders) ReminderScheduler.cancel(context, r);
                    if (onDone != null) onDone.run();
                }));
    }
}
