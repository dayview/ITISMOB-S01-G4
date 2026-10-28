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
            if (!Repo.uid().equals(p.ownerId)) linkTv.setVisibility(View.GONE);
        });
    }

    private void save() {
        String name = Ui.text(nameEt);
        if (name.isEmpty()) {
            nameEt.setError("Pet name is required");
            return;
        }

        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("species", speciesSp.getSelectedItem().toString());
        m.put("breed", Ui.text(breedEt));
        m.put("birthday", birthday);
        m.put("weight", Ui.text(weightEt));
        m.put("gender", genderSp.getSelectedItem().toString());
        m.put("about", Ui.text(aboutEt));

        String id;
        if (petId == null) {
            DocumentReference ref = Repo.pets().document();
            id = ref.getId();
            m.put("ownerId", Repo.uid());
            m.put("members", Arrays.asList(Repo.uid()));
            m.put("createdAt", System.currentTimeMillis());

            Map<String, Object> me = new HashMap<>();
            me.put("uid", Repo.uid());
            me.put("name", Repo.myName());
            me.put("email", Repo.auth().getCurrentUser().getEmail());
            me.put("role", "Owner");
            me.put("canView", true);
            me.put("canLog", true);
            me.put("canReminders", true);
            me.put("canDocs", true);

            WriteBatch batch = Repo.db().batch();
            batch.set(ref, m);
            batch.set(ref.collection("circle").document(Repo.uid()), me);
            batch.commit();
            Session.setPet(this, id, name);
        } else {
            id = petId;
            Repo.pets().document(id).update(m);
            if (id.equals(Session.petId(this))) Session.setPet(this, id, name);
            Repo.activity(id, "Edited pet profile", name);
        }
        if (photoUri != null) uploadPhoto(id, photoUri);
        if (onboarding) goMain();
        else finish();
    }

    private void uploadPhoto(String id, Uri uri) {
        StorageReference ref = FirebaseStorage.getInstance().getReference("pets/" + id + "/profile.jpg");
        ref.putFile(uri).addOnSuccessListener(t ->
                ref.getDownloadUrl().addOnSuccessListener(url ->
                        Repo.pets().document(id).update("photoUrl", url.toString())));
    }

    private void confirmRemove() {
        Ui.confirm(this, "Remove pet?", "This removes the profile for everyone in the Care Circle.", () -> {
            Repo.pets().document(petId).delete();
            Session.clearPet(this);
            goMain();
        });
    }

    private void goMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
