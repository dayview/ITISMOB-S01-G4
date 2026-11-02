package com.purrfolio.mco1

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * F2.3
 * Edit Pet Profile: every field on one scrolling page -> "Save changes" at the bottom.
 * Leaving with unsaved changes asks for confirmation.
 */
class EditPetScreen(
    private val a: Activity,
    private val draft: PetDraft,
    private val existingPets: List<Pet>,
    private val host: PetScreenHost
) {
    private val ui = PetUi(a)
    private val form = PetForm(ui, draft, existingPets, { host.choosePhoto() }, { renderBody() })
    private lateinit var body: LinearLayout

    fun build(): View {
        val root = ui.screenRoot()

        val top = ui.row().apply { setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4)) }
        top.addView(ui.iconButton("Back", "Back") { leave() })
        top.addView(
            ui.heading("Edit pet profile", 22f).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        top.addView(View(a), LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)))
        root.addView(top, ui.lp())

        val (scroll, content) = ui.scrollBody()
        body = content
        root.addView(scroll)

        val actions = ui.column().apply { setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(12)) }
        actions.addView(ui.button("Save changes") { save() }, ui.lp())
        actions.addView(ui.textButton("Cancel") { leave() }, ui.lp(topDp = 4))
        root.addView(actions, ui.lp())

        host.setBackHandler { leave() }
        renderBody()
        return root
    }

    private fun renderBody() {
        body.removeAllViews()
        form.addPhotoAndName(body)
        form.addSpecies(body)
        form.addDetails(body)
    }

    private fun save() {
        if (form.validateAll()) host.savePet(draft.toPet(), false)
    }

    private fun leave() {
        if (!draft.isDirty) {
            host.exitForm()
            return
        }
        AlertDialog.Builder(a)
            .setTitle("Discard changes?")
            .setMessage("Your changes to ${draft.name.ifBlank { "this pet" }} won't be saved.")
            .setNegativeButton("Keep editing", null)
            .setPositiveButton("Discard") { _, _ -> host.exitForm() }
            .show()
    }
}
