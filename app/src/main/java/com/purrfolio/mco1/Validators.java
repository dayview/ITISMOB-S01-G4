package com.purrfolio.mco1;

import android.util.Patterns;
import java.util.Arrays;
import java.util.List;

/** Client-side checks shared by the account forms; a null result means the value is valid. */
public final class Validators {
    private Validators() {}

    public static String fullName(String value) {
        return value.trim().length() < 2 ? "Enter your full name." : null;
    }

    public static String email(String value) {
        if (value.trim().isEmpty()) return "Enter your email address.";
        if (!Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()) return "That email address doesn't look right.";
        return null;
    }

    /** Same order as the Create Account checklist: 8+ characters, a letter and a number, a special character. */
    public static List<Boolean> passwordRules(String value) {
        boolean letter = false, digit = false, special = false;
        for (char c : value.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            else if (Character.isDigit(c)) digit = true;
            else special = true;
        }
        return Arrays.asList(value.length() >= 8, letter && digit, special);
    }

    public static String password(String value) {
        return passwordRules(value).contains(false) ? "Password must meet all three requirements." : null;
    }
}
