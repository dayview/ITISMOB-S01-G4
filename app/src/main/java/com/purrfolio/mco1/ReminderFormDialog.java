package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.purrfolio.mco1.databinding.DialogReminderFormBinding;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class ReminderFormDialog {
    private ReminderFormDialog() { }

    public static void show(Activity activity, List<PetRef> pets, PetRef current, Reminder existing) {
        if (pets.isEmpty()) {
            Toast.makeText(activity, "Add a pet first.", Toast.LENGTH_SHORT).show();
            return;
        }
        DialogReminderFormBinding b = DialogReminderFormBinding.inflate(activity.getLayoutInflater());

        String[] petNames = new String[pets.size()];
        int petIndex = 0;
        String wanted = existing != null ? existing.petId : current.id;
        for (int i = 0; i < pets.size(); i++) {
            petNames[i] = pets.get(i).name;
            if (pets.get(i).id.equals(wanted)) petIndex = i;
        }
        b.reminderPetSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, petNames));
        b.reminderPetSpinner.setSelection(petIndex);
        b.reminderCategorySpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, Reminder.CATEGORIES));

        Calendar when = Calendar.getInstance();
        when.add(Calendar.HOUR_OF_DAY, 1);
        when.set(Calendar.MINUTE, 0);
        when.set(Calendar.SECOND, 0);
        when.set(Calendar.MILLISECOND, 0);

        if (existing != null) {
            b.reminderPetSpinner.setEnabled(false);
            b.reminderTitleInput.setText(existing.title);
            b.reminderDetailInput.setText(existing.detail);
            b.reminderCategorySpinner.setSelection(Math.max(0, Arrays.asList(Reminder.CATEGORIES).indexOf(existing.category)));
            b.reminderRepeatCheck.setChecked(existing.repeatDaily);
            when.setTimeInMillis(existing.timeMillis);
        }

        Runnable showWhen = () -> {
            b.reminderDateButton.setText("Date · " + new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(when.getTime()));
            b.reminderTimeButton.setText("Time · " + new SimpleDateFormat("h:mm a", Locale.US).format(when.getTime()));
        };
        showWhen.run();
        b.reminderDateButton.setOnClickListener(v -> new DatePickerDialog(activity,
                (view, y, m, d) -> { when.set(y, m, d); showWhen.run(); },
                when.get(Calendar.YEAR), when.get(Calendar.MONTH), when.get(Calendar.DAY_OF_MONTH)).show());
        b.reminderTimeButton.setOnClickListener(v -> new TimePickerDialog(activity, (view, h, min) -> { when.set(Calendar.HOUR_OF_DAY, h); when.set(Calendar.MINUTE, min); showWhen.run(); },
                when.get(Calendar.HOUR_OF_DAY), when.get(Calendar.MINUTE), false).show());

        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(existing == null ? "New care reminder" : "Edit reminder").setView(b.getRoot()).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = b.reminderTitleInput.getText().toString();
            boolean repeat = b.reminderRepeatCheck.isChecked();
            String error = ReminderManager.validate(title, when.getTimeInMillis(), repeat);
            if (error != null) {
                Toast.makeText(activity, error, Toast.LENGTH_SHORT).show();
                return;
            }
            String detail = b.reminderDetailInput.getText().toString();
            String category = Reminder.CATEGORIES[b.reminderCategorySpinner.getSelectedItemPosition()];
            Context app = activity.getApplicationContext();

            if (existing != null) {
                PetRef pet = pets.get(b.reminderPetSpinner.getSelectedItemPosition());
                ReminderManager.create(app, pet, title, detail, category, when.getTimeInMillis(), repeat);
            } else {
                existing.title = title.trim();
                existing.detail = detail.trim();
                existing.category = category;
                existing.timeMillis = when.getTimeInMillis();
                existing.repeatDaily = repeat;
                ReminderManager.update(app, existing);
            }
            Toast.makeText(activity, "Reminder saved.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }
}
