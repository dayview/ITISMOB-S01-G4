package com.itismob.mco.purrfolio;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QuickLogActivity extends AppCompatActivity {
    public static final String TYPE = "type";
    public static final String LOG_ID = "logId";
    private static final String[] TYPES = {"Feeding", "Medication", "Water", "Playtime", "Vet Visit", "Grooming", "Other"};
    private TextView titleTv, subtitleTv, petTv, timeTv, promptTv, heardTv, confirmTitleTv;
    private Spinner typeSp;
    private EditText amountEt, noteEt;
    private MaterialSwitch finishedSw;
    private MaterialButton micBtn, saveBtn;
    private View voicePanel;
    private String petId, petName, logId;
    private String pendingField;
    private boolean timeSet;
    private final Calendar cal = Calendar.getInstance();
    private final Map<String, Pet> petByName = new HashMap<>();

    private final ActivityResultLauncher<Intent> speech = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
        ArrayList<String> heard = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (heard != null && !heard.isEmpty()) handleSpeech(heard.get(0));
    });

    protected boolean voiceMode() { return false; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_quick_log);
        this.petId = Session.petId(this);
        this.petName = Session.petName(this);
        this.logId = getIntent().getStringExtra(LOG_ID);
        this.titleTv = findViewById(R.id.titleTv);
        this.subtitleTv = findViewById(R.id.subtitleTv);
        this.petTv = findViewById(R.id.petTv);
        this.timeTv = findViewById(R.id.timeTv);
        this.promptTv = findViewById(R.id.promptTv);
        this.heardTv = findViewById(R.id.heardTv);
        this.confirmTitleTv = findViewById(R.id.confirmTitleTv);
        this.typeSp = findViewById(R.id.typeSp);
        this.amountEt = findViewById(R.id.amountEt);
        this.noteEt = findViewById(R.id.noteEt);
        this.finishedSw = findViewById(R.id.finishedSw);
        this.micBtn = findViewById(R.id.micBtn);
        this.saveBtn = findViewById(R.id.saveBtn);
        this.voicePanel = findViewById(R.id.voicePanel);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        petTv.setText("Pet - " + petName);
        titleTv.setText(logId != null ? "Edit Care Log" : voiceMode() ? "Voice Log" : "Log Care");
        subtitleTv.setText(voiceMode() ? "Just say what happened" : "Quickly add an activity for " + petName);
        voicePanel.setVisibility(voiceMode() ? View.VISIBLE : View.GONE);
        confirmTitleTv.setText(voiceMode() ? "Details (filled in as you speak)" : "Confirm details");

        if (voiceMode()) {
            String[] withHint = new String[TYPES.length + 1];
            withHint[0] = "Choose activity...";
            System.arraycopy(TYPES, 0, withHint, 1, TYPES.length);
            Ui.spinner(typeSp, withHint);
        } else {
            Ui.spinner(typeSp, TYPES);
        }

        timeSet = !voiceMode();
        refreshTime();
        timeTv.setOnClickListener(v -> Ui.pickDate(this, cal, () -> Ui.pickTime(this, cal, () -> { timeSet = true; refreshTime(); })));
        select(getIntent().getStringExtra(TYPE));
        if (logId != null) loadExisting();
        micBtn.setOnClickListener(v -> {
            pendingField = null;
            listen("Say what happened, e.g. “Fed " + petName + " one cup at 7:30 AM”");
        });
        saveBtn.setOnClickListener(v -> save());
        if (voiceMode()) {
            Repo.loadMyPets(list -> { for (Pet p : list) petByName.put(p.name.toLowerCase(Locale.ROOT), p); });
            if (savedInstanceState == null && logId == null) micBtn.performClick();
        }
    }

    private void refreshTime() {
        timeTv.setText(timeSet ? "Time - " + Ui.dateTime(cal.getTimeInMillis()) : "Time - tap to set");
    }

    private void select(String type) {
        if (type == null) return;
        for (int i = 0; i < typeSp.getCount(); i++) {
            if (typeSp.getItemAtPosition(i).toString().equals(type)) {
                typeSp.setSelection(i); return; }
        }
    }

    private String selectedType() {
        if (voiceMode() && typeSp.getSelectedItemPosition() == 0) return null;
        return typeSp.getSelectedItem().toString();
    }

    private boolean needsAmount(String type) { return type.equals("Feeding") || type.equals("Medication"); }
    private void loadExisting() {
        Repo.sub(petId, "logs").document(logId).get().addOnSuccessListener(d -> {
            select(d.getString("type"));
            amountEt.setText(Repo.str(d, "amount"));
            noteEt.setText(Repo.str(d, "note"));
            Long ts = d.getLong("timestamp");
            if (ts != null) { cal.setTimeInMillis(ts); refreshTime(); }
            Boolean f =  d.getBoolean("finished");
            finishedSw.setChecked(f == null || f);
        });
    }

    private void listen(String prompt) {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        try {
            speech.launch(i);
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "Speech recognition isn't available on this device. Fill in the fields below instead.");
        }
    }

    private void handleSpeech(String heard) {
        confirmTitleTv.setText("Cofirm details");
        heardTv.setText("Heard: “" + heard + "”");
        String s = heard.toLowerCase(Locale.getDefault());

        if (pendingField == null) {
            select(VoiceParser.detectType(s));
            String amount = VoiceParser.detectAmount(s);
            if (amount != null) amountEt.setText(amount);
            Long ts = VoiceParser.detectTime(s);
            if (ts != null) { cal.setTimeInMillis(ts); timeSet = true; refreshTime(); }
            for (Map.Entry<String, Pet> e : petByName.entrySet()) {
                if (s.contains(e.getKey())) {
                    petId = e.getValue().id;
                    petName = e.getValue().name;
                    petTv.setText("Pet - " + petName);
                }
            }
        } else {
            boolean ok = false;
            if (pendingField.equals("type")) {
                String t = VoiceParser.detectType(s);
                if (t != null) { select(t); ok = true; }
            } else if (pendingField.equals("amount")) {
                String a = VoiceParser.detectAmount(s);
                amountEt.setText(a != null ? a : heard);
                ok = true;
            } else {
                Long ts = VoiceParser.detectTime(s);
                if (ts != null) { cal.setTimeInMillis(ts); timeSet = true; refreshTime(); ok = true; }
            }
            if (!ok) {
                promptTv.setText("I didn't catch that. You can fill it in below.");
                pendingField = null;
                return;
            }
        }
        pendingField = null;
        askNextMissing();
    }

    private void askNextMissing() {
        String type = selectedType();
        if (type == null) { ask("type", "What activity was it? (feeding, medication, water...)"); return; }
        if (needsAmount(type) && Ui.text(amountEt).isEmpty()) { ask("time", "What time was it? (say “7:30 AM” or “just now”)"); return; }
        promptTv.setText("All set. Check the details, then tap Save Log.");
    }

    private void ask(String field, String question) {
        pendingField = field;
        promptTv.setText(question);
        listen(question);
    }

    private void save() {
        String type = selectedType();
        if (type == null) { Ui.toast(this, "Choose an activity first"); return; }
        String amount = Ui.text(amountEt);
        if (needsAmount(type) && amount.isEmpty()) { amountEt.setError("Amount needed"); return; }
        if (!timeSet) { Ui.toast(this, "Set the time first"); return; }
        long ts = cal.getTimeInMillis();
        String note = Ui.text(noteEt);
        boolean finished = finishedSw.isChecked();

        if (logId == null) {
            Repo.addLog(petId, petName, type, amount, note, ts, finished);
        } else {
            Repo.sub(petId, "logs").document(logId).update("type", type, "amount", amount, "note", note, "timestamp", ts, "finished", finished);
            Repo.activity(petId, "Edited " + type + " log", Ui.time(ts));
        }
        finish();
    }
}
