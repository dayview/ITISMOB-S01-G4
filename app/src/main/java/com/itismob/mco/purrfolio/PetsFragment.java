package com.itismob.mco.purrfolio;

import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

public class PetsFragment extends BaseListFragment {
    @Override
    protected void onReady() {
        titleTv.setText("My Pets");
        actionBtn.setText("+ Add Another Pet");
        actionBtn.setVisibility(android.view.View.VISIBLE);
        actionBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), PetFormActivity.class)));
    }

    @Override
    protected void load() {
        Repo.loadMyPets(pets -> {
            if (!isAdded()) return;
            List<Row> out = new ArrayList<>();
            for (Pet p : pets) {
                boolean current = p.id.equals(Session.petId(requireContext()));
                out.add(new Row(p.id, p.name, p.summary(), current ? "Current ›" : "›"));
            }
            subtitleTv.setText(pets.isEmpty() ? "Add your first pet to get started" : pets.size() + (pets.size() == 1 ? " pet" : " pets") + " in your care");
            showRows(out);
        });
    }

    @Override
    protected void onRowClick(Row row) {
        Session.setPet(requireContext(), row.id, row.title);
        Intent i = new Intent(requireContext(), PetDetailActivity.class);
        i.putExtra(Ui.PET_ID, row.id);
        startActivity(i);
    }
}

