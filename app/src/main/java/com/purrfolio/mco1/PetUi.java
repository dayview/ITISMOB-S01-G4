package com.purrfolio.mco1;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A self-contained UI kit for the pet screens, so no other file needs to change to use it.
 * Colors are the same light/dark values the existing screens use, plus a stronger border color for
 * the input fields so they meet the 3:1 non-text contrast rule from WCAG 1.4.11.
 */
public class PetUi {
    public final Activity a;
    public final boolean night;

    public final int bg, surface, elevated, primary, lavender, onLavender, ink, sub, coral, mint;
    /** Decorative dividers and card edges only. Too faint to mark where an input is. */
    public final int outline;
    /** Input borders: 4.5:1 on the light background and 5.4:1 on the dark one. */
    public final int fieldBorder;

    public PetUi(Activity activity) {
        a = activity;
        night = (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        bg = Color.parseColor(night ? "#101014" : "#FCF9FF");
        surface = Color.parseColor(night ? "#1C1B26" : "#F3EDF8");
        elevated = Color.parseColor(night ? "#24222F" : "#EDE2F5");
        primary = Color.parseColor(night ? "#D8B4F8" : "#8051A8");
        lavender = Color.parseColor("#D8B4F8");
        onLavender = Color.parseColor("#201528");
        ink = Color.parseColor(night ? "#F8F2FF" : "#292130");
        sub = Color.parseColor(night ? "#BDB3CD" : "#72647D");
        coral = Color.parseColor(night ? "#FF8298" : "#B32646");
        mint = Color.parseColor("#91E5C8");
        outline = Color.parseColor(night ? "#373243" : "#DED1E8");
        fieldBorder = Color.parseColor(night ? "#8E84A3" : "#7D6E8A");
    }

    // ---- sizes and basic containers ----

    public int dp(int value) { return (int) (value * a.getResources().getDisplayMetrics().density + .5f); }

    private int px(float dpValue) {
        return Math.max(1, (int) (dpValue * a.getResources().getDisplayMetrics().density + .5f));
    }

    public LinearLayout column() {
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public LinearLayout row() {
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public View space(int heightDp) {
        View view = new View(a);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(heightDp)));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    public LinearLayout.LayoutParams lp() { return lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0f); }

    public LinearLayout.LayoutParams lp(int topDp, int bottomDp) {
        return lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, topDp, bottomDp, 0f);
    }

    /** Full-width params with only a top margin. */
    public LinearLayout.LayoutParams lpTop(int topDp) { return lp(topDp, 0); }

    /** Full-width params with only a bottom margin. */
    public LinearLayout.LayoutParams lpBottom(int bottomDp) { return lp(0, bottomDp); }

    public LinearLayout.LayoutParams lp(int width, int height, int topDp, int bottomDp, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
        params.topMargin = dp(topDp);
        params.bottomMargin = dp(bottomDp);
        return params;
    }

    // ---- drawables ----

    public GradientDrawable rounded(int fill, int radiusDp) { return rounded(fill, radiusDp, null, px(1f)); }

    public GradientDrawable rounded(int fill, int radiusDp, Integer strokeColor) {
        return rounded(fill, radiusDp, strokeColor, px(1f));
    }

    public GradientDrawable rounded(int fill, int radiusDp, Integer strokeColor, int strokeWidthPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius((float) dp(radiusDp));
        if (strokeColor != null) drawable.setStroke(strokeWidthPx, strokeColor);
        return drawable;
    }

    public Drawable interactiveBackground(int fill, int radiusDp) { return interactiveBackground(fill, radiusDp, null); }

