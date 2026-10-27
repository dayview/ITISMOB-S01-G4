package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PetFormActivity extends AppCompatActivity {
    public static final String ONBOARDING = "onboarding";
    private static final String[] SPECIES = {"Cat", "Dog", "Rabbit", "Bird", "Other"};
    private static final String[] GENDERS = {"Male", "Female", "Uknown"};
    private EditText nameEt, breedEt, weightEt, aboutEt;
    private TextView titleTv, subtitleTv, linkTv, birthdayTv;
    private Spinner speciesSp, genderSp;
    private ImageView photoIv;
    private TextView photoHintTv;
    private MaterialButton saveBtn;
    private boolean onboarding;
    private String petId;
    private String birthday = "";
    private Uri photoUri;

    private final ActivityResultLauncher<String> pickPhoto = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri == null) return;
        photoUri = uri;
        Glide.with(this).load(uri).centerCrop().into(photoIv);
        photoHintTv.setText("Photo selected ✓ · tap to change");
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_pet_form);
        this.nameEt = findViewById(R.id.nameEt);
        this.breedEt = findViewById(R.id.breedEt);
        this.weightEt = findViewById(R.id.weightEt);
        this.aboutEt = findViewById(R.id.aboutEt);
        this.titleTv = findViewById(R.id.titleTv);
        this.subtitleTv = findViewById(R.id.subtitleTv);
        this.linkTv = findViewById(R.id.linkTv);
        this.birthdayTv = findViewById(R.id.birthdayTv);
        this.speciesSp = findViewById(R.id.speciesSp);
        this.genderSp = findViewById(R.id.genderSp);
        this.photoIv = findViewById(R.id.photoIv);
        this.photoHintTv = findViewById(R.id.photoHintTv);
        this.saveBtn = findViewById(R.id.saveBtn);

        Ui.spinner(speciesSp, SPECIES);
        Ui.spinner(genderSp, GENDERS);

        this.onboarding = getIntent().getBooleanExtra(ONBOARDING, false);
        this.petId = getIntent().getStringExtra(Ui.PET_ID);
        if (petId != null) {
            titleTv.setText("Edit Pet Profile");
            subtitleTv.setText("Update their details");
            saveBtn.setText("Save Changes");
            linkTv.setText("Remove");
            loadPet();
        } else if (onboarding) {
            linkTv.setText("Skip");
        } else {
            saveBtn.setText("Save Pet");
            linkTv.setVisibility(View.GONE);
        }

        photoIv.setOnClickListener(v -> pickPhoto.launch("image/*"));
        birthdayTv.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            Ui.pickDate(this, cal, () -> {
                birthday = Ui.ymd(cal);
                birthdayTv.setText(birthday);
            });
        });
        linkTv.setOnClickListener(v -> {
            if (petId == null) goMain();
            else confirmRemove();
        });
        saveBtn.setOnClickListener(v -> save());
    }

    private void loadPet() {
        Repo.pets().document(petId).get().addOnSuccessListener(d -> {
            Pet p = d.toObject(Pet.class);
            if (p == null) return;
            nameEt.setText(p.name);
            breedEt.setText(p.breed);
            weightEt.setText(p.weight);
            aboutEt.setText(p.about);
            speciesSp.setSelection(Math.max(0, Arrays.asList(SPECIES).indexOf(p.species)));
            genderSp.setSelection(Math.max(0, Arrays.asList(GENDERS).indexOf(p.gender)));
            birthday = p.birthday;
            if (!birthday.isEmpty()) birthdayTv.setText(birthday);
            if (!p.photoUrl.isEmpty()) Glide.with(this).load(p.photoUrl).centerCrop().into(photoIv);
            if (!p.ownerId.equals(Repo.uid())) linkTv.setVisibility(View.GONE);
        });
    }

    private void goMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void confirmRemove() {
        Ui.confirm(this, "Remove Pet", "Are you sure you want to remove this pet profile?", () -> {
            if (petId == null) return;
            Repo.pets().document(petId).delete().addOnSuccessListener(aVoid -> {
                Ui.toast(this, "Pet removed");
                if (petId.equals(Session.petId(this))) {
                    Session.clearPet(this);
                }
                finish();
            }).addOnFailureListener(e -> Ui.toast(this, "Failed to remove pet: " + e.getMessage()));
        });
    }

    private void save() {
        String name = Ui.text(nameEt);
        if (name.isEmpty()) {
            nameEt.setError("Pet name is required");
            return;
        }

        saveBtn.setEnabled(false);
        if (photoUri != null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference().child("pet_photos/" + System.currentTimeMillis() + ".jpg");
            ref.putFile(photoUri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> saveToFirestore(uri.toString())))
                    .addOnFailureListener(e -> {
                        saveBtn.setEnabled(true);
                        Ui.toast(this, "Failed to upload photo");
                    });
        } else {
            saveToFirestore(null);
        }
    }

    private void saveToFirestore(String photoUrl) {
        String name = Ui.text(nameEt);
        String breed = Ui.text(breedEt);
        String weight = Ui.text(weightEt);
        String about = Ui.text(aboutEt);
        String species = speciesSp.getSelectedItem().toString();
        String gender = genderSp.getSelectedItem().toString();

        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("species", species);
        m.put("breed", breed);
        m.put("gender", gender);
        m.put("weight", weight);
        m.put("about", about);
        m.put("birthday", birthday);
        if (photoUrl != null) {
            m.put("photoUrl", photoUrl);
        }

        if (petId != null) {
            Repo.pets().document(petId).update(m).addOnSuccessListener(aVoid -> {
                Ui.toast(this, "Pet updated");
                finish();
            }).addOnFailureListener(e -> {
                saveBtn.setEnabled(true);
                Ui.toast(this, "Failed to update pet: " + e.getMessage());
            });
        } else {
            String uid = Repo.uid();
            if (uid == null) {
                saveBtn.setEnabled(true);
                Ui.toast(this, "User is not logged in");
                return;
            }
            DocumentReference doc = Repo.pets().document();
            m.put("ownerId", uid);
            m.put("members", Collections.singletonList(uid));
            if (photoUrl == null) m.put("photoUrl", "");

            WriteBatch batch = Repo.db().batch();
            batch.set(doc, m);

            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("role", "Owner");
            memberMap.put("fullName", Repo.myName());
            memberMap.put("canView", true);
            memberMap.put("canLog", true);
            memberMap.put("canReminders", true);
            memberMap.put("canDocs", true);
            batch.set(doc.collection("circle").document(uid), memberMap);

            batch.commit().addOnSuccessListener(aVoid -> {
                Ui.toast(this, "Pet saved");
                Session.setPet(this, doc.getId(), name);
                if (onboarding) {
                    goMain();
                } else {
                    finish();
                }
            }).addOnFailureListener(e -> {
                saveBtn.setEnabled(true);
                Ui.toast(this, "Failed to save pet: " + e.getMessage());
            });
        }
    }
}

