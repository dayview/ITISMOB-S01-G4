package com.purrfolio.mco1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Keeps pet photos in the app's own private folder.
 *
 * Why copy? The Android photo picker only grants temporary access to the picked image, so saving its
 * URI would break later. A small JPEG copy always works, needs no permission, and is removed with the app.
 */
public final class PetPhotoStore {
    private PetPhotoStore() {}

    private static final String FOLDER = "pets";
    private static final int MAX_SIDE = 1024;

    private static File folder(Context context) {
        File folder = new File(context.getFilesDir(), FOLDER);
        folder.mkdirs();
        return folder;
    }

    /** Copies source into private storage and returns a file:// URI string, or null if it couldn't be read. */
    public static String importPhoto(Context context, Uri source, String petId) {
        try {
            android.content.ContentResolver resolver = context.getContentResolver();

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = resolver.openInputStream(source)) {
                if (in != null) BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) sample *= 2;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            Bitmap decoded;
            try (InputStream in = resolver.openInputStream(source)) {
                decoded = in == null ? null : BitmapFactory.decodeStream(in, null, options);
            }
            if (decoded == null) return null;

            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream in = resolver.openInputStream(source)) {
                if (in != null) orientation = new ExifInterface(in)
                        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }

            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90f);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(180f);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(270f);

            int largest = Math.max(decoded.getWidth(), decoded.getHeight());
            if (largest > MAX_SIDE) {
                float scale = (float) MAX_SIDE / largest;
                matrix.postScale(scale, scale);
            }

            Bitmap finished = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);

            // A new file name each time means the picture refreshes on screen and old copies can be deleted.
            File file = new File(folder(context), petId + "-" + System.currentTimeMillis() + ".jpg");
            try (OutputStream out = new FileOutputStream(file)) {
                finished.compress(Bitmap.CompressFormat.JPEG, 88, out);
            }
            return Uri.fromFile(file).toString();
        } catch (Exception error) {
            return null;
        }
    }

    /** Deletes photoUri only if it is one of our own copies. Anything else is left alone. */
    public static void deleteIfOurs(Context context, String photoUri) {
        if (photoUri == null || photoUri.trim().isEmpty()) return;
        try {
            String path = Uri.parse(photoUri).getPath();
            if (path == null) return;
            File file = new File(path);
            boolean inside = file.getCanonicalPath().startsWith(folder(context).getCanonicalPath() + File.separator);
            if (inside) file.delete();
        } catch (Exception ignored) { }
    }
}
