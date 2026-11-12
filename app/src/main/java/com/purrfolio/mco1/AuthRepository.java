package com.purrfolio.mco1;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import java.util.function.Function;

/**
 * Firebase Authentication wrapper. Every result arrives on the main thread through a {@link Callback};
 * failures carry a message that can be shown to the user as-is.
 */
public class AuthRepository {
    /** Receives either the result or a user-ready error message. */
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    private final FirebaseAuth auth;

    public AuthRepository() { this(FirebaseAuth.getInstance()); }
    public AuthRepository(FirebaseAuth auth) { this.auth = auth; }

    public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }

    /** True when the user can sign in with an email and password (false for a Google-only account). */
    public boolean hasPasswordLogin() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return false;
        for (UserInfo info : user.getProviderData())
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) return true;
        return false;
    }

    public void signUp(String fullName, String email, String password, Callback<FirebaseUser> callback) {
        Task<FirebaseUser> task = auth.createUserWithEmailAndPassword(email.trim(), password).onSuccessTask(result -> {
            FirebaseUser user = result.getUser();
            UserProfileChangeRequest name = new UserProfileChangeRequest.Builder().setDisplayName(fullName.trim()).build();
            return user.updateProfile(name).onSuccessTask(ignored -> {
                // Verification is optional for the spec, so a failed send must not fail the sign-up.
                user.sendEmailVerification();
                return Tasks.forResult(user);
            });
        });
        deliver(task, callback);
    }

    public void logIn(String email, String password, Callback<FirebaseUser> callback) {
        deliver(auth.signInWithEmailAndPassword(email.trim(), password)
                .onSuccessTask(result -> Tasks.forResult(result.getUser())), callback);
    }

    /** Succeeds even for unknown emails (email enumeration protection), so the UI should say "if an account exists". */
    public void sendPasswordReset(String email, Callback<Void> callback) {
        deliver(auth.sendPasswordResetEmail(email.trim()), callback);
    }

    public void changePassword(String currentPassword, String newPassword, Callback<Void> callback) {
        deliver(reauthenticate(currentPassword).onSuccessTask(user -> user.updatePassword(newPassword)), callback);
    }

    /**
     * Re-checks the password, runs {@code deleteData} (e.g. delete users/{uid}) while still authorised,
     * then removes the login itself.
     */
    public void deleteAccount(String currentPassword, Function<String, Task<Void>> deleteData, Callback<Void> callback) {
        deliver(reauthenticate(currentPassword).onSuccessTask(user ->
                deleteData.apply(user.getUid()).onSuccessTask(ignored -> user.delete())), callback);
    }

    public void logOut() { auth.signOut(); }

    /** Confirms the current password; a wrong one gets its own message instead of "Email or password is incorrect." */
    private Task<FirebaseUser> reauthenticate(String password) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Please log in again."));
        String email = user.getEmail();
        if (email == null) return Tasks.forException(new IllegalStateException("This account has no password to confirm."));
        return user.reauthenticate(EmailAuthProvider.getCredential(email, password)).continueWithTask(task -> {
            if (task.isSuccessful()) return Tasks.forResult(user);
            Exception error = task.getException();
            if (error instanceof FirebaseAuthInvalidCredentialsException)
                throw new IllegalArgumentException("Current password is incorrect.", error);
            throw error != null ? error : new IllegalStateException("Something went wrong. Please try again.");
        });
    }

    private static <T> void deliver(Task<T> task, Callback<T> callback) {
        task.addOnCompleteListener(done -> {
            if (done.isSuccessful()) callback.onSuccess(done.getResult());
            else callback.onError(message(done.getException()));
        });
    }

    private static String message(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) return "Choose a stronger password.";
        if (e instanceof FirebaseAuthUserCollisionException) return "An account with this email already exists.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "Email or password is incorrect.";
        if (e instanceof FirebaseAuthInvalidUserException) return "No account matches this email.";
        if (e instanceof FirebaseAuthRecentLoginRequiredException) return "Please log in again to continue.";
        if (e instanceof FirebaseNetworkException) return "You're offline. Check your connection and try again.";
        if (e instanceof FirebaseTooManyRequestsException) return "Too many attempts. Please wait a few minutes and try again.";
        if (e != null && e.getLocalizedMessage() != null) return e.getLocalizedMessage();
        return "Something went wrong. Please try again.";
    }
}
