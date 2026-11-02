package com.purrfolio.mco1

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * F2.1 Add Pet
 * Photo + name -> species/breed/sex -> extra details -> Review
 */
class AddPetFlow(
    private val a: Activity,
    private val draft: PetDraft,
    private val existingPets: List<Pet>,
    private val host: PetScreenHost
) {
    private val ui = PetUi(a)
    private val form = PetForm(ui, draft, existingPets, { host.choosePhoto() }, { showStep() })

    private val titles = listOf("Meet your pet", "About your pet", "Extra details", "Review")
    private val subtitles = listOf(
        "Add a photo and a name. You can change everything later.",
        "This helps Purrfolio show the right details.",
        "All of these are optional. Add what you know.",
        "Check everything, then save. You can edit it any time."
    )

    private lateinit var scroll: ScrollView
    private lateinit var body: LinearLayout
    private lateinit var stepLabel: TextView
    private lateinit var progress: LinearLayout
    private lateinit var actions: LinearLayout

    fun build(): View {
        val root = ui.screenRoot()

        val top = ui.row().apply { setPadding(ui.dp(8), ui.dp(4), ui.dp(8), 0) }
        top.addView(ui.iconButton("Back", "Back") { goBack() })
        top.addView(
            ui.heading("Add your pet", 22f).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        top.addView(View(a), LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)))
        root.addView(top, ui.lp())

        stepLabel = ui.text("", 14f, ui.sub, 700).apply { setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(6)) }
        root.addView(stepLabel, ui.lp())

        progress = ui.row().apply {
            setPadding(ui.dp(20), 0, ui.dp(20), ui.dp(4))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        root.addView(progress, ui.lp())

        val (scrollView, content) = ui.scrollBody()
        scroll = scrollView
        body = content
        root.addView(scroll)

        actions = ui.column().apply { setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(12)) }
        root.addView(actions, ui.lp())

        host.setBackHandler { goBack() }
        showStep()
        return root
    }

    // step navigation
    private fun goToStep(step: Int) {
        draft.step = step
        showStep(announce = true)
    }

    private fun goBack() {
        if (draft.step > 0) goToStep(draft.step - 1) else leave()
    }

    private fun leave() {
        if (!draft.isDirty) {
            host.exitForm()
            return
        }
        AlertDialog.Builder(a)
            .setTitle("Discard this pet?")
            .setMessage("What you entered won't be saved.")
            .setNegativeButton("Keep editing", null)
            .setPositiveButton("Discard") { _, _ -> host.exitForm() }
            .show()
    }

    private fun next() {
        when (draft.step) {
            0 -> if (form.validatePhotoAndName()) goToStep(1)
            1 -> if (form.validateSpecies()) goToStep(2)
            2 -> if (form.validateDetails()) goToStep(3)
            else -> save()
        }
    }

    private fun save() {
        if (PetRules.nameError(draft.name) != null) { goToStep(0); form.validatePhotoAndName();
            return }
        host.savePet(draft.toPet(), true)
    }

    // drawing one step
    private fun showStep(announce: Boolean = false) {
        body.removeAllViews()
        actions.removeAllViews()
        val last = titles.lastIndex
        stepLabel.text = "Step ${draft.step + 1} of ${titles.size}"
        paintProgress()

        val title = ui.heading(titles[draft.step], 28f)
        body.addView(title, ui.lp(bottomDp = 6))
        body.addView(ui.text(subtitles[draft.step], 15f, ui.sub), ui.lp(bottomDp = 22))

        when (draft.step) {
            0 -> form.addPhotoAndName(body)
            1 -> form.addSpecies(body)
            2 -> form.addDetails(body)
            else -> addReview()
        }

        val primaryLabel = when (draft.step) { 0, 1 -> "Next"; 2-> "Review"; else -> "Save pet" }
        actions.addView(ui.button(primaryLabel) { next() }, ui.lp())
        if (draft.step == 2) {
            actions.addView(ui.textButton("Skip this step") { goToStep(last) }, ui.lp(topDp = 4))
        }

        scroll.scrollTo(0, 0)
        if (announce) ui.announce(title, "Step ${draft.step + 1} of ${titles.size}, ${titles[draft.step]}")
    }

    private fun paintProgress() {
        progress.removeAllViews()
        titles.forEachIndexed { index, _ ->
            val segment = View(a).apply {
                background = ui.rounded(if (index <= draft.step) ui.primary else ui.outline, 4)
            }
            progress.addView(segment, LinearLayout.LayoutParams(0, ui.dp(6), 1f).apply {
                if (index > 0) leftMargin = ui.dp(6)
            })
        }
    }

    // review step
    private fun addReview() {
        val pet = draft.toPet()
        val none = "Not added"

        val nameCard = summaryCard(title="Photo and name", stepIndex=0)
        nameCard.addView(ui.portrait(pet.photoUri, pet.species, 72, if (pet.photoUri.isBlank()) "No photo yet" else "Your pet's photo"),
            LinearLayout.LayoutParams(ui.dp(72), ui.dp(72)).apply { bottomMargin = ui.dp(12) })
        summaryRow(nameCard, label="Name", value=pet.name, filled=true)

        val aboutCard = summaryCard(title="About", stepIndex=1)
        summaryRow(aboutCard, label="Species", value=pet.species, filled=true)
        summaryRow(aboutCard, label="Breed", value=pet.breed.ifBlank { none }, filled=pet.breed.isNotBlank())
        summaryRow(aboutCard, label="Sex", value=pet.sex.ifBlank { none }, filled=pet.sex.isNotBlank())

        val birthday = if (pet.birthday.isBlank()) none else
            PetRules.displayDate(pet.birthday) + (PetRules.ageLabel(pet.birthday)?.let { " ($it)" } ?: "")
        val detailCard = summaryCard(title="Details", stepIndex=2)
        summaryRow(detailCard, label="Birthday", value=birthday, filled=pet.birthday.isNotBlank())
        summaryRow(detailCard, label="Weight", value=if (pet.weightKg.isBlank()) none else "${pet.weightKg} kg", filled=pet.weightKg.isNotBlank())
        summaryRow(detailCard, label="Notes", value=pet.notes.ifBlank { none }, filled=pet.notes.isNotBlank())
    }

    private fun summaryCard(title: String, stepIndex: Int): LinearLayout {
        val card = ui.column().apply {
            background = ui.rounded(ui.surface, 20, ui.outline)
            setPadding(ui.dp(16), ui.dp(8), ui.dp(8), ui.dp(14))
        }
        val header = ui.row()
        header.addView(ui.text(title, 17f, ui.primary, 700).also { ui.markHeading(it) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(ui.textButton("Change", "Change $title") { goToStep(stepIndex) })
        card.addView(header, ui.lp())
        body.addView(card, ui.lp(bottomDp = 14))
        return card
    }

    private fun summaryRow(card: LinearLayout, label: String, value: String, filled: Boolean) {
        val line = ui.column().apply { contentDescription = "$label: $value" }
        line.addView(ui.text(label, 13f, ui.sub, 600), ui.lp())
        line.addView(ui.text(value, 16f, if (filled) ui.ink else ui.sub, 600), ui.lp(topDp = 2))
        card.addView(line, ui.lp(bottomDp = 10))
    }
}