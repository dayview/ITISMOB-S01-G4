package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.purrfolio.mco1.databinding.ItemCalendarDayBinding;
import com.purrfolio.mco1.databinding.ItemReminderBinding;
import com.purrfolio.mco1.databinding.ItemSectionLabelBinding;
import com.purrfolio.mco1.databinding.ScreenCareCalendarBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CareCalendarScreen {
    private final Activity activity;
    private final ScreenCareCalendarBinding binding;
    private final List<PetRef> pets;
    private final Calendar month = Calendar.getInstance();
    private final Runnable observer = this::render;
    private long selectedDay = CareTime.startOfDay(System.currentTimeMillis());

    public CareCalendarScreen(Activity activity, ScreenCareCalendarBinding binding, List<PetRef> pets) {
        this.activity = activity;
        this.binding = binding;
        this.pets = pets;
    }

    private int dp(int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density);
    }

    public void bind() {
        month.set(Calendar.DAY_OF_MONTH, 1);
        BrandType.apply(binding.calendarEyebrow, false, 700);
        BrandType.apply(binding.calendarTitle, true, 600);
        BrandType.apply(binding.calendarSubtitle, false, 400);
        BrandType.apply(binding.monthLabel, true, 600);
        BrandType.apply(binding.monthPrev, true, 600);
        BrandType.apply(binding.monthNext, true, 600);
        BrandType.apply(binding.selectedDayLabel, false, 700);
        BrandType.apply(binding.addToCalendarButton, false, 700);

        binding.monthPrev.setOnClickListener(v -> { month.add(Calendar.MONTH, -1); render(); });
        binding.monthNext.setOnClickListener(v -> { month.add(Calendar.MONTH, 1); render(); });
        binding.addToCalendarButton.setOnClickListener(v -> chooseItemToExport());

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
        renderMonth();
        renderDay();
    }

    // ----- the month grid with dots -----
    private void renderMonth() {
        binding.monthLabel.setText(new SimpleDateFormat("MMMM yyyy", Locale.US).format(month.getTime()));
        GridLayout grid = binding.calendarGrid;
        grid.removeAllViews();

        Calendar first = (Calendar) month.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;     // the grid starts on Sunday
        int days = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int total = ((offset + days + 6) / 7) * 7;
        long today = CareTime.startOfDay(System.currentTimeMillis());
        List<Reminder> all = BackendHub.get().reminders();

        for (int cell = 0; cell < total; cell++) {
            ItemCalendarDayBinding item = ItemCalendarDayBinding.inflate(activity.getLayoutInflater(), grid, false);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.height = dp(52);

            int dayNumber = cell - offset + 1;
            if (dayNumber < 1 || dayNumber > days) {            // empty cell before/after the month
                item.dayNumber.setText("");
                item.dayDot.setVisibility(View.INVISIBLE);
                grid.addView(item.getRoot(), lp);
                continue;
            }

            Calendar c = (Calendar) first.clone();
            c.set(Calendar.DAY_OF_MONTH, dayNumber);
            long day = CareTime.startOfDay(c.getTimeInMillis());
            boolean selected = day == selectedDay;

            item.dayNumber.setText(String.valueOf(dayNumber));
            BrandType.apply(item.dayNumber, false, day == today ? 700 : 400);
            int numberColor = selected ? R.color.on_primary : (day == today ? R.color.primary : R.color.text_primary);
            item.dayNumber.setTextColor(ContextCompat.getColor(activity, numberColor));
            if (selected) item.dayCell.setBackgroundResource(R.drawable.legacy_button);

            List<String> categories = CareCalendar.categoriesOn(all, day);
            if (categories.isEmpty()) {
                item.dayDot.setVisibility(View.INVISIBLE);
            } else {
                item.dayDot.setTextColor(ContextCompat.getColor(activity, CareCalendar.colorRes(categories.get(0))));
            }

            item.dayCell.setOnClickListener(v -> { selectedDay = day; render(); });
            grid.addView(item.getRoot(), lp);
        }
    }

    // ----- the selected day -----
    private void renderDay() {
        binding.dayContainer.removeAllViews();
        String name = new SimpleDateFormat("EEEE, MMM d", Locale.US).format(new Date(selectedDay));
        binding.selectedDayLabel.setText(("Care on " + name).toUpperCase(Locale.US));

        List<CareCalendar.Item> items = CareCalendar.itemsOn(BackendHub.get().reminders(), selectedDay, "All");
        if (items.isEmpty()) {
            ItemSectionLabelBinding empty = ItemSectionLabelBinding.inflate(activity.getLayoutInflater(), binding.dayContainer, false);
            empty.sectionLabel.setText("No care scheduled for this day");
            empty.sectionLabel.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
            BrandType.apply(empty.sectionLabel, false, 700);
            binding.dayContainer.addView(empty.getRoot());
            return;
        }
        for (CareCalendar.Item entry : items) {
            ItemReminderBinding row = ItemReminderBinding.inflate(activity.getLayoutInflater(), binding.dayContainer, false);
            row.reminderIcon.setText(CareCalendar.glyph(entry.reminder));
            row.reminderIcon.setTextColor(ContextCompat.getColor(activity, CareCalendar.colorRes(entry.reminder.category)));
            row.reminderTitleText.setText(entry.reminder.title);
            String time = new SimpleDateFormat("h:mm a", Locale.US).format(new Date(entry.timeMillis));
            row.reminderDetailText.setText(time + " · " + entry.detailLine());
            row.reminderStatus.setText(entry.done ? "✓" : "○");
            row.reminderStatus.setTextColor(ContextCompat.getColor(activity, entry.done ? R.color.green : R.color.primary));
            BrandType.apply(row.reminderIcon, true, 600);
            BrandType.apply(row.reminderTitleText, false, 700);
            BrandType.apply(row.reminderDetailText, false, 400);
            BrandType.apply(row.reminderStatus, true, 600);
            row.getRoot().setOnClickListener(v -> CalendarExport.add(activity, entry.reminder, entry.timeMillis));   // 7.2
            binding.dayContainer.addView(row.getRoot());
        }
    }

    private void chooseItemToExport() {
        List<CareCalendar.Item> items = CareCalendar.itemsOn(BackendHub.get().reminders(), selectedDay, "All");
        if (items.isEmpty()) {
            Toast.makeText(activity, "Nothing to add for this day.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[items.size()];
        for (int i = 0; i < labels.length; i++) {
            CareCalendar.Item it = items.get(i);
            labels[i] = new SimpleDateFormat("h:mm a", Locale.US).format(new Date(it.timeMillis)) + " · " + it.reminder.title;
        }
        new AlertDialog.Builder(activity).setTitle("Add to phone calendar").setItems(labels, (dialog, which) ->
                CalendarExport.add(activity, items.get(which).reminder, items.get(which).timeMillis)).show();
    }
}
