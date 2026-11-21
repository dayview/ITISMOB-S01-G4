package com.purrfolio.mco1;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Plain Java rules for saving a document on the phone (5.1 Pet Paperless Inbox).
 * No Android types, so the rules can be checked on the JVM (see DocumentRulesTest).
 */
public final class DocumentRules {
    private DocumentRules() {}

    /** The same four types the team's documents list filters by. */
    public static final List<String> TYPES =
            Collections.unmodifiableList(Arrays.asList("Vaccination", "Medication", "Receipt", "Other"));
    public static final String DEFAULT_TYPE = "Other";

    public static final int TITLE_MAX = 60;
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png", "webp");

    /** A known type, or "Other" for anything else (including null). */
    public static String typeOrDefault(String type) {
        if (type != null) {
            String trimmed = type.trim();
            for (String known : TYPES) if (known.equalsIgnoreCase(trimmed)) return known;
        }
        return DEFAULT_TYPE;
    }

    /** Lower-case extension without the dot, or "" when there is none. */
    public static String extensionOf(String fileName) {
        if (fileName == null) return "";
        String name = fileName.trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isImage(String fileName) {
        return IMAGE_EXTENSIONS.contains(extensionOf(fileName));
    }

    public static boolean isAllowedFile(String fileName) {
        return ALLOWED_EXTENSIONS.contains(extensionOf(fileName));
    }

    /** Null when the file can be saved, otherwise a message that says how to fix it. */
    public static String fileError(String fileName, long sizeBytes) {
        if (!isAllowedFile(fileName)) return "Purrfolio can save PDF, JPG, PNG or WEBP files.";
        if (sizeBytes <= 0) return "This file is empty. Choose another one.";
        if (sizeBytes > MAX_BYTES) return "This file is larger than 10 MB. Choose a smaller one.";
        return null;
    }

    /** Null when the title is fine. */
    public static String titleError(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) return "Enter a title for this document";
        if (trimmed.length() > TITLE_MAX) return "Title must be " + TITLE_MAX + " characters or fewer";
        return null;
    }

    /** "vet_receipt-nov.pdf" becomes "vet receipt nov". Falls back to "Document". */
    public static String defaultTitle(String fileName) {
        String name = fileName == null ? "" : fileName.trim();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) name = name.substring(0, dot);
        name = name.replaceAll("[_\\-]+", " ").replaceAll("\\s+", " ").trim();
        if (name.isEmpty()) return "Document";
        return name.length() > TITLE_MAX ? name.substring(0, TITLE_MAX).trim() : name;
    }

    /** "340 KB", "1.2 MB". Used on the preview and in the list. */
    public static String sizeLabel(long bytes) {
        if (bytes < 1024) return Math.max(0, bytes) + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Where the saved copy lives, relative to the app's files folder: docs/&lt;petId&gt;/&lt;id&gt;.&lt;ext&gt;.
     * Characters that are not letters, digits, "-" or "_" in the ids are replaced so the path stays inside docs/.
     */
    public static String relativePath(String petId, String id, String fileName) {
        String ext = extensionOf(fileName);
        return "docs/" + safeSegment(petId) + "/" + safeSegment(id) + (ext.isEmpty() ? "" : "." + ext);
    }

    private static String safeSegment(String value) {
        String raw = value == null ? "" : value.trim();
        String safe = raw.replaceAll("[^A-Za-z0-9_\\-]", "_");
        return safe.isEmpty() ? "_" : safe;
    }
}
