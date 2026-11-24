package com.purrfolio.mco1;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Keeps saved documents in the app's own private folder: files/docs/&lt;petId&gt;/&lt;id&gt;.&lt;ext&gt;.
 *
 * Why copy? The system file picker only gives temporary access to the picked file, so saving its
 * URI would break later (same reason as {@link PetPhotoStore}). The copy needs no permission and is
 * removed with the app. Rules about what may be saved live in {@link DocumentRules}.
 */
public final class DocumentFileStore {
    private DocumentFileStore() {}

    private static final String ROOT = "docs";

    /** Name and size of a file the user picked. {@code sizeBytes} is -1 when the provider does not say. */
    public static final class Picked {
        public final String fileName;
        public final long sizeBytes;

        Picked(String fileName, long sizeBytes) {
            this.fileName = fileName;
            this.sizeBytes = sizeBytes;
        }
    }

    /** Reads the file's name and size so the form can check and preview it before anything is copied. */
    public static Picked describe(Context context, Uri source) {
        String name = null;
        long size = -1;
        try (Cursor cursor = context.getContentResolver().query(source,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn);
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn);
            }
        } catch (RuntimeException ignored) {
            // Some providers refuse the query. The name falls back to the last part of the URI below.
        }
        if (name == null || name.trim().isEmpty()) {
            String last = source.getLastPathSegment();
            name = last == null ? "" : last;
        }
        return new Picked(name, size);
    }

    /**
     * Copies source to docs/&lt;petId&gt;/&lt;id&gt;.&lt;ext&gt; and returns that relative path, or null if it could not
     * be saved. The bytes go to a temporary file first and are renamed only when the whole copy worked,
     * so a failure half way never leaves a broken document behind. The size limit is checked while copying,
     * because some providers do not report a size.
     */
    public static String save(Context context, Uri source, String petId, String docId, String fileName) {
        String relative = DocumentRules.relativePath(petId, docId, fileName);
        File target = fileFor(context, relative);
        if (target == null) return null;
        File folder = target.getParentFile();
        if (folder == null || (!folder.isDirectory() && !folder.mkdirs())) return null;

        File temp = new File(folder, target.getName() + ".tmp");
        boolean copied = false;
        try (InputStream in = openInput(context.getContentResolver(), source);
             OutputStream out = new FileOutputStream(temp)) {
            if (in == null) return null;
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > DocumentRules.MAX_BYTES) return null;
                out.write(buffer, 0, read);
            }
            out.flush();
            copied = total > 0;
        } catch (IOException | RuntimeException failed) {
            copied = false;
        } finally {
            if (!copied) temp.delete();
        }
        if (!copied) return null;
        if (target.exists() && !target.delete()) {
            temp.delete();
            return null;
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            return null;
        }
        return relative;
    }

    /** The real file for a saved path, or null when the path is empty or points outside files/docs. */
    public static File fileFor(Context context, String relativePath) {
        if (relativePath == null || !relativePath.startsWith(ROOT + "/")) return null;
        try {
            File root = new File(context.getFilesDir(), ROOT).getCanonicalFile();
            File file = new File(context.getFilesDir(), relativePath).getCanonicalFile();
            return file.getPath().startsWith(root.getPath() + File.separator) ? file : null;
        } catch (IOException e) {
            return null;
        }
    }

    /** Removes the saved copy (and the pet's folder when it becomes empty). True when the file is gone. */
    public static boolean delete(Context context, String relativePath) {
        File file = fileFor(context, relativePath);
        if (file == null) return false;
        boolean gone = !file.exists() || file.delete();
        File folder = file.getParentFile();
        if (gone && folder != null) {
            String[] left = folder.list();
            if (left != null && left.length == 0) folder.delete();
        }
        return gone;
    }

    private static InputStream openInput(ContentResolver resolver, Uri source) throws IOException {
        try {
            return resolver.openInputStream(source);
        } catch (java.io.FileNotFoundException | SecurityException e) {
            throw new IOException("Cannot open the picked file", e);
        }
    }
}
