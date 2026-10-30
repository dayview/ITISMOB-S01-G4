package com.purrfolio.mco1

import android.util.Patterns

/** Client-side checks shared by the account forms; a null result means the value is valid. */
object Validators {
    fun fullName(value: String): String? = if (value.trim().length < 2) "Enter your full name." else null

    fun email(value: String): String? = when {
        value.isBlank() -> "Enter your email address."
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "That email address doesn't look right."
        else -> null
    }

    /** Same order as the Create Account checklist: 8+ characters, a letter and a number, a special character. */
    fun passwordRules(value: String): List<Boolean> = listOf(
        value.length >= 8,
        value.any { it.isLetter() } && value.any { it.isDigit() },
        value.any { !it.isLetterOrDigit() }
    )

    fun password(value: String): String? =
        if (passwordRules(value).all { it }) null else "Password must meet all three requirements."
}
