package com.itismob.mco.purrfolio;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Reminder r = ReminderScheduler.fromIntent(intent);
        if (r.id == null) return;
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        if (ReminderScheduler.ACTION_GIVEN.equals(intent.getAction())) {
            ReminderScheduler.markGiven(context, r);
            nm.cancel(r.id.hashCode());
            return;
        }

        if (r.repeatDaily) ReminderScheduler.schedule(context, r);
        if (!Session.notif(context, "care")) return;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        PendingIntent open = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Intent action = ReminderScheduler.intentFor(context, r);
        action.setAction(ReminderScheduler.ACTION_GIVEN);
        PendingIntent given = PendingIntent.getBroadcast(context, r.id.hashCode() + 1, action, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(context, PurrfolioApp.CHANNEL_ID).setSmallIcon(R.drawable.ic_pets).setContentTitle(r.title + " · " + r.petName).setContentText(r.detail.isEmpty() ? "Time for " + r.petName + "'s care" : r.detail).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open).addAction(0, r.actionWord(), given).build();
        nm.notify(r.id.hashCode(), n);
    }
}
