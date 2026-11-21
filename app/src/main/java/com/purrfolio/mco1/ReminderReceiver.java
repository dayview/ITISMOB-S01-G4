package com.purrfolio.mco1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Reminder r = ReminderScheduler.fromIntent(intent);
        if (r.id.isEmpty()) return;
        if (ReminderScheduler.ACTION_GIVEN.equals(intent.getAction())) {
            PendingResult pending = goAsync();
            ReminderManager.markGiven(context.getApplicationContext(), r);
            NotificationHelper.cancel(context, r);
            new Handler(Looper.getMainLooper()).postDelayed(pending::finish, 3000);
            return;
        }

        if (r.repeatDaily) ReminderScheduler.schedule(context, r);   // queue tomorrow’s alarm
        NotificationHelper.show(context, r);
    }
}
