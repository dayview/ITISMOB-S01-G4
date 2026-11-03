package com.purrfolio.mco1

import android.app.Activity
import android.app.DatePickerDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

/**
 *  A self-contained UI kit for the pet screens as no other file needs to change to use this.
 *  Colors are the same light/dark values the existing screens use, plus a stronger border color for
 *  the input fields so they meet the 3:1 non-text contrast rule from WCAG 1.4.11.
 */
class PetUi(val a: Activity) {
    val night = (a.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun c(hex: String) = Color.parseColor(hex)
    val bg = c(if (night) "#101014" else "#FCF9FF")
    val surface = c(if (night) "#1C1B26" else "#F3EDF8")
    val elevated = c(if (night) "#24222F" else "#EDE2F5")
    val primary = c(if (night) "#D8B4F8" else "#8051A8")
    val lavender = c("#D8B4F8")
    val onLavender = c("#201528")
    val ink = c(if (night) "#F8F2FF" else "#292130")
    val sub = c(if (night) "#BDB3CD" else "#72647D")
    val coral = c(if (night) "#FF8298" else "#B32646")
    val mint = c("#91E5C8")

    /** Decorative dividers and card edges only. Too faint to mark where an input is. */
    val outline = c(if (night) "#373243" else "#DED1E8")

    /** Input borders: 4.5:1 on the light background and 5.4:1 on the dark one. */
    val fieldBorder = c(if (night) "#8E84A3" else "#7D6E8A")

    // sizes and basic containers
    fun dp(value: Int) = (value * a.resources.displayMetrics.density + .5f).toInt()
    private fun px(dpValue: Float) = (dpValue * a.resources.displayMetrics.density + .5f).toInt().coerceAtLeast(1)

    fun column() = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
    fun row() = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL;
    gravity = Gravity.CENTER_VERTICAL }

