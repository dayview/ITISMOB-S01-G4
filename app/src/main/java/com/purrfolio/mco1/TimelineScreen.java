package com.purrfolio.mco1;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * F5.6 Unified Pet Timeline: care logs, reminders and documents of one pet in a single list.
 * This class only draws. {@link TimelineBuilder} decides which rows exist and in what order.
 *
 * Each row is one TalkBack stop with one spoken sentence. The rail (line and dot) is decoration
 * and is hidden from TalkBack. Status is written as a word, never shown by colour alone.
 */
public class TimelineScreen {
    private static final List<String> FILTERS = Arrays.asList("All", "Care", "Reminders", "Documents");

    private final Activity a;
    private final String petName;
    private final Supplier<List<TimelineEntry>> source;
    private final boolean signedIn;
    private final PetScreenHost host;
    private final PetUi ui;
    private final Locale locale = Locale.getDefault();
    private final TimeZone zone = TimeZone.getDefault();

    private LinearLayout listHolder;
    private LinearLayout banner;
    private String filter = "All";
    /** How many past rows to draw. "Show earlier" raises it by one page. */
    private int limit = TimelineBuilder.DEFAULT_LIMIT;
    /** Called by BackendHub when reminders or documents change. */
    private final Runnable observer = this::fillList;

    /** Called with a document row's id when the user taps it. Null means document rows are not tappable. */
    private final Consumer<String> onOpenDocument;

    public TimelineScreen(Activity a, String petName, Supplier<List<TimelineEntry>> source, boolean signedIn,
                          PetScreenHost host) {
        this(a, petName, source, signedIn, host, null);
    }

    /**
     * @param source gives the pet's raw rows each time it is called, so the list can be rebuilt when data arrives.
     * @param onOpenDocument opens the document with this id. The host decides where the file lives.
     */
    public TimelineScreen(Activity a, String petName, Supplier<List<TimelineEntry>> source, boolean signedIn,
                          PetScreenHost host, Consumer<String> onOpenDocument) {
        this.onOpenDocument = onOpenDocument;
        this.a = a;
        this.petName = petName == null ? "" : petName.trim();
        this.source = source;
        this.signedIn = signedIn;
        this.host = host;
        this.ui = new PetUi(a);
    }

    public View build() {
        LinearLayout root = ui.screenRoot();

        LinearLayout top = ui.row();
        top.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        top.addView(ui.iconButton("Back", "Back", host::back));
        TextView title = ui.heading("Pet timeline", 22f);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        // Same width as the Back button so the title stays centred.
        top.addView(new View(a), new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));
        root.addView(top, ui.lp());

        PetUi.ScrollBody scrollBody = ui.scrollBody();
        LinearLayout body = scrollBody.body;
        root.addView(scrollBody.scroll);

        if (!petName.isEmpty()) body.addView(ui.text(petName, 16f, ui.sub, 600), ui.lpBottom(12));
        body.addView(ui.choiceChips("Show", false, FILTERS, filter, 2, choice -> {
            filter = choice;
            limit = TimelineBuilder.DEFAULT_LIMIT;
            showBannerFor(choice);
            fillList();
        }), ui.lpBottom(12));

