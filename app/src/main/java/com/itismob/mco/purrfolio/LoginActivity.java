package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEt, passwordEt;
    private MaterialButton loginBtn;
    private TextView loginSubtitleTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_login);

        this.emailEt = findViewById(R.id.emailEt);
        this.passwordEt = findViewById(R.id.passwordEt);
        this.loginBtn = findViewById(R.id.loginBtn);
        this.loginSubtitleTv = findViewById(R.id.loginSubtitleTv);
        this.loginBtn.setOnClickListener(v -> login());
        findViewById(R.id.forgotTv).setOnClickListener(v -> startActivity(new Intent(this, ResetPasswordActivity.class)));
        findViewById(R.id.createTv).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
        findViewById(R.id.googleBtn).setOnClickListener(v -> Ui.toast(this, "Google sign-in is not available yet"));
        findViewById(R.id.facebookBtn).setOnClickListener(v -> Ui.toast(this, "Facebook sign-in is not available yet"));
    }

    private void login() {
        String email = Ui.text(emailEt);
        String pw = passwordEt.getText().toString();

        if (email.isEmpty() || pw.isEmpty()) {
            Ui.toast(this, "Enter your email and password");
            return;
        }

        loginBtn.setEnabled(false);
        Repo.auth().signInWithEmailAndPassword(email, pw).addOnSuccessListener(result -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }).addOnFailureListener(e -> {
            loginBtn.setEnabled(true);
            loginSubtitleTv.setText("Log in failed: " + e.getMessage());
            loginSubtitleTv.setTextColor(ContextCompat.getColor(this, R.color.purr_danger));
        });
    }
}

