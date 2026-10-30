package com.purrfolio.mco1

/** Account operations MainActivity performs for the User Management screens. */
interface AccountActions {
    val emailVerified: Boolean
    fun signUp(fullName: String, email: String, password: String)
    fun logIn(email: String, password: String)
    fun sendPasswordReset(email: String)
    fun logOut()
    /** Keys are Firestore field names: fullName, phone, birthday, country. */
    fun saveProfile(fields: Map<String, String>)
    fun changeProfilePhoto()
}
