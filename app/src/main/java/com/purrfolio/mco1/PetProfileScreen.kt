package com.purrfolio.mco1

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * F2.2 View Pet Profile: the pet's own information -> plus shortcuts to its care history ->
 * reminders, records, passport, and care circle. Empty values are shown as "Not added yet" and open Edit.
 */
class PetProfileScreen(
    private val a: Activity,
    private val pet: Pet,
    private val host: PetScreenHost
) {
    private val ui = PetUi(a)

    fun build(): View {
        val root = ui.screenRoot()

        val top = ui.row().apply { setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4)) }
        top.addView(ui.iconButton("Back", "Back") { host.back() })
        top.addView(ui.heading("Pet profile", 22f).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(ui.iconButton("Edit", "Edit ${pet.name}'s profile") { host.go("Edit Pet Profile") })
        root.addView(top, ui.lp())

        val (scroll, body) = ui.scrollBody()
        root.addView(scroll)

        val hero = ui.column().apply { gravity = Gravity.CENTER_HORIZONTAL }
        hero.addView(ui.portrait(pet.photoUri, pet.species, 120, "${pet.name}'s photo"), LinearLayout.LayoutParams(ui.dp(120), ui.dp(120)))
        hero.addView(ui.heading(pet.name, 30f).apply { gravity = Gravity.CENTER }, ui.lp(topDp = 12))
        val kind = listOf(pet.species, pet.breed).filter { it.isNotBlank() }.joinToString(" · ")
        hero.addView(ui.text(kind, 16f, ui.sub, 600).apply { gravity = Gravity.CENTER }, ui.lp(topDp = 4))
        body.addView(hero, ui.lp(bottomDp = 20))

        val info = ui.column().apply {
            background = ui.rounded(ui.surface, 20, ui.outline)
            setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4))
        }
        val age = PetRules.ageLabel(pet.birthday)
        val birthday = if (pet.birthday.isBlank()) null else
            PetRules.displayDate(pet.birthday) + (age?.let { " · $it" } ?: "")
        infoRow(info, "Birthday", birthday)
        infoRow(info, "Sex", pet.sex.ifBlank { null })
        infoRow(info, "Weight", pet.weightKg.ifBlank { null }?.let { "$it kg" })
        infoRow(info, "Notes", pet.notes.ifBlank { null })
        body.addView(info, ui.lp(bottomDp = 22))

        body.addView(ui.heading("Care and records", 20f).apply { }, ui.lp(bottomDp = 8))
        val links = ui.column().apply { background = ui.rounded(ui.surface, 20, ui.outline) }
        listOf(
            "Care history" to "Care History",
            "Reminders" to "Reminders",
            "Documents and records" to "Records",
            "Pet passport" to "Pet Passport",
            "Care circle" to "Care Circle"
        ).forEach { (label, target) -> linkRow(links, label, target) }
        body.addView(links, ui.lp(bottomDp = 20))

        body.addView(ui.button("Edit profile") { host.go("Edit Pet Profile") }, ui.lp(bottomDp = 8))

        root.addView(host.dock("Pets"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(78)).apply {
            setMargins(ui.dp(8), 0, ui.dp(8), ui.dp(5))
        })
        host.setBackHandler(null)
        return root
    }

    private fun infoRow(into: LinearLayout, label: String, value: String?) {
        val line = ui.column().apply {
            minimumHeight = ui.dp(56)
            setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10))
            isFocusable = true
            isClickable = true
            background = ui.interactiveBackground(android.graphics.Color.TRANSPARENT, 16)
            setOnClickListener { host.go("Edit Pet Profile") }
            contentDescription = if (value == null) "$label, not added yet. Double tap to add." else "$label, $value. Double tap to edit."
        }
        line.addView(ui.text(label, 13f, ui.sub, 600), ui.lp())
        line.addView(ui.text(value ?: "Not added yet", 16f, if (value == null) ui.sub else ui.ink, 600), ui.lp(topDp = 2))
        ui.role(line, android.widget.Button::class.java)
        into.addView(line, ui.lp())
    }

    private fun linkRow(into: LinearLayout, label: String, target: String) {
        val line = ui.row().apply {
            minimumHeight = ui.dp(56)
            setPadding(ui.dp(14), ui.dp(8), ui.dp(14), ui.dp(8))
            isFocusable = true
            isClickable = true
            background = ui.interactiveBackground(android.graphics.Color.TRANSPARENT, 16)
            setOnClickListener { host.go(target) }
            contentDescription = label
        }
        line.addView(ui.text(label, 16f, ui.ink, 700), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        line.addView(ui.icon("Chevron", 22), LinearLayout.LayoutParams(ui.dp(22), ui.dp(22)))
        ui.role(line, android.widget.Button::class.java)
        into.addView(line, ui.lp())
    }
}
