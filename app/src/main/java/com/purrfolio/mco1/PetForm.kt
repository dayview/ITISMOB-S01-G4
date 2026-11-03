package com.purrfolio.mco1

import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout

/**
 * The three groups of fields shared by the multi-step "Add pet" flow and the one-page "Edit pet" screen.
 * Every field writes into the [PetDraft] as the user types, so nothing is lost when a screen is rebuilt.
 */
class PetForm(
    private val ui: PetUi,
    private val draft: PetDraft,
    private val existingPets: List<Pet>,
    private val choosePhoto: () -> Unit,
    private val refresh: () -> Unit
) {
    private var nameField: PetUi.LabeledField? = null
    private var otherField: PetUi.LabeledField? = null
    private var breedField: PetUi.LabeledField? = null
    private var birthdayField: PetUi.LabeledField? = null
    private var weightField: PetUi.LabeledField? = null
    private var notesField: PetUi.LabeledField? = null

    private fun add(into: LinearLayout, view: View, bottomDp: Int = 18) = into.addView(view, ui.lp(bottomDp = bottomDp))

    // photo and name group
    fun addPhotoAndName(into: LinearLayout) {
        val hasPhoto = draft.photoUri.isNotBlank()
        val hero = ui.column().apply { gravity = Gravity.CENTER_HORIZONTAL }
        hero.addView(
            ui.portrait(draft.photoUri, draft.finalSpecies(), 140, if
                    (hasPhoto) "Your pet's photo" else "No photo yet"),
                    LinearLayout.LayoutParams(ui.dp(140), ui.dp(140))
        )
        val buttons = ui.row().apply { gravity = Gravity.CENTER }
        buttons.addView(ui.button(if (hasPhoto) "Change photo" else "Add photo", primaryStyle = false) { choosePhoto() },
            LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        if (hasPhoto) {
            buttons.addView(ui.textButton("Remove", "Remove photo") {
                if (draft.photoUri != draft.originalPhotoUri)
                    PetPhotoStore.deleteIfOurs(ui.a, draft.photoUri)
                draft.photoUri = ""
                refresh()
            })
        }
        hero.addView(buttons, ui.lp(topDp = 12))
        add(into, hero, 24)

        val field = ui.textField(
            label = "Pet name", optional = false, initial = draft.name,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
            maxLength = PetRules.NAME_MAX
        ) { value -> draft.name = value; updateDuplicateHint() }
        nameField = field
        add(into, field.root)
        updateDuplicateHint()
    }

    private fun updateDuplicateHint() {
        val duplicate = PetRules.isDuplicateName(draft.name, draft.id, existingPets.map { it.id to it.name })
        nameField?.setHelper(if (duplicate) "You already have a pet named ${draft.name.trim()}. You can still save." else null)
    }

    // species, breed, sex group
    fun addSpecies(into: LinearLayout) {
        val other = ui.textField("What kind of pet?", false, draft.otherSpecies,
            helper = "For example: Hamster or Parrot", maxLength = PetRules.TEXT_MAX) { draft.otherSpecies = it }
        other.root.visibility = if (draft.species == PetDraft.OTHER) View.VISIBLE else View.GONE
        otherField = other

        add(into, ui.choiceChips("Species", false, PetDraft.SPECIES_CHOICES, draft.species) { choice ->
            draft.species = choice
            other.root.visibility = if (choice == PetDraft.OTHER) View.VISIBLE else View.GONE
            other.showError(null)
        })
        add(into, other.root)

        breedField = ui.textField("Breed", true, draft.breed, helper = "For example: Mixed breed or Siamese",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
            maxLength = PetRules.TEXT_MAX) { draft.breed = it }.also { add(into, it.root) }

        add(into, ui.choiceChips("Sex", true, PetDraft.SEX_CHOICES, draft.sex.ifBlank { "" }) { draft.sex = it })
    }

    // birthday, weight, notes group

    fun addDetails(into: LinearLayout) {
        birthdayField = ui.dateField("Birthday", true, draft.birthday, "Used to show your pet's age.") { draft.birthday = it }
            .also { add(into, it.root) }

        weightField = ui.textField(
            "Weight in kg", true, draft.weight, helper = "For example: 4.2",
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, maxLength = 6
        ) { draft.weight = it }.also { add(into, it.root) }

        notesField = ui.textField(
            "Notes", true, draft.notes, helper = "Anything helpful, like favorite treats or health notes.",
            multiline = true, maxLength = PetRules.NOTES_MAX
        ) { draft.notes = it }.also { add(into, it.root) }
    }

    // error validation: show every error, then move focus to the first one
    private fun apply(checks: List<Pair<PetUi.LabeledField?, String?>>): Boolean {
        var firstBad: PetUi.LabeledField? = null
        var anyBad = false
        for ((field, error) in checks) {
            field?.showError(error)
            if (error != null) {
                anyBad = true
                if (firstBad == null) firstBad = field
            }
        }
        firstBad?.focus()
        return !anyBad
    }

    fun validatePhotoAndName(): Boolean = apply(listOf(nameField to PetRules.nameError(draft.name)))

    fun validateSpecies(): Boolean = apply(listOf(otherField to if (draft.species == PetDraft.OTHER)
    PetRules.shortTextError(draft.otherSpecies, "Pet type") else null,
        breedField to PetRules.shortTextError(draft.breed, "Breed")
    ))

    fun validateDetails(): Boolean = apply(listOf(
        birthdayField to PetRules.birthdayError(draft.birthday),
        weightField to PetRules.weightError(draft.weight),
        notesField to PetRules.notesError(draft.notes)
    ))

    /** Used by the one-page Edit screen, where every field is on screen at once. */
    fun validateAll(): Boolean = apply(listOf(
        nameField to PetRules.nameError(draft.name),
        otherField to if (draft.species == PetDraft.OTHER) PetRules.shortTextError(draft.otherSpecies, "Pet type") else null,
        breedField to PetRules.shortTextError(draft.breed, "Breed"),
        birthdayField to PetRules.birthdayError(draft.birthday),
        weightField to PetRules.weightError(draft.weight),
        notesField to PetRules.notesError(draft.notes)
    ))
}