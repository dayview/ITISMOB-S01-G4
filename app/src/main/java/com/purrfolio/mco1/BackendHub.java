package com.purrfolio.mco1;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class BackendHub {
    private static final BackendHub INSTANCE = new BackendHub();
    public static BackendHub get() { return INSTANCE; }
    private BackendHub() { }
    private final List<Reminder> reminders = new ArrayList<>();
    private final List<PetDocument> documents = new ArrayList<>();
    private final List<Runnable> observers = new ArrayList<>();
    private Runnable stopReminders, stopDocuments;
    private String remindersKey = "", documentsPetId = "";

    public List<Reminder> reminders() { return reminders; }
    public List<PetDocument> documents() { return documents; }

    public void start(Context context, List<PetRef> pets) {
        StringBuilder key = new StringBuilder();
        for (PetRef p : pets) key.append(p.id).append(',');
        if (key.toString().equals(remindersKey)) return;
        remindersKey = key.toString();

        if (stopReminders != null) stopReminders.run();
        reminders.clear();
        Context app = context.getApplicationContext();
        stopReminders = ReminderRepository.listen(pets, list -> {
            reminders.clear();
            reminders.addAll(list);
            ReminderScheduler.scheduleAll(app, list);
            notifyObservers();
        });
    }

    public void watchDocuments(String petId) {
        if (petId == null || petId.isEmpty() || petId.equals(documentsPetId)) return;
        documentsPetId = petId;
        if (stopDocuments != null) stopDocuments.run();
        documents.clear();
        stopDocuments = DocumentRepository.listen(petId, list -> {
            documents.clear();
            documents.addAll(list);
            notifyObservers();
        });
    }

    public void restart(Context context, List<PetRef> pets) {
        reset();
        start(context, pets);
    }

    public void reset() {
        if (stopReminders != null) stopReminders.run();
        if (stopDocuments != null) stopDocuments.run();
        stopReminders = null;
        stopDocuments = null;
        remindersKey = "";
        documentsPetId = "";
        reminders.clear();
        documents.clear();
        notifyObservers();
    }

    public void observe(Runnable observer) { if (!observers.contains(observer)) observers.add(observer); }
    public void unobserve(Runnable observer) { observers.remove(observer); }

    private void notifyObservers() {
        for (Runnable o : new ArrayList<>(observers)) o.run();
    }
}
