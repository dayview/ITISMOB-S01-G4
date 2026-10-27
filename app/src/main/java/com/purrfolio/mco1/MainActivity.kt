package com.purrfolio.mco1

import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.content.res.ColorStateList
import android.widget.FrameLayout
import android.text.TextWatcher
import android.text.Editable
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.view.WindowInsets
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Offline, self-contained MCO1 UX prototype. No remote operations are simulated as successful. */
class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private var screen = "Welcome"
    private var loggedIn = false
    private var authStep = 0
    private var pendingCareCategory = "Feeding"
    private var petIndex = 0
    private val preferences by lazy { getSharedPreferences("purrfolio_demo", MODE_PRIVATE) }
    private val pets = mutableListOf<JSONObject>()
    private val logs = mutableListOf<JSONObject>()
    private val reminders = mutableListOf<JSONObject>()
    private var night = false
    private var bg = 0; private var surface = 0; private var secondarySurface = 0
    private var primary = 0; private var primarySoft = 0; private var ink = 0; private var sub = 0
    private var line = 0; private var pink = 0; private var danger = 0; private var green = 0
    private val mainTabs = setOf("Home","Pets","Care","Records","More")
    private val guestPages = setOf("Welcome","Create Account","Log In","Reset Password")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.background)
        window.navigationBarColor = getColor(R.color.background)
        night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply { isAppearanceLightStatusBars = !night; isAppearanceLightNavigationBars = !night }
        loadColors(); restore(); render(if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) intent.getStringExtra("preview_screen") ?: "Welcome" else "Welcome")
    }

    private fun loadColors() {
        bg = getColor(R.color.background); surface = getColor(R.color.surface)
        secondarySurface = getColor(R.color.surface_alt); primary = getColor(R.color.primary)
        primarySoft = getColor(R.color.primary_soft); ink = getColor(R.color.text_primary)
        sub = getColor(R.color.text_secondary); line = getColor(R.color.border)
        pink = getColor(R.color.pink); danger = getColor(R.color.urgent); green = getColor(R.color.green)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 20, outline: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); if (outline != 0) setStroke(dp(1), outline)
    }
    private fun label(value: String, size: Float = 15f, color: Int = ink, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        BrandType.apply(this, bold && size >= 19f, if (bold && size >= 19f) 600 else if (bold) 700 else 400)
        includeFontPadding = true
    }
    private fun pad(view: View, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
        view.setPadding(dp(left), dp(top), dp(right), dp(bottom))
    }
    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun horizontal(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun gap(n: Int) { body.addView(View(this), LinearLayout.LayoutParams(1,dp(n))) }
    private fun note(msg: String) { Toast.makeText(this,msg,Toast.LENGTH_LONG).show() }
    private fun hTitle(s: String, title: String, subtitle: String) {
        val eyebrow = label(s.uppercase(Locale.getDefault()), 11f, primary, true).apply { letterSpacing = .18f }
        body.addView(eyebrow); gap(10)
        body.addView(label(title, 30f, ink, true)); gap(5)
        body.addView(label(subtitle, 14f, sub)); gap(22)
    }
    private fun pill(title: String, callback: () -> Unit, secondary: Boolean = false): TextView = label(title, 15f, if (secondary) primary else getColor(R.color.on_primary), true).apply {
        gravity = Gravity.CENTER; background = shape(if (secondary) secondarySurface else primary, 30, if (secondary) line else 0)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)).apply { topMargin = dp(8) }
        setOnClickListener { callback() }; isClickable = true
    }
    private fun surfaceCard(block: (LinearLayout) -> Unit): LinearLayout {
        val box = column().apply { background = shape(surface, 22, line); pad(this, 17, 16, 17, 17) }
        body.addView(box,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(12) })
        block(box); return box
    }
    private fun cardLink(heading: String, description: String, icon: String = "✦", color: Int = primary, onClick: () -> Unit) {
        surfaceCard { box ->
            val row = horizontal()
            val glyph = label(icon,23f,color,true); row.addView(glyph,LinearLayout.LayoutParams(dp(38),-2))
            val texts = column(); texts.addView(label(heading,17f,ink,true)); texts.addView(label(description,13f,sub))
            row.addView(texts,LinearLayout.LayoutParams(0,-2,1f));row.addView(label("›",27f,primary))
            box.addView(row);box.setOnClickListener { onClick() }
        }
    }
    private fun miniTag(txt: String, color: Int = primary): TextView = label(txt, 12f, color, true).apply {
        background = shape(secondarySurface,14);pad(this,10,7,10,7)
    }
    private fun section(t: String) { gap(10); body.addView(label(t,19f,ink,true)); gap(12) }
    private fun selectedPet() = pets.getOrNull(petIndex) ?: pets.first()
    private fun petName() = selectedPet().optString("name","Mochi")
    private fun render(to: String) {
        if (to.startsWith("Quick Log:")) {
            pendingCareCategory = to.substringAfter(":")
            render("Quick Log")
            return
        }
        screen = to
        if (to in ReferenceScreens.destinations) {
            setContentView(ReferenceScreens(this, selectedPet(), { render(it) }, { choosePet() }).build(to))
            return
        }
        if (to in PhaseTwoScreens.destinations) {
            setContentView(PhaseTwoScreens(
                this, selectedPet(), pets, logs, reminders,
                { render(it) }, { choosePet() },
                { index -> petIndex=index; render("Pet Detail") },
                { values, adding ->
                    val updated=if(adding) JSONObject() else JSONObject(selectedPet().toString())
                    listOf("name","species","breed","birthday","sex","notes").forEach { key ->
                        if(values.containsKey(key)) updated.put(key,values[key])
                    }
                    if(adding){pets.add(updated);petIndex=pets.lastIndex}else pets[petIndex]=updated
                    save();note("Pet profile saved on this device.");render("Pet Detail")
                },
                { kind, detail ->
                    logs.add(0,JSONObject().put("pet",petName()).put("kind",kind).put("text",detail).put("date",stamp()))
                    save();note("Care log saved on this device.");render("Care History")
                },
                { addReminder() },
                { reminder -> reminder.put("done",!reminder.optBoolean("done"));save();render("Reminders") },
                { loggedIn=true;render("Home") }
            ).build(to))
            return
        }
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        root = column().apply { setBackgroundColor(bg) }
        setContentView(root)
        // Android 15+ enforces edge-to-edge. Protect all screens from the clock,
        // camera cutout, notification icons, navigation bar and gesture area.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout() or androidx.core.view.WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(root)
        val top = horizontal().apply { pad(this,22,12,22,12) }
        if (to !in guestPages) {
            // Primary destinations use the brand; secondary destinations use a
            // consistently located, explicitly labelled back affordance.
            if (to in mainTabs) {
                val logo = ImageView(this).apply { setImageResource(R.drawable.purrfolio_wordmark); scaleType=ImageView.ScaleType.FIT_START; contentDescription="PURRFOLIO" }
                top.addView(logo, LinearLayout.LayoutParams(0,dp(35),1f))
            } else {
                val back = label("‹", 34f, primary, true).apply {
                    gravity = Gravity.CENTER
                    contentDescription = "Back"
                    setOnClickListener { render(when(to) {
                        "Pet Detail", "Edit Pet Profile", "Add Your Pet" -> "Pets"
                        "Quick Log", "Voice Log", "Sound Journal", "Care History" -> "Care"
                        "Pet Passport", "Pet Timeline", "Paperless Inbox", "Vaccination Card", "Browse Records" -> "Records"
                        "Personal Information", "Account Settings", "Notifications" -> "Profile"
                        else -> "More"
                    }) }
                }
                top.addView(back, LinearLayout.LayoutParams(dp(44),dp(42)))
                val title = label(to, 19f, primary, true).apply { gravity=Gravity.CENTER_VERTICAL; maxLines=1; ellipsize=android.text.TextUtils.TruncateAt.END }
                top.addView(title, LinearLayout.LayoutParams(0,dp(42),1f))
            }
            val change = label("♡ " + petName() + " ⌄",12f,ink,true).apply { background=shape(surface,20,line);pad(this,10,9,10,9);setOnClickListener { choosePet() } }
            top.addView(change)
            root.addView(top)
        }
        val scroll = ScrollView(this).apply { clipToPadding = false; isFillViewport = false; isVerticalScrollBarEnabled = false }
        body = column().apply { pad(this,22,if (to in guestPages) 24 else 16,22,24) }
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        when (to) {
            "Welcome" -> welcome()
            "Create Account" -> createAccount()
            "Log In" -> logIn()
            "Reset Password" -> resetPassword()
            "Profile" -> userProfile()
            "Personal Information" -> personalInformation()
            "Account Settings" -> accountSettings()
            "Notifications" -> notificationSettings()
            "Edit Pet Profile" -> editPetScreen()
            "Add Your Pet" -> addPetScreen()
            "Vaccination Card" -> vaccinationCard()
            "Browse Records" -> browseRecords()
            "Care Circle Access" -> careCircleAccess()
            "Home" -> home()
            "Pets" -> petList()
            "Pet Detail" -> petDetail()
            "Care" -> care()
            "Quick Log" -> quickLog()
            "Voice Log" -> voiceLog()
            "Sound Journal" -> soundJournal()
            "Care History" -> careHistory()
            "Records" -> records()
            "Paperless Inbox" -> paperless()
            "Pet Timeline" -> timeline()
            "Pet Passport" -> passport()
            "Reminders" -> remindersPage()
            "Calendar" -> calendar()
            "Pet ID QR" -> qr()
            "Emergency" -> emergency()
            "Care Circle" -> careCircle()
            "Activity Log" -> activityLog()
            "QR Privacy" -> privacy()
            "Offline & Sync" -> offline()
            "Account" -> account()
            "More" -> more()
            else -> more()
        }
        if (to in guestPages) return
        val nav = horizontal().apply { background=shape(surface,22,line);pad(this,5,8,5,8) }
        for ((name,emoji) in listOf("Home" to "⌂","Pets" to "♧","Care" to "+","Records" to "▤","More" to "•••")) {
            val cell = column().apply { gravity=Gravity.CENTER;pad(this,2,3,2,3);setOnClickListener { render(name) } }
            cell.addView(label(emoji,if(name=="Care")29f else 23f,if (to == name || name == "Care") primary else sub,true).apply { gravity=Gravity.CENTER; if(name=="Care") background=shape(primarySoft,30) })
            cell.addView(label(name,11f,if (to == name) primary else sub,to==name).apply { gravity=Gravity.CENTER })
            nav.addView(cell,LinearLayout.LayoutParams(0,dp(56),1f))
        }
        root.addView(nav, LinearLayout.LayoutParams(-1,-2).apply { setMargins(dp(12),0,dp(12),dp(6)) })
    }

    private fun artwork(size: Int = 145): ImageView = ImageView(this).apply {
        setImageResource(R.drawable.purrfolio_cat);scaleType=ImageView.ScaleType.CENTER_CROP
        background=shape(primarySoft,size/2,primary);clipToOutline=true
        contentDescription="Fluffy pet portrait"
        layoutParams=LinearLayout.LayoutParams(dp(size),dp(size)).apply {gravity=Gravity.CENTER_HORIZONTAL;topMargin=dp(12);bottomMargin=dp(12)}
    }
    private fun wordmark(big: Boolean = true) {
        val logo=ImageView(this).apply {setImageResource(R.drawable.purrfolio_wordmark);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription="PURRFOLIO logo"}
        body.addView(logo,LinearLayout.LayoutParams(-1,dp(if(big)105 else 62)))
    }
    private fun centered(t:String,size:Float=18f,color:Int=ink,bold:Boolean=false) {body.addView(label(t,size,color,bold).apply {gravity=Gravity.CENTER;textAlignment=View.TEXT_ALIGNMENT_CENTER})}
    private fun tinySpacer() { gap(18) }
    private fun field(text:String,example:String = "", password:Boolean=false): EditText {
        body.addView(label(text,12f,primary,true));gap(6)
        return input(text,example).apply { if(password) inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
    }
    private fun iconRow(text:String,detail:String, icon:String="✦", screenName:String="") {
        cardLink(text,detail,icon) { if (screenName.isNotBlank()) render(screenName) else note("Frontend preview — integration pending") }
    }
    private fun settingSwitch(title:String,description:String,default:Boolean=true) {
        val card=column().apply {background=shape(surface,19,line);pad(this,14,14,14,14)}
        val row=horizontal();val text=column();text.addView(label(title,15f,ink,true));text.addView(label(description,12f,sub))
        row.addView(text,LinearLayout.LayoutParams(0,-2,1f))
        val toggle=Switch(this).apply {isChecked=preferences.getBoolean("setting:"+title,default);thumbTintList=ColorStateList.valueOf(primary);setOnCheckedChangeListener { _, checked -> preferences.edit().putBoolean("setting:"+title,checked).apply() }}
        row.addView(toggle);card.addView(row);body.addView(card,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(10)})
    }
    private fun welcome() {
        gap(22);wordmark();centered("Your Pet's Life in Pawgress",18f,primary,true)
        gap(46);body.addView(artwork(245));centered("♡    ✧    ♡",28f,pink);gap(35)
        body.addView(pill("Get Started",{render("Create Account")}))
        body.addView(pill("Log In",{render("Log In")},true))
        gap(22);centered("or continue with",13f,sub)
        gap(15);centered(" G     ●     f ",25f,primary,true)
    }
    private fun createAccount() {
        centered("●━━○━━○",18f,primary);gap(19);centered("Create Account",32f,primary,true)
        centered("Let's get to know you!",17f,ink,true);gap(26)
        field("Full Name","Gracezel Castrence");field("Email Address","youremail@dlsu.edu.ph")
        field("Password","",true)
        surfaceCard {it.addView(label("✓  At least 8 characters",14f,primary));it.addView(label("✓  1 letter and 1 number",14f,primary));it.addView(label("✓  1 special character",14f,primary))}
        body.addView(pill("Continue",{render("Add Your Pet")}));gap(18)
        centered("or continue with",13f,sub);gap(8);centered(" G       ●       f ",24f,primary,true)
        gap(25);body.addView(pill("Already have an account? Log In",{render("Log In")},true))
    }
    private fun logIn() {
        centered("Welcome back!",32f,primary,true);centered("Good to see you again ♡",17f,ink,true)
        gap(22);body.addView(artwork(150));gap(10)
        field("Email Address","youremail@dlsu.edu.ph");field("Password","",true)
        val forgot=label("Forgot password?",14f,primary,true).apply {gravity=Gravity.RIGHT;setOnClickListener {render("Reset Password")}}
        body.addView(forgot);gap(12)
        body.addView(pill("Log In",{loggedIn=true;render("Home")}))
        gap(18);centered("or continue with",13f,sub);gap(12);centered(" G       ●       f ",24f,primary,true)
        gap(24);cardLink("New here?","Create an account to start your pet care journey.","♡") {render("Create Account")}
    }
    private fun resetPassword() {
        centered("Reset Password",31f,primary,true);gap(10);centered("No worries!",20f,ink,true)
        centered("We'll send you a link to reset your password.",14f,sub);gap(23)
        centered("♡   🐱   ✉   ✧",43f,primary);gap(27)
        field("Email Address","youremail@dlsu.edu.ph");body.addView(pill("Send Reset Link",{note("Password recovery requires Firebase integration")}))
        gap(22);surfaceCard {it.addView(label("Didn't receive the email?",18f,ink,true))
            listOf("✓  Check your spam folder","✓  Make sure you entered the correct email","✓  Try again in a few minutes").forEach {t ->it.addView(label(t,14f,primary))}}
    }
    private fun userProfile() {
        hTitle("Account","Profile","A little space for the person behind the paws.")
        body.addView(artwork(130));centered("Gracezel Castrence",20f,ink,true);centered("youremail@dlsu.edu.ph",13f,sub);gap(24)
        iconRow("Account Complete","4 of 5 completed","◉","Personal Information")
        iconRow("Personal Information","Name, email, phone and birthday","♙","Personal Information")
        iconRow("Account Settings","Password, appearance and account","⚙","Account Settings")
        iconRow("Notifications","Care reminder preferences","♧","Notifications")
        iconRow("Privacy & Security","Control personal information","◇","QR Privacy")
        iconRow("Linked Accounts","Social account connections","⌁","Account Settings")
        body.addView(pill("Log Out",{loggedIn=false;render("Welcome")}))
    }
    private fun personalInformation() {
        hTitle("Profile","Personal Information","Keep your details up to date.");body.addView(artwork(112))
        field("Full Name","Gracezel Castrence");field("Email Address","youremail@dlsu.edu.ph")
        field("Phone Number","+63 912 345 6789");field("Date of Birth","Sep 25, 2004")
        field("Country / Region","Philippines");body.addView(pill("Save Changes",{note("Saved for frontend demonstration")}))
    }
    private fun accountSettings() {
        hTitle("Profile","Account Settings","Make PURRFOLIO feel like yours.")
        iconRow("Change Password","Protect your account","♙","Reset Password")
        iconRow("Email Preferences","Control account emails","✉","Notifications")
        iconRow("App Theme","Follows your system Light / Dark preference","☾", "")
        iconRow("Language","English","◎", "")
        iconRow("Data & Storage","Local records and syncing","▤","Offline & Sync")
        gap(10);cardLink("Delete Account","Account deletion requires authenticated backend","♧",danger) {note("Not available in frontend demonstration")}
    }
    private fun notificationSettings() {
        hTitle("Profile","Notifications","Only what matters, right on time.")
        section("Care Reminders");settingSwitch("Care Reminders","Medication, feeding, appointments")
        section("Updates");settingSwitch("App Updates","New features and improvements")
        settingSwitch("Tips & Insights","Pet care tips and suggestions")
        section("Shared Care");settingSwitch("Care Circle Activity","When caregivers update a record")
        section("Marketing");settingSwitch("Product News","Occasional updates and offers",false)
        gap(16);centered("♡  We'll only notify you about what matters.",13f,sub)
    }
    private fun addPetScreen() {
        centered("●━━○━━○",19f,primary);hTitle("Pet management","Add Your Pet","Let's create their profile!")
        body.addView(artwork(115));iconRow("Pet Name","Species • Breed • Birthday • Weight","♡")
        body.addView(pill("Continue",{editPet(-1)}))
    }
    private fun editPetScreen() {
        hTitle("Pet management","Edit Pet Profile","Every little detail about ${petName()}.")
        body.addView(artwork(120));val p=selectedPet()
        listOf("Pet Name" to p.optString("name"),"Species" to p.optString("species"),"Breed" to p.optString("breed"),"Birthday" to p.optString("birthday"),"Sex" to p.optString("sex"),"About ${petName()}" to p.optString("notes")).forEach { (name,value)-> iconRow(name,value,"♡") }
        body.addView(pill("Save Changes",{editPet(petIndex)}))
    }
    private fun vaccinationCard() {
        hTitle("Pet records","Vaccination Card","Keep every vaccine detail on hand.")
        surfaceCard { c->c.setBackgroundColor(primarySoft);c.addView(label("♡  Pet Vaccination Record",20f,getColor(R.color.on_primary),true))
            c.addView(label("${petName()}  •  ${selectedPet().optString("breed")}",15f,getColor(R.color.on_primary)))
            c.addView(label("VACCINE                   DATE             NEXT DUE",12f,getColor(R.color.on_primary),true))
            c.addView(label("Rabies                      Not added",14f,getColor(R.color.on_primary)))
            c.addView(label("Distemper                 Not added",14f,getColor(R.color.on_primary)))}
        listOf("View Full Screen","Download","Share","Rename","Delete").forEach {cardLink(it,"Document action preview","▤") { note("Requires file storage integration") }}
    }
    private fun browseRecords() {
        hTitle("Pet records","Records","Find specific documents, notes and care logs.")
        input("Search records...");section("Filter by Type")
        centered("All     Vaccinations     Medications",13f,primary,true);gap(14)
        centered("Appointments     Documents     Care Logs",13f,primary,true)
        section("Recent Results")
        iconRow("Vaccination Card","Vaccinations • ${petName()}","▤","Vaccination Card")
        iconRow("Vet Receipt","Documents • ${petName()}","▤","Paperless Inbox")
        iconRow("Care History","Activities • ${petName()}","♧","Care History")
    }
    private fun careCircleAccess() {
        hTitle("Shared care","Care Circle","Manage who can care for ${petName()}.")
        body.addView(artwork(125));centered("♡   You     Mom     Ana     Jan",15f,primary,true)
        body.addView(pill("Invite Caregiver",{note("Invitations require Firebase integration")}))
        section("Access Permissions")
        settingSwitch("View Profile & Records","See the pet's profile and documents")
        settingSwitch("Add & Edit Care Logs","Add and edit care information")
        settingSwitch("Manage Reminders","Create and edit schedules",false)
        settingSwitch("Manage Documents","Upload and remove documents",false)
        body.addView(pill("Save Permissions",{note("UI preferences saved; access control requires Firebase")}))
    }

    private fun choosePet() {
        AlertDialog.Builder(this).setTitle("Choose a pet")
            .setItems(pets.map { it.optString("name") }.toTypedArray()) { _, i -> petIndex=i;render(screen) }
            .setPositiveButton("Add pet") { _, _ -> editPet(-1) }.setNegativeButton("Cancel",null).show()
    }
    private fun home() {
        val greeting = when(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
        body.addView(label("$greeting, ${petName()}!", 30f, primary, true))
        gap(7)
        body.addView(label(SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date()), 14f, sub))
        gap(16)
        val days = horizontal().apply { gravity=Gravity.CENTER }
        for (offset in -3..3) {
            val date=java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, offset) }
            val display=SimpleDateFormat("EEE",Locale.getDefault()).format(date.time)
            val item=column().apply { gravity=Gravity.CENTER; pad(this,0,8,0,8);background=shape(if(offset==0) primarySoft else secondarySurface,15,if(offset==0)primary else line) }
            item.addView(label(display,11f,if(offset==0) getColor(R.color.on_primary) else sub,true).apply {gravity=Gravity.CENTER})
            item.addView(label(date.get(java.util.Calendar.DAY_OF_MONTH).toString(),17f,if(offset==0)getColor(R.color.on_primary) else ink,true).apply {gravity=Gravity.CENTER})
            days.addView(item,LinearLayout.LayoutParams(0,dp(56),1f).apply {setMargins(dp(2),0,dp(2),0)})
        }
        body.addView(days);gap(14)
        surfaceCard { box ->
            val row=horizontal();row.addView(artwork(80));val col=column();col.addView(label(petName(),23f,ink,true));col.addView(label("Your furry little sunshine",13f,sub));row.addView(col);box.addView(row)
        }
        section("Today's Care · 3 of 5 done")
        listOf("Breakfast" to "8:00 AM • ½ cup kibble","Vitamins" to "9:00 AM • 1 tablet","Playtime" to "3:00 PM • 20 mins","Dinner" to "7:00 PM • ½ cup kibble").forEach { (title,detail)->iconRow(title,detail,when(title){"Breakfast","Dinner"->"◕";"Playtime"->"♡";else->"◈"},"Reminders") }
        section("Upcoming")
        iconRow("Care reminders","${reminders.count{!it.optBoolean("done")}} tasks waiting for you","◷","Reminders")
        section("Our little family")
        pets.forEachIndexed { i,p->cardLink(p.optString("name"),"${p.optString("species")} • ${p.optString("breed")}","♡") {petIndex=i;render("Pet Detail")} }
        body.addView(pill("+ Log Care",{render("Quick Log")}))
    }
    private fun petList() {
        hTitle("Pet management","My Pets","Every pet has their own little story.")
        pets.forEachIndexed { i,p ->
            surfaceCard { box-> val row=horizontal();val portrait=artwork(62);row.addView(portrait)
                val data=column();data.addView(label(p.optString("name"),19f,ink,true));data.addView(label("${p.optString("species")} • ${p.optString("breed")}",13f,sub));row.addView(data,LinearLayout.LayoutParams(0,-2,1f));row.addView(label("›",27f,primary));box.addView(row);box.setOnClickListener {petIndex=i;render("Pet Detail")} }
        }
        body.addView(pill("+ Add Another Pet",{render("Add Your Pet")}))
    }
    private fun petDetail() {
        val p=selectedPet();centered(p.optString("name")+" ♡",27f,primary,true)
        centered("${p.optString("species")} • ${p.optString("breed")}",13f,sub);body.addView(artwork(175))
        centered("♡  ✧  ♡",23f,pink)
        gap(18);centered("Overview       Records       Health       Notes",12f,primary,true)
        section("Today's Care")
        listOf("Breakfast" to "8:00 AM • ½ cup kibble","Vitamins" to "9:00 AM • 1 tablet","Playtime" to "3:00 PM • 20 mins","Dinner" to "7:00 PM • ½ cup kibble").forEach {(name,detail)->iconRow(name,detail,"♡","Care History")}
        section("Upcoming");iconRow("Vitamins","Tomorrow • 9:00 AM","◈","Reminders")
        body.addView(pill("Edit Pet Profile",{render("Edit Pet Profile")},true))
        iconRow("Care Circle","The people who care for ${petName()}","♡","Care Circle")
    }
    private fun care() {
        hTitle("Care logging","Log Care","Quickly add an activity for ${petName()}.")
        val types=listOf("Feeding" to "◕","Medication" to "◈","Water" to "♧","Playtime" to "♡","Vet Visit" to "⚕","Grooming" to "✎")
        types.chunked(2).forEach { pair->
            val row=horizontal(); pair.forEach { (name,icon)->
                val cell=column().apply {gravity=Gravity.CENTER;pad(this,14,22,14,22);background=shape(surface,23,line);setOnClickListener {render("Quick Log")}}
                cell.addView(label(icon,30f,primary,true).apply {gravity=Gravity.CENTER});cell.addView(label(name,14f,ink,true).apply {gravity=Gravity.CENTER})
                row.addView(cell,LinearLayout.LayoutParams(0,dp(105),1f).apply {setMargins(dp(3),dp(4),dp(3),dp(4))})
            };body.addView(row)
        }
        gap(15);surfaceCard { c-> c.addView(label("♫  Log with Voice",21f,primary,true));c.addView(label("Just say what happened!",14f,sub));c.addView(label("▂▅▃▇▅▂    ●    ▂▅▇▃▂",24f,primary,true).apply {gravity=Gravity.CENTER});c.setOnClickListener {render("Voice Log")} }
        iconRow("Sound Event Journal","Record unusual sounds and observations","◉","Sound Journal")
        iconRow("Care History","Your recent care logs","◷","Care History")
    }
    private fun quickLog() {
        hTitle("Care logging", "Log care", "A quick record for ${petName()}.")
        val category = Spinner(this);val categories=listOf("Feeding","Medication","Vitamins","Water","Playtime","Grooming","Vet Visit","Other")
        category.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,categories)
        category.setSelection(categories.indexOf(pendingCareCategory).coerceAtLeast(0))
        fieldWrapper("Activity",category)
        val detail=input("What happened? (amount, notes, etc.)")
        body.addView(pill("Save care log", {
            if (detail.text.toString().isBlank()) note("Add a short care note first.")
            else {logs.add(0,JSONObject().put("pet",petName()).put("kind",categories[category.selectedItemPosition]).put("text",detail.text.toString().trim()).put("date",stamp()));save();note("Care log saved on this device.");render("Care History")}
        }))
    }
    private fun voiceLog() {
        hTitle("Care logging", "Pet Voice Log", "Capture natural speech and confirm the details.")
        surfaceCard { it.addView(label("♫  Voice transcription is not connected in this demo.",16f,ink,true));it.addView(label("You can enter a spoken-style sentence below, then save it as a structured care note.",14f,sub)) }
        val text=input("e.g. Fed Mochi one cup at 7:30 AM")
        body.addView(pill("Review and save text", {
            if (text.text.isBlank()) note("Enter a care note first") else {logs.add(0,JSONObject().put("pet",petName()).put("kind","Voice-style entry").put("text",text.text.toString()).put("date",stamp()));save();render("Care History")}
        }))
    }
    private fun soundJournal() {
        hTitle("Care logging", "Sound Event Journal", "Keep a timestamped observation for your records.")
        surfaceCard { it.addView(label("◉  Audio recording is not connected yet.",16f,ink,true));it.addView(label("Save a written observation now; audio attachments need a future integration.",14f,sub)) }
        val text=input("Describe the sound or observation")
        body.addView(pill("Save observation", {
            if(text.text.isBlank()) note("Describe the observation first") else {logs.add(0,JSONObject().put("pet",petName()).put("kind","Sound observation").put("text",text.text.toString()).put("date",stamp()));save();render("Care History")}
        }))
    }
    private fun careHistory() {
        hTitle("Care logging", "Care history", "Your saved care moments for ${petName()}.")
        val filtered=logs.filter {it.optString("pet")==petName()}
        if(filtered.isEmpty()) surfaceCard {it.addView(label("No care entries yet",18f,ink,true));it.addView(label("Use Quick Log to capture the first one.",14f,sub))}
        filtered.forEach {entry -> cardLink(entry.optString("kind"),"${entry.optString("date")} • ${entry.optString("text")}","✎") {
            AlertDialog.Builder(this).setTitle("Care entry").setMessage(entry.optString("text"))
                .setPositiveButton("Close",null).setNegativeButton("Delete") {_,_->logs.remove(entry);save();render("Care History")}.show()
        }}
        body.addView(pill("+ New care entry",{render("Quick Log")}))
    }
    private fun records() {
        hTitle("Pet records", "Every detail, together.", "Organize what matters for ${petName()}.")
        cardLink("Paperless Inbox", "Vaccination cards, prescriptions and documents", "▧") { render("Paperless Inbox") }
        cardLink("Unified Pet Timeline", "Care and record history in date order", "◷") { render("Pet Timeline") }
        cardLink("Pet Passport", "An easy-to-read summary of key details", "▤") { render("Pet Passport") }
        cardLink("Browse and filter records", "Find documents by type, date and keyword", "⌕") { render("Browse Records") }
    }
    private fun paperless() {
        hTitle("Pet records", "Paperless Inbox", "Keep vaccinations, prescriptions and receipts together.")
        surfaceCard { it.addView(label("▧  Document storage integration required",16f,ink,true));it.addView(label("Firebase Storage and secure file selection are not connected in this standalone prototype.",14f,sub)) }
        cardLink("Review care records","Your locally saved care entries","✎") {render("Care History")}
    }
    private fun timeline() {
        hTitle("Pet records", "Pet timeline", "${petName()}'s story, in chronological order.")
        logs.filter {it.optString("pet")==petName()}.forEach { entry ->
            cardLink(entry.optString("kind"),"${entry.optString("date")} • ${entry.optString("text")}","●") {render("Care History")}
        }
        if (logs.none {it.optString("pet")==petName()}) surfaceCard {it.addView(label("Your timeline begins with the first care log.",15f,sub))}
        body.addView(pill("+ Add timeline entry",{render("Quick Log")}))
    }
    private fun passport() {
        val p=selectedPet();hTitle("Pet records", "Pet Passport", "${petName()}'s essentials in one place.")
        surfaceCard {box ->
            box.addView(label("🐾  ${p.optString("name")}",24f,primary,true));box.addView(label("${p.optString("species")} • ${p.optString("breed")}",15f,sub))
            box.addView(label("Birthday: ${p.optString("birthday")}",15f));box.addView(label("Sex: ${p.optString("sex")}",15f))
            box.addView(label("Notes: ${p.optString("notes","Not specified")}",14f,sub))
            box.addView(label("Care logs: ${logs.count { it.optString("pet")==petName() }}",14f,primary,true))
        }
        surfaceCard {it.addView(label("Vaccinations, allergies, medications and vet history",16f,ink,true));it.addView(label("These clinical record sections require real user-entered data and secure record storage. No medical details are inferred.",14f,sub))}
        body.addView(pill("Pet ID and sharing settings",{render("Pet ID QR")},true))
    }
    private fun remindersPage() {
        hTitle("Schedule", "Care reminders", "A gentle nudge for what's next.")
        reminders.forEach { r ->cardLink("${if (r.optBoolean("done")) "✓ " else "○ "}${r.optString("title")}","${r.optString("pet")} • ${r.optString("when")}","◷",if(r.optBoolean("done"))green else primary){
            AlertDialog.Builder(this).setTitle(r.optString("title")).setItems(arrayOf("Mark ${if(r.optBoolean("done")) "pending" else "done"}","Delete")) {_,which->
                if(which==0)r.put("done",!r.optBoolean("done")) else reminders.remove(r);save();render("Reminders")
            }.show()
        }}
        if(reminders.isEmpty())surfaceCard {it.addView(label("No reminders yet. You're all caught up!",15f,sub))}
        body.addView(pill("+ Add reminder",{ addReminder() }))
        gap(12);body.addView(label("Device notifications are not scheduled in this offline prototype.",12f,sub))
    }
    private fun calendar() {
        hTitle("Schedule", "Care calendar", "The next little things worth remembering.")
        surfaceCard {it.addView(label(SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(Date()),22f,primary,true));it.addView(label("Calendar preview based on locally saved reminder dates",13f,sub))}
        reminders.forEach {r ->cardLink(r.optString("title"),"${r.optString("when")} • ${r.optString("pet")}","◷") {render("Reminders")}}
        body.addView(pill("+ Add care schedule",{addReminder()}))
        gap(10);body.addView(label("Google/device calendar export requires Calendar API integration.",12f,sub))
    }
    private fun qr() {
        hTitle("Pet identification", "Pet ID QR", "Share only the details you choose.")
        surfaceCard {it.addView(label("▦",90f,primary,true).apply {gravity=Gravity.CENTER});it.addView(label("QR generation and scanning are not yet integrated.",14f,sub).apply {gravity=Gravity.CENTER})}
        cardLink("QR information privacy", "Review the details that may be shared", "◉") {render("QR Privacy")}
    }
    private fun emergency() {
        hTitle("Important details", "Emergency Mode", "Find essential information when every second matters.")
        surfaceCard {it.addView(label("♡  ${petName()}",23f,danger,true));it.addView(label("Emergency information is not yet configured.",15f,ink));it.addView(label("Do not rely on this prototype for veterinary emergencies.",13f,sub)) }
        cardLink("Pet Passport", "Review available pet information", "▤") {render("Pet Passport")}
        cardLink("Edit pet details", "Add your own pet notes", "✎") { editPet(petIndex) }
    }
    private fun careCircle() {
        hTitle("Shared care", "Care Circle", "The people who help care for ${petName()}.")
        surfaceCard {it.addView(label("♡  Built for trusted caregivers",19f,ink,true));it.addView(label("Invitations, role-based permissions and cloud sharing require account authentication and backend access control. They are not enabled in this prototype.",14f,sub))}
        cardLink("Care Circle activity log", "See how caregiver updates will be tracked", "◷") {render("Activity Log")};cardLink("Access Permissions", "Review caregiver permissions", "◉") {render("Care Circle Access")}
        cardLink("Access control", "Understand permission requirements", "◉") {render("QR Privacy")}
    }
    private fun activityLog() {
        hTitle("Security & privacy", "Care Circle activity log", "Know who contributed to each care task.")
        surfaceCard {it.addView(label("Caregiver attribution needs authorized shared accounts.",15f,sub))}
        cardLink("Local care history","Activity recorded on this device only","◷") {render("Care History")}
    }
    private fun privacy() {
        hTitle("Security & privacy", "QR information privacy", "You decide which details are shared.")
        for (s in listOf("Pet name","Pet photo","Species and breed","Age or birthday","Owner contact information","Medical information","Veterinary information")) {
            val check=CheckBox(this).apply { text=s;setTextColor(ink);isChecked=false;buttonTintList=android.content.res.ColorStateList.valueOf(primary) }
            body.addView(check)
        }
        gap(10);surfaceCard {it.addView(label("Privacy-first prototype",17f,ink,true));it.addView(label("These controls are previews only. No public QR or owner information is published or shared.",13f,sub))}
    }
    private fun offline() {
        hTitle("Offline & sync", "Your care, even offline.", "Keep logging the moments that matter.")
        surfaceCard {it.addView(label("✓  Local demo data available",18f,green,true));it.addView(label("${pets.size} pet profiles • ${logs.size} care logs • ${reminders.size} reminders",14f,ink));it.addView(label("Data are stored in this app's local preferences on this device.",13f,sub))}
        surfaceCard {it.addView(label("Cloud sync not connected",16f,ink,true));it.addView(label("Changes made here will NOT automatically upload to Firebase or other devices.",14f,sub))}
    }
    private fun account() {
        hTitle("User management", "My account", "A simple place for your personal details.")
        surfaceCard {it.addView(label("Local demonstration mode",17f,ink,true));it.addView(label("Firebase registration, login, password reset and profile updates must be integrated with your team's backend.",14f,sub))}
        cardLink("Privacy & access","Care Circle permissions and QR sharing","◉") {render("QR Privacy")}
    }
    private fun more() {
        hTitle("Explore PURRFOLIO", "More to care for.", "Everything you need, right where you expect it.")
        listOf(
            Triple("Care reminders","Schedule medication, feeding and visits","Reminders"),
            Triple("Care calendar","See upcoming pet-care activities","Calendar"),
            Triple("Sitter Mode","Care instructions and caregiver access","Sitter Mode"),
            Triple("Pet ID QR","Identification and sharing","Pet ID QR"),
            Triple("Emergency Mode","Important pet details","Emergency"),
            Triple("Offline & Sync","Local changes and cloud sync status","Offline & Sync"),
            Triple("Security & Privacy","Manage what can be shared","QR Privacy"),
            Triple("My Profile","Personal information and settings","Profile")
        ).forEach { (name,detail,target)->cardLink(name,detail,"✦") {render(target)} }
    }
    private fun fieldWrapper(name:String,view: View) {
        gap(10);body.addView(label(name,13f,primary,true));gap(6)
        body.addView(view,LinearLayout.LayoutParams(-1,dp(54)))
    }
    private fun input(hint:String,initial:String=""):EditText {
        val e=EditText(this).apply {setSingleLine(true);setText(initial);this.hint=hint;textSize=15f;setTextColor(ink);setHintTextColor(sub);background=shape(surface,17,line);pad(this,15,7,15,7)}
        body.addView(e,LinearLayout.LayoutParams(-1,dp(55)).apply {bottomMargin=dp(10)})
        return e
    }
    private fun dialogField(hint:String,value:String=""):EditText=EditText(this).apply {setText(value);this.hint=hint;setSingleLine(true);textSize=15f;pad(this,12,4,12,4)}
    private fun editPet(index:Int) {
        val old=if(index>=0)pets[index] else JSONObject()
        val form=column().apply {pad(this,22,10,22,8)}
        val name=dialogField("Pet name",old.optString("name"));form.addView(name)
        val species=dialogField("Species (e.g. Cat)",old.optString("species"));form.addView(species)
        val breed=dialogField("Breed",old.optString("breed"));form.addView(breed)
        val birthday=dialogField("Birthday (YYYY-MM-DD)",old.optString("birthday"));form.addView(birthday)
        val sex=dialogField("Sex",old.optString("sex"));form.addView(sex)
        val notes=dialogField("Care notes (optional)",old.optString("notes"));form.addView(notes)
        AlertDialog.Builder(this).setTitle(if(index>=0)"Edit pet profile" else "Add your pet").setView(form)
            .setNegativeButton("Cancel",null).setPositiveButton("Save") {_,_->
                if(name.text.isBlank()) {note("Pet name is required");return@setPositiveButton}
                val p=JSONObject().put("name",name.text.toString().trim()).put("species",species.text.toString().trim())
                    .put("breed",breed.text.toString().trim()).put("birthday",birthday.text.toString().trim())
                    .put("sex",sex.text.toString().trim()).put("notes",notes.text.toString().trim())
                if(index<0) {pets.add(p);petIndex=pets.lastIndex} else pets[index]=p
                save();render("Pet Detail")
            }.show()
    }
    private fun addReminder() {
        val form=column().apply {pad(this,22,12,22,8)}
        val title=dialogField("Task: vaccination, medication, feeding...");form.addView(title)
        val whenField=dialogField("Date and time (e.g. Nov 3, 9:00 AM)");form.addView(whenField)
        AlertDialog.Builder(this).setTitle("New care reminder").setView(form).setNegativeButton("Cancel",null)
            .setPositiveButton("Save") {_,_->
                if(title.text.isBlank()||whenField.text.isBlank()) {note("Both fields are required");return@setPositiveButton}
                reminders.add(0,JSONObject().put("title",title.text.toString().trim()).put("when",whenField.text.toString().trim()).put("pet",petName()).put("done",false))
                save();render("Reminders")
            }.show()
    }
    private fun stamp()=SimpleDateFormat("MMM d, yyyy · h:mm a",Locale.getDefault()).format(Date())
    private fun restore() {
        fun pull(key:String,out:MutableList<JSONObject>) {val a=JSONArray(preferences.getString(key,"[]"));for(i in 0 until a.length()) out.add(a.getJSONObject(i))}
        try { pull("pets",pets);pull("logs",logs);pull("reminders",reminders) }catch(_:Exception) {pets.clear();logs.clear();reminders.clear()}
        if(pets.isEmpty()) {
            pets.add(JSONObject().put("name","Mochi").put("species","Cat").put("breed","Mixed Breed").put("birthday","2024-09-26").put("sex","Female").put("notes","Playful, loves treats and sunny spots."))
            pets.add(JSONObject().put("name","Bailey").put("species","Dog").put("breed","Golden Retriever").put("birthday","2022-05-13").put("sex","Male").put("notes","Enjoys long walks."))
            save()
        }
    }
    private fun save() {
        fun json(items:MutableList<JSONObject>)=JSONArray().apply {items.forEach {put(it)}}.toString()
        preferences.edit().putString("pets",json(pets)).putString("logs",json(logs)).putString("reminders",json(reminders)).apply()
    }
    @Deprecated("Back navigation handled by main tab scaffold")
    override fun onBackPressed() { if (screen=="Home" || screen=="Welcome") super.onBackPressed() else render(when(screen){"Create Account","Log In","Reset Password"->"Welcome";"Pet Detail","Edit Pet Profile","Add Your Pet"->"Pets";"Quick Log","Voice Log","Sound Journal","Care History"->"Care";"Paperless Inbox","Pet Timeline","Pet Passport","Browse Records","Vaccination Card"->"Records";"Personal Information","Account Settings","Notifications"->"Profile";else->"More"}) }
}
