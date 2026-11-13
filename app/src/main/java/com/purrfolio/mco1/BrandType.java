package com.purrfolio.mco1;

import android.os.Build;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;

public final class BrandType {
    private BrandType() {}

    public static void apply(TextView view, boolean display, int weight) {
        view.setTypeface(ResourcesCompat.getFont(view.getContext(), display ? R.font.fredoka : R.font.nunito));
        view.getPaint().setFakeBoldText(false);
        if (Build.VERSION.SDK_INT >= 26) view.setFontVariationSettings("'wght' " + weight);
    }
}
