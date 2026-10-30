package com.itismob.mco.purrfolio;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.SetOptions;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class PersonalInfoActivity extends AppCompatActivity {
    private EditText nameEt, emailEt, phoneEt, countryEt;
    private TextView dobTv;
    private String dob = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_personal_info);
        nameEt = findViewById(R.id.nameEt);
        emailEt = findViewById(R.id.emailEt);
        phoneEt = findViewById(R.id.phoneEt);
        countryEt = findViewById(R.id.countryEt);
        dobTv = findViewById(R.id.dobTv);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        dobTv.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            Ui.pickDate(this, cal, () -> {
                dob = Ui.ymd(cal);
                dobTv.setText(dob);
            });
        });

        findViewById(R.id.saveBtn).setOnClickListener(v -> save());
        emailEt.setText(Repo.auth().getCurrentUser().getEmail());
        Repo.db().collection("users").document(Repo.uid()).get().addOnSuccessListener(d -> {
            nameEt.setText(Repo.str(d, "fullName"));
            phoneEt.setText(Repo.str(d, "phone"));
            countryEt.setText(Repo.str(d, "country"));
            dob = Repo.str(d, "dob");
            if (!dob.isEmpty()) dobTv.setText(dob);
        });
    }

    private void save() {
        String name = Ui.text(nameEt);
        if (name.isEmpty()) { nameEt.setError("Name is required"); return; }
        Map<String, Object> m = new HashMap<>();
        m.put("fullName", name);
        m.put("phone", Ui.text(phoneEt));
        m.put("dob", dob);
        m.put("country", Ui.text(countryEt));
        Repo.db().collection("users").document(Repo.uid()).set(m, SetOptions.merge());
        Repo.auth().getCurrentUser().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());

        Ui.toast(this, "Saved");
        finish();
    }
}