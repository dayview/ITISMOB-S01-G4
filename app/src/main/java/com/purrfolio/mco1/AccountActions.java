package com.purrfolio.mco1;

import java.util.Map;
import java.util.function.Consumer;

/** Account operations MainActivity performs for the User Management screens. */
public interface AccountActions {
    boolean isEmailVerified();
    void signUp(String fullName, String email, String password);
    void logIn(String email, String password);
    void sendPasswordReset(String email);
    void logOut();
    /** Keys are Firestore field names: fullName, phone, birthday, country. */
    void saveProfile(Map<String, String> fields);
    void changeProfilePhoto();

    /** False for accounts without an email/password sign-in, which hides Change Password. */
    boolean hasPasswordLogin();
    /** {@code done} gets null on success, or a message to show the user. */
    void changePassword(String currentPassword, String newPassword, Consumer<String> done);
    /** Deletes users/{uid} and the login, then returns to Welcome; {@code done} gets null on success or the error. */
    void deleteAccount(String currentPassword, Consumer<String> done);
    /** Key is a notifications.* field: careReminders, appUpdates, tips, careCircle, productNews. */
    void setNotification(String key, boolean enabled);
}