    fun space(heightDp: Int) = View(a).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp (heightDp))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun lp(width: Int = ViewGroup.LayoutParams.MATCH_PARENT, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
           topDp: Int = 0, bottomDp: Int = 0, weight: Float = 0f) =
        LinearLayout.LayoutParams(width, height, weight).apply { topMargin = dp(topDp); bottomMargin = dp (bottomDp) }

    // drawables

    fun rounded(fill: Int, radiusDp: Int, strokeColor: Int? = null,strokeWidthPx: Int = px(1f)) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != null) setStroke(strokeWidthPx, strokeColor)
        }

    /** A background that also shows a clear 3px focus ring for keyboard / switch / TalkBack focus. */
    fun interactiveBackground(fill: Int, radiusDp: Int, strokeColor: Int? = null): Drawable =
        StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), rounded(fill, radiusDp, primary, px(3f)))
            addState(intArrayOf(android.R.attr.state_pressed), rounded(elevated, radiusDp, strokeColor ?: elevated, px(1.5f)))
            addState(intArrayOf(), rounded(fill, radiusDp, strokeColor, px(1.5f)))
        }

    fun fieldBackground(error: Boolean): Drawable = StateListDrawable().apply {
        val idle = if (error) coral else fieldBorder
        addState(intArrayOf(android.R.attr.state_focused), rounded(bg, 14, if (error) coral else primary, px(3f)))
        addState(intArrayOf(), rounded(bg, 14, idle, px(if (error) 2.5f else 1.5f)))
    }

    // text
    fun text(value: String, sizeSp: Float = 15f, color: Int = ink, weight: Int = 400, display: Boolean = false) =
        TextView(a).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)
            includeFontPadding = false
            BrandType.apply(this, display, weight)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    fun heading(value: String, sizeSp: Float = 26f, color: Int = primary) = text(value, sizeSp, color, 600, true).also {
        markHeading(it) }

    fun markHeading(view: View) {
        if (Build.VERSION.SDK_INT >= 28) view.isAccessibilityHeading = true
    }

    // accessibility helpers
    /** Tells TalkBack what kind of control a plain View is, e.g. a Button, and (optionally) its checked state. */
    fun role(view: View, type: Class<*>, checked: Boolean? = null) {
        view.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = type.name
                if (checked != null) {
                    info.isCheckable = true
                    info.isChecked = checked
                }
            }
        })
    }

    fun announce(view: View, message: String) {
        @Suppress("DEPRECATION")
        view.announceForAccessibility(message)
    }

    // controls
    private val careKinds = setOf(
        "Feeding", "Medication", "Water", "Playtime", "Vet Visit", "Grooming",
        "Home", "Pets", "Care", "Notes", "More", "Plus", "Back", "Close",
        "Arrow", "Chevron", "Check", "Energy", "Food", "Mood", "Mic", "Wave",
        "Link", "Eye", "Share", "QR", "Microchip", "Edit", "Heart", "Weight",
        "Emergency", "Google", "Apple", "Facebook"
    )

    fun icon(kind: String, sizeDp: Int = 26, tint: Int = primary): View =
        (if (kind in careKinds) CareArtwork(a, kind, tint) else DetailIcon (a, kind, tint)).apply {
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

    /** A tappable icon with a 48dp * 48dp touch area (Android's recommended minimum). */
    fun iconButton(kind: String, description: String, onClick: () -> Unit): FrameLayout =
        FrameLayout(a).apply {
            contentDescription = description
            isFocusable = true
            isClickable = true
            background = interactiveBackground(Color.TRANSPARENT, 24)
            setOnClickListener { onClick() }
            addView(icon(kind, 26), FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            role(this, android.widget.Button::class.java)
        }.also { it.layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)) }

    /** Filled (primary) or outlined (secondary) text button, at least 52dp tall. */
    fun button(label: String, primaryStyle: Boolean = true, onClick: () -> Unit): TextView =
        text(label, 16f, if (primaryStyle) onLavender else primary, 700).apply {
            gravity = Gravity.CENTER
            minHeight = dp(52)
            setPadding(dp(20), dp(10), dp(20), dp(10))
            isFocusable = true
            isClickable = true
            background = if (primaryStyle) interactiveBackground(lavender, 28)  else interactiveBackground(bg, 28, fieldBorder)
            setOnClickListener { onClick() }
            role(this, android.widget.Button::class.java)
        }

    /** A low-emphasis text-only button with a 48dp minimum height (used for "Change", "Clear"). */
    fun textButton(label: String, description: String? = null, onClick: () -> Unit): TextView =
        text(label, 15f, primary, 700).
                apply {
                    gravity = Gravity.CENTER
                    minHeight = dp(48)
                    minWidth = dp(48)
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    isFocusable = true
                    isClickable = true
                    background = interactiveBackground(Color.TRANSPARENT, 16)
                    if (description != null) contentDescription = description
                    setOnClickListener { onClick() }
                    role(this, android.widget.Button::class.java)
                }

    fun defaultPortrait(species: String): Int = when (species) {
        "Dog" -> R.drawable.pdf_bailey
        "Rabbit" -> R.drawable.pdf_luna
        else -> R.drawable.pdf_mochi
    }

    /** Round pet picture with a ring. Uses the saved photo when there is one, otherwise a stock illustration. */
    fun portrait(photoUri: String, species: String, sizeDp: Int, description: String, fallbackRes: Int? = null): FrameLayout {
        val frame = FrameLayout(a)
        frame.background = rounded(primary, sizeDp)
        frame.setPadding(dp(3), dp(3), dp(3), dp(3))
        val image = ImageView(a).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(surface, sizeDp)
            clipToOutline = true
            contentDescription = description
        }
        var loaded = false
        if (photoUri.isNotBlank()) {
            loaded = runCatching { image.setImageURI(Uri.parse(photoUri));
            image.drawable != null }.getOrDefault(false)
        }
        if (!loaded) image.setImageResource(fallbackRes ?: defaultPortrait(species))
        frame.addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        frame.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return frame
    }

    // screen frame
    /** Root of every pet screen: themed background, system-bar insets (status bar, cutout, keyboard). */
    fun screenRoot(): LinearLayout {
        WindowCompat.setDecorFitsSystemWindows(a.window, false)
        WindowCompat.getInsetsController(a.window, a.window.decorView).
        apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
        @Suppress("DEPRECATION")
        run { a.window.statusBarColor = bg; a.window.navigationBarColor = bg }
        val root = column().apply { setBackgroundColor(bg) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        return root
    }

    /** A vertically scrolling area that takes all spare height. Returns the scroll view and its content column. */
    fun scrollBody(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(a).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        val body = column().apply { setPadding(dp(20), dp(8), dp(20), dp(16)) }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        scroll.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        return scroll to body
    }

    // form fields
    /** Label above, input, then helper text. An error message replaces the helper text (Material guidance). */
    inner class LabeledField(
        val root: LinearLayout,
        val input: EditText?,
        private val helperView: TextView,
        private val errorView: TextView,
        private val focusTarget: View
    ) {
        private var helperText: String? = null

        fun setHelper(message: String?) {
            helperText = message
            if (errorView.visibility != View.VISIBLE) showHelper()
        }

        private fun showHelper() {
            helperView.text = helperText.orEmpty()
            helperView.visibility = if (helperText.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        /** Pass null to clear the error. A non-null message is shown, announced, and the border turns red. */
        fun showError(message: String?) {
            if (message == null) {
                errorView.visibility = View.GONE
                errorView.text = ""
                input?.background = fieldBackground(false)
                showHelper()
            } else {
                helperView.visibility = View.GONE
                errorView.text = message
                errorView.visibility = View.VISIBLE
                input?.background = fieldBackground(true)
            }
        }

        fun focus() {
            focusTarget.requestFocus()
            val message = errorView.text.toString()
            if (message.isNotBlank()) announce(focusTarget, message)
        }
    }

    private fun fieldLabel(label: String, optional: Boolean): TextView =
        text(if (optional) "$label (optional)" else label, 14f, ink, 700)

    private fun helperAndError(parent: LinearLayout, helper: String?): Pair<TextView, TextView> {
        val helperView = text(helper.orEmpty(), 13f, sub).apply {
            visibility = if (helper.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        val errorView = text("", 13f, coral, 700).apply {
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        parent.addView(helperView, lp(topDp = 6))
        parent.addView(errorView, lp(topDp = 6))
        return helperView to errorView
    }

    fun textField(
        label: String,
        optional: Boolean,
        initial: String,
        helper: String? = null,
        inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
        multiline: Boolean = false,
        maxLength: Int = 0,
        onChange: (String) -> Unit
    ): LabeledField {
        val container = column()
        val labelView = fieldLabel(label, optional)
        container.addView(labelView, lp(bottomDp = 6))
        val edit = EditText(a).apply {
            id = View.generateViewId()
            setText(initial)
            textSize = 16f
            setTextColor(ink)
            BrandType.apply(this, false, 500)
            includeFontPadding = false
            background = fieldBackground(false)
            setPadding(dp(14), dp(12), dp(12), dp(12))
            minHeight = dp(52)
            this.inputType = if (multiline) inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE else inputType
            if (multiline) { minLines = 3; gravity = Gravity.TOP or Gravity.START } else isSingleLine = true
            if (maxLength > 0) filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
        }
        labelView.labelFor = edit.id
        container.addView(edit, lp())
        val (helperView, errorView) = helperAndError(container, helper)
        val field = LabeledField(container, edit, helperView, errorView, edit)
        field.setHelper(helper)
        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                field.showError(null)
                onChange(s?.toString().orEmpty())
            }
        })
        return field
    }

    /**
     * A group of mutually exclusive choices shown as chips (better than a dropdown for 3-4 options).
     * Each chip is announced by TalkBack as a radio button with its selected state.
     */
    fun choiceChips(label: String, optional: Boolean, options: List<String>, selected: String, onSelect: (String) -> Unit): LinearLayout {
        val container = column()
        container.addView(fieldLabel(label, optional), lp(bottomDp = 8))
        val perRow = if (a.resources.configuration.fontScale > 1.3f) 2 else options.size
        val chips = mutableListOf<TextView>()
        var current = selected

        fun paint() {
            options.forEachIndexed { index, option ->
                val chip = chips[index]
                val on = option == current
                chip.setTextColor(if (on) onLavender else ink)
                chip.background = if (on) interactiveBackground(lavender, 24) else interactiveBackground(bg, 24, fieldBorder)
                role(chip, RadioButton::class.java, on)
            }
        }

        options.chunked(perRow).forEach { group ->
            val line = row()
            group.forEach { option ->
                val chip = text(option, 15f, ink, 700).apply {
                    gravity = Gravity.CENTER
                    minHeight = dp(48)
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    isFocusable = true
                    isClickable = true
                    setOnClickListener { current = option; paint(); onSelect(option) }
                }
                chips.add(chip)
                line.addView(chip, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                })
            }
            container.addView(line, lp())
        }
        paint()
        return container
    }

    /** Read-only field: tap to open the calendar, "Clear" removes it. The calendar blocks future dates. */
    fun dateField(label: String, optional: Boolean, initialIso: String, helper: String?, onChange: (String) -> Unit): LabeledField {
        val container = column()
        var iso = initialIso
        container.addView(fieldLabel(label, optional), lp(bottomDp = 6))

        val value = text("", 16f, ink, 500)
        val box = row().apply {
            minimumHeight = dp(52)
            setPadding(dp(14), dp(10), dp(8), dp(10))
            isFocusable = true
            isClickable = true
            background = fieldBackground(false)
        }
        box.addView(value, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(icon("Down", 22), LinearLayout.LayoutParams(dp(22), dp(22)))

        lateinit var clear: TextView
        fun update() {
            val shown = if (iso.isBlank()) "" else PetRules.displayDate(iso)
            value.text = if (shown.isBlank()) "Not set" else shown
            value.setTextColor(if (shown.isBlank()) sub else ink)
            box.contentDescription = if (shown.isBlank()) "$label, not set. Double tap to choose a date." else "$label, $shown. Double tap to change."
            clear.visibility = if (shown.isBlank()) View.GONE else View.VISIBLE
        }
        clear = textButton("Clear", "Clear $label") { iso = ""; onChange(iso); update() }

        val line = row()
        line.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        line.addView(clear, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(4) })
        container.addView(line, lp())

        val (helperView, errorView) = helperAndError(container, helper)
        val field = LabeledField(container, null, helperView, errorView, box)
        field.setHelper(helper)

        box.setOnClickListener {
            val start = PetRules.parseIso(iso) ?: Calendar.getInstance()
            val dialog = DatePickerDialog(a, { _, year, month, day ->
                iso = PetRules.toIso(year, month, day)
                onChange(iso)
                field.showError(null)
                update()
            }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH))
            dialog.datePicker.maxDate = System.currentTimeMillis()
            dialog.show()
        }
        role(box, android.widget.Button::class.java)
        update()
        return field
    }
}