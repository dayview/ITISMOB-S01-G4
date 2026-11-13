package com.purrfolio.mco1;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * F2.2 View Pet Profile: the pet's own information, plus shortcuts to its care history,
 * reminders, records, passport, and care circle. Empty values are shown as "Not added yet" and open Edit.
 */
public class PetProfileScreen {
    private final Pet pet;
    private final PetScreenHost host;
    private final PetUi ui;

    public PetProfileScreen(Activity a, Pet pet, PetScreenHost host) {
        this.pet = pet;
        this.host = host;
        this.ui = new PetUi(a);
    }

    public View build() {
        LinearLayout root = ui.screenRoot();

        LinearLayout top = ui.row();
        top.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        top.addView(ui.iconButton("Back", "Back", host::back));
        TextView title = ui.heading("Pet profile", 22f);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(ui.iconButton("Edit", "Edit " + pet.name + "'s profile", () -> host.go("Edit Pet Profile")));
        root.addView(top, ui.lp());

        PetUi.ScrollBody scrollBody = ui.scrollBody();
        LinearLayout body = scrollBody.body;
        root.addView(scrollBody.scroll);

        LinearLayout hero = ui.column();
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(ui.portrait(pet.photoUri, pet.species, 120, pet.name + "'s photo"),
                new LinearLayout.LayoutParams(ui.dp(120), ui.dp(120)));
        TextView name = ui.heading(pet.name, 30f);
        name.setGravity(Gravity.CENTER);
        hero.addView(name, ui.lpTop(12));
        List<String> kindParts = new ArrayList<>();
        if (!pet.species.trim().isEmpty()) kindParts.add(pet.species);
        if (!pet.breed.trim().isEmpty()) kindParts.add(pet.breed);
        TextView kind = ui.text(String.join(" · ", kindParts), 16f, ui.sub, 600);
        kind.setGravity(Gravity.CENTER);
        hero.addView(kind, ui.lpTop(4));
        body.addView(hero, ui.lpBottom(20));

        LinearLayout info = ui.column();
        info.setBackground(ui.rounded(ui.surface, 20, ui.outline));
        info.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        String age = PetRules.ageLabel(pet.birthday);
        String birthday = pet.birthday.trim().isEmpty() ? null
                : PetRules.displayDate(pet.birthday) + (age == null ? "" : " · " + age);
        infoRow(info, "Birthday", birthday);
        infoRow(info, "Sex", blankToNull(pet.sex));
        String weight = blankToNull(pet.weightKg);
        infoRow(info, "Weight", weight == null ? null : weight + " kg");
        infoRow(info, "Notes", blankToNull(pet.notes));
        body.addView(info, ui.lpBottom(22));

        body.addView(ui.heading("Care and records", 20f), ui.lpBottom(8));
        LinearLayout links = ui.column();
        links.setBackground(ui.rounded(ui.surface, 20, ui.outline));
        String[][] entries = {
                {"Care history", "Care History"},
                {"Reminders", "Reminders"},
                {"Documents and records", "Records"},
                {"Pet passport", "Pet Passport"},
                {"Care circle", "Care Circle"}};
        for (String[] entry : entries) linkRow(links, entry[0], entry[1]);
        body.addView(links, ui.lpBottom(20));

        body.addView(ui.button("Edit profile", () -> host.go("Edit Pet Profile")), ui.lpBottom(8));

        LinearLayout.LayoutParams dockParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(78));
        dockParams.setMargins(ui.dp(8), 0, ui.dp(8), ui.dp(5));
        root.addView(host.dock("Pets"), dockParams);
        host.setBackHandler(null);
        return root;
    }

    private static String blankToNull(String value) { return value.trim().isEmpty() ? null : value; }

    private void infoRow(LinearLayout into, String label, String value) {
        LinearLayout line = ui.column();
        line.setMinimumHeight(ui.dp(56));
        line.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10));
        line.setFocusable(true);
        line.setClickable(true);
        line.setBackground(ui.interactiveBackground(Color.TRANSPARENT, 16));
        line.setOnClickListener(v -> host.go("Edit Pet Profile"));
        line.setContentDescription(value == null ? label + ", not added yet. Double tap to add."
                : label + ", " + value + ". Double tap to edit.");
        line.addView(ui.text(label, 13f, ui.sub, 600), ui.lp());
        line.addView(ui.text(value == null ? "Not added yet" : value, 16f, value == null ? ui.sub : ui.ink, 600), ui.lpTop(2));
        ui.role(line, Button.class);
        into.addView(line, ui.lp());
    }

    private void linkRow(LinearLayout into, String label, String target) {
        LinearLayout line = ui.row();
        line.setMinimumHeight(ui.dp(56));
        line.setPadding(ui.dp(14), ui.dp(8), ui.dp(14), ui.dp(8));
        line.setFocusable(true);
        line.setClickable(true);
        line.setBackground(ui.interactiveBackground(Color.TRANSPARENT, 16));
        line.setOnClickListener(v -> host.go(target));
        line.setContentDescription(label);
        line.addView(ui.text(label, 16f, ui.ink, 700), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(ui.icon("Chevron", 22), new LinearLayout.LayoutParams(ui.dp(22), ui.dp(22)));
        ui.role(line, Button.class);
        into.addView(line, ui.lp());
    }
}
