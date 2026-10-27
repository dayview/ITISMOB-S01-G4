package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText fullNameEt, emailEt, passwordEt;
    private TextView ruleLenTv, ruleMixTv, ruleSpecialTv;
    private MaterialButton continueBtn;
    private TextView registerSubtitleTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_register);
        this.fullNameEt = findViewById(R.id.fullNameEt);
        this.emailEt = findViewById(R.id.emailEt);
        this.passwordEt = findViewById(R.id.passwordEt);
        this.ruleLenTv = findViewById(R.id.ruleLenTv);
        this.ruleMixTv = findViewById(R.id.ruleMixTv);
        this.ruleSpecialTv = findViewById(R.id.ruleSpecialTv);
        this.continueBtn = findViewById(R.id.continueBtn);
        this.registerSubtitleTv = findViewById(R.id.registerSubtitleTv);

        updateRules("");
        this.passwordEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { updateRules(s.toString()); }
        });

        this.continueBtn.setOnClickListener(v -> register());
        findViewById(R.id.googleBtn).setOnClickListener(v -> Ui.toast(this, "Google sign-in is not available yet"));
        findViewById(R.id.facebookBtn).setOnClickListener(v -> Ui.toast(this, "Facebook sign-in is not available yet"));
        findViewById(R.id.loginLinkTv).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    // show error message
    private void showError(String msg) {
        registerSubtitleTv.setText(msg);
        registerSubtitleTv.setTextColor(ContextCompat.getColor(this, R.color.purr_danger));
    }

    private boolean lenOk(String p) { return p.length() >= 8; }
    private boolean mixOk(String p) { return p.matches(".*[A-Za-z].*") && p.matches(".*\\d.*"); }
    private boolean specialOk(String p) { return p.matches(".*[^A-Za-z0-9].*"); }
    private void updateRules(String p) {
        setRule(ruleLenTv, "At least 8 characters", lenOk(p));
        setRule(ruleMixTv, "1 letter and 1 number", mixOk(p));
        setRule(ruleSpecialTv, "1 special character", specialOk(p));
    }
    private void setRule(TextView tv, String label, boolean ok) {
        tv.setText((ok ? "✓ " : "○ ") + label);
        tv.setTextColor(ContextCompat.getColor(this, ok ? R.color.purr_primary : R.color.purr_subtle));
    }

    private void register() {
        String name = Ui.text(fullNameEt);
        String email = Ui.text(emailEt);
        String pw = passwordEt.getText().toString();

        if (name.isEmpty() || email.isEmpty()) {
            showError("Please enter your name and email");
            return;
        }
        if (!lenOk(pw) || !mixOk(pw) || !specialOk(pw)) {
            showError("Password does not match all requirements");
            return;
        }

        continueBtn.setEnabled(false);
        Repo.auth().createUserWithEmailAndPassword(email, pw).addOnSuccessListener(result -> {
            FirebaseUser user = result.getUser();
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());
            Map<String, Object> doc = new HashMap<>();
            doc.put("fullName", name);
            doc.put("email", email.toLowerCase());
            Repo.db().collection("users").document(user.getUid()).set(doc);

            Intent i = new Intent(this, PetFormActivity.class);
            i.putExtra(PetFormActivity.ONBOARDING, true);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }).addOnFailureListener(e -> {
            continueBtn.setEnabled(true);
            showError(e.getMessage());
        });
    }
}

