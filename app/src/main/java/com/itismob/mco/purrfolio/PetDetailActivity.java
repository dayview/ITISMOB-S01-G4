package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class PetDetailActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent i = new Intent(this, PetFormActivity.class);
        i.putExtra(Ui.PET_ID, getIntent().getStringExtra(Ui.PET_ID));
        startActivity(i);
        finish();
    }
}
