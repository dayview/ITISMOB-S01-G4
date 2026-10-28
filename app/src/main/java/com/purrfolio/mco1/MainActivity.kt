package com.purrfolio.mco1

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.speech.RecognizerIntent
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/** Coordinates screen navigation, local state, and XML-backed screen controllers. */
class MainActivity : AppCompatActivity() {
    private var screen = "Welcome"
    private val screenHistory = ArrayDeque<String>()
    private var pendingCareCategory = "Feeding"
    private var petIndex = 0
    private val preferences by lazy { getSharedPreferences("purrfolio_demo", MODE_PRIVATE) }
    private val pets = mutableListOf<JSONObject>()
    private val logs = mutableListOf<JSONObject>()
    private val reminders = mutableListOf<JSONObject>()
    private var night = false
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStartedAt = 0L
    private var player: MediaPlayer? = null
    private val speechResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val words = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        words?.firstOrNull()?.let { captureVoice(it) } ?: note("No speech was recognized.")
    }
    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSoundRecording() else note("Microphone permission is needed to record audio.")
    }
    private var pendingNewPetPhotoUri: String? = null
    private var petPhotoPickerTarget = "Pet Detail"
    private val petPhotoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            if (petPhotoPickerTarget == "Add Your Pet") {
                pendingNewPetPhotoUri = uri.toString()
            } else {
                selectedPet().put("photoUri", uri.toString())
                save()
            }
            render(petPhotoPickerTarget)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySaved(this)
        super.onCreate(savedInstanceState)
        night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        restore()
        savedInstanceState?.getStringArrayList("screenHistory")?.forEach { screenHistory.addLast(it) }
        pendingNewPetPhotoUri = savedInstanceState?.getString("pendingNewPetPhotoUri")
        petPhotoPickerTarget = savedInstanceState?.getString("petPhotoPickerTarget") ?: "Pet Detail"
        val destination = savedInstanceState?.getString("screen")
            ?: if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
                intent.getStringExtra("preview_screen") ?: "Welcome" else "Welcome"
        render(destination, recordHistory = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("screen", screen)
        outState.putStringArrayList("screenHistory", ArrayList(screenHistory))
        outState.putString("pendingNewPetPhotoUri", pendingNewPetPhotoUri)
        outState.putString("petPhotoPickerTarget", petPhotoPickerTarget)
        super.onSaveInstanceState(outState)
    }

    private fun selectedPet() = pets.getOrNull(petIndex) ?: pets.first()
    private fun petId(pet: JSONObject): String {
        if (!pet.has("id")) { pet.put("id", UUID.randomUUID().toString()); save() }
        return pet.getString("id")
    }
    private fun entryPetId(entry: JSONObject) = entry.optString("petId").ifBlank {
        pets.firstOrNull { it.optString("name") == entry.optString("pet") }?.let { petId(it) } ?: ""
    }
    private fun newLog(pet: JSONObject, kind: String, detail: String, source: String = "text", occurredAt: Long = System.currentTimeMillis()): JSONObject =
        JSONObject().put("id", UUID.randomUUID().toString()).put("petId", petId(pet))
            .put("pet", pet.optString("name")).put("kind", kind).put("text", detail)
            .put("source", source).put("occurredAt", occurredAt)
            .put("date", SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(occurredAt)))
    private fun petName() = selectedPet().optString("name", "Mochi")
    private fun note(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun editPetPhoto(addingPet:Boolean=false) {
        val target = if(addingPet) "Add Your Pet" else screen
        val photoUri = if(addingPet) pendingNewPetPhotoUri else selectedPet().optString("photoUri").takeIf{it.isNotBlank()}
        val hasPhoto = !photoUri.isNullOrBlank()
        val options = if (hasPhoto) arrayOf("Choose from device", "Remove photo", "Cancel") else arrayOf("Choose from device", "Cancel")
        AlertDialog.Builder(this).setTitle(if(addingPet)"Pet photo" else "${petName()}'s photo").setItems(options) { _, index ->
            when {
                index == 0 -> { petPhotoPickerTarget=target;petPhotoPicker.launch(arrayOf("image/*")) }
                index == 1 && hasPhoto -> {
                    runCatching { contentResolver.releasePersistableUriPermission(Uri.parse(photoUri),Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    if(addingPet)pendingNewPetPhotoUri=null else{selectedPet().remove("photoUri");save()}
                    render(target)
                }
            }
        }.show()
    }

    private fun navigateBack() {
        if (screenHistory.isNotEmpty()) {
            render(screenHistory.removeLast(), false)
        } else {
            render(when(screen){
                "Create Account","Log In","Reset Password"->"Welcome"
                "Pet Detail","Edit Pet Profile","Add Your Pet"->"Pets"
                "Quick Log","Voice Log","Sound Journal","Care History"->"Care"
                "Paperless Inbox","Pet Timeline","Pet Passport","Browse Records","Vaccination Card"->"Records"
                "Personal Information","Account Settings","Notifications"->"Profile"
                "App Theme"->"Account Settings"
                else->"More"
            }, false)
        }
    }

    private fun render(destination: String, recordHistory: Boolean = true) {
        if (destination.startsWith("Quick Log:")) {
            pendingCareCategory = destination.substringAfter(":")
            render("Quick Log", recordHistory)
            return
        }
        if (screen == "Add Your Pet" && destination != "Add Your Pet") pendingNewPetPhotoUri = null
        if (recordHistory && destination != screen) screenHistory.addLast(screen)
        screen = destination
        when (destination) {
            "Welcome" -> { showWelcome(); return }
            "App Theme" -> { showAppTheme(); return }
            "Quick Log" -> { showQuickLog(); return }
            "Care History" -> { showCareHistory(); return }
        }
        if (destination in ReferenceScreens.destinations) {
            val view = ReferenceScreens(this, selectedPet(), { render(it) }, { choosePet() }, { editPetPhoto() }, { navigateBack() }).build(destination)
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
                    listOf("name", "species", "breed", "birthday", "sex", "notes", "photoUri").forEach { key ->
                        if (values.containsKey(key)) updated.put(key, values[key])
                    }
                    if (adding) { pets.add(updated); petIndex = pets.lastIndex } else {
                        updated.put("id", petId(selectedPet()))
                        pets[petIndex] = updated
                    }
                    save(); note("Pet profile saved on this device."); render("Pet Detail")
                },
                { kind, detail ->
                    logs.add(0, newLog(selectedPet(), kind, detail))
                    save(); note("Care log saved on this device."); render("Care History")
                },
                { recognizeVoice() }, { transcript -> captureVoice(transcript) },
                { toggleSoundRecording() }, { soundRows() }, { entry -> playRecording(entry) },
                { addReminder() },
                { reminder -> reminder.put("done", !reminder.optBoolean("done")); save(); render("Reminders") },
                { render("Home") },
                { navigateBack() },
                pendingNewPetPhotoUri,
                { adding -> editPetPhoto(adding) }
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
        binding.petChooser.text = "${petName()} ⌄"
        BrandType.apply(binding.backButton, true, 600)
        BrandType.apply(binding.screenTitle, true, 600)
        BrandType.apply(binding.petChooser, false, 700)
        binding.backButton.setOnClickListener { navigateBack() }
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
                logs.add(0, newLog(selectedPet(), categories[binding.categorySpinner.selectedItemPosition], detail))
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
        val filtered = logs.filter { entryPetId(it) == petId(selectedPet()) }.sortedByDescending { it.optLong("occurredAt", 0L) }
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
                val actions = if (entry.optString("audioFile").isNotBlank()) arrayOf("Play recording", "Edit", "Delete") else arrayOf("Edit", "Delete")
                AlertDialog.Builder(this).setTitle(entry.optString("kind")).setMessage(entry.optString("text"))
                    .setItems(actions) { _, index ->
                        when (actions[index]) {
                            "Play recording" -> playRecording(entry)
                            "Edit" -> editLog(entry)
                            "Delete" -> confirmDelete(entry)
                        }
                    }.setNegativeButton("Close", null).show()
            }
            binding.entryContainer.addView(item.root)
        }
        binding.newEntryButton.setOnClickListener { render("Quick Log") }
    }

    private fun editLog(entry: JSONObject) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 0) }
        val kinds = arrayOf("Feeding", "Medication", "Vitamins", "Water", "Playtime", "Grooming", "Vet Visit", "Other", "Observation")
        val category = android.widget.Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, kinds)
            setSelection(kinds.indexOf(entry.optString("kind")).coerceAtLeast(0))
        }
        val detail = android.widget.EditText(this).apply { setText(entry.optString("text")); hint = "Details" }
        layout.addView(category); layout.addView(detail)
        AlertDialog.Builder(this).setTitle("Edit care entry").setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val value = detail.text.toString().trim()
                if (value.isBlank()) { note("Add a short care note first."); return@setPositiveButton }
                entry.put("kind", category.selectedItem.toString()).put("text", value)
                save(); render("Care History")
            }.setNegativeButton("Cancel", null).show()
    }

    private fun confirmDelete(entry: JSONObject) {
        AlertDialog.Builder(this).setTitle("Delete care entry?").setMessage("This removes the entry and its audio from this device.")
            .setPositiveButton("Delete") { _, _ ->
                entry.optString("audioFile").takeIf { it.isNotBlank() }?.let { File(filesDir, it).delete() }
                logs.remove(entry); save(); render("Care History")
            }.setNegativeButton("Cancel", null).show()
    }

    private fun recognizeVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your pet's care")
        }
        runCatching { speechResult.launch(intent) }.onFailure { note("Speech recognition is unavailable on this device.") }
    }

    private fun captureVoice(transcript: String) {
        val draft = CareCapture.parse(transcript, pets.map { it.optString("name") })
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 8, 40, 0) }
        val transcriptView = TextView(this).apply { text = "Heard: $transcript" }
        fields.addView(transcriptView)
        val names = listOf("Select pet") + pets.map { it.optString("name") }
        val chosenPet = android.widget.Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names)
            setSelection(names.indexOf(draft.pet).coerceAtLeast(0))
        }
        val kinds = arrayOf("Select activity", "Feeding", "Medication", "Vitamins", "Water", "Playtime", "Grooming", "Vet Visit", "Other")
        val kind = android.widget.Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, kinds)
            setSelection(kinds.indexOf(draft.activity).coerceAtLeast(0))
        }
        val amount = android.widget.EditText(this).apply { hint = "Amount (if applicable)"; setText(draft.amount.orEmpty()) }
        val time = android.widget.EditText(this).apply { hint = "Time (e.g. 7:30 AM)"; setText(draft.time.orEmpty()) }
        fields.addView(TextView(this).apply { text = "Pet" }); fields.addView(chosenPet)
        fields.addView(TextView(this).apply { text = "Activity" }); fields.addView(kind)
        fields.addView(amount); fields.addView(time)
        AlertDialog.Builder(this).setTitle("Confirm care details").setView(fields)
            .setPositiveButton("Save", null).setNegativeButton("Cancel", null).create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (chosenPet.selectedItemPosition == 0) { note("Choose the pet."); return@setOnClickListener }
                        if (kind.selectedItemPosition == 0) { note("Choose the activity."); return@setOnClickListener }
                        if (time.text.toString().isBlank()) { time.error = "Enter the time"; return@setOnClickListener }
                        val pet = pets[chosenPet.selectedItemPosition - 1]
                        val detail = listOf(amount.text.toString().trim(), "at ${time.text.toString().trim()}", transcript).filter { it.isNotBlank() }.joinToString(" · ")
                        logs.add(0, newLog(pet, kind.selectedItem.toString(), detail, "voice").put("transcript", transcript))
                        save(); dismiss(); note("Care log saved on this device."); render("Care History")
                    }
                }; show()
            }
    }

    private fun soundRows() = logs.filter { entryPetId(it) == petId(selectedPet()) && it.optString("audioFile").isNotBlank() }
    private fun toggleSoundRecording() {
        if (recorder != null) { stopSoundRecording(); return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        else startSoundRecording()
    }
    private fun startSoundRecording() {
        val file = File(filesDir, "sound_${UUID.randomUUID()}.m4a")
        try {
            @Suppress("DEPRECATION")
            val active = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare(); start()
            }
            recorder = active; recordingFile = file; recordingStartedAt = System.currentTimeMillis()
            note("Recording started. Tap the button again to stop.")
        } catch (error: Exception) { recorder?.release(); recorder = null; file.delete(); note("Could not start audio recording.") }
    }
    private fun stopSoundRecording() {
        val file = recordingFile ?: return
        val duration = System.currentTimeMillis() - recordingStartedAt
        val success = runCatching { recorder?.stop(); true }.getOrDefault(false)
        recorder?.release(); recorder = null; recordingFile = null
        if (!success || duration < 1000L) { file.delete(); note("Record at least one second of audio."); return }
        val input = android.widget.EditText(this).apply { hint = "What did you hear? (e.g. coughing)" }
        AlertDialog.Builder(this).setTitle("Save sound observation").setView(input)
            .setPositiveButton("Save", null).setNegativeButton("Discard") { _, _ -> file.delete() }
            .setOnCancelListener { file.delete() }.create().apply {
                setOnShowListener { getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val detail = input.text.toString().trim()
                    if (detail.isBlank()) { input.error = "Describe the sound"; return@setOnClickListener }
                    logs.add(0, newLog(selectedPet(), "Observation", detail, "audio", recordingStartedAt)
                        .put("audioFile", file.name).put("durationMs", duration))
                    save(); dismiss(); render("Sound Journal")
                } }; show()
            }
    }
    private fun playRecording(entry: JSONObject) {
        val file = File(filesDir, entry.optString("audioFile").substringAfterLast('/'))
        if (!file.isFile) { note("Recording file is missing."); return }
        player?.release()
        runCatching {
            player = MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); setOnCompletionListener { it.release(); player = null }; start() }
        }.onFailure { note("Could not play this recording.") }
    }

    override fun onDestroy() {
        recorder?.release(); recordingFile?.delete(); player?.release()
        super.onDestroy()
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
        if (night) binding.welcomeLogo.clearColorFilter()
        else binding.welcomeLogo.setColorFilter(getColor(R.color.welcome_primary), PorterDuff.Mode.SRC_IN)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.welcomeRoot) { view, insets ->
            val safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(binding.welcomeRoot)
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
        binding.themeBack.setOnClickListener { navigateBack() }
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
        pets.forEach { if (!it.has("id")) it.put("id", UUID.randomUUID().toString()) }
        logs.forEach { entry ->
            if (!entry.has("id")) entry.put("id", UUID.randomUUID().toString())
            if (!entry.has("petId")) pets.firstOrNull { it.optString("name") == entry.optString("pet") }
                ?.let { entry.put("petId", it.optString("id")) }
        }
        if (pets.isNotEmpty() || logs.isNotEmpty()) save()
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
    override fun onBackPressed() {
        if (screenHistory.isEmpty() && (screen == "Home" || screen == "Welcome")) super.onBackPressed()
        else navigateBack()
    }
}
