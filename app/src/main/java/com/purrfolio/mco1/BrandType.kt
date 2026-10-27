package com.purrfolio.mco1

import android.os.Build
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/** Real variable-font weights; never adds synthetic bold on top of the font. */
object BrandType {
    fun apply(view: TextView, display: Boolean, weight: Int) {
        view.typeface = ResourcesCompat.getFont(view.context, if (display) R.font.fredoka else R.font.nunito)
        view.paint.isFakeBoldText = false
        if (Build.VERSION.SDK_INT >= 26) view.fontVariationSettings = "'wght' $weight"
    }
}
