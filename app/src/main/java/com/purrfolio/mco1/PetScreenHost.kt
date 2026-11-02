package com.purrfolio.mco1

import android.view.View

/**
 * Everything the pet screens need from the rest of the app. MainActivity implements this once
 * Screens never touch MainActivity directly, so they are easy to move, test, or reuse.
 */
interface PetScreenHost {
    /** Navigate to a named screen, e.g. "Home" or "Care History". */
    fun go(destination: String)

    /** Same as the system Back button. */
    fun back()

    /** Open the system photo picker for the current draft. */
    fun choosePhoto()

    /** Save a new pet (adding = true) or the changes to an existing pet. Navigates to the pet afterwards. */
    fun savePet(pet: Pet, adding: Boolean)

    /** Make the pet at [index] the selected pet and open it. */
    fun selectPet(index: Int)

    /** Development only: copy a demo pet into the saved pets. */
    fun addDemoPet(pet: Pet)

    /** Leave the Add / Edit form without saving (the host throws the draft away). */
    fun exitForm()

    /** Lets a form handle the system Back button itself (e.g. go to the previous step). Null removes it. */
    fun setBackHandler(handler: (() -> Unit)?)

    /** The floating bottom navigation bar, with the given tab highlighted ("Pets", "Home", ...). */
    fun dock(screen: String): View
}