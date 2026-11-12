package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.purrfolio.mco1.databinding.ItemReminderBinding;
import com.purrfolio.mco1.databinding.ItemSectionLabelBinding;
import com.purrfolio.mco1.databinding.ScreenRemindersBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class RemindersScreen {
    private static final String[] TABS = {"All", "Medication", "Feeding", "Others"};
    private final Activity activity;
    private final ScreenRemindersBinding binding;
    private final List<PetRef> pets;
    private final PetRef current;
    private final TextView[] chips;
    private final Runnable observer = this::render;
    private String filter = "All";

    public RemindersScreen(Activity activity, ScreenRemindersBinding binding, List<PetRef> pets, PetRef current) {
        this.activity = activity;
        this.binding = binding;
        this.pets = pets;
        this.current = current;
        this.chips = new TextView[]{binding.filterAll, binding.filterMedication, binding.filterFeeding, binding.filterOthers};
    }

    public void bind() {
        BrandType.apply(binding.remindersEyebrow, false, 700);
        BrandType.apply(binding.reminderTitle, true, 600);
        BrandType.apply(binding.remindersSubtitle, false, 400);
        BrandType.apply(binding.newReminderButton, false, 700);
        for (int i = 0; i < chips.length; i++) {
            String tab = TABS[i];
            BrandType.apply(chips[i], false, 700);
            chips[i].setOnClickListener(v -> { filter = tab; render(); });
        }
        binding.newReminderButton.setOnClickListener(v -> ReminderFormDialog.show(activity, pets, current, null));

        BackendHub hub = BackendHub.get();
        hub.start(activity, pets);
        hub.observe(observer);
        binding.getRoot().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { }
            @Override public void onViewDetachedFromWindow(View v) { hub.unobserve(observer); }
        });
        render();
    }

    private void render() {
        for (int i = 0; i < chips.length; i++) {
            boolean selected = TABS[i].equals(filter);
            chips[i].setBackgroundResource(selected ? R.drawable.legacy_button : R.drawable.legacy_chip);
            chips[i].setTextColor(ContextCompat.getColor(activity, selected ? R.color.on_primary : R.color.text_primary));
        }
        binding.reminderContainer.removeAllViews();

        List<Reminder> all = BackendHub.get().reminders();
        long today = CareTime.startOfDay(System.currentTimeMillis());
        addDay("Today", today, all);
        addDay("Tomorrow", CareTime.addDays(today, 1), all);
        addLater(all, today);
    }

    private void label(String text, boolean muted) {
        ItemSectionLabelBinding label = ItemSectionLabelBinding.inflate(activity.getLayoutInflater(), binding.reminderContainer, false);
        label.sectionLabel.setText(text);
        BrandType.apply(label.sectionLabel, false, 700);
        if (muted) label.sectionLabel.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
        binding.reminderContainer.addView(label.getRoot());
    }

    private void addDay(String name, long day, List<Reminder> all) {
        String date = new SimpleDateFormat("EEE, MMM d", Locale.US).format(new Date(day));
        label((name + " · " + date).toUpperCase(Locale.US), false);
        List<CareCalendar.Item> items = CareCalendar.itemsOn(all, day, filter);
        if (items.isEmpty()) label("Nothing scheduled", true);
        for (CareCalendar.Item item : items) {
            String time = new SimpleDateFormat("h:mm a", Locale.US).format(new Date(item.timeMillis));
            addRow(item.reminder, time + " · " + item.detailLine(), item.done);
        }
    }

    private void addLater(List<Reminder> all, long today) {
        List<Reminder> later = new ArrayList<>();
        for (Reminder r : all) {
            if (r.matches(filter) && !r.repeatDaily && r.timeMillis >= CareTime.addDays(today, 2)) later.add(r);
        }
        if (later.isEmpty()) return;
        Collections.sort(later, (a, b) -> Long.compare(a.timeMillis, b.timeMillis));
        label("LATER", false);
        for (Reminder r : later) {
            String when = new SimpleDateFormat("EEE, MMM d · h:mm a", Locale.US).format(new Date(r.timeMillis));
            addRow(r, when + " · " + r.petName, false);
        }
    }

    private void addRow(Reminder r, String detail, boolean done) {
        ItemReminderBinding item = ItemReminderBinding.inflate(activity.getLayoutInflater(), binding.reminderContainer, false);
        item.reminderIcon.setText(CareCalendar.glyph(r));
        item.reminderIcon.setTextColor(ContextCompat.getColor(activity, CareCalendar.colorRes(r.category)));
        item.reminderTitleText.setText(r.title);
        item.reminderDetailText.setText(detail);
        item.reminderStatus.setText(done ? "✓" : "○");
        item.reminderStatus.setTextColor(ContextCompat.getColor(activity, done ? R.color.green : R.color.primary));
        BrandType.apply(item.reminderIcon, true, 600);
        BrandType.apply(item.reminderTitleText, false, 700);
        BrandType.apply(item.reminderDetailText, false, 400);
        BrandType.apply(item.reminderStatus, true, 600);
        item.getRoot().setOnClickListener(v -> manage(r));
        binding.reminderContainer.addView(item.getRoot());
    }

    private void manage(Reminder r) {
        String[] options = {"Mark as " + r.actionWord(), "Edit / reschedule", "Delete"};
        new AlertDialog.Builder(activity).setTitle(r.title).setItems(options, (dialog, which) -> {
            if (which == 0) {
                ReminderManager.markGiven(activity.getApplicationContext(), r);
            } else if (which == 1) {
                ReminderFormDialog.show(activity, pets, current, r);
            } else {
                new AlertDialog.Builder(activity).setTitle("Delete reminder?").setMessage(r.title)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (d, w) -> ReminderManager.delete(activity.getApplicationContext(), r))
                        .show();
            }
        }).show();
    }
}
