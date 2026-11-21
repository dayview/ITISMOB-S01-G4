package com.purrfolio.mco1;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Hands an exported summary (5.5) to other apps through Android's share sheet, or to the system
 * "Save as" screen so the user can keep it on the phone or in Google Drive.
 *
 * Facebook and Instagram are reached through the share sheet like any other app; their own APIs need
 * Meta app review and business accounts, which this project does not use.
 */
public final class ExportFiles {
    private ExportFiles() {}

    /** Implemented by the activity, which owns the "Save as" result launcher. */
    public interface Host {
        void saveAs(String fileName, String mimeType, byte[] bytes);
    }

    public static final String PDF = "application/pdf", TEXT = "text/plain";

    /** Must match android:authorities of the FileProvider in AndroidManifest.xml. */
    static String authority(Activity activity) { return activity.getPackageName() + ".fileprovider"; }

    public static String fileName(RecordExport.Summary summary, boolean pdf) {
        return summary.fileBaseName + (pdf ? ".pdf" : ".txt");
    }

    public static byte[] bytes(RecordExport.Summary summary, boolean pdf) throws IOException {
        if (!pdf) return RecordExport.toText(summary).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordExport.writePdf(summary, out);
        return out.toByteArray();
    }

    /** Opens the share sheet. Text goes in the message itself; a PDF is attached as a file. */
    public static void share(Activity activity, RecordExport.Summary summary, boolean pdf) {
        Intent send = new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, summary.title);
        try {
            if (pdf) {
                File file = writeToCache(activity, fileName(summary, true), bytes(summary, true));
                Uri uri = FileProvider.getUriForFile(activity, authority(activity), file);
                send.setType(PDF).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                send.setClipData(ClipData.newRawUri(summary.title, uri)); // lets the chooser preview the file
            } else {
                send.setType(TEXT).putExtra(Intent.EXTRA_TEXT, RecordExport.toText(summary));
            }
            activity.startActivity(Intent.createChooser(send, "Share " + summary.title));
        } catch (IOException error) {
            Toast.makeText(activity, "Couldn't create the PDF. Please try again.", Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException error) {
            Toast.makeText(activity, "No app is available to share with on this device.", Toast.LENGTH_LONG).show();
        }
    }

    /** Asks the activity to show the system "Save as" screen for the file. */
    public static void save(Activity activity, RecordExport.Summary summary, boolean pdf) {
        if (!(activity instanceof Host)) return;
        try {
            ((Host) activity).saveAs(fileName(summary, pdf), pdf ? PDF : TEXT, bytes(summary, pdf));
        } catch (IOException error) {
            Toast.makeText(activity, "Couldn't create the PDF. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    /** Files shared through the FileProvider live in cache/exports; older exports are deleted first. */
    private static File writeToCache(Activity activity, String name, byte[] bytes) throws IOException {
        File folder = new File(activity.getCacheDir(), "exports");
        File[] old = folder.listFiles();
        if (old != null) for (File file : old) //noinspection ResultOfMethodCallIgnored
            file.delete();
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Can't create " + folder);
        File file = new File(folder, name);
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
        return file;
    }
}