    /** A background that also shows a clear 3px focus ring for keyboard / switch / TalkBack focus. */
    public Drawable interactiveBackground(int fill, int radiusDp, Integer strokeColor) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, rounded(fill, radiusDp, primary, px(3f)));
        states.addState(new int[]{android.R.attr.state_pressed},
                rounded(elevated, radiusDp, strokeColor != null ? strokeColor : elevated, px(1.5f)));
        states.addState(new int[]{}, rounded(fill, radiusDp, strokeColor, px(1.5f)));
        return states;
    }

    public Drawable fieldBackground(boolean error) {
        StateListDrawable states = new StateListDrawable();
        int idle = error ? coral : fieldBorder;
        states.addState(new int[]{android.R.attr.state_focused}, rounded(bg, 14, error ? coral : primary, px(3f)));
        states.addState(new int[]{}, rounded(bg, 14, idle, px(error ? 2.5f : 1.5f)));
        return states;
    }

    // ---- text ----

    public TextView text(String value) { return text(value, 15f, ink, 400, false); }

    public TextView text(String value, float sizeSp, int color) { return text(value, sizeSp, color, 400, false); }

    public TextView text(String value, float sizeSp, int color, int weight) { return text(value, sizeSp, color, weight, false); }

    public TextView text(String value, float sizeSp, int color, int weight, boolean display) {
        TextView view = new TextView(a);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        BrandType.apply(view, display, weight);
        view.setLineSpacing((float) dp(2), 1f);
        return view;
    }

    public TextView heading(String value, float sizeSp) { return heading(value, sizeSp, primary); }

    public TextView heading(String value, float sizeSp, int color) {
        TextView view = text(value, sizeSp, color, 600, true);
        markHeading(view);
        return view;
    }

    public void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= 28) view.setAccessibilityHeading(true);
    }

    // ---- accessibility helpers ----

    public void role(View view, Class<?> type) { role(view, type, null); }

    /** Tells TalkBack what kind of control a plain View is, e.g. a Button, and (optionally) its checked state. */
    public void role(View view, Class<?> type, Boolean checked) {
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(type.getName());
                if (checked != null) {
                    info.setCheckable(true);
                    info.setChecked(checked);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    public void announce(View view, String message) { view.announceForAccessibility(message); }

    // ---- controls ----

    private final Set<String> careKinds = new HashSet<>(Arrays.asList(
            "Feeding", "Medication", "Water", "Playtime", "Vet Visit", "Grooming",
            "Home", "Pets", "Care", "Notes", "More", "Plus", "Back", "Close",
            "Arrow", "Chevron", "Check", "Energy", "Food", "Mood", "Mic", "Wave",
            "Link", "Eye", "Share", "QR", "Microchip", "Edit", "Heart", "Weight",
            "Emergency", "Google", "Apple", "Facebook"));

    public View icon(String kind, int sizeDp) { return icon(kind, sizeDp, primary); }

    public View icon(String kind, int sizeDp, int tint) {
        View view = careKinds.contains(kind) ? new CareArtwork(a, kind, tint) : new DetailIcon(a, kind, tint);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    /** A tappable icon with a 48dp * 48dp touch area (Android's recommended minimum). */
    public FrameLayout iconButton(String kind, String description, Runnable onClick) {
        FrameLayout frame = new FrameLayout(a);
        frame.setContentDescription(description);
        frame.setFocusable(true);
        frame.setClickable(true);
        frame.setBackground(interactiveBackground(Color.TRANSPARENT, 24));
        frame.setOnClickListener(v -> onClick.run());
        frame.addView(icon(kind, 26), new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        frame.setMinimumWidth(dp(48));
        frame.setMinimumHeight(dp(48));
        role(frame, Button.class);
        frame.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        return frame;
    }

    public TextView button(String label, Runnable onClick) { return button(label, true, onClick); }

    /** Filled (primary) or outlined (secondary) text button, at least 52dp tall. */
    public TextView button(String label, boolean primaryStyle, Runnable onClick) {
        TextView view = text(label, 16f, primaryStyle ? onLavender : primary, 700);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(52));
        view.setPadding(dp(20), dp(10), dp(20), dp(10));
        view.setFocusable(true);
        view.setClickable(true);
        view.setBackground(primaryStyle ? interactiveBackground(lavender, 28) : interactiveBackground(bg, 28, fieldBorder));
        view.setOnClickListener(v -> onClick.run());
        role(view, Button.class);
        return view;
    }

    public TextView textButton(String label, Runnable onClick) { return textButton(label, null, onClick); }

    /** A low-emphasis text-only button with a 48dp minimum height (used for "Change", "Clear"). */
    public TextView textButton(String label, String description, Runnable onClick) {
        TextView view = text(label, 15f, primary, 700);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(48));
        view.setMinWidth(dp(48));
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setFocusable(true);
        view.setClickable(true);
        view.setBackground(interactiveBackground(Color.TRANSPARENT, 16));
        if (description != null) view.setContentDescription(description);
        view.setOnClickListener(v -> onClick.run());
        role(view, Button.class);
        return view;
    }

    public int defaultPortrait(String species) {
        if ("Dog".equals(species)) return R.drawable.pdf_bailey;
        if ("Rabbit".equals(species)) return R.drawable.pdf_luna;
        return R.drawable.pdf_mochi;
    }

    public FrameLayout portrait(String photoUri, String species, int sizeDp, String description) {
        return portrait(photoUri, species, sizeDp, description, null);
    }

    /** Round pet picture with a ring. Uses the saved photo when there is one, otherwise a stock illustration. */
    public FrameLayout portrait(String photoUri, String species, int sizeDp, String description, Integer fallbackRes) {
        FrameLayout frame = new FrameLayout(a);
        frame.setBackground(rounded(primary, sizeDp));
        frame.setPadding(dp(3), dp(3), dp(3), dp(3));
        ImageView image = new ImageView(a);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(rounded(surface, sizeDp));
        image.setClipToOutline(true);
        image.setContentDescription(description);
        boolean loaded = false;
        if (!photoUri.trim().isEmpty()) {
            try {
                image.setImageURI(Uri.parse(photoUri));
                loaded = image.getDrawable() != null;
            } catch (Exception ignored) { loaded = false; }
        }
        if (!loaded) image.setImageResource(fallbackRes != null ? fallbackRes : defaultPortrait(species));
        frame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return frame;
    }

    // ---- screen frame ----

    /** Root of every pet screen: themed background, system-bar insets (status bar, cutout, keyboard). */
    @SuppressWarnings("deprecation")
    public LinearLayout screenRoot() {
        WindowCompat.setDecorFitsSystemWindows(a.getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(a.getWindow(), a.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);
        a.getWindow().setStatusBarColor(bg);
        a.getWindow().setNavigationBarColor(bg);
        LinearLayout root = column();
        root.setBackgroundColor(bg);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        return root;
    }

    /** A vertically scrolling area plus its content column. */
    public static final class ScrollBody {
        public final ScrollView scroll;
        public final LinearLayout body;
        ScrollBody(ScrollView scroll, LinearLayout body) { this.scroll = scroll; this.body = body; }
    }

    /** A vertically scrolling area that takes all spare height. */
    public ScrollBody scrollBody() {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout body = column();
        body.setPadding(dp(20), dp(8), dp(20), dp(16));
        scroll.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return new ScrollBody(scroll, body);
    }

    // ---- form fields ----

    /** Label above, input, then helper text. An error message replaces the helper text (Material guidance). */
    public class LabeledField {
        public final LinearLayout root;
        public final EditText input;
        private final TextView helperView;
        private final TextView errorView;
        private final View focusTarget;
        private String helperText;

        LabeledField(LinearLayout root, EditText input, TextView helperView, TextView errorView, View focusTarget) {
            this.root = root;
            this.input = input;
            this.helperView = helperView;
            this.errorView = errorView;
            this.focusTarget = focusTarget;
        }

        public void setHelper(String message) {
            helperText = message;
            if (errorView.getVisibility() != View.VISIBLE) showHelper();
        }

        private void showHelper() {
            boolean blank = helperText == null || helperText.trim().isEmpty();
            helperView.setText(blank ? "" : helperText);
            helperView.setVisibility(blank ? View.GONE : View.VISIBLE);
        }

        /** Pass null to clear the error. A non-null message is shown, announced, and the border turns red. */
        public void showError(String message) {
            if (message == null) {
                errorView.setVisibility(View.GONE);
                errorView.setText("");
                if (input != null) input.setBackground(fieldBackground(false));
                showHelper();
            } else {
                helperView.setVisibility(View.GONE);
                errorView.setText(message);
                errorView.setVisibility(View.VISIBLE);
                if (input != null) input.setBackground(fieldBackground(true));
            }
        }

        public void focus() {
            focusTarget.requestFocus();
            String message = errorView.getText().toString();
            if (!message.trim().isEmpty()) announce(focusTarget, message);
        }
    }

    private TextView fieldLabel(String label, boolean optional) {
        return text(optional ? label + " (optional)" : label, 14f, ink, 700);
    }

    private TextView[] helperAndError(LinearLayout parent, String helper) {
        TextView helperView = text(helper == null ? "" : helper, 13f, sub);
        helperView.setVisibility(helper == null || helper.trim().isEmpty() ? View.GONE : View.VISIBLE);
        TextView errorView = text("", 13f, coral, 700);
        errorView.setVisibility(View.GONE);
        errorView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        parent.addView(helperView, lpTop(6));
        parent.addView(errorView, lpTop(6));
        return new TextView[]{helperView, errorView};
    }

    public static final int DEFAULT_TEXT_INPUT = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;

    public LabeledField textField(String label, boolean optional, String initial, String helper,
                                  int inputType, boolean multiline, int maxLength, Consumer<String> onChange) {
        LinearLayout container = column();
        TextView labelView = fieldLabel(label, optional);
        container.addView(labelView, lpBottom(6));
        EditText edit = new EditText(a);
        edit.setId(View.generateViewId());
        edit.setText(initial);
        edit.setTextSize(16f);
        edit.setTextColor(ink);
        BrandType.apply(edit, false, 500);
        edit.setIncludeFontPadding(false);
        edit.setBackground(fieldBackground(false));
        edit.setPadding(dp(14), dp(12), dp(12), dp(12));
        edit.setMinHeight(dp(52));
        edit.setInputType(multiline ? inputType | InputType.TYPE_TEXT_FLAG_MULTI_LINE : inputType);
        if (multiline) {
            edit.setMinLines(3);
            edit.setGravity(Gravity.TOP | Gravity.START);
        } else {
            edit.setSingleLine(true);
        }
        if (maxLength > 0) edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        labelView.setLabelFor(edit.getId());
        container.addView(edit, lp());
        TextView[] helperAndError = helperAndError(container, helper);
        LabeledField field = new LabeledField(container, edit, helperAndError[0], helperAndError[1], edit);
        field.setHelper(helper);
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                field.showError(null);
                onChange.accept(s == null ? "" : s.toString());
            }
        });
        return field;
    }

    /**
     * A group of mutually exclusive choices shown as chips (better than a dropdown for 3-4 options).
     * Each chip is announced by TalkBack as a radio button with its selected state.
     */
    public LinearLayout choiceChips(String label, boolean optional, List<String> options, String selected,
                                    Consumer<String> onSelect) {
        LinearLayout container = column();
        container.addView(fieldLabel(label, optional), lpBottom(8));
        int perRow = a.getResources().getConfiguration().fontScale > 1.3f ? 2 : options.size();
        List<TextView> chips = new ArrayList<>();
        String[] current = {selected};

        Runnable paint = () -> {
            for (int index = 0; index < options.size(); index++) {
                TextView chip = chips.get(index);
                boolean on = options.get(index).equals(current[0]);
                chip.setTextColor(on ? onLavender : ink);
                chip.setBackground(on ? interactiveBackground(lavender, 24) : interactiveBackground(bg, 24, fieldBorder));
                role(chip, RadioButton.class, on);
            }
        };

        for (int start = 0; start < options.size(); start += perRow) {
            LinearLayout line = row();
            for (String option : options.subList(start, Math.min(start + perRow, options.size()))) {
                TextView chip = text(option, 15f, ink, 700);
                chip.setGravity(Gravity.CENTER);
                chip.setMinHeight(dp(48));
                chip.setPadding(dp(8), dp(8), dp(8), dp(8));
                chip.setFocusable(true);
                chip.setClickable(true);
                chip.setOnClickListener(v -> { current[0] = option; paint.run(); onSelect.accept(option); });
                chips.add(chip);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                line.addView(chip, params);
            }
            container.addView(line, lp());
        }
        paint.run();
        return container;
    }

    /** Read-only field: tap to open the calendar, "Clear" removes it. The calendar blocks future dates. */
    public LabeledField dateField(String label, boolean optional, String initialIso, String helper, Consumer<String> onChange) {
        LinearLayout container = column();
        String[] iso = {initialIso};
        container.addView(fieldLabel(label, optional), lpBottom(6));

        TextView value = text("", 16f, ink, 500);
        LinearLayout box = row();
        box.setMinimumHeight(dp(52));
        box.setPadding(dp(14), dp(10), dp(8), dp(10));
        box.setFocusable(true);
        box.setClickable(true);
        box.setBackground(fieldBackground(false));
        box.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(icon("Down", 22), new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView[] clear = new TextView[1];
        Runnable update = () -> {
            String shown = iso[0].trim().isEmpty() ? "" : PetRules.displayDate(iso[0]);
            boolean blank = shown.isEmpty();
            value.setText(blank ? "Not set" : shown);
            value.setTextColor(blank ? sub : ink);
            box.setContentDescription(blank ? label + ", not set. Double tap to choose a date."
                    : label + ", " + shown + ". Double tap to change.");
            clear[0].setVisibility(blank ? View.GONE : View.VISIBLE);
        };
        clear[0] = textButton("Clear", "Clear " + label, () -> {
            iso[0] = "";
            onChange.accept(iso[0]);
            update.run();
        });

        LinearLayout line = row();
        line.addView(box, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams clearParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clearParams.leftMargin = dp(4);
        line.addView(clear[0], clearParams);
        container.addView(line, lp());

        TextView[] helperAndError = helperAndError(container, helper);
        LabeledField field = new LabeledField(container, null, helperAndError[0], helperAndError[1], box);
        field.setHelper(helper);

        box.setOnClickListener(v -> {
            Calendar parsed = PetRules.parseIso(iso[0]);
            Calendar start = parsed != null ? parsed : Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(a, (picker, year, month, day) -> {
                iso[0] = PetRules.toIso(year, month, day);
                onChange.accept(iso[0]);
                field.showError(null);
                update.run();
            }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
        role(box, Button.class);
        update.run();
        return field;
    }
}
