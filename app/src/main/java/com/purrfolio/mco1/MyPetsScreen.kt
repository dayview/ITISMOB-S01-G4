package com.purrfolio.mco1

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * F2.4 Manage Multiple Pets: all pets in one list -> the selected one clearly marked ->
 * -> tap a pet to switch to it and open it. In development (debuggable) builds, two demo pets are listed too.
 */
class MyPetsScreen(
    private val a: Activity,
    private val pets: List<Pet>,
    private val selectedId: String,
    private val host: PetScreenHost
) {
    private val ui = PetUi(a)

    fun build(): View {
        val root = ui.screenRoot()

        val top = ui.row().apply { setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4)) }
        top.addView(ui.iconButton("Back", "Back") { host.back() })
        top.addView(ui.heading("My Pets", 28f).apply { setPadding(ui.dp(8), 0, 0, 0) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(ui.iconButton("Plus", "Add pet") { host.go("Add Your Pet") })
        root.addView(top, ui.lp())

        val (scroll, body) = ui.scrollBody()
        root.addView(scroll)

        if (pets.isEmpty()) {
            body.addView(ui.text("No pets yet", 20f, ui.primary, 600, true), ui.lp(bottomDp = 6))
            body.addView(ui.text("Add your first pet to start keeping their care in one place.", 15f, ui.sub), ui.lp(bottomDp = 18))
        }
        pets.forEachIndexed { index, pet ->
            body.addView(card(pet, ui.portrait(pet.photoUri, pet.species, 72, "${pet.name}'s photo"),
                pet.id == selectedId, demo = false) { host.selectPet(index) }, ui.lp(bottomDp = 12))
        }

        body.addView(ui.button("Add another pet") { host.go("Add Your Pet") }, ui.lp(topDp = 4, bottomDp = 8))

        if (DemoPets.enabled(a)) {
            body.addView(ui.text("Demo pets (development builds only)", 15f, ui.sub, 700), ui.lp(topDp = 16, bottomDp = 10))
            DemoPets.all().forEach { demo ->
                val pet = demo.pet
                val portrait = ui.portrait("", pet.species, 72, "${pet.name}'s demo photo", demo.portraitRes)
                body.addView(card(pet, portrait, selected = false, demo = true) { askToAdd(pet) }, ui.lp(bottomDp = 12))
            }
        }

        root.addView(host.dock("Pets"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(78)).apply {
            setMargins(ui.dp(8), 0, ui.dp(8), ui.dp(5))
        })
        host.setBackHandler(null)
        return root
    }

    private fun card(pet: Pet, portrait: View, selected: Boolean, demo: Boolean, onClick: () -> Unit): View {
        val age = PetRules.ageLabel(pet.birthday) ?: "Age not set"
        val kind = listOf(pet.species, pet.breed).filter { it.isNotBlank() }.joinToString(" · ")

        val card = ui.row().apply {
            minimumHeight = ui.dp(96)
            setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12))
            isFocusable = true
            isClickable = true
            background = ui.interactiveBackground(ui.surface, 22, if (selected) ui.primary else ui.outline)
            setOnClickListener { onClick() }
        }
        card.addView(portrait, LinearLayout.LayoutParams(ui.dp(72), ui.dp(72)).apply { rightMargin = ui.dp(14) })

        val labels = ui.column()
        labels.addView(ui.text(pet.name, 22f, ui.primary, 600, true), ui.lp())
        labels.addView(ui.text(kind, 14f, ui.sub), ui.lp(topDp = 4))
        labels.addView(ui.text(age, 14f, ui.sub), ui.lp(topDp = 2))
        if (selected) labels.addView(ui.text("Selected", 13f, ui.primary, 700), ui.lp(topDp = 6))
        if (demo) labels.addView(ui.text("Demo", 13f, ui.sub, 700), ui.lp(topDp = 6))
        card.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(ui.icon("Chevron", 24), LinearLayout.LayoutParams(ui.dp(24), ui.dp(24)).apply { gravity = Gravity.CENTER_VERTICAL })

        val state = when {
            selected -> ", selected pet"
            demo -> ", demo pet"
            else -> ""
        }
        card.contentDescription = "${pet.name}, $kind, $age$state"
        ui.role(card, android.widget.Button::class.java)
        return card
    }

    private fun askToAdd(pet: Pet) {
        AlertDialog.Builder(a)
            .setTitle("${pet.name} (demo)")
            .setMessage("This is a sample pet for testing. Add a real copy to try switching between pets.")
            .setNegativeButton("Close", null)
            .setPositiveButton("Add to my pets") { _, _ -> host.addDemoPet(pet) }
            .show()
    }
}
