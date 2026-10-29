package com.itismob.mco.purrfolio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LogFragment extends Fragment {
    private static final String[] TYPES = {"Feeding", "Medication", "Water", "Playtime", "Vet Visit", "Grooming"};
    private static final int[] IDS = {R.id.feedingBtn, R.id.medicationBtn, R.id.waterBtn, R.id.playtimeBtn, R.id.vetBtn, R.id.groomingBtn};
    private TextView subtitleTv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.subtitleTv = view.findViewById(R.id.subtitleTv);
        for (int i = 0; i < TYPES.length; i++) {
            String type = TYPES[i];
            view.findViewById(IDS[i]).setOnClickListener(v -> open(QuickLogActivity.class, type));
        }
        view.findViewById(R.id.voiceBtn).setOnClickListener(v -> open(VoiceLogActivity.class, null));
        view.findViewById(R.id.soundBtn).setOnClickListener(v -> open(SoundJournalActivity.class, null));
    }

    @Override
    public void onResume() {
        super.onResume();
        subtitleTv.setText(Session.petId(requireContext()) == null ? "Add a pet first" : "Quickly add an activity for " + Session.petName(requireContext()));
    }

    private void open(Class<?> cls, String type) {
        String petId = Session.petId(requireContext());
        if (petId == null) {
            Ui.toast(requireContext(), "Add or select a pet first");
            return;
        }
        Repo.can(petId, "canLog", ok -> {
            if (!isAdded()) return;
            if (!ok) {
                Ui.toast(requireContext(), "You don't have permission to add care logs to for this pet");
                return;
            }
            Intent i = new Intent(requireContext(), cls);
            if (type != null) i.putExtra(QuickLogActivity.TYPE, type);
            startActivity(i);
        });
    }
}
