package com.itismob.mco.purrfolio;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Ui {
    public static final String PET_ID = "petId";
    // Every layout root uses android:id="@+id/main"
    public static void screen(AppCompatActivity a, int layout) {
        EdgeToEdge.enable(a);
        a.setContentView(layout);
        insets(a.findViewById(R.id.main));
    }
    public static void insets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
    public static void toast(Context c, String msg) { Toast.makeText(c, msg, Toast.LENGTH_SHORT).show(); }
    public static String text(EditText e) { return e.getText().toString().trim(); }
    public static void spinner(Spinner sp, String... items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(sp.getContext(), android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
    }
    public static void confirm(Context c, String title, String message, Runnable onYes) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(message)
                .setPositiveButton("Yes", (d, w) -> onYes.run())
                .setNegativeButton("Cancel", null).show();
    }
    public static void share(Context c, String subject, String body) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, body);
        c.startActivity(Intent.createChooser(i, "Share via"));
    }
    // dates
    private static String fmt(String pattern, long ms) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(ms));
    }
    public static String date(long ms) { return fmt("MMM d, yyyy", ms); }
    public static String time(long ms) { return fmt("h:mm a", ms); }
    public static String dateTime(long ms) { return fmt("MMM d, yyyy · h:mm a", ms); }
    public static long startOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
    public static long addDays(long dayStart, int days) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayStart);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTimeInMillis();
    }
    public static String dayLabel(long ms) {
        long today = startOfDay(System.currentTimeMillis());
        long d = startOfDay(ms);
        String base = fmt("EEE, MMM d", ms);
        if (d == today) return "Today · " + base;
        if (d == addDays(today, 1)) return "Tomorrow · " + base;
        if (d == addDays(today, -1)) return "Yesterday · " + base;
        return base;
    }
    // dates are stored in Firestore as yyyy-MM-dd text
    public static String ymd(int y, int monthZeroBased, int d) {
        return String.format(Locale.US, "%04d-%02d-%02d", y, monthZeroBased + 1, d);
    }
    public static String ymd(Calendar c) { return ymd(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)); }
    public static long parseDate(String ymd) {
        try {
            return LocalDate.parse(ymd).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }
    public static String age(String birthday) {
        try {
            Period p = Period.between(LocalDate.parse(birthday), LocalDate.now());
            if (p.getYears() >= 1) return p.getYears() + (p.getYears() == 1 ? " year old" : " years old");
            return p.getMonths() + (p.getMonths() == 1 ? " month old" : " months old");
        } catch (Exception e) {
            return "Age unknown";
        }
    }
    public static void pickDate(Context c, Calendar cal, Runnable done) {
        new DatePickerDialog(c, (v, y, m, d) -> {
            cal.set(y, m, d);
            done.run();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
    public static void pickTime(Context c, Calendar cal, Runnable done) {
        new TimePickerDialog(c, (v, h, m) -> {
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            done.run();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }
    public static String fileName(Context c, Uri uri) {
        try (Cursor cur = c.getContentResolver().query(uri, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                int i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return cur.getString(i);
            }
        } catch (Exception ignored) { }
        return "document_" + System.currentTimeMillis();
    }
}