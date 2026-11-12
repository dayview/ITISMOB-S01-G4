package com.purrfolio.mco1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicBoolean;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        PendingResult pending = goAsync();
        AtomicBoolean finished = new AtomicBoolean(false);
        Runnable finish = () -> { if (finished.compareAndSet(false, true)) pending.finish(); };

        ReminderManager.syncAlarms(context.getApplicationContext(), finish);
        new Handler(Looper.getMainLooper()).postDelayed(finish, 8000);
    }
}
