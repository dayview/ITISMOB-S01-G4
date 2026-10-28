package com.purrfolio.mco1

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.purrfolio.mco1.databinding.CareHistoryEmptyBinding
import com.purrfolio.mco1.databinding.CareHistoryEntryBinding
import com.purrfolio.mco1.databinding.DialogAddReminderBinding
import com.purrfolio.mco1.databinding.LegacyScreenHeaderBinding
import com.purrfolio.mco1.databinding.FloatingBottomNavBinding
import com.purrfolio.mco1.databinding.ScreenAppThemeBinding
import com.purrfolio.mco1.databinding.ScreenCareHistoryBinding
import com.purrfolio.mco1.databinding.ScreenQuickLogBinding
import com.purrfolio.mco1.databinding.ScreenWelcomeBinding
import com.purrfolio.mco1.databinding.ThemeOptionBinding
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Coordinates screen navigation, local state, and XML-backed screen controllers. */
class MainActivity : AppCompatActivity() {
    private var screen = "Welcome"
    private var pendingCareCategory = "Feeding"
    private var petIndex = 0
    private val preferences by lazy { getSharedPreferences("purrfolio_demo", MODE_PRIVATE) }
    private val pets = mutableListOf<JSONObject>()
    private val logs = mutableListOf<JSONObject>()
    private val reminders = mutableListOf<JSONObject>()
    private var night = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySaved(this)
        super.onCreate(savedInstanceState)
        night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        restore()
        val destination = savedInstanceState?.getString("screen")
            ?: if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
                intent.getStringExtra("preview_screen") ?: "Welcome" else "Welcome"
        render(destination)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("screen", screen)
        super.onSaveInstanceState(outState)
    }

    private fun selectedPet() = pets.getOrNull(petIndex) ?: pets.first()
    private fun petName() = selectedPet().optString("name", "Mochi")
    private fun note(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun render(destination: String) {
        if (destination.startsWith("Quick Log:")) {
            pendingCareCategory = destination.substringAfter(":")
            render("Quick Log")
            return
        }
        screen = destination
        when (destination) {
            "Welcome" -> { showWelcome(); return }
            "App Theme" -> { showAppTheme(); return }
            "Quick Log" -> { showQuickLog(); return }
            "Care History" -> { showCareHistory(); return }
        }
        if (destination in ReferenceScreens.destinations) {
            val view = ReferenceScreens(this, selectedPet(), { render(it) }, { choosePet() }).build(destination)
            setContentView(view)
            ViewCompat.requestApplyInsets(view)
            return
        }
        if (destination in PhaseTwoScreens.destinations) {
            val view = PhaseTwoScreens(
                this, selectedPet(), pets, logs, reminders,
                { render(it) }, { choosePet() },
                { index -> petIndex = index; render("Pet Detail") },
                { values, adding ->
                    val updated = if (adding) JSONObject() else JSONObject(selectedPet().toString())
                    listOf("name", "species", "breed", "birthday", "sex", "notes").forEach { key ->
                        if (values.containsKey(key)) updated.put(key, values[key])
                    }
                    if (adding) { pets.add(updated); petIndex = pets.lastIndex } else pets[petIndex] = updated
                    save(); note("Pet profile saved on this device."); render("Pet Detail")
                },
                { kind, detail ->
                    logs.add(0, JSONObject().put("pet", petName()).put("kind", kind).put("text", detail).put("date", stamp()))
                    save(); note("Care log saved on this device."); render("Care History")
                },
                { addReminder() },
                { reminder -> reminder.put("done", !reminder.optBoolean("done")); save(); render("Reminders") },
                { render("Home") }
            ).build(destination)
            setContentView(view)
            ViewCompat.requestApplyInsets(view)
            return
        }
        render("More")
    }

    private fun applyLegacyInsets(root: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.background)
        window.navigationBarColor = getColor(R.color.background)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun bindLegacyHeader(binding: LegacyScreenHeaderBinding, title: String) {
        binding.screenTitle.text = title
        binding.petChooser.text = "♡ ${petName()} ⌄"
        BrandType.apply(binding.backButton, true, 600)
        BrandType.apply(binding.screenTitle, true, 600)
        BrandType.apply(binding.petChooser, false, 700)
        binding.backButton.setOnClickListener { render("Care") }
        binding.petChooser.setOnClickListener { choosePet() }
    }

    private fun bindCareDock(binding: FloatingBottomNavBinding, screen: String) {
        ReferenceScreens(this, selectedPet(), { render(it) }, { choosePet() }).bindDock(binding, screen)
    }

    private fun showQuickLog() {
        val binding = ScreenQuickLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyLegacyInsets(binding.quickLogRoot)
        bindLegacyHeader(binding.header, "Quick Log")
        bindCareDock(binding.bottomNav, "Quick Log")
        BrandType.apply(binding.eyebrow, false, 700)
        BrandType.apply(binding.pageTitle, true, 600)
        BrandType.apply(binding.pageSubtitle, false, 400)
        BrandType.apply(binding.activityLabel, false, 700)
        BrandType.apply(binding.saveButton, false, 700)
        binding.pageSubtitle.text = "A quick record for ${petName()}."
        val categories = listOf("Feeding", "Medication", "Vitamins", "Water", "Playtime", "Grooming", "Vet Visit", "Other")
        binding.categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.categorySpinner.setSelection(categories.indexOf(pendingCareCategory).coerceAtLeast(0))
        binding.saveButton.setOnClickListener {
            val detail = binding.detailInput.text.toString().trim()
            if (detail.isBlank()) note("Add a short care note first.")
            else {
                logs.add(0, JSONObject().put("pet", petName()).put("kind", categories[binding.categorySpinner.selectedItemPosition])
                    .put("text", detail).put("date", stamp()))
                save(); note("Care log saved on this device."); render("Care History")
            }
        }
    }

    private fun showCareHistory() {
        val binding = ScreenCareHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyLegacyInsets(binding.careHistoryRoot)
        bindLegacyHeader(binding.header, "Care History")
        bindCareDock(binding.bottomNav, "Care History")
        BrandType.apply(binding.eyebrow, false, 700)
        BrandType.apply(binding.pageTitle, true, 600)
        BrandType.apply(binding.pageSubtitle, false, 400)
        BrandType.apply(binding.newEntryButton, false, 700)
        binding.pageSubtitle.text = "your saved care moments"
        val filtered = logs.filter { it.optString("pet") == petName() }
        if (filtered.isEmpty()) {
            val empty = CareHistoryEmptyBinding.inflate(layoutInflater, binding.entryContainer, false)
            BrandType.apply(empty.emptyTitle, false, 700)
            BrandType.apply(empty.emptyDescription, false, 400)
            binding.entryContainer.addView(empty.root)
        }
        filtered.forEach { entry ->
            val item = CareHistoryEntryBinding.inflate(layoutInflater, binding.entryContainer, false)
            item.entryTitle.text = entry.optString("kind")
            item.entryDescription.text = "${entry.optString("date")} • ${entry.optString("text")}"
            BrandType.apply(item.entryIcon, true, 600)
            BrandType.apply(item.entryTitle, false, 700)
            BrandType.apply(item.entryDescription, false, 400)
            item.root.setOnClickListener {
                AlertDialog.Builder(this).setTitle("Care entry").setMessage(entry.optString("text"))
                    .setPositiveButton("Close", null)
                    .setNegativeButton("Delete") { _, _ -> logs.remove(entry); save(); render("Care History") }
                    .show()
            }
            binding.entryContainer.addView(item.root)
        }
        binding.newEntryButton.setOnClickListener { render("Quick Log") }
    }

    private fun showWelcome() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.welcome_background)
        window.navigationBarColor = getColor(R.color.welcome_background)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
        val binding = ScreenWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.welcomeRoot) { view, insets ->
            val safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(binding.welcomeRoot)
        BrandType.apply(binding.welcomeTagline, true, 600)
        BrandType.apply(binding.getStartedButton, false, 700)
        BrandType.apply(binding.logInButton, false, 700)
        BrandType.apply(binding.continueWithLabel, false, 400)
        binding.getStartedButton.setOnClickListener { render("Create Account") }
        binding.logInButton.setOnClickListener { render("Log In") }
        listOf(
            binding.googleButton to "Google",
            binding.appleButton to "Apple",
            binding.facebookButton to "Facebook"
        ).forEach { (view, provider) ->
            view.setOnClickListener {
                AlertDialog.Builder(this).setTitle("Frontend preview")
                    .setMessage("$provider sign-in requires authentication integration.")
                    .setPositiveButton("OK", null).show()
            }
        }
    }

    private fun showAppTheme() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.welcome_background)
        window.navigationBarColor = getColor(R.color.welcome_background)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
        val binding = ScreenAppThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.themeRoot) { view, insets ->
            val safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                androidx.core.view.WindowInsetsCompat.Type.displayCutout() or
                androidx.core.view.WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(binding.themeRoot)
        BrandType.apply(binding.themeHeading, true, 600)
        BrandType.apply(binding.themeSubtitle, false, 400)
        binding.themeBack.setOnClickListener { render("Account Settings") }
        val selected = AppTheme.selected(this)
        fun option(view: ThemeOptionBinding, choice: String, title: String, description: String) {
            val checked = selected == choice
            view.optionTitle.text = title
            view.optionDescription.text = description
            BrandType.apply(view.optionTitle, false, 700)
            BrandType.apply(view.optionDescription, false, 400)
            view.root.setBackgroundResource(if (checked) R.drawable.theme_option_selected else R.drawable.theme_option_normal)
            view.optionTitle.setTextColor(getColor(if (checked) R.color.screen_primary else R.color.screen_text))
            view.optionRadio.isChecked = checked
            view.root.contentDescription = "$title, ${if (checked) "selected" else "not selected"}. $description"
            view.root.setOnClickListener {
                if (!checked) {
                    AppTheme.select(this, choice)
                    render("App Theme")
                }
            }
        }
        option(binding.lightOption, AppTheme.LIGHT, "Light Mode", "Always use PURRFOLIO's light theme.")
        option(binding.darkOption, AppTheme.DARK, "Dark Mode", "Always use PURRFOLIO's dark theme.")
        option(binding.systemOption, AppTheme.SYSTEM, "System Default", "Follow your device appearance.")
        val nav = binding.bottomNav
        listOf(nav.navHome to "Home", nav.navPets to "Pets", nav.navLogCare to "Care",
            nav.navCare to "Pet Timeline", nav.navMore to "More").forEach { (view, target) ->
            view.setOnClickListener { render(target) }
        }
        listOf(nav.navHome, nav.navPets, nav.navCare, nav.navMore).forEach { cell ->
            (cell.getChildAt(1) as? TextView)?.let { BrandType.apply(it, false, 600) }
        }
    }

    private fun choosePet() {
        AlertDialog.Builder(this).setTitle("Choose a pet")
            .setItems(pets.map { it.optString("name") }.toTypedArray()) { _, index ->
                petIndex = index; render(screen)
            }
            .setPositiveButton("Add pet") { _, _ -> render("Add Your Pet") }
            .setNegativeButton("Cancel", null).show()
    }

    private fun addReminder() {
        val binding = DialogAddReminderBinding.inflate(layoutInflater)
        AlertDialog.Builder(this).setTitle("New care reminder").setView(binding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val title = binding.reminderTitle.text.toString().trim()
                val whenText = binding.reminderWhen.text.toString().trim()
                if (title.isBlank() || whenText.isBlank()) {
                    note("Both fields are required")
                    return@setPositiveButton
                }
                reminders.add(0, JSONObject().put("title", title).put("when", whenText)
                    .put("pet", petName()).put("done", false))
                save(); render("Reminders")
            }.show()
    }

    private fun stamp() = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date())

    private fun restore() {
        fun pull(key: String, out: MutableList<JSONObject>) {
            val array = JSONArray(preferences.getString(key, "[]"))
            for (index in 0 until array.length()) out.add(array.getJSONObject(index))
        }
        try { pull("pets", pets); pull("logs", logs); pull("reminders", reminders) }
        catch (_: Exception) { pets.clear(); logs.clear(); reminders.clear() }
        if (pets.isEmpty()) {
            pets.add(JSONObject().put("name", "Mochi").put("species", "Cat").put("breed", "Mixed Breed")
                .put("birthday", "2024-09-26").put("sex", "Female")
                .put("notes", "Playful, loves treats and sunny spots."))
            pets.add(JSONObject().put("name", "Bailey").put("species", "Dog")
                .put("breed", "Golden Retriever").put("birthday", "2022-05-13")
                .put("sex", "Male").put("notes", "Enjoys long walks."))
            save()
        }
    }

    private fun save() {
        fun json(items: MutableList<JSONObject>) = JSONArray().apply { items.forEach { put(it) } }.toString()
        preferences.edit().putString("pets", json(pets)).putString("logs", json(logs))
            .putString("reminders", json(reminders)).apply()
    }

    @Deprecated("Back navigation handled by main tab scaffold")
    override fun onBackPressed() { if (screen=="Home" || screen=="Welcome") super.onBackPressed() else render(when(screen){"Create Account","Log In","Reset Password"->"Welcome";"Pet Detail","Edit Pet Profile","Add Your Pet"->"Pets";"Quick Log","Voice Log","Sound Journal","Care History"->"Care";"Paperless Inbox","Pet Timeline","Pet Passport","Browse Records","Vaccination Card"->"Records";"Personal Information","Account Settings","Notifications"->"Profile";"App Theme"->"Account Settings";else->"More"}) }
}
