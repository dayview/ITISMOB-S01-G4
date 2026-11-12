package com.purrfolio.mco1;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Profile document users/{uid} in Cloud Firestore.
 *
 * Firebase Storage needs the paid Blaze plan, so the profile photo is stored in the document itself
 * as a small Base64 JPEG (field "photo"); a Firestore document can hold up to 1 MiB.
 */
public class UserRepository {
    /** notifications.* switches and their starting values for a new account. */
    public static final Map<String, Boolean> NOTIFICATION_DEFAULTS;
    static {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        defaults.put("careReminders", true);
        defaults.put("appUpdates", true);
        defaults.put("tips", true);
        defaults.put("careCircle", true);
        defaults.put("productNews", false);
        NOTIFICATION_DEFAULTS = Collections.unmodifiableMap(defaults);
    }

    private static final int MAX_SIDE = 512;
    private static final int MAX_BYTES = 700 * 1024; // leaves room for the other fields under the 1 MiB limit
    private static final Executor PHOTO_WORKER = Executors.newSingleThreadExecutor();

    private final FirebaseFirestore db;

    public UserRepository() { this(FirebaseFirestore.getInstance()); }
    public UserRepository(FirebaseFirestore db) { this.db = db; }

    private DocumentReference doc(String uid) { return db.collection("users").document(uid); }

    public Task<Void> create(String uid, String fullName, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", fullName.trim());
        data.put("email", email.trim());
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("notifications", new HashMap<>(NOTIFICATION_DEFAULTS));
        return doc(uid).set(data);
    }

    /** Gives an empty map when the document does not exist yet (e.g. first social sign-in). */
    public Task<Map<String, Object>> load(String uid) {
        return doc(uid).get().onSuccessTask(snapshot -> {
            Map<String, Object> data = snapshot.getData();
            return Tasks.forResult(data == null ? new HashMap<>() : data);
        });
    }

    /**
     * Merges only the given fields. Note: set(merge) does NOT treat dots as paths, so "notifications.tips"
     * would create a field literally named that; nested maps (see setNotification) are merged instead.
     */
    public Task<Void> update(String uid, Map<String, Object> fields) {
        return doc(uid).set(fields, SetOptions.merge());
    }

    /** Turns one notifications.* switch on or off and keeps the others. */
    public Task<Void> setNotification(String uid, String key, boolean enabled) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("notifications", Collections.singletonMap(key, enabled));
        return update(uid, fields);
    }

    /** Shrinks the picked image off the main thread; save the result with update(uid, {"photo": it}). */
    public Task<String> preparePhoto(ContentResolver resolver, Uri uri) {
        return Tasks.call(PHOTO_WORKER, () -> encodePhoto(resolver, uri));
    }

    public Task<Void> deleteAll(String uid) { return doc(uid).delete(); }

    /** Downscales to at most 512 px, fixes camera rotation and JPEG-encodes until it fits. */
    public static String encodePhoto(ContentResolver resolver, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(resolver, uri)) { BitmapFactory.decodeStream(in, null, bounds); }
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap decoded;
        try (InputStream in = open(resolver, uri)) { decoded = BitmapFactory.decodeStream(in, null, options); }
        if (decoded == null) throw new IllegalArgumentException("That file isn't an image.");
        Bitmap bitmap = scaleAndRotate(decoded, rotation(resolver, uri));
        int quality = 80;
        while (true) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bytes);
            String encoded = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP);
            if (encoded.length() <= MAX_BYTES || quality <= 30) return encoded;
            quality -= 10;
        }
    }

    /** Turns a stored "photo" field back into an image, or null if it can't be read. */
    public static Bitmap decodePhoto(String photo) {
        try {
            byte[] bytes = Base64.decode(photo, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static InputStream open(ContentResolver resolver, Uri uri) throws IOException {
        InputStream in = resolver.openInputStream(uri);
        if (in == null) throw new IOException("Couldn't open the image.");
        return in;
    }

    private static int rotation(ContentResolver resolver, Uri uri) {
        try (InputStream in = open(resolver, uri)) {
            switch (new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception error) {
            return 0;
        }
    }

    private static Bitmap scaleAndRotate(Bitmap source, int degrees) {
        float scale = Math.min(1f, (float) MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
        if (scale == 1f && degrees == 0) return source;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
