package com.itismob.mco.purrfolio;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.appcompat.app.AppCompatDelegate;

public class PurrfolioApp extends Application {
    public static final String CHANNEL_ID = "purrfolio_care";

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Session.getThemeMode(this));
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Care Reminders", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Medication, feeding, vaccination and vet reminders");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}

