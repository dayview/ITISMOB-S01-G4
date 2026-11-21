package com.purrfolio.mco1;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

/** Displays the app's Lucide vector icons with the requested tint. */
public class CareArtwork extends View {
    private String kind = "";
    private int tint = Color.rgb(216, 180, 248);
    private final Paint brandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CareArtwork(Context context) { this(context, (AttributeSet) null, 0); }
    public CareArtwork(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public CareArtwork(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.CareArtwork);
            try {
                String value = values.getString(R.styleable.CareArtwork_artKind);
                kind = value == null ? "" : value;
                tint = values.getColor(R.styleable.CareArtwork_artTint, tint);
            } finally { values.recycle(); }
        }
    }
    public CareArtwork(Context context, String kind, int tint) {
        this(context);
        setArtwork(kind, tint);
    }
    public CareArtwork(Context context, String kind) {
        this(context, kind, Color.rgb(216, 180, 248));
    }
    public void setArtwork(String kind, int tint) {
        this.kind = kind;
        this.tint = tint;
        invalidate();
    }

    private static String lucideName(String kind) {
        switch (kind) {
            case "Feeding": return "bone";
            case "Medication": return "pill";
            case "Water": return "droplet";
            case "Playtime": return "volleyball";
            case "Vet Visit": return "stethoscope";
            case "Grooming": return "scissors";
            case "Home": return "house";
            case "Pets": case "Vaccinations": return "paw_print";
            case "Care": case "Notes": return "calendar_days";
            case "More": return "ellipsis";
            case "Plus": return "plus";
            case "Back": return "chevron_left";
            case "Close": return "x";
            case "Arrow": return "arrow_right";
            case "Chevron": return "chevron_right";
            case "Check": return "check";
            case "Energy": case "Allergies": return "zap";
            case "Food": return "utensils";
            case "Mood": return "circle_check";
            case "Mic": return "mic";
            case "Wave": return "audio_lines";
            case "Link": return "link";
            case "Eye": return "eye";
            case "Share": return "share";
            case "QR": case "Microchip": return "qr_code";
            case "Edit": return "pencil";
            case "Heart": return "heart";
            case "Weight": return "weight";
            case "Emergency": return "cross";
            case "Sparkle": return "sparkles";
            case "Warning": return "circle_question_mark";
            case "Progress": return "circle_gauge";
            case "Person": return "user_round";
            case "Mail": return "mail";
            case "Lock": return "lock_keyhole";
            case "Camera": return "camera";
            case "Settings": return "settings";
            case "Bell": return "bell";
            case "Phone": return "phone";
            case "Location": return "map_pin";
            case "Globe": return "globe";
            case "Moon": return "moon";
            case "Database": return "database";
            case "Trash": return "trash";
            case "Down": return "chevron_down";
            case "Clock": return "clock";
            case "Search": return "search";
            case "Filter": return "sliders_horizontal";
            case "Document": return "file_text";
            case "Medical": return "file_plus";
            case "Folder": return "folder";
            case "Download": return "download";
            case "Expand": return "expand";
            case "Print": return "printer";
            case "Photo": return "image";
            case "Cloud": return "cloud_upload";
            case "Wifi": return "wifi";
            case "WifiOff": return "wifi_off";
            case "Star": return "star";
            case "Megaphone": return "megaphone";
            case "Play": return "play";
            case "Stop": return "square";
            case "Logout": return "log_out";
            case "Sex": return "venus";
            case "Cat": return "cat";
            default: return "circle_question_mark";
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if ("Google".equals(kind)) {
            brandPaint.setColor(tint);
            brandPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            brandPaint.setTextAlign(Paint.Align.CENTER);
            brandPaint.setTextSize(Math.min(getWidth(), getHeight()) * .8f);
            Paint.FontMetrics metrics = brandPaint.getFontMetrics();
            canvas.drawText("G", getWidth() / 2f,
                    getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f, brandPaint);
            return;
        }
        String name = "lucide_" + lucideName(kind);
        int id = getResources().getIdentifier(name, "drawable", getContext().getPackageName());
        if (id == 0) return;
        Drawable drawable = AppCompatResources.getDrawable(getContext(), id);
        if (drawable == null) return;
        drawable = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(drawable, tint);
        int side = Math.min(getWidth(), getHeight());
        int left = (getWidth() - side) / 2;
        int top = (getHeight() - side) / 2;
        drawable.setBounds(left, top, left + side, top + side);
        drawable.draw(canvas);
    }
}
