package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * F2.4 Manage Multiple Pets: all pets in one list -> the selected one clearly marked ->
 * tap a pet to switch to it and open it. In development (debuggable) builds, two demo pets are listed too.
 */
public class MyPetsScreen {
    private final Activity a;
    private final List<Pet> pets;
    private final String selectedId;
    private final PetScreenHost host;
    private final PetUi ui;

    public MyPetsScreen(Activity a, List<Pet> pets, String selectedId, PetScreenHost host) {
        this.a = a;
        this.pets = pets;
        this.selectedId = selectedId;
        this.host = host;
        this.ui = new PetUi(a);
    }

    public View build() {
        LinearLayout root = ui.screenRoot();

        LinearLayout top = ui.row();
        top.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        top.addView(ui.iconButton("Back", "Back", host::back));
        android.widget.TextView title = ui.heading("My Pets", 28f);
        title.setPadding(ui.dp(8), 0, 0, 0);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(ui.iconButton("Plus", "Add pet", () -> host.go("Add Your Pet")));
        root.addView(top, ui.lp());

        PetUi.ScrollBody scrollBody = ui.scrollBody();
        LinearLayout body = scrollBody.body;
        root.addView(scrollBody.scroll);

        if (pets.isEmpty()) {
            body.addView(ui.text("No pets yet", 20f, ui.primary, 600, true), ui.lpBottom(6));
            body.addView(ui.text("Add your first pet to start keeping their care in one place.", 15f, ui.sub),
                    ui.lpBottom(18));
        }
        for (int index = 0; index < pets.size(); index++) {
            Pet pet = pets.get(index);
            int position = index;
            body.addView(card(pet, ui.portrait(pet.photoUri, pet.species, 72, pet.name + "'s photo"),
                    pet.id.equals(selectedId), false, () -> host.selectPet(position)), ui.lpBottom(12));
        }

        body.addView(ui.button("Add another pet", () -> host.go("Add Your Pet")), ui.lp(4, 8));

        if (DemoPets.enabled(a)) {
            body.addView(ui.text("Demo pets (development builds only)", 15f, ui.sub, 700), ui.lp(16, 10));
            for (DemoPets.Demo demo : DemoPets.all()) {
                Pet pet = demo.pet;
                View portrait = ui.portrait("", pet.species, 72, pet.name + "'s demo photo", demo.portraitRes);
                body.addView(card(pet, portrait, false, true, () -> askToAdd(pet)), ui.lpBottom(12));
            }
        }

        LinearLayout.LayoutParams dockParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(78));
        dockParams.setMargins(ui.dp(8), 0, ui.dp(8), ui.dp(5));
        root.addView(host.dock("Pets"), dockParams);
        host.setBackHandler(null);
        return root;
    }

    private View card(Pet pet, View portrait, boolean selected, boolean demo, Runnable onClick) {
        String age = PetRules.ageLabel(pet.birthday);
        if (age == null) age = "Age not set";
        List<String> kindParts = new ArrayList<>();
        if (!pet.species.trim().isEmpty()) kindParts.add(pet.species);
        if (!pet.breed.trim().isEmpty()) kindParts.add(pet.breed);
        String kind = String.join(" · ", kindParts);

        LinearLayout card = ui.row();
        card.setMinimumHeight(ui.dp(96));
        card.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        card.setFocusable(true);
        card.setClickable(true);
        card.setBackground(ui.interactiveBackground(ui.surface, 22, selected ? ui.primary : ui.outline));
        card.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams portraitParams = new LinearLayout.LayoutParams(ui.dp(72), ui.dp(72));
        portraitParams.rightMargin = ui.dp(14);
        card.addView(portrait, portraitParams);

        LinearLayout labels = ui.column();
        labels.addView(ui.text(pet.name, 22f, ui.primary, 600, true), ui.lp());
        labels.addView(ui.text(kind, 14f, ui.sub), ui.lpTop(4));
        labels.addView(ui.text(age, 14f, ui.sub), ui.lpTop(2));
        if (selected) labels.addView(ui.text("Selected", 13f, ui.primary, 700), ui.lpTop(6));
        if (demo) labels.addView(ui.text("Demo", 13f, ui.sub, 700), ui.lpTop(6));
        card.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams chevron = new LinearLayout.LayoutParams(ui.dp(24), ui.dp(24));
        chevron.gravity = Gravity.CENTER_VERTICAL;
        card.addView(ui.icon("Chevron", 24), chevron);

        String state = selected ? ", selected pet" : demo ? ", demo pet" : "";
        card.setContentDescription(pet.name + ", " + kind + ", " + age + state);
        ui.role(card, Button.class);
        return card;
    }

    private void askToAdd(Pet pet) {
        new AlertDialog.Builder(a)
                .setTitle(pet.name + " (demo)")
                .setMessage("This is a sample pet for testing. Add a real copy to try switching between pets.")
                .setNegativeButton("Close", null)
                .setPositiveButton("Add to my pets", (dialog, which) -> host.addDemoPet(pet))
                .show();
    }
}
