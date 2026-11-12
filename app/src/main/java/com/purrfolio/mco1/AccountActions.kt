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

    /** False for accounts without an email/password sign-in, which hides Change Password. */
    val hasPasswordLogin: Boolean
    /** [done] gets null on success, or a message to show under the current password. */
    fun changePassword(currentPassword: String, newPassword: String, done: (error: String?) -> Unit)
    /** Deletes users/{uid} and the login, then returns to Welcome; [done] gets null on success or the error. */
    fun deleteAccount(currentPassword: String, done: (error: String?) -> Unit)
    /** Key is a notifications.* field: careReminders, appUpdates, tips, careCircle, productNews. */
    fun setNotification(key: String, enabled: Boolean)
}
