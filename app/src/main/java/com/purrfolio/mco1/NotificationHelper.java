package com.purrfolio.mco1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public final class NotificationHelper {
    private NotificationHelper() { }
    public static final String CHANNEL_ID = "purrfolio_care";
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Care reminders", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Medication, feeding, vaccination and vet reminders");
        nm.createNotificationChannel(channel);
    }
    public static boolean canPost(Context context) {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
    private static boolean switchedOn(Context context) {
        return context.getSharedPreferences("purrfolio_demo", Context.MODE_PRIVATE).getBoolean("setting:Care Reminders", true);
    }

    @SuppressLint("MissingPermission")
    public static void show(Context context, Reminder r) {
        if (!switchedOn(context) || !canPost(context)) return;
        ensureChannel(context);

        PendingIntent open = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        Intent actionIntent = ReminderScheduler.intentFor(context, r).setAction(ReminderScheduler.ACTION_GIVEN);
        PendingIntent given = PendingIntent.getBroadcast(context, r.id.hashCode() + 1, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = r.detail.isEmpty() ? "Time for " + r.petName + "'s care" : r.detail;
        android.app.Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_paw).setContentTitle(r.title + " · " + r.petName).setContentText(text).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open).addAction(0, r.actionWord(), given).build();
        NotificationManagerCompat.from(context).notify(r.id.hashCode(), notification);
    }

    public static void cancel(Context context, Reminder r) {
        NotificationManagerCompat.from(context).cancel(r.id.hashCode());
    }
}
