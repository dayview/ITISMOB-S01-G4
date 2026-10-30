package com.purrfolio.mco1

/** Account operations MainActivity performs for the User Management screens. */
interface AccountActions {
    fun signUp(fullName: String, email: String, password: String)
    fun logIn(email: String, password: String)
    fun sendPasswordReset(email: String)
    fun logOut()
}
