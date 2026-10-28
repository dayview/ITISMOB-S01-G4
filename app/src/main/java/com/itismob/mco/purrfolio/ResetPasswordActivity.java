package com.itismob.mco.purrfolio;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ResetPasswordActivity extends AppCompatActivity {
    private TextView resetSubtitleTv, resetTipsTitleTv, resetTipsTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_reset_password);

        resetSubtitleTv = findViewById(R.id.resetSubtitleTv);
        resetTipsTitleTv = findViewById(R.id.resetTipsTitleTv);
        resetTipsTv = findViewById(R.id.resetTipsTv);
        EditText emailEt = findViewById(R.id.emailEt);
        findViewById(R.id.sendBtn).setOnClickListener(v -> {
            String email = Ui.text(emailEt);
            if (email.isEmpty()) {
                Ui.toast(this, "Enter your email address");
                return;
            }
            Repo.auth().sendPasswordResetEmail(email).addOnSuccessListener(r -> showSent(email)).addOnFailureListener(e -> Ui.toast(this, e.getMessage()));
        });
    }

    private void showSent(String email) {
        resetSubtitleTv.setText("Link sent to " + email);
        resetSubtitleTv.setTextColor(ContextCompat.getColor(this, R.color.purr_primary));
        resetTipsTitleTv.setText("Check your inbox");
        resetTipsTv.setText("✓ It can take a few minutes\n✓Look in your spam folder\n✓ Open the link, set a new password, then log in");
    }
}
