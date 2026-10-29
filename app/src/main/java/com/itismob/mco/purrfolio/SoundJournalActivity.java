package com.itismob.mco.purrfolio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SoundJournalActivity extends BaseListActivity {
    private static final String HINT = "Keep a record of unusual sounds to share with your veterinarian,";
    private static final String[] LABELS = {"Coughing", "Sneezing", "Unusual breathing", "Meowing (prolonged)", "Barking (prolonged)", "Other"};
    private MediaRecorder recorder;
    private MediaPlayer player;
    private File audioFile;
    private boolean recording;
    private long startedAt;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            long s = (System.currentTimeMillis() - startedAt) / 1000;
            bannerTv.setText(String.format(Locale.US, "● Recording - %02d:%02d", s / 60, s % 60));
            handler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String> micPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) startRecording();
        else Ui.toast(this, "Microphone permission is needed to record sounds");
    });

    @Override
    protected void onScreenCreated() {
        titleTv.setText("Sound Event Journal");
        subtitleTv.setText("Record unusual sounds or behaviors");
        bannerTv.setText(HINT);
        show(bannerTv, true);
        actionBtn.setText("● Start Recording");
        show(actionBtn, true);
        actionBtn.setOnClickListener(v -> {
            if (recording) stopAndSave();
            else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording();
            else micPermission.launch(Manifest.permission.RECORD_AUDIO);
        });
    }

    private void startRecording() {
        audioFile = new File(getCacheDir(), "sound_" + System.currentTimeMillis() + ".m4a");
        recorder = Build.VERSION.SDK_INT >= 31 ? new MediaRecorder(this) : new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(audioFile.getAbsolutePath());
        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException | RuntimeException e) {
            Ui.toast(this, "Could not start recording");
            recorder.release();
            recorder = null;
            return;
        }
        recording = true;
        startedAt = System.currentTimeMillis();
        actionBtn.setText("⏺️ Stop & Save");
        handler.post(ticker);
    }

    private void stopAndSave() {
        handler.removeCallbacks(ticker);
        boolean ok = true;
        try { recorder.stop(); } catch (RuntimeException e) { ok = false; }
        recorder.release();
        recorder = null;
        recording = false;
        actionBtn.setText("● Start Recording");
        bannerTv.setText(HINT);
        if (!ok) { Ui.toast(this, "That recording was too short"); return; }

        new AlertDialog.Builder(this).setTitle("What did you hear?").setItems(LABELS, (d, which) -> upload(LABELS[which])).show();
    }

    private void upload(String label) {
        String pid = petId();
        long ts = System.currentTimeMillis();
        Context app = getApplicationContext();
        StorageReference ref = FirebaseStorage.getInstance().getReference("pets/" + pid + "/sounds/" + ts + ".m4a");
        Ui.toast(app, "Saving recording...");
        ref.putFile(Uri.fromFile(audioFile)).addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(url -> {
            Map<String, Object> m = new HashMap<>();
            m.put("label", label);
            m.put("timestamp", ts);
            m.put("url", url.toString());
            m.put("authorName", Repo.myName());
            Repo.sub(pid, "sounds").add(m);
            Repo.activity(pid, "Recorded sound", label);
            Ui.toast(app, "Recording saved");
            if (!isFinishing()) load();
        })).addOnFailureListener(e -> Ui.toast(app, "Upload failed: " + e.getMessage()));
    }

    @Override
    protected void load() {
        Repo.sub(petId(), "sounds").orderBy("timestamp", Query.Direction.DESCENDING).get().addOnSuccessListener(snap -> {
            List<Row> out = new ArrayList<>();
            out.add(Row.header("Recent Recordings (" + snap.size() + ")"));
            for (DocumentSnapshot d : snap) {
                Long ts = d.getLong("timestamp");
                Row r = new Row(d.getId(), Repo.str(d, "label"), Ui.dateTime(ts == null ? 0 : ts), "▶️");
                r.extra = Repo.str(d, "url");
                out.add(r);
            }
            showRows(out);
        });
    }

    @Override
    protected void onRowClick(Row row) {
        if (row.extra == null || row.extra.isEmpty()) return;
        if (player != null) player.release();
        player = new MediaPlayer();
        try {
            player.setDataSource(row.extra);
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(MediaPlayer::release);
            player.prepareAsync();
        } catch (IOException e) {
            Ui.toast(this, "Could not play this recording");
        }
    }

    @Override
    protected void onRowLongClick(Row row) {
        Ui.confirm(this, "Delete recording?", row.title + " · " + row.subtitle, () -> {
            Repo.sub(petId(), "sounds").document(row.id).delete();
            Repo.activity(petId(), "Delete sound recording", row.title);
            rows.remove(row);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(ticker);
        if (player != null) player.release();
        if (recorder != null) {
            try { recorder.stop(); } catch (RuntimeException ignored) { }
            recorder.release();
        }
    }
}
