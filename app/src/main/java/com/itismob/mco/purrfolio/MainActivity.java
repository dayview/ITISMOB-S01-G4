package com.itismob.mco.purrfolio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private final ActivityResultLauncher<String> notifPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_main);

        this.bottomNav = findViewById(R.id.bottomNav);
        this.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if (id == R.id.nav_home) f = new HomeFragment();
            else if (id == R.id.nav_pets) f = new PetsFragment();
            else if (id == R.id.nav_log) f = new LogFragment();
            else if (id == R.id.nav_care) f = new CareFragment();
            else f = new MoreFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, f).commit();
            return true;
        });

        if (savedInstanceState == null) this.bottomNav.setSelectedItemId(R.id.nav_home);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        ensureCurrentPet();
        ReminderScheduler.syncAll(this);
    }

    private void ensureCurrentPet() {
        Repo.loadMyPets(list -> {
            String current = Session.petId(this);
            boolean valid = false;
            for (Pet p : list) if (p.id.equals(current)) valid = true;
            if (valid) return;
            if (list.isEmpty()) Session.clearPet(this);
            else Session.setPet(this, list.get(0).id, list.get(0).name);
        });
    }
}




