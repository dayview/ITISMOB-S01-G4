package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Repo.uid() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        Ui.screen(this, R.layout.activity_welcome);

        android.widget.TextView appNameTv = findViewById(R.id.appNameTv);
        appNameTv.setText(R.string.app_name);
        findViewById(R.id.getStartedBtn).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.loginBtn).setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }
}

