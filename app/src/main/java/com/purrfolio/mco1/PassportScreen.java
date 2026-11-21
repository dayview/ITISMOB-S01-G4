package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
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
 * F5.7 Pet Passport: one read-only page with the pet's identity, health and emergency details.
 * Every row opens Edit Pet Profile, where the values are typed. Empty values say "Not added yet".
 * Nothing here leaves the device; the details are stored with the pet (see Pet and PetDraft).
 */
public class PassportScreen {
    private final Activity a;
    private final Pet pet;
    private final PetScreenHost host;
    private final PetUi ui;

    public PassportScreen(Activity a, Pet pet, PetScreenHost host) {
        this.a = a;
        this.pet = pet;
        this.host = host;
        this.ui = new PetUi(a);
    }

    public View build() {
        LinearLayout root = ui.screenRoot();

        LinearLayout top = ui.row();
        top.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        top.addView(ui.iconButton("Back", "Back", host::back));
        TextView title = ui.heading("Pet passport", 22f);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(ui.iconButton("Edit", "Edit " + pet.name + "'s profile", () -> host.go("Edit Pet Profile")));
        root.addView(top, ui.lp());

        PetUi.ScrollBody scrollBody = ui.scrollBody();
        LinearLayout body = scrollBody.body;
        root.addView(scrollBody.scroll);

        body.addView(identityCard(), ui.lpBottom(20));
        if (!hasPassportDetails()) body.addView(ui.text(
                "Add health and contact details so they're ready when you need them.", 15f, ui.sub),
                ui.lpBottom(16));

        LinearLayout health = section(body, "Health");
        infoRow(health, "Medical information", blankToNull(pet.medicalInfo), "Edit Pet Profile", "edit");
        infoRow(health, "Vaccination documents", "View saved documents", "Paperless Inbox", "open");

        LinearLayout contacts = section(body, "Care contacts");
        callRow(contacts, "Veterinarian", PetRules.vetLine(pet), blankToNull(pet.vetPhone),
                "Call " + (blankToNull(pet.vetClinic) == null ? "the veterinarian" : pet.vetClinic.trim()));
        callRow(contacts, "Emergency contact", PetRules.contactLine(pet), blankToNull(pet.emergencyContactPhone),
                "Call " + (blankToNull(pet.emergencyContactName) == null ? "the emergency contact"
                        : pet.emergencyContactName.trim()));

        LinearLayout identity = section(body, "Identification");
        infoRow(identity, "Microchip", blankToNull(pet.microchipId), "Edit Pet Profile", "edit");
        String weight = blankToNull(pet.weightKg);
        infoRow(identity, "Weight", weight == null ? null : weight + " kg", "Edit Pet Profile", "edit");
        body.addView(ui.button("Share passport", true, this::confirmShare), ui.lp(20, 8));

        LinearLayout.LayoutParams dockParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(78));
        dockParams.setMargins(ui.dp(8), 0, ui.dp(8), ui.dp(5));
        root.addView(host.dock("Pets"), dockParams);
        host.setBackHandler(null);
        return root;
    }

    // ---- pieces ----

    private View identityCard() {
        LinearLayout card = ui.row();
        card.setBackground(ui.rounded(ui.surface, 22, ui.outline));
        card.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        LinearLayout.LayoutParams photo = new LinearLayout.LayoutParams(ui.dp(76), ui.dp(76));
        photo.rightMargin = ui.dp(16);
        card.addView(ui.portrait(pet.photoUri, pet.species, 76, pet.name + "'s photo"), photo);

        LinearLayout labels = ui.column();
        labels.addView(ui.heading(pet.name, 26f), ui.lp());
        List<String> kindParts = new ArrayList<>();
        if (!pet.species.trim().isEmpty()) kindParts.add(pet.species);
        String age = PetRules.ageLabel(pet.birthday);
        if (age != null) kindParts.add(age);
        if (!kindParts.isEmpty()) labels.addView(ui.text(String.join(" · ", kindParts), 14f, ui.sub, 600), ui.lpTop(4));
        card.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private LinearLayout section(LinearLayout body, String title) {
        TextView heading = ui.heading(title, 18f);
        ui.markHeading(heading);
        body.addView(heading, ui.lp(12, 8));
        LinearLayout rows = ui.column();
        body.addView(rows, ui.lpBottom(8));
        return rows;
    }

    /** One tap stop: label, value, and a single spoken sentence for TalkBack. */
    private void infoRow(LinearLayout into, String label, String value, String target, String verb) {
        if (into.getChildCount() > 0) into.addView(divider());
        into.addView(rowView(label, value, target, verb), ui.lp());
    }

    /**
     * Like infoRow, plus a separate "Call" button when a phone number is saved.
     * The text still opens Edit; only the button opens the dialer (and the user presses call themselves).
     */
    private void callRow(LinearLayout into, String label, String value, String phone, String callDescription) {
        if (into.getChildCount() > 0) into.addView(divider());
        View text = rowView(label, value, "Edit Pet Profile", "edit");
        if (phone == null) {
            into.addView(text, ui.lp());
            return;
        }
        LinearLayout line = ui.row();
        line.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams callParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        callParams.rightMargin = ui.dp(8);
        line.addView(ui.textButton("Call", callDescription, () -> host.dial(phone)), callParams);
        into.addView(line, ui.lp());
    }

    private View rowView(String label, String value, String target, String verb) {
        LinearLayout line = ui.column();
        line.setMinimumHeight(ui.dp(56));
        line.setPadding(ui.dp(4), ui.dp(10), ui.dp(4), ui.dp(10));
        line.setFocusable(true);
        line.setClickable(true);
        line.setBackground(ui.interactiveBackground(Color.TRANSPARENT, 16));
        line.setOnClickListener(v -> host.go(target));
        line.setContentDescription(value == null
                ? label + ", not added yet. Double tap to add."
                : label + ", " + value + ". Double tap to " + verb + ".");
        if (value == null) {
            LinearLayout empty = ui.row();
            empty.addView(ui.text(label, 16f, ui.ink, 600),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            empty.addView(ui.text("Add", 14f, ui.primary, 700));
            line.addView(empty, ui.lp());
        } else {
            line.addView(ui.text(label, 13f, ui.sub, 600), ui.lp());
            line.addView(ui.text(value, 16f, ui.ink, 600), ui.lpTop(2));
        }
        ui.role(line, Button.class);
        return line;
    }

    private View divider() {
        View line = new View(a);
        line.setBackgroundColor(ui.outline);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(1)));
        line.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return line;
    }

    /** Sharing sends personal details out of the app, so always ask first and say exactly what happens. */
    private void confirmShare() {
        String summary = PetRules.passportSummary(pet);
        if (summary.isEmpty()) {
            new AlertDialog.Builder(a)
                    .setTitle("Nothing to share yet")
                    .setMessage("Add health, vet or emergency details first.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Add details", (dialog, which) -> host.go("Edit Pet Profile"))
                    .show();
            return;
        }
        new AlertDialog.Builder(a)
                .setTitle("Share " + pet.name.trim() + "'s passport?")
                .setMessage("This sends the details on this page as text to the app you choose. "
                        + "Purrfolio doesn't upload anything. Medical and contact details are personal, "
                        + "so share only with people you trust.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Share", (dialog, which) ->
                        host.shareText("Pet passport: " + pet.name.trim(), summary))
                .show();
    }

    // ---- values ----

    private boolean hasPassportDetails() {
        return blankToNull(pet.medicalInfo) != null || blankToNull(pet.vetClinic) != null
                || blankToNull(pet.vetPhone) != null || blankToNull(pet.emergencyContactName) != null
                || blankToNull(pet.emergencyContactPhone) != null || blankToNull(pet.microchipId) != null;
    }

    private static String blankToNull(String value) { return value.trim().isEmpty() ? null : value.trim(); }
}
