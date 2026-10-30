package com.purrfolio.mco1

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Firebase Authentication wrapper; every failure carries a message that can be shown to the user as-is. */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signUp(fullName: String, email: String, password: String): Result<FirebaseUser> = attempt {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user!!
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(fullName.trim()).build()).await()
        // Verification is optional for the spec, so a failed send must not fail the sign-up.
        runCatching { user.sendEmailVerification().await() }
        user
    }

    suspend fun logIn(email: String, password: String): Result<FirebaseUser> = attempt {
        auth.signInWithEmailAndPassword(email.trim(), password).await().user!!
    }

    /** Succeeds even for unknown emails (email enumeration protection), so the UI should say "if an account exists". */
    suspend fun sendPasswordReset(email: String): Result<Unit> = attempt {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = attempt {
        reauthenticate(currentPassword).updatePassword(newPassword).await()
        Unit
    }

    /** Re-checks the password, lets the caller delete the user's data while still authorised, then removes the account. */
    suspend fun deleteAccount(currentPassword: String, deleteData: suspend (uid: String) -> Unit): Result<Unit> = attempt {
        val user = reauthenticate(currentPassword)
        deleteData(user.uid)
        user.delete().await()
        Unit
    }

    fun logOut() = auth.signOut()

    private suspend fun reauthenticate(password: String): FirebaseUser {
        val user = auth.currentUser ?: throw IllegalStateException("Please log in again.")
        user.reauthenticate(EmailAuthProvider.getCredential(user.email!!, password)).await()
        return user
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception(message(e), e))
    }

    private fun message(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Choose a stronger password."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidCredentialsException -> "Email or password is incorrect."
        is FirebaseAuthInvalidUserException -> "No account matches this email."
        is FirebaseAuthRecentLoginRequiredException -> "Please log in again to continue."
        is FirebaseNetworkException -> "You're offline. Check your connection and try again."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }
}
