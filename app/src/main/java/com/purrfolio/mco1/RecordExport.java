package com.purrfolio.mco1;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Share / Export Records (5.5): turns a pet and the records the user picked into a summary that can be
 * shared as plain text or as a PDF.
 *
 * The summary is built once as a list of sections, then written out as text ({@link #toText}) or a PDF
 * ({@link #writePdf}), so both formats always contain the same information. The PDF uses Android's own
 * {@link PdfDocument} (free, no library). Everything except {@link #writePdf} is plain Java and unit tested.
 */
public final class RecordExport {
    private RecordExport() {}

    /** What the user chose to include. Owner contact details are off by default for privacy. */
    public static final class Options {
        public boolean includePetDetails = true;
        public boolean includeOwnerContact = false;
        public boolean includeRecordDetails = true;
    }

    /** The owner's contact details, from the profile (any of them may be empty). */
    public static final class Owner {
        public final String name, phone, email;

        public Owner(String name, String phone, String email) {
            this.name = name == null ? "" : name.trim();
            this.phone = phone == null ? "" : phone.trim();
            this.email = email == null ? "" : email.trim();
        }
    }

    /** A titled block of lines, e.g. "Pet Details" or "Medications (2)". */
    public static final class Section {
        public final String heading;
        public final List<String> lines;

        Section(String heading, List<String> lines) {
            this.heading = heading;
            this.lines = Collections.unmodifiableList(lines);
        }
    }

    /** Everything that goes into the shared file. */
    public static final class Summary {
        public final String title, subtitle, footer, fileBaseName;
        public final List<Section> sections;

        Summary(String title, String subtitle, String footer, String fileBaseName, List<Section> sections) {
            this.title = title;
            this.subtitle = subtitle;
            this.footer = footer;
            this.fileBaseName = fileBaseName;
            this.sections = Collections.unmodifiableList(sections);
        }
    }

    /**
     * Builds the summary.
     *
     * @param records     the records to include (normally the current search results), in any order
     * @param filterLabel a short description of the search filters, e.g. "Medications · This Month"; blank for none
     */
    public static Summary build(Pet pet, Owner owner, List<PetRecord> records, String filterLabel,
                                Options options, long now, Locale locale) {
        String petName = pet.name == null || pet.name.trim().isEmpty() ? "Pet" : pet.name.trim();
        SimpleDateFormat day = new SimpleDateFormat("MMM d, yyyy", locale);
        SimpleDateFormat dayTime = new SimpleDateFormat("MMM d, yyyy · h:mm a", locale);

        String subtitle = records.size() == 1 ? "1 record" : records.size() + " records";
        if (filterLabel != null && !filterLabel.trim().isEmpty()) subtitle += " · " + filterLabel.trim();

        List<Section> sections = new ArrayList<>();
        if (options.includePetDetails) {
            List<String> lines = new ArrayList<>();
            line(lines, "Name", petName);
            line(lines, "Species", pet.species);
            line(lines, "Breed", pet.breed);
            line(lines, "Sex", pet.sex);
            String birthday = pet.birthday;
            Calendar birth = PetRules.parseIso(birthday);
            if (birth != null) {
                Calendar today = Calendar.getInstance();
                today.setTimeInMillis(now);
                String age = PetRules.ageLabel(birthday, today);
                birthday = day.format(birth.getTime()) + (age == null ? "" : " (" + age + ")");
            }
            line(lines, "Birthday", birthday);
            line(lines, "Weight", pet.weightKg == null || pet.weightKg.trim().isEmpty() ? "" : pet.weightKg.trim() + " kg");
            line(lines, "Microchip", pet.microchipId);
            line(lines, "Vet clinic", join(pet.vetClinic, pet.vetPhone));
            line(lines, "Medical notes", pet.medicalInfo);
            sections.add(new Section("Pet Details", lines));
        }
        if (options.includeOwnerContact && owner != null) {
            List<String> lines = new ArrayList<>();
            line(lines, "Owner", owner.name);
            line(lines, "Phone", owner.phone);
            line(lines, "Email", owner.email);
            if (!lines.isEmpty()) sections.add(new Section("Owner Contact", lines));
        }

        // Records grouped by type in the same order as the search chips; newest first inside each group.
        Map<PetRecord.Type, List<PetRecord>> byType = new EnumMap<>(PetRecord.Type.class);
        for (PetRecord record : RecordSearch.filter(records, null, null, RecordSearch.DateRange.ALL, "")) {
            List<PetRecord> group = byType.get(record.type);
            if (group == null) byType.put(record.type, group = new ArrayList<>());
            group.add(record);
        }
        for (Map.Entry<PetRecord.Type, List<PetRecord>> group : byType.entrySet()) {
            List<String> lines = new ArrayList<>();
            for (PetRecord record : group.getValue()) {
                String when = record.hasTime() ? dayTime.format(new Date(record.timeMillis)) : "No date";
                String text = when + " — " + record.title;
                if (options.includeRecordDetails && !record.detail.trim().isEmpty()) text += ": " + record.detail.trim();
                lines.add(text);
            }
            sections.add(new Section(group.getKey().label + " (" + lines.size() + ")", lines));
        }
        if (byType.isEmpty()) sections.add(new Section("Records", Collections.singletonList("No records match these filters.")));

        String footer = "Generated by Purrfolio on " + day.format(new Date(now));
        String fileBaseName = safeFileName(petName) + "-records-" + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(now));
        return new Summary(petName + "'s Records", subtitle, footer, fileBaseName, sections);
    }

    /** The summary as plain text, for messaging apps and email bodies. */
    public static String toText(Summary summary) {
        StringBuilder text = new StringBuilder(summary.title).append('\n').append(summary.subtitle).append('\n');
        for (Section section : summary.sections) {
            text.append('\n').append(section.heading).append('\n');
            for (String line : section.lines) text.append("• ").append(line).append('\n');
        }
        return text.append('\n').append(summary.footer).append('\n').toString();
    }

    /** Letters, digits, dashes and underscores only, so the name works as a file name everywhere. */
    static String safeFileName(String name) {
        String safe = name.trim().replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        return safe.isEmpty() ? "Pet" : safe;
    }

    private static void line(List<String> lines, String label, String value) {
        if (value != null && !value.trim().isEmpty()) lines.add(label + ": " + value.trim());
    }

    private static String join(String first, String second) {
        String a = first == null ? "" : first.trim(), b = second == null ? "" : second.trim();
        if (a.isEmpty()) return b;
        return b.isEmpty() ? a : a + " · " + b;
    }

    // ---- PDF (Android only) ----

    private static final int PAGE_WIDTH = 595, PAGE_HEIGHT = 842, MARGIN = 48; // A4 in PostScript points

    /** Writes the summary as an A4 PDF. Long lines wrap and new pages are added as needed. */
    public static void writePdf(Summary summary, OutputStream out) throws IOException {
        PdfDocument pdf = new PdfDocument();
        try {
            new PdfWriter(pdf).write(summary);
            pdf.writeTo(out);
        } finally {
            pdf.close();
        }
    }

    private static final class PdfWriter {
        private final PdfDocument pdf;
        private final Paint title = paint(20, true, "#5B3A7A"), subtitle = paint(11, false, "#72647D"),
                heading = paint(13, true, "#8051A8"), body = paint(10.5f, false, "#292130"),
                footer = paint(9, false, "#72647D");
        private PdfDocument.Page page;
        private int pageNumber;
        private float y;

        PdfWriter(PdfDocument pdf) { this.pdf = pdf; }

        void write(Summary summary) {
            newPage();
            text(summary.title, title, 0, 6);
            text(summary.subtitle, subtitle, 0, 16);
            for (Section section : summary.sections) {
                if (y + 40 > PAGE_HEIGHT - MARGIN) newPage(); // keep a heading with its first line
                text(section.heading, heading, 0, 6);
                for (String line : section.lines) text("•  " + line, body, 10, 3);
                y += 10;
            }
            text(summary.footer, footer, 0, 0);
            pdf.finishPage(page);
        }

        private void newPage() {
            if (page != null) pdf.finishPage(page);
            pageNumber++;
            page = pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
            y = MARGIN;
            String number = "Page " + pageNumber;
            page.getCanvas().drawText(number, PAGE_WIDTH - MARGIN - footer.measureText(number), PAGE_HEIGHT - MARGIN / 2f, footer);
        }

        /** Draws {@code value} word-wrapped to the page width, indented by {@code indent}, then adds {@code after}. */
        private void text(String value, Paint paint, float indent, float after) {
            float width = PAGE_WIDTH - 2 * MARGIN - indent, lineHeight = paint.getTextSize() * 1.35f;
            for (String line : wrap(value, paint, width)) {
                if (y + lineHeight > PAGE_HEIGHT - MARGIN) newPage();
                y += lineHeight;
                page.getCanvas().drawText(line, MARGIN + indent, y, paint);
            }
            y += after;
        }

        private static List<String> wrap(String value, Paint paint, float width) {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : value.split(" ")) {
                String next = current.length() == 0 ? word : current + " " + word;
                if (paint.measureText(next) <= width) { current.setLength(0); current.append(next); continue; }
                if (current.length() > 0) lines.add(current.toString());
                // A single word longer than the line (e.g. a long file name) is split by characters.
                while (paint.measureText(word) > width) {
                    int fit = Math.max(1, paint.breakText(word, true, width, null));
                    lines.add(word.substring(0, fit));
                    word = word.substring(fit);
                }
                current.setLength(0);
                current.append(word);
            }
            if (current.length() > 0) lines.add(current.toString());
            return lines;
        }

        private static Paint paint(float size, boolean bold, String color) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(size);
            paint.setColor(Color.parseColor(color));
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
            return paint;
        }
    }
}
