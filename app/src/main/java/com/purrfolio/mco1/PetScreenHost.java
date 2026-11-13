package com.purrfolio.mco1;

import android.view.View;

/**
 * Everything the pet screens need from the rest of the app. MainActivity implements this once.
 * Screens never touch MainActivity directly, so they are easy to move, test, or reuse.
 */
public interface PetScreenHost {
    /** Navigate to a named screen, e.g. "Home" or "Care History". */
    void go(String destination);

    /** Same as the system Back button. */
    void back();

    /** Open the system photo picker for the current draft. */
    void choosePhoto();

    /** Save a new pet (adding = true) or the changes to an existing pet. Navigates to the pet afterwards. */
    void savePet(Pet pet, boolean adding);

    /** Make the pet at index the selected pet and open it. */
    void selectPet(int index);

    /** Development only: copy a demo pet into the saved pets. */
    void addDemoPet(Pet pet);

    /** Leave the Add / Edit form without saving (the host throws the draft away). */
    void exitForm();

    /** Lets a form handle the system Back button itself (e.g. go to the previous step). Null removes it. */
    void setBackHandler(Runnable handler);

    /** The floating bottom navigation bar, with the given tab highlighted ("Pets", "Home", ...). */
    View dock(String screen);
}