        banner = ui.column();
        banner.setBackground(ui.rounded(ui.surface, 16, ui.outline));
        banner.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(4));
        TextView bannerText = ui.text("Sign in to see reminders and online documents. Care logs and documents saved on this phone still show here.", 14f, ui.sub, 600);
        bannerText.setFocusable(true);
        banner.addView(bannerText, ui.lp());
        TextView signIn = ui.textButton("Sign in", () -> host.go("Log In"));
        LinearLayout.LayoutParams signInParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        signInParams.gravity = Gravity.END;
        banner.addView(signIn, signInParams);
        banner.setVisibility(View.GONE);
        body.addView(banner, ui.lpBottom(4));
        showBannerFor(filter);

        listHolder = ui.column();
        body.addView(listHolder, ui.lp());
        fillList();

        LinearLayout.LayoutParams dockParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(78));
        dockParams.setMargins(ui.dp(8), 0, ui.dp(8), ui.dp(5));
        root.addView(host.dock("Care"), dockParams);
        // Listen for reminders and documents only while this screen is showing.
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { BackendHub.get().observe(observer); }
            @Override public void onViewDetachedFromWindow(View v) { BackendHub.get().unobserve(observer); }
        });
        host.setBackHandler(null);
        return root;
    }

    /** Reminders and documents need a signed-in user. The Care filter doesn't use them, so no note there. */
    private void showBannerFor(String choice) {
        banner.setVisibility(!signedIn && !"Care".equals(choice) ? View.VISIBLE : View.GONE);
    }

    // ---- list ----

    private void fillList() {
        listHolder.removeAllViews();
        TimelineBuilder.Result result = TimelineBuilder.build(filtered(), System.currentTimeMillis(), zone, locale, limit);
        for (TimelineBuilder.Group group : result.groups) {
            TextView header = ui.text(group.label, 15f, ui.sub, 700);
            ui.markHeading(header);
            listHolder.addView(header, ui.lp(12, 4));
            boolean upcoming = TimelineBuilder.COMING_UP.equals(group.label);
            for (TimelineEntry entry : group.entries) listHolder.addView(rowView(entry, upcoming), ui.lp());
        }
        if (result.groups.isEmpty()) listHolder.addView(emptyView(), ui.lpTop(8));
        if (result.hiddenCount > 0) {
            String more = "Show earlier (" + result.hiddenCount + " more)";
            listHolder.addView(ui.button(more, false, () -> {
                limit += TimelineBuilder.DEFAULT_LIMIT;
                fillList();
            }), ui.lpTop(12));
        }
    }

    /** No rows to show: either the pet has nothing at all, or only the chosen filter has nothing. */
    private View emptyView() {
        List<TimelineEntry> all = source.get();
        boolean nothingAtAll = "All".equals(filter) || all == null || all.isEmpty();
        LinearLayout card = ui.column();
        card.setBackground(ui.rounded(ui.surface, 20, ui.outline));
        card.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16));
        if (nothingAtAll) {
            String name = petName.isEmpty() ? "this pet" : petName;
            card.addView(ui.text("Nothing on the timeline yet", 20f, ui.primary, 600, true), ui.lpBottom(6));
            card.addView(ui.text("Care logs, reminders and documents for " + name + " will appear here.", 15f, ui.sub),
                    ui.lpBottom(14));
            card.addView(ui.button("Log care", () -> host.go("Quick Log")), ui.lp());
        } else {
            card.addView(ui.text("Nothing to show for \"" + filter + "\". Try another filter.", 15f, ui.sub), ui.lp());
        }
        return card;
    }

    private List<TimelineEntry> filtered() {
        List<TimelineEntry> kept = new ArrayList<>();
        List<TimelineEntry> all = source.get();
        for (TimelineEntry entry : all == null ? new ArrayList<TimelineEntry>() : all) {
            if (entry == null) continue;
            boolean keep = "All".equals(filter)
                    || ("Care".equals(filter) && entry.kind == TimelineEntry.Kind.LOG)
                    || ("Reminders".equals(filter) && entry.kind == TimelineEntry.Kind.REMINDER)
                    || ("Documents".equals(filter) && entry.kind == TimelineEntry.Kind.DOCUMENT);
            if (keep) kept.add(entry);
        }
        return kept;
    }

    // ---- one row ----

    private View rowView(TimelineEntry entry, boolean showDate) {
        LinearLayout line = ui.row();
        line.setMinimumHeight(ui.dp(56));
        line.setFocusable(true);
        line.setContentDescription(spoken(entry, showDate));
        if (entry.kind == TimelineEntry.Kind.DOCUMENT && onOpenDocument != null) {
            // Tapping a document opens the file. The background shows pressed and focus states.
            line.setBackground(ui.interactiveBackground(ui.bg, 12));
            line.setClickable(true);
            ui.role(line, android.widget.Button.class);
            line.setOnClickListener(v -> onOpenDocument.accept(entry.id));
        }

        line.addView(rail(entry), new LinearLayout.LayoutParams(ui.dp(24), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = ui.row();
        content.setPadding(ui.dp(8), ui.dp(10), ui.dp(4), ui.dp(10));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(ui.dp(28), ui.dp(28));
        iconParams.rightMargin = ui.dp(12);
        iconParams.gravity = Gravity.TOP;
        content.addView(ui.icon(iconFor(entry), 28), iconParams);

        LinearLayout labels = ui.column();
        TextView title = ui.text(entry.title.isEmpty() ? "Untitled" : entry.title, 16f, ui.ink, 700);
        labels.addView(title, ui.lp());
        String status = statusWord(entry.status);
        String time = timeText(entry, showDate);
        String meta = status.isEmpty() ? time : (time.isEmpty() ? status : status + " · " + time);
        if (!meta.isEmpty()) {
            int color = entry.status == TimelineEntry.Status.MISSED ? ui.coral
                    : entry.status == TimelineEntry.Status.NONE ? ui.sub : ui.primary;
            labels.addView(ui.text(meta, 13f, color, 700), ui.lpTop(2));
        }
        if (!entry.detail.isEmpty()) labels.addView(ui.text(entry.detail, 14f, ui.sub), ui.lpTop(2));
        content.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return line;
    }

    /** The vertical line with a dot. Decoration only, so TalkBack skips it. */
    private View rail(TimelineEntry entry) {
        FrameLayout rail = new FrameLayout(a);
        rail.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        View stem = new View(a);
        stem.setBackgroundColor(ui.outline);
        rail.addView(stem, new FrameLayout.LayoutParams(ui.dp(2), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL));
        View dot = new View(a);
        dot.setBackground(ui.rounded(entry.status == TimelineEntry.Status.MISSED ? ui.coral : ui.primary, 6));
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(ui.dp(12), ui.dp(12),
                Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        dotParams.topMargin = ui.dp(16);
        rail.addView(dot, dotParams);
        return rail;
    }

    // ---- text ----

    private String iconFor(TimelineEntry entry) {
        switch (entry.kind) {
            case REMINDER: return "Bell";
            case DOCUMENT: return "Document";
            default: return "Care";
        }
    }

    private static String statusWord(TimelineEntry.Status status) {
        switch (status) {
            case DONE: return "Done";
            case MISSED: return "Missed";
            case UPCOMING: return "Upcoming";
            default: return "";
        }
    }

    private static String kindWord(TimelineEntry.Kind kind) {
        switch (kind) {
            case REMINDER: return "Reminder";
            case DOCUMENT: return "Document";
            default: return "Care log";
        }
    }

    /** "9:00 AM" inside a day group, "Nov 22, 9:00 AM" under Coming up. Empty when the time is unknown. */
    private String timeText(TimelineEntry entry, boolean showDate) {
        if (entry.timeMillis <= 0) return "";
        SimpleDateFormat format = new SimpleDateFormat(showDate ? "MMM d, h:mm a" : "h:mm a", locale);
        format.setTimeZone(zone);
        return format.format(new Date(entry.timeMillis));
    }

    private String spoken(TimelineEntry entry, boolean showDate) {
        StringBuilder sb = new StringBuilder(kindWord(entry.kind)).append(": ")
                .append(entry.title.isEmpty() ? "untitled" : entry.title);
        String status = statusWord(entry.status);
        if (!status.isEmpty()) sb.append(", ").append(status.toLowerCase(locale));
        String time = timeText(entry, showDate);
        if (!time.isEmpty()) sb.append(", ").append(time);
        if (!entry.detail.isEmpty()) sb.append(". ").append(entry.detail);
        return sb.toString();
    }
}
