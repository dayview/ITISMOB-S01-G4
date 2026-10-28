package com.purrfolio.mco1

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Native, individually composed screens measured from docs/phase2/references.
 * Existing model mutations are supplied by MainActivity; UI preferences stay local.
 */
class PhaseTwoScreens(
    private val a:Activity,
    private val pet:JSONObject,
    private val pets:List<JSONObject>,
    private val logs:List<JSONObject>,
    private val reminders:List<JSONObject>,
    private val go:(String)->Unit,
    private val choose:()->Unit,
    private val selectPet:(Int)->Unit,
    private val savePet:(Map<String,String>,Boolean)->Unit,
    private val saveLog:(String,String)->Unit,
    private val addReminder:()->Unit,
    private val toggleReminder:(JSONObject)->Unit,
    private val login:()->Unit
) {
    companion object {
        val destinations=setOf("Create Account","Log In","Reset Password","Profile","Personal Information",
            "Account Settings","App Theme","Notifications","Add Your Pet","Edit Pet Profile","Pets","Care Circle",
            "Voice Log","Sound Journal","Records","Paperless Inbox","Vaccination Card","Browse Records",
            "Reminders","Calendar","Pet ID QR","Emergency","Offline & Sync","Care Circle Access",
            "Activity Log","QR Privacy","More")
        val screens=listOf("Create Account","Log In","Reset Password","Profile","Personal Information",
            "Account Settings","App Theme","Notifications","Add Your Pet","Edit Pet Profile","Pets","Care Circle",
            "Voice Log","Sound Journal","Records","Vaccination Card","Browse Records","Reminders",
            "Calendar","Pet ID QR","Emergency","Offline & Sync","Care Circle Access","Activity Log","QR Privacy")
    }
    private val name get()=pet.optString("name","Mochi")
    private val prefs=a.getSharedPreferences("purrfolio_demo",Activity.MODE_PRIVATE)
    private val night=a.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES
    private fun color(hex:String)=Color.parseColor(hex)
    private val bg=color(if(night)"#101014" else "#FCF9FF")
    private val surface=color(if(night)"#1C1B26" else "#F3EDF8")
    private val elevated=color(if(night)"#24222F" else "#EDE2F5")
    private val primary=color(if(night)"#D8B4F8" else "#8051A8")
    private val lavender=color("#D8B4F8")
    private val ink=color(if(night)"#F8F2FF" else "#292130")
    private val sub=color(if(night)"#BDB3CD" else "#72647D")
    private val outline=color(if(night)"#373243" else "#DED1E8")
    private val pink=color("#F3B6D7")
    private val mint=color("#91E5C8")
    private val coral=color(if(night)"#FF8298" else "#B32646")
    private val yellow=color("#FFD18C")
    private val blue=color("#9BCBFF")
    private lateinit var body:LinearLayout
    private var activeScreen=""
    private val careKinds=setOf("Feeding","Medication","Water","Playtime","Vet Visit","Grooming","Home","Pets",
        "Care","Notes","More","Plus","Back","Close","Arrow","Chevron","Check","Energy","Food","Mood",
        "Mic","Wave","Link","Eye","Share","QR","Microchip","Edit","Heart","Weight","Emergency","Google","Apple","Facebook")
    private fun dp(v:Int)=(v*a.resources.displayMetrics.density+.5f).toInt()
    private fun col()=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL}
    private fun row()=LinearLayout(a).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    private fun pad(v:View,l:Int,t:Int=l,r:Int=l,b:Int=t){v.setPadding(dp(l),dp(t),dp(r),dp(b))}
    private fun shape(c:Int=surface,r:Int=20,border:Boolean=true)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();if(border)setStroke(dp(1),outline)}
    private fun text(s:String,size:Float=14f,c:Int=ink,weight:Int=400,display:Boolean=false)=TextView(a).apply{
        text=s;textSize=size;setTextColor(c);includeFontPadding=false;BrandType.apply(this,display,weight);setLineSpacing(dp(2).toFloat(),1f)
    }
    private fun heading(s:String,size:Float=30f)=text(s,size,primary,600,true)
    private fun section(s:String)=text(s,19f,primary,600,true)
    private fun icon(kind:String,size:Int=26,tint:Int=primary):View =
        (if(kind in careKinds)CareArtwork(a,kind,tint)else DetailIcon(a,kind,tint)).apply{
            layoutParams=LinearLayout.LayoutParams(dp(size),dp(size));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    private fun add(v:View,height:Int=-2,gap:Int=0){body.addView(v,LinearLayout.LayoutParams(-1,if(height<0)height else dp(height)).apply{bottomMargin=dp(gap)})}
    private fun gap(height:Int){add(View(a),height)}
    private fun push(min:Int=6){body.addView(View(a),LinearLayout.LayoutParams(1,dp(min),1f))}
    private fun steps(count:Int)=row().apply{
        gravity=Gravity.CENTER
        repeat(count){i->
            if(i>0)addView(View(a).apply{setBackgroundColor(outline)},LinearLayout.LayoutParams(dp(22),dp(2)).apply{setMargins(dp(4),0,dp(4),0)})
            addView(View(a).apply{background=shape(if(i==0)lavender else bg,12,true)},LinearLayout.LayoutParams(dp(if(i==0)16 else 12),dp(if(i==0)16 else 12)))
        }
        contentDescription="Step 1 of $count"
    }
    private fun center(s:String,size:Float=14f,c:Int=sub,weight:Int=400)=text(s,size,c,weight).apply{gravity=Gravity.CENTER}
    private fun action(kind:String,label:String,callback:()->Unit)=FrameLayout(a).apply{
        contentDescription=label;isFocusable=true;setOnClickListener{callback()}
        addView(icon(kind,tint=if(kind=="Check"||kind=="Camera")color("#201528")else if(kind=="Warning")coral else primary),FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));minimumWidth=dp(48);minimumHeight=dp(48)
    }
    private fun button(label:String,secondary:Boolean=false,tint:Int=lavender,callback:()->Unit)=text(label,15f,if(secondary)primary else color("#201528"),700).apply{
        gravity=Gravity.CENTER;minHeight=dp(52);background=shape(if(secondary)elevated else tint,30,secondary);isFocusable=true;setOnClickListener{callback()}
    }
    private fun preview(message:String){AlertDialog.Builder(a).setTitle("Frontend preview").setMessage(message).setPositiveButton("OK",null).show()}
    private fun notice(message:String){Toast.makeText(a,message,Toast.LENGTH_LONG).show()}
    private fun demoLabel(label:String="Sample content"){add(text(label,11f,sub,500).apply{gravity=Gravity.END},18,3)}
    private fun header(title:String,subtitle:String?=null,back:String="More",extra:String?=null,onExtra:(()->Unit)?=null,size:Float=30f,left:Boolean=false){
        val panel=FrameLayout(a)
        val h=if(title.length>23)92 else 68
        if(!left)panel.addView(action("Back","Back"){go(back)},FrameLayout.LayoutParams(dp(40),dp(40),Gravity.TOP or Gravity.START))
        panel.addView(heading(title,size).apply{if(title=="Emergency Mode")setTextColor(coral);gravity=if(left)Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER;maxLines=2},FrameLayout.LayoutParams(-1,dp(h-16)).apply{topMargin=dp(16);if(left)rightMargin=dp(50)})
        if(extra!=null)panel.addView(action(extra,if(extra=="Plus")"Add" else extra){onExtra?.invoke()},FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP or Gravity.END))
        add(panel,h,if(subtitle==null)10 else 0)
        if(subtitle!=null)add(center(subtitle),-2,18)
    }
    private fun art(resource:Int,height:Int):ImageView=ImageView(a).apply{
        setImageResource(resource);scaleType=ImageView.ScaleType.FIT_CENTER;adjustViewBounds=true
        contentDescription="PURRFOLIO illustration from the supplied wireframe";layoutParams=LinearLayout.LayoutParams(-1,dp(height))
    }
    private fun portrait(resource:Int=R.drawable.pdf_mochi,size:Int=100,ring:Int=primary):FrameLayout=FrameLayout(a).apply{
        background=shape(ring,size,false);pad(this,3)
        addView(ImageView(a).apply{setImageResource(resource);scaleType=ImageView.ScaleType.CENTER_CROP;background=shape(surface,size,false);clipToOutline=true;contentDescription="Pet portrait"},FrameLayout.LayoutParams(-1,-1))
        layoutParams=LinearLayout.LayoutParams(dp(size),dp(size))
    }
    private fun portraitHero(size:Int=130,decorated:Boolean=true,editable:Boolean=true):View{
        val frame=FrameLayout(a)
        frame.addView(portrait(size=size),FrameLayout.LayoutParams(dp(size),dp(size),Gravity.CENTER))
        if(editable)frame.addView(action("Camera","Change photo"){preview("Photo upload is not connected. The supplied image is shown for this frontend preview.")}.apply{background=shape(lavender,30,false);translationX=dp((size-35)/2).toFloat();translationY=dp((size-30)/2).toFloat()},FrameLayout.LayoutParams(dp(44),dp(44),Gravity.CENTER))
        if(decorated){frame.addView(icon("Heart",26,pink).apply{translationX=-dp((size+65)/2).toFloat();translationY=-dp(15).toFloat()},FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));frame.addView(icon("Sparkle",24).apply{translationX=dp((size+65)/2).toFloat();translationY=dp(20).toFloat()},FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER))}
        return frame
    }
    private fun field(label:String,value:String="",kind:String="Person",hint:String="",password:Boolean=false,multiline:Boolean=false,dropdown:List<String>?=null,parent:LinearLayout=body):EditText{
        val box=row().apply{background=shape();pad(this,15,10)}
        box.addView(icon(kind,26),LinearLayout.LayoutParams(dp(30),dp(32)).apply{rightMargin=dp(14)})
        val labels=col();labels.addView(text(label,12f,sub))
        val entry=EditText(a).apply{setText(value);this.hint=hint;textSize=15f;setTextColor(ink);setHintTextColor(sub);BrandType.apply(this,false,500);background=null;setPadding(0,dp(3),0,0);includeFontPadding=false;isSingleLine=!multiline;contentDescription=label
            inputType=when{password->InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;label.contains("Email")->InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;label.contains("Phone")->InputType.TYPE_CLASS_PHONE;label=="Weight"->InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL;multiline->InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE;else->InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES}
            if(multiline){minLines=2;gravity=Gravity.TOP}
        }
        labels.addView(entry,LinearLayout.LayoutParams(-1,-2));box.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
        if(password){entry.transformationMethod=PasswordTransformationMethod.getInstance();box.addView(action("Eye","Show or hide password"){val start=entry.selectionStart;entry.transformationMethod=if(entry.transformationMethod==null)PasswordTransformationMethod.getInstance()else null;entry.setSelection(start.coerceAtLeast(0))},LinearLayout.LayoutParams(dp(40),dp(40)))}
        if(dropdown!=null){entry.isFocusable=false;val pick={AlertDialog.Builder(a).setTitle(label).setItems(dropdown.toTypedArray()){_,i->entry.setText(dropdown[i])}.show();Unit};box.setOnClickListener{pick()};entry.setOnClickListener{pick()};box.addView(action("Down","Choose $label",pick),LinearLayout.LayoutParams(dp(34),dp(40)))}
        if(label.contains("Birthday")||label=="Date of Birth"){
            entry.isFocusable=false;val pick={val now=Calendar.getInstance();DatePickerDialog(a,{_,y,m,d->entry.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d))},now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();Unit};entry.setOnClickListener{pick()};box.setOnClickListener{pick()}
            box.addView(icon("Down",22))
        }
        parent.addView(box,LinearLayout.LayoutParams(-1,if(multiline)dp(102)else dp(70)).apply{bottomMargin=dp(8)})
        return entry
    }
    private fun infoRow(kind:String,label:String,detail:String?=null,trailing:String="Chevron",tint:Int=primary,callback:()->Unit):View{
        val r=row().apply{background=shape();pad(this,15,10);isFocusable=true;setOnClickListener{callback()}}
        r.addView(icon(kind,27,tint),LinearLayout.LayoutParams(dp(32),dp(34)).apply{rightMargin=dp(14)})
        val labels=col();labels.addView(text(label,15f,if(tint==coral)coral else ink,700));if(detail!=null)labels.addView(text(detail,12f,sub),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        r.addView(labels,LinearLayout.LayoutParams(0,-2,1f));if(trailing.isNotBlank())r.addView(icon(trailing,23),LinearLayout.LayoutParams(dp(27),dp(30)))
        return r
    }
    private fun toggleRow(kind:String,label:String,description:String?=null,default:Boolean=true,key:String="setting:$label",persist:Boolean=true):View{
        val r=row().apply{background=shape();pad(this,14,8)};r.addView(icon(kind,27),LinearLayout.LayoutParams(dp(30),dp(32)).apply{rightMargin=dp(12)})
        val labels=col();labels.addView(text(label,14f,ink,700));if(description!=null)labels.addView(text(description,11f,sub),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        r.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
        val toggle=Switch(a).apply{contentDescription=label;isChecked=prefs.getBoolean(key,default);showText=false;splitTrack=false;switchMinWidth=dp(46)
            thumbDrawable=shape(Color.WHITE,20,false).apply{setSize(dp(24),dp(24))};trackDrawable=shape(lavender,20,false).apply{setSize(dp(46),dp(27))}
            thumbTintList=ColorStateList.valueOf(Color.WHITE);trackTintList=ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf()),intArrayOf(lavender,outline))
            setOnCheckedChangeListener{_,checked->if(persist)prefs.edit().putBoolean(key,checked).apply()}
        };r.addView(toggle,LinearLayout.LayoutParams(dp(50),dp(44)));return r
    }
    private fun sectionLabel(label:String,gap:Int=12){add(section(label),-2,gap)}
    private fun social(){
        gap(16);val divider=row();divider.addView(View(a).apply{setBackgroundColor(outline)},LinearLayout.LayoutParams(0,dp(1),1f));divider.addView(center("or continue with",13f),LinearLayout.LayoutParams(dp(136),dp(26)));divider.addView(View(a).apply{setBackgroundColor(outline)},LinearLayout.LayoutParams(0,dp(1),1f));add(divider,30,8)
        val r=row().apply{gravity=Gravity.CENTER};listOf("Apple","Google","Facebook").forEach{s->val cell=FrameLayout(a).apply{background=shape(bg,40,true);contentDescription="Continue with $s";setOnClickListener{preview("$s sign-in is not connected.")};addView(icon(s,30,ink),FrameLayout.LayoutParams(dp(30),dp(30),Gravity.CENTER))};r.addView(cell,LinearLayout.LayoutParams(dp(58),dp(58)).apply{setMargins(dp(12),0,dp(12),0)})};add(r,62,12)
    }
    private fun chips(labels:List<String>,selected:String=labels.first(),onSelect:(String)->Unit):View{
        val r=row();labels.forEach{label->r.addView(text(label,12f,if(label==selected)color("#24192E") else ink,700).apply{gravity=Gravity.CENTER;background=shape(if(label==selected)lavender else surface,24,true);pad(this,10,9);isFocusable=true;setOnClickListener{onSelect(label)}},LinearLayout.LayoutParams(0,dp(38),1f).apply{setMargins(dp(2),0,dp(2),0)})};return r
    }
    fun build(screen:String):View{
        activeScreen=screen
        WindowCompat.setDecorFitsSystemWindows(a.window,false)
        WindowCompat.getInsetsController(a.window,a.window.decorView).apply{isAppearanceLightStatusBars=!night;isAppearanceLightNavigationBars=!night}
        val root=col().apply{setBackgroundColor(bg)}
        ViewCompat.setOnApplyWindowInsetsListener(root){v,insets->val safe=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);insets}
        val scroll=ScrollView(a).apply{isFillViewport=true;isVerticalScrollBarEnabled=false}
        body=col().apply{pad(this,20,8,20,12)};scroll.addView(body,ViewGroup.LayoutParams(-1,-2));root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        when(screen){
            "Create Account"->createAccount();"Log In"->logIn();"Reset Password"->resetPassword()
            "Profile"->profile();"Personal Information"->personalInformation();"Account Settings"->accountSettings();"Notifications"->notifications()
            "Add Your Pet"->petForm(false);"Edit Pet Profile"->petForm(true);"Pets"->myPets();"Care Circle"->careCircle(false)
            "Voice Log"->voiceLog();"Sound Journal"->soundJournal();"Records","Paperless Inbox"->records();"Vaccination Card"->vaccination();"Browse Records"->recordsSearch()
            "Reminders"->reminders();"Calendar"->calendar();"Pet ID QR"->petId();"Emergency"->emergency();"Offline & Sync"->offline()
            "Care Circle Access"->careCircle(true);"Activity Log"->activityLog();"QR Privacy"->privacy();"More"->more()
        }
        val withoutDock=setOf("Create Account","Log In","Reset Password","Personal Information","Add Your Pet","Edit Pet Profile")
        if(screen !in withoutDock)root.addView(ReferenceScreens(a,pet,go,choose).dock(screen),LinearLayout.LayoutParams(-1,dp(78)).apply{setMargins(dp(8),0,dp(8),dp(5))})
        root.isFocusableInTouchMode=true;root.requestFocus();ViewCompat.requestApplyInsets(root)
        return root
    }

    // Individual compositions follow; shared helpers only cover visually identical controls.
    private fun createAccount(){
        val top=row();top.addView(action("Back","Back"){go("Welcome")},LinearLayout.LayoutParams(dp(44),dp(44)));top.addView(steps(3),LinearLayout.LayoutParams(0,dp(44),1f));top.addView(View(a),LinearLayout.LayoutParams(dp(44),1));add(top,48,12)
        add(heading("Create Account",32f).apply{gravity=Gravity.CENTER},48,4);add(center("Let's get to know you!",17f,ink,600),28,24)
        field("Full Name","Gracezel Castrence");field("Email Address","youremail@dlsu.edu.ph","Mail")
        val password=field("Password","","Lock",password=true)
        val requirements=col().apply{background=shape();pad(this,18,12)}
        val checks=listOf("At least 8 characters","1 letter and 1 number","1 special character").map{label->val r=row();val status=icon("Check",22);r.addView(status,LinearLayout.LayoutParams(dp(26),dp(26)).apply{rightMargin=dp(12)});r.addView(text(label,14f,sub));requirements.addView(r,LinearLayout.LayoutParams(-1,dp(32)));r}
        password.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){val v=s.toString();val ok=listOf(v.length>=8,v.any{it.isLetter()}&&v.any{it.isDigit()},v.any{!it.isLetterOrDigit()});checks.forEachIndexed{i,r->r.alpha=if(ok[i])1f else .45f}};override fun afterTextChanged(s:Editable?) {}})
        add(requirements,124,20);add(button("Continue"){go("Add Your Pet")},52);social();push(16)
        add(center("Already have an account?  Log In",14f,sub).apply{setOnClickListener{go("Log In")};minHeight=dp(48)},48)
    }
    private fun logIn(){
        header("Welcome back!","Good to see you again",back="Welcome",size=32f);add(portraitHero(145,true,false),170,14)
        field("Email Address","youremail@dlsu.edu.ph","Person");field("Password","","Lock",password=true)
        add(text("Forgot password?",14f,primary,700).apply{gravity=Gravity.END;setOnClickListener{go("Reset Password")};minHeight=dp(40)},40,8)
        add(button("Log In"){login()},52);social()
        val card=row().apply{background=shape();pad(this,12,12);setOnClickListener{go("Create Account")}}
        card.addView(art(R.drawable.pdf_sleeping_cat,105),LinearLayout.LayoutParams(dp(120),dp(105)))
        val copy=col();copy.addView(section("New here?"));copy.addView(text("Create an account to start\nyour pet care journey.",13f,sub),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)});copy.addView(text("Create Account  →",15f,primary,700),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)});card.addView(copy,LinearLayout.LayoutParams(0,-2,1f));add(card,136)
    }
    private fun resetPassword(){
        header("Reset Password",back="Log In",size=31f);add(center("No worries!",20f,ink,600),30,6);add(center("We'll send you a link to reset\nyour password.",14f),44,12)
        add(art(R.drawable.pdf_reset_cat,175),175,10);field("Email Address","youremail@dlsu.edu.ph","Person")
        add(button("Send Reset Link"){preview("Password recovery requires the existing authentication integration. No reset email has been sent.")},52,26)
        val card=col().apply{background=shape();pad(this,18,18)};card.addView(section("Didn't receive the email?"))
        listOf("Check your spam folder","Make sure you entered the\ncorrect email","Try again in a few minutes").forEach{s->val r=row();r.addView(icon("Check",25),LinearLayout.LayoutParams(dp(32),dp(32)).apply{rightMargin=dp(12)});r.addView(text(s,14f,sub));card.addView(r,LinearLayout.LayoutParams(-1,dp(54)))};add(card)
    }
    private fun profile(){
        header("Profile",extra="Settings",onExtra={go("Account Settings")},left=true);add(portraitHero(130),150,8)
        add(center(prefs.getString("profile:name","Gracezel Castrence")!!,20f,ink,700),28,3);add(center(prefs.getString("profile:email","youremail@dlsu.edu.ph")!!,13f),24,16)
        val complete=infoRow("Progress","Account Complete","4 of 5 completed"){go("Personal Information")};complete.background=shape(elevated);add(complete,72,12)
        listOf(Triple("Person","Personal Information","Personal Information"),Triple("Settings","Account Settings","Account Settings"),Triple("Bell","Notifications","Notifications"),Triple("Lock","Privacy & Security","QR Privacy"),Triple("Link","Linked Accounts","Account Settings")).forEach{(i,l,d)->add(infoRow(i,l){go(d)},56,5)}
        gap(8);add(button("Log Out"){go("Welcome")},52);push()
    }
    private fun personalInformation(){
        header("Personal Information",back="Profile");add(portraitHero(125,false),155,18)
        val fields=linkedMapOf<String,EditText>()
        fields["name"]=field("Full Name",prefs.getString("profile:name","Gracezel Castrence")!!)
        fields["email"]=field("Email Address",prefs.getString("profile:email","youremail@dlsu.edu.ph")!!,"Mail")
        fields["phone"]=field("Phone Number",prefs.getString("profile:phone","+63 912 345 6789")!!,"Phone")
        fields["birthday"]=field("Date of Birth",prefs.getString("profile:birthday","2004-09-25")!!,"Care")
        fields["country"]=field("Country / Region",prefs.getString("profile:country","Philippines")!!,"Location",dropdown=listOf("Philippines","Singapore","Malaysia","Other"))
        gap(20);add(button("Save Changes"){prefs.edit().apply{fields.forEach{(k,v)->putString("profile:$k",v.text.toString())}}.apply();notice("Profile saved on this device.");go("Profile")},52)
    }
    private fun accountSettings(){
        header("Account Settings",back="Profile");gap(12)
        add(infoRow("Lock","Change Password"){go("Reset Password")},66,8)
        add(infoRow("Mail","Email Preferences"){go("Notifications")},66,8)
        add(infoRow("Moon","App Theme",AppTheme.label(a)){go("App Theme")},72,8)
        add(infoRow("Globe","Language","English"){preview("English is the available language in this frontend.")},72,8)
        add(infoRow("Database","Data & Storage"){go("Offline & Sync")},66,24)
        sectionLabel("Account");add(infoRow("Trash","Delete Account",tint=coral){preview("Account deletion requires authenticated account services. No account or local records have been deleted.")},66)
    }
    private fun notifications(){
        header("Notifications",back="Profile");sectionLabel("Care Reminders")
        add(toggleRow("Bell","Care Reminders","Medication, feeding, playtime, etc."),70,22)
        sectionLabel("Updates");add(toggleRow("Megaphone","App Updates","New features and improvements"),70,4);add(toggleRow("Star","Tips & Insights","Pet care tips and recommendations"),70,22)
        sectionLabel("Shared Care");add(toggleRow("Pets","Care Circle Activity","When someone updates a record"),70,22)
        sectionLabel("Marketing");add(toggleRow("Mail","Product News","Occasional updates and offers",false),70,14)
        add(art(R.drawable.pdf_notification_cat,76),76,4);add(center("We'll only notify you\nabout what matters.",14f,ink,500),44)
    }
    private fun petForm(edit:Boolean){
        if(edit)header("Edit Pet Profile",back="Pet Detail") else{
            val top=row();top.addView(action("Back","Back"){go("Create Account")},LinearLayout.LayoutParams(dp(44),dp(44)));top.addView(steps(4),LinearLayout.LayoutParams(0,dp(44),1f));top.addView(text("Skip",14f,primary,700).apply{setOnClickListener{go("Home")}},LinearLayout.LayoutParams(dp(42),dp(44)));add(top,48,8)
            add(heading("Add Your Pet").apply{gravity=Gravity.CENTER},44,4);add(center("Let's create their profile!",17f,primary,600),28,10)
        }
        if(edit){val r=row();r.addView(portraitHero(125,false),LinearLayout.LayoutParams(0,dp(145),1f));r.addView(button("Remove",true){preview("Photo editing is a preview; the reference portrait is retained.")},LinearLayout.LayoutParams(dp(130),dp(50)));add(r,145,12)}else add(portraitHero(140),166,10)
        val fields=linkedMapOf<String,EditText>()
        fields["name"]=field("Pet Name",if(edit)name else "","Pets",hint="Mochi")
        fields["species"]=field("Species",if(edit)pet.optString("species")else "Cat","Cat",dropdown=listOf("Cat","Dog","Rabbit","Other"))
        fields["breed"]=field("Breed",if(edit)pet.optString("breed")else "","Pets",hint="Mixed Breed")
        fields["birthday"]=field("Birthday",if(edit)pet.optString("birthday")else "","Care",hint="YYYY-MM-DD")
        var weight:EditText
        if(edit){val r=row();val l=col();val right=col();weight=field("Weight",prefs.getString("pet-ui-weight:$name","")!!,"Weight",hint="kg",parent=l);fields["sex"]=field("Sex",pet.optString("sex"),"Sex",dropdown=listOf("Female","Male","Unknown"),parent=right);r.addView(l,LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(4)});r.addView(right,LinearLayout.LayoutParams(0,-2,1f).apply{leftMargin=dp(4)});add(r)
            fields["notes"]=field("About $name",pet.optString("notes"),"Notes",multiline=true)
        }else weight=field("Weight", "","Weight",hint="kg (optional)")
        gap(10);add(button(if(edit)"Save Changes"else "Continue"){
            if(fields.getValue("name").text.isBlank()){fields.getValue("name").error="Pet name is required";return@button}
            val values=fields.mapValues{it.value.text.toString().trim()};prefs.edit().putString("pet-ui-weight:${values["name"]}",weight.text.toString()).apply();savePet(values,!edit)
        },52)
    }
    private fun myPets(){
        header("My Pets",back="Home",extra="Plus",onExtra={go("Add Your Pet")},left=true)
        pets.forEachIndexed{i,p->petCard(p.optString("name"),"${p.optString("species")} · ${if(i==0)"2" else "3"} years old",if(p.optString("species")=="Dog")R.drawable.pdf_bailey else R.drawable.pdf_mochi,p.optString("name")==name){selectPet(i)}}
        if(pets.size<4){petCard("Luna","1 year old · Rabbit · Demo",R.drawable.pdf_luna,false){preview("Luna is a sample pet from the wireframe. Use Add Another Pet to create your own profile.")};petCard("Simba","4 years old · Cat · Demo",R.drawable.pdf_simba,false){preview("Simba is a sample pet from the wireframe.")}}
        gap(14);add(button("+  Add Another Pet"){go("Add Your Pet")},52)
    }
    private fun petCard(label:String,detail:String,image:Int,selected:Boolean,callback:()->Unit){
        val r=row().apply{background=shape(if(selected)elevated else surface,24).apply{if(selected)setStroke(dp(2),lavender)};pad(this,13,12);setOnClickListener{callback()}}
        r.addView(portrait(image,82),LinearLayout.LayoutParams(dp(82),dp(82)).apply{rightMargin=dp(16)})
        val labels=col();labels.addView(heading(label,23f));labels.addView(text(detail,13f,sub),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)});r.addView(labels,LinearLayout.LayoutParams(0,-2,1f));r.addView(icon("Chevron",24));add(r,110,12)
    }

    private val caregivers=listOf(Triple("You","Owner",R.drawable.pdf_owner),Triple("Mom","Family",R.drawable.pdf_mom),Triple("Ate","Sitter",R.drawable.pdf_ate),Triple("Jam","Family",R.drawable.pdf_jam))
    private fun circleDiagram(access:Boolean):View{
        val frame=FrameLayout(a)
        fun avatar(label:String,role:String,image:Int,size:Int,x:Float,y:Int){
            val c=col().apply{gravity=Gravity.CENTER;contentDescription="$label, $role, sample caregiver";setOnClickListener{if(label==name)go("Pet Detail")else go("Care Circle Access")}}
            c.addView(portrait(image,size,if(label=="Ate"||label=="Mom")mint else primary));c.addView(center(label,14f,ink,700),LinearLayout.LayoutParams(-1,dp(21)));if(role.isNotEmpty())c.addView(center(role,12f,sub),LinearLayout.LayoutParams(-1,dp(19)))
            frame.addView(c,FrameLayout.LayoutParams(dp(size+14),-2).apply{topMargin=dp(y)})
            c.tag=x
        }
        if(access){avatar("You","Owner",R.drawable.pdf_owner,60,.13f,6);avatar(name,"",R.drawable.pdf_mochi,96,.5f,6);avatar("Mom","Family",R.drawable.pdf_mom,60,.87f,6);avatar("Ate","Sitter",R.drawable.pdf_ate,60,.13f,125);avatar("Jam","Family",R.drawable.pdf_jam,60,.5f,140)
            val invite=col().apply{gravity=Gravity.CENTER;tag=.87f;setOnClickListener{preview("Caregiver invitations require authenticated shared accounts.")}};invite.addView(action("Plus","Invite caregiver"){preview("Invitations are a frontend preview.")}.apply{background=shape(surface,40)},LinearLayout.LayoutParams(dp(64),dp(64)));invite.addView(center("Invite",14f,ink,700));frame.addView(invite,FrameLayout.LayoutParams(dp(74),dp(96)).apply{topMargin=dp(127)})
        }else{avatar(name,"",R.drawable.pdf_mochi,120,.5f,40);avatar("You","Owner",R.drawable.pdf_owner,54,.12f,8);avatar("Mom","Family",R.drawable.pdf_mom,54,.88f,8);avatar("Ate","Sitter",R.drawable.pdf_ate,54,.12f,120);avatar("Jam","Family",R.drawable.pdf_jam,54,.88f,120)}
        frame.addOnLayoutChangeListener{v,_,_,_,_,_,_,_,_->for(i in 0 until frame.childCount){val child=frame.getChildAt(i);val fraction=child.tag as? Float?:continue;child.translationX=v.width*fraction-child.width/2f}}
        val heart=icon("Heart",23,pink);frame.addView(heart,FrameLayout.LayoutParams(dp(23),dp(23),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        return frame
    }
    private fun careCircle(access:Boolean){
        header("Care Circle",if(access)"People who help care for $name." else "People who care for $name",back="More",extra=if(access)"Person"else null,onExtra={preview("Invitations require connected shared accounts.")})
        if(!access)add(center("Share access so family and sitters\ncan help keep $name happy and healthy.",14f),44,8)
        add(circleDiagram(access),if(access)250 else 235,10)
        if(access){
            add(chips(listOf("Access Control","Activity Log")){if(it=="Activity Log")go("Activity Log")},42,16)
            sectionLabel("Access Permissions")
            add(toggleRow("Document","View Profile & Records","Can view all pet information",true,"access:view"),65,3)
            add(toggleRow("Edit","Add & Edit Care Logs","Can add and edit care activities",true,"access:logs"),65,3)
            add(toggleRow("Bell","Manage Reminders","Can create and edit reminders",false,"access:reminders"),65,3)
            add(toggleRow("Folder","Manage Documents","Can add and edit documents",false,"access:documents"),65,8)
            add(button("Save Permissions"){preview("Preferences are saved on this device as a preview. No caregiver access has been granted.")},50)
        }else{
            add(button("Invite Caregiver"){preview("Sample Care Circle: invitations and shared access are not connected.")},50,20)
            sectionLabel("Care Circle (4) · Demo",10)
            caregivers.forEach{(label,role,image)->val r=row().apply{background=shape();pad(this,10,7);setOnClickListener{go("Care Circle Access")}};r.addView(portrait(image,46),LinearLayout.LayoutParams(dp(46),dp(46)).apply{rightMargin=dp(14)});val labels=col();labels.addView(text(label,15f,ink,700));labels.addView(text("$role · ${if(label=="You")"Full access"else if(label=="Ate")"Today's care only"else "Care + Records"}",12f,sub),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)});r.addView(labels,LinearLayout.LayoutParams(0,-2,1f));r.addView(icon("Chevron",22));add(r,62,4)}
        }
    }
    private fun voiceLog(){
        header("Voice Log","Just say what happened.",back="Care");add(art(R.drawable.pdf_voice_cat,76),76)
        val transcription=col().apply{background=shape(elevated,22);pad(this,20,16);gravity=Gravity.CENTER}
        transcription.addView(heading("“Fed $name one cup\nat 7:30 AM”",21f).apply{gravity=Gravity.CENTER})
        transcription.addView(Waveform(a,primary),LinearLayout.LayoutParams(-1,dp(42)).apply{topMargin=dp(12)})
        add(transcription,142,12);demoLabel("Text-entry preview · speech recognition not connected")
        val details=col().apply{background=shape();pad(this,16,16)};details.addView(section("Confirm details"),LinearLayout.LayoutParams(-1,dp(36)))
        fun detail(label:String,kind:String,initial:String,options:List<String>?=null):EditText{
            val r=row();r.addView(icon(kind,24),LinearLayout.LayoutParams(dp(30),dp(34)).apply{rightMargin=dp(10)});r.addView(text(label,13f,ink,700),LinearLayout.LayoutParams(0,dp(36),1f))
            val input=EditText(a).apply{setText(initial);textSize=13f;BrandType.apply(this,false,400);setTextColor(ink);background=shape(elevated,15);setSingleLine();pad(this,12,6);contentDescription=label}
            if(options!=null){input.isFocusable=false;input.setOnClickListener{AlertDialog.Builder(a).setTitle(label).setItems(options.toTypedArray()){_,i->input.setText(options[i])}.show()}}
            r.addView(input,LinearLayout.LayoutParams(dp(164),dp(44)));details.addView(r,LinearLayout.LayoutParams(-1,dp(48)));return input
        }
        val chosenPet=detail("Pet","Person",name,pets.map{it.optString("name")});val kind=detail("Activity","Pets","Feeding",listOf("Feeding","Medication","Water","Playtime","Vet Visit","Grooming"))
        val amount=detail("Amount","Care","1 cup");val time=detail("Time","Clock","7:30 AM")
        details.addView(text("Add a note (optional)",13f,sub),LinearLayout.LayoutParams(-1,dp(28)).apply{topMargin=dp(8)})
        val note=EditText(a).apply{setText("Finished everything!");textSize=14f;setTextColor(ink);BrandType.apply(this,false,400);background=shape(elevated,14);pad(this,12,8);contentDescription="Optional note"};details.addView(note,LinearLayout.LayoutParams(-1,dp(48)))
        add(details,-2,12);add(button("Save Log"){
            if(chosenPet.text.toString()!=name){preview("Select ${chosenPet.text} from My Pets before saving their care entry.");return@button}
            if(amount.text.isBlank()||time.text.isBlank()){notice("Enter an amount and time.");return@button}
            saveLog(kind.text.toString(),"${amount.text} at ${time.text}. ${note.text}")
        },52)
    }
    private fun soundJournal(){
        header("Sound Event Journal","Record unusual sounds or observations\nto keep track over time.",back="Care")
        val visual=FrameLayout(a);visual.addView(Waveform(a,primary,true),FrameLayout.LayoutParams(dp(200),dp(200),Gravity.CENTER));visual.addView(center("00:12",16f,primary,700).apply{translationY=dp(70).toFloat()},FrameLayout.LayoutParams(dp(70),dp(28),Gravity.CENTER));add(visual,210,8)
        val controls=row().apply{gravity=Gravity.CENTER};controls.addView(action("Stop","Stop preview"){notice("Preview stopped. No audio was recorded.")}.apply{background=shape()},LinearLayout.LayoutParams(dp(105),dp(58)).apply{rightMargin=dp(44)});controls.addView(action("Check","Save written sound observation"){go("Quick Log:Other")}.apply{background=shape(lavender,40,false)},LinearLayout.LayoutParams(dp(64),dp(64)));add(controls,70,10)
        demoLabel("Audio preview · recording and playback not connected");sectionLabel("Recent Recordings (3)")
        listOf(Triple("Coughing","00:18","Sep 25, 2026 · 8:14 PM"),Triple("Meowing (prolonged)","00:12","Sep 20, 2026 · 6:03 PM"),Triple("Sneezing","00:06","Sep 12, 2026 · 10:21 AM")).forEach{(label,duration,date)->add(infoRow("Play",label,"$duration · $date","More"){preview("This is a sample recording row. No audio file is attached.")},62,5)}
        val card=row().apply{background=shape();pad(this,10,9)};card.addView(art(R.drawable.pdf_sound_cat,76),LinearLayout.LayoutParams(dp(88),dp(76)));card.addView(text("Keep a record of unusual sounds\nto share with your veterinarian\nwhen needed.",12f,sub),LinearLayout.LayoutParams(0,-2,1f));add(card,96)
    }
    private data class SampleDocument(val name:String,val date:String,val type:String,val tint:String)
    private val documents=listOf(SampleDocument("Vaccination Card","Sep 12, 2026","Vaccinations","#F3B6D7"),SampleDocument("Vet Receipt","Aug 28, 2026","Documents","#D8B4F8"),SampleDocument("Prescription","Aug 10, 2026","Medications","#FFD0BC"),SampleDocument("Vet Visit Notes","Jul 15, 2026","Appointments","#9BCBFF"))
    private fun documentRow(d:SampleDocument,tag:Boolean=false):View{
        val r=row().apply{background=shape();pad(this,13,9);setOnClickListener{if(d.name=="Vaccination Card")go("Vaccination Card")else preview("${d.name} is a sample document. No file is attached.")}}
        r.addView(icon("Document",32,color(d.tint)),LinearLayout.LayoutParams(dp(34),dp(36)).apply{rightMargin=dp(14)})
        val copy=col();copy.addView(text(d.name,14f,ink,700));copy.addView(text(d.date,12f,sub),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)});r.addView(copy,LinearLayout.LayoutParams(0,-2,1f))
        if(tag)r.addView(text(d.type.removeSuffix("s"),10f,primary,500).apply{background=shape(elevated,18,false);pad(this,7,5)},LinearLayout.LayoutParams(-2,-2).apply{rightMargin=dp(7)})
        r.addView(icon(if(tag)"Chevron"else "More",20));return r
    }
    private fun records(){
        header("Pet Records","Keep all of $name's important\ndocuments in one place.",back="More")
        val identity=row().apply{background=shape(elevated,20);pad(this,12,8);setOnClickListener{choose()}};identity.addView(portrait(size=58),LinearLayout.LayoutParams(dp(58),dp(58)).apply{rightMargin=dp(16)});val copy=col();copy.addView(heading(name,23f));copy.addView(text("${pet.optString("species")} · 2 years old",12f,sub));identity.addView(copy,LinearLayout.LayoutParams(0,-2,1f));identity.addView(icon("Down",24));add(identity,80,12)
        add(chips(listOf("All","Vaccinations","Medications","Receipts")){go("Browse Records")},38,16)
        val upload=col().apply{background=shape(elevated,24);pad(this,16,14);gravity=Gravity.CENTER}
        upload.addView(art(R.drawable.pdf_folder,118),LinearLayout.LayoutParams(-1,dp(118)));upload.addView(section("Scan or upload a document").apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,dp(32)));upload.addView(center("Vaccination cards, prescriptions,\nreceipts, and more.",14f),LinearLayout.LayoutParams(-1,dp(45)));upload.addView(button("Add Document"){preview("Document upload requires file selection and storage integration. No sample file has been uploaded.")},LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(8)});add(upload,278,18)
        val h=row();h.addView(section("Recent Documents"),LinearLayout.LayoutParams(0,-2,1f));h.addView(text("See All",12f,primary,700).apply{setOnClickListener{go("Browse Records")}});add(h,28,8)
        documents.take(2).forEach{add(documentRow(it),66,5)};demoLabel("Sample documents")
    }
    private fun vaccination(){
        header("Vaccination Card","Added on Sep 12, 2026 · Sample document",back="Records",extra="More",onExtra={preview("This document is a supplied wireframe sample.")})
        val card=col().apply{background=shape();pad(this,11,11)}
        val image=art(R.drawable.pdf_vaccine_record,330).apply{contentDescription="Sample vaccination record from the PDF; not a clinical record for this pet"}
        card.addView(image,LinearLayout.LayoutParams(-1,dp(330)))
        val meta=row();meta.addView(icon("Document",32,pink),LinearLayout.LayoutParams(dp(42),dp(48)));val labels=col();labels.addView(text("Vaccination Card",15f,ink,700));labels.addView(text("Sep 12, 2026 · 2.4 MB · PDF",12f,sub));meta.addView(labels);card.addView(meta,LinearLayout.LayoutParams(-1,dp(64)));add(card,-2,6)
        val actions=col().apply{background=shape();pad(this,2,3)}
        listOf("Expand" to "View Full Screen","Download" to "Download","Share" to "Share","Edit" to "Rename","Trash" to "Delete").forEachIndexed{i,(kind,label)->val item=infoRow(kind,label,trailing="",tint=if(i>=3)coral else primary){
            if(i==0){val full=ImageView(a).apply{setImageResource(R.drawable.pdf_vaccine_record);adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER};AlertDialog.Builder(a).setTitle("Sample vaccination record").setView(full).setPositiveButton("Close",null).show()}
            else preview("$label requires an attached document. This is a visual sample; no file was changed or shared.")
        };item.background=null;actions.addView(item,LinearLayout.LayoutParams(-1,dp(46)));if(i<4)actions.addView(View(a).apply{setBackgroundColor(outline)},LinearLayout.LayoutParams(-1,dp(1)).apply{leftMargin=dp(16);rightMargin=dp(16)})};add(actions)
    }
    private fun recordsSearch(){
        header("Records","Find specific documents, visits,\nmedications, and more.",back="Records")
        var selectedType="All";var selectedDate="All"
        val search=row();val query=EditText(a).apply{hint="Search records…";contentDescription="Search records";textSize=15f;BrandType.apply(this,false,400);setTextColor(ink);setHintTextColor(sub);background=shape(surface,30);setSingleLine();pad(this,18,10)};search.addView(query,LinearLayout.LayoutParams(0,dp(48),1f));search.addView(action("Filter","Filter records"){notice("Choose a type or date below.")}.apply{background=shape(elevated,28)},LinearLayout.LayoutParams(dp(48),dp(48)).apply{leftMargin=dp(10)});add(search,48,22)
        sectionLabel("Filter by Type");val typeControls=col();add(typeControls,-2,18)
        sectionLabel("Filter by Date");val dateControls=col();add(dateControls,-2,20)
        val count=section("Recent Results (4)");add(count,28,8);val results=col();add(results);demoLabel("Searchable sample documents")
        fun refresh(){results.removeAllViews();val matches=documents.filter{d->(selectedType=="All"||d.type==selectedType)&&(query.text.isBlank()||d.name.contains(query.text.toString(),true))&&(selectedDate!="This Month"||d.date.startsWith("Sep"))&&(selectedDate!="Custom"||d.date.startsWith("Sep 12"))};count.text="Recent Results (${matches.size})";matches.forEach{results.addView(documentRow(it,true),LinearLayout.LayoutParams(-1,dp(62)).apply{bottomMargin=dp(5)})};if(matches.isEmpty())results.addView(center("No matching sample documents.",14f))}
        fun drawTypes(){typeControls.removeAllViews();listOf(listOf("All","Vaccinations","Medications"),listOf("Appointments","Documents","Care Logs")).forEach{labels->typeControls.addView(chips(labels,selectedType){selectedType=it;drawTypeSelection(typeControls,it);refresh()},LinearLayout.LayoutParams(-1,dp(42)))}}
        fun drawDates(){dateControls.removeAllViews();listOf(listOf("All","This Month","Last 3 Months"),listOf("Last Year","Custom")).forEach{labels->dateControls.addView(chips(labels,selectedDate){selectedDate=it;drawTypeSelection(dateControls,it);if(it=="Custom")notice("Showing the sample document dated Sep 12, 2026.");refresh()},LinearLayout.LayoutParams(-1,dp(42)))}}
        query.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){refresh()};override fun afterTextChanged(s:Editable?) {}});drawTypes();drawDates();refresh()
    }
    private fun drawTypeSelection(container:ViewGroup,selected:String){for(i in 0 until container.childCount){val child=container.getChildAt(i);if(child is ViewGroup)drawTypeSelection(child,selected)else if(child is TextView){child.background=shape(if(child.text.toString()==selected)lavender else surface,24);child.setTextColor(if(child.text.toString()==selected)color("#24192E")else ink)}}}

    private val schedule=listOf(Triple("Breakfast","8:00 AM","Feeding"),Triple("Vitamins","12:00 PM","Medication"),Triple("Playtime","5:00 PM","Playtime"),Triple("Dinner","7:00 PM","Feeding"))
    private fun scheduleRow(label:String,time:String,kind:String,detail:String,complete:Boolean=false,onClick:()->Unit):View{
        val r=row().apply{background=shape();pad(this,14,8);setOnClickListener{onClick()}};r.addView(icon(kind,36),LinearLayout.LayoutParams(dp(40),dp(40)).apply{rightMargin=dp(14)})
        val texts=col();texts.addView(text(time,12f,sub));texts.addView(text(label,16f,ink,700));texts.addView(text(detail,12f,sub));r.addView(texts,LinearLayout.LayoutParams(0,-2,1f));val status=FrameLayout(a).apply{background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(if(complete)mint else Color.TRANSPARENT);if(!complete)setStroke(dp(2),outline)};if(complete)addView(icon("Check",22,color("#264439")),FrameLayout.LayoutParams(-1,-1))};r.addView(status,LinearLayout.LayoutParams(dp(27),dp(27)));return r
    }
    private fun reminders(){
        header("Reminders","Never miss a moment\nof their care.",back="More",extra="Plus",onExtra=addReminder)
        var filter="All";val filters=chips(listOf("All","Medication","Feeding","Others")){filter=it;goFilteredReminders(it)};add(filters,38,18)
        val filterSaved=prefs.getString("ui:reminder-filter","All")!!;filter=filterSaved;drawTypeSelection(filters as ViewGroup,filter)
        sectionLabel("Today · Thu, Sep 25",10)
        schedule.forEachIndexed{i,(label,time,kind)->if(filter=="All"||filter==kind||(filter=="Others"&&kind=="Playtime"))add(scheduleRow(label,time,kind,"${if(i==1)"1 tablet"else if(i==2)"20 mins"else "1/2 cup kibble"} · $name",i==0){go("Quick Log:$kind")},73,5)}
        gap(16);sectionLabel("Tomorrow · Fri, Sep 26",10)
        if(filter=="All"||filter=="Medication")add(scheduleRow("Medication","8:00 AM","Medication","1 tablet · Luna (demo)"){go("Quick Log:Medication")},73,5)
        if(filter=="All"||filter=="Others")add(scheduleRow("Vet Visit","6:00 PM","Vet Visit","Annual check-up · Bailey (demo)"){addReminder()},73,5)
        demoLabel("Sample schedule")
        if(reminders.isNotEmpty()){sectionLabel("Your reminders");reminders.forEach{r->add(scheduleRow(r.optString("title"),r.optString("when"),"Care",r.optString("pet"),r.optBoolean("done")){toggleReminder(r)},73,5)}}
    }
    private fun goFilteredReminders(filter:String){prefs.edit().putString("ui:reminder-filter",filter).apply();go("Reminders")}
    private fun calendar(){
        header("Care Calendar","See all of $name's care at a glance.",back="More",extra="Care",onExtra=addReminder)
        val date=Calendar.getInstance().apply{set(2026,Calendar.SEPTEMBER,25,12,0,0)}
        val calendar=col();add(calendar)
        val scheduleBox=col();add(scheduleBox)
        fun renderCalendar(){
            calendar.removeAllViews();scheduleBox.removeAllViews()
            val month=row().apply{background=shape()};val label=center(SimpleDateFormat("MMMM yyyy",Locale.US).format(date.time),17f,primary,700)
            month.addView(action("Back","Previous month"){date.add(Calendar.MONTH,-1);renderCalendar()},LinearLayout.LayoutParams(dp(44),dp(44)));month.addView(label,LinearLayout.LayoutParams(0,dp(44),1f));month.addView(action("Chevron","Next month"){date.add(Calendar.MONTH,1);renderCalendar()},LinearLayout.LayoutParams(dp(44),dp(44)));calendar.addView(month)
            val weekdays=row();listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach{weekdays.addView(center(it,12f,sub),LinearLayout.LayoutParams(0,dp(42),1f))};calendar.addView(weekdays)
            val start=date.clone() as Calendar;start.add(Calendar.DAY_OF_MONTH,-(start.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY))
            repeat(2){week->val r=row();repeat(7){day->val cellDate=start.clone() as Calendar;cellDate.add(Calendar.DAY_OF_MONTH,week*7+day);val chosen=cellDate.get(Calendar.DAY_OF_YEAR)==date.get(Calendar.DAY_OF_YEAR)&&cellDate.get(Calendar.YEAR)==date.get(Calendar.YEAR);val cell=col().apply{gravity=Gravity.CENTER;contentDescription=SimpleDateFormat("MMMM d, yyyy",Locale.US).format(cellDate.time);setOnClickListener{date.time=cellDate.time;renderCalendar()}}
                cell.addView(center(cellDate.get(Calendar.DAY_OF_MONTH).toString(),15f,if(chosen)color("#26192E")else ink,if(chosen)700 else 400).apply{if(chosen)background=shape(lavender,28,false)},LinearLayout.LayoutParams(dp(38),dp(38)))
                cell.addView(View(a).apply{background=shape(listOf(mint,yellow,pink,primary)[day%4],6,false)},LinearLayout.LayoutParams(dp(6),dp(6)).apply{topMargin=dp(3)})
                r.addView(cell,LinearLayout.LayoutParams(0,dp(54),1f))};calendar.addView(r)}
            val selected=center(SimpleDateFormat("EEE, MMM d, yyyy",Locale.US).format(date.time),16f,ink,700).apply{background=shape();gravity=Gravity.CENTER_VERTICAL;pad(this,16,8)};calendar.addView(selected,LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(16);bottomMargin=dp(8)})
            schedule.forEachIndexed{i,(label,time,kind)->val r=row();val railColor=listOf(mint,pink,yellow,yellow)[i];val rail=FrameLayout(a).apply{addView(View(a).apply{background=shape(railColor,2,false)},FrameLayout.LayoutParams(dp(2),-1,Gravity.CENTER));addView(View(a).apply{background=shape(railColor,12,false)},FrameLayout.LayoutParams(dp(12),dp(12),Gravity.CENTER))};r.addView(rail,LinearLayout.LayoutParams(dp(20),dp(76)).apply{rightMargin=dp(8)});r.addView(text(time,11f,sub),LinearLayout.LayoutParams(dp(61),-2));r.addView(scheduleRow(label,"",kind,if(i==2)"20 mins · $name"else if(i==1)"1 tablet · $name"else "1/2 cup · $name",i==0){go("Quick Log:$kind")},LinearLayout.LayoutParams(0,dp(68),1f));scheduleBox.addView(r,LinearLayout.LayoutParams(-1,dp(76)))}
        }
        renderCalendar();gap(18);add(button("Add to Google Calendar"){preview("This is a sample schedule. Google Calendar export is not connected.")},50,4);demoLabel("Sample schedule · dates are selectable")
    }
    private fun idCard(horizontal:Boolean=false):View{
        val card=if(horizontal)row()else col().apply{gravity=Gravity.CENTER}
        card.background=PetIdBackground(horizontal);pad(card,18,16)
        val identity=col().apply{gravity=if(horizontal)Gravity.START else Gravity.CENTER};identity.addView(portrait(size=if(horizontal)90 else 138,ring=color("#D995DF")));identity.addView(text(name,if(horizontal)23f else 27f,color("#211626"),600,true),LinearLayout.LayoutParams(-2,-2).apply{topMargin=dp(5)});identity.addView(text("${pet.optString("species")} · 2 years old",13f,color("#33223D")).apply{gravity=if(horizontal)Gravity.START else Gravity.CENTER})
        if(horizontal){card.addView(identity,LinearLayout.LayoutParams(0,-2,1f));card.addView(DemoQr(a),LinearLayout.LayoutParams(dp(112),dp(112)))}else{card.addView(identity);card.addView(DemoQr(a),LinearLayout.LayoutParams(dp(146),dp(146)).apply{topMargin=dp(10)});card.addView(text("Demo QR · not scannable",12f,color("#33223D"),700),LinearLayout.LayoutParams(-2,dp(28)).apply{topMargin=dp(5)})}
        return card
    }
    private fun petId(){
        header("Pet ID QR","A safer tomorrow for your furbaby.",back="More");add(idCard(),398,10)
        add(button("Customize Info"){go("QR Privacy")},44,18);sectionLabel("Share or Download")
        val actions=row();listOf("Share" to "Share","Download" to "Download","Print" to "Print","Photo" to "Save to Photos").forEach{(kind,label)->val c=col().apply{gravity=Gravity.CENTER};c.addView(action(kind,label){preview("The QR card is a non-scannable demo. $label is not connected to an exportable pet ID.")}.apply{background=shape(surface,40)},LinearLayout.LayoutParams(dp(58),dp(58)));c.addView(center(label,11f,ink),LinearLayout.LayoutParams(-1,dp(27)));actions.addView(c,LinearLayout.LayoutParams(0,dp(86),1f))};add(actions,86,14)
        val info=row().apply{background=shape();pad(this,10,7)};info.addView(art(R.drawable.pdf_id_cat,64),LinearLayout.LayoutParams(dp(88),dp(64)));info.addView(text("You can choose what information\nis shown on $name's ID card.",12f,sub),LinearLayout.LayoutParams(0,-2,1f));add(info,80)
    }
    private fun emergency(){
        header("Emergency Mode","Quick access to important\ninformation when it matters.",back="More",extra="Warning",onExtra={preview("Emergency details have not been configured in this prototype.")})
        val card=row().apply{background=shape(if(night)color("#302026")else color("#FBE5EA"));pad(this,14,14)};card.addView(portrait(size=102,ring=coral),LinearLayout.LayoutParams(dp(102),dp(102)).apply{rightMargin=dp(14)})
        val copy=col();copy.addView(heading(name,26f));copy.addView(text("${pet.optString("species")} · 2 years old",13f,sub));copy.addView(button("EMERGENCY INFO",tint=color("#FF8298")){preview("No emergency contact or clinical record is configured. Open Pet Passport to review available details.")},LinearLayout.LayoutParams(-1,dp(42)).apply{topMargin=dp(10)});card.addView(copy,LinearLayout.LayoutParams(0,-2,1f));add(card,142,12)
        add(infoRow("Medical","Medical Information","Allergies, medications, conditions",tint=coral){go("Pet Passport")},68,6)
        add(infoRow("Vet Visit","Veterinarian","Contact not configured","Phone",coral){preview("Add a verified veterinary contact before using this action.")},68,6)
        add(infoRow("Phone","Emergency Contact","Contact not configured","Phone",coral){go("Personal Information")},68,6)
        add(infoRow("Pets","Care Circle","View your caregivers",tint=coral){go("Care Circle")},68,6)
        add(infoRow("Document","Pet Passport","View and share full summary",tint=coral){go("Pet Passport")},68,20)
        add(button("Share Emergency Info",tint=color("#FF8298")){preview("Emergency sharing is not connected. No information has been shared.")},52)
    }
    private fun offline(){
        header("Offline & Sync","Keep recording, even without\ninternet connection.",back="More");add(art(R.drawable.pdf_offline_cat,112),112)
        val status=infoRow("Wifi","You're offline","Your entries are saved locally.\nCloud sync is not connected.","",pink){preview("This is the offline presentation state. Device network connectivity is not being measured.")};add(status,116,20)
        val pending=if(logs.isEmpty())3 else logs.size
        val h=row();h.addView(section("Pending Sync ($pending)"),LinearLayout.LayoutParams(0,-2,1f));h.addView(button("Sync Now"){preview("Firebase synchronization is not connected. Local entries remain on this device.")},LinearLayout.LayoutParams(dp(118),dp(40)));add(h,44,10)
        if(logs.isEmpty()){demoLabel("Offline preview · sample pending entries");listOf(Triple("Feeding","Sep 25, 8:00 AM","Feeding"),Triple("Medication","Sep 25, 12:00 PM","Medication"),Triple("Vet Visit","Sep 24, 4:00 PM","Vet Visit")).forEach{(label,date,kind)->add(infoRow(kind,label,"$date · $name","Cloud"){preview("Sample pending entry; cloud sync is not connected.")},72,4)}}else logs.forEach{entry->add(infoRow("Notes",entry.optString("kind"),entry.optString("date"),"Cloud"){go("Care History")},72,4)}
        gap(12);val info=row().apply{background=shape();pad(this,14,10)};info.addView(icon("Cloud",32),LinearLayout.LayoutParams(dp(40),dp(42)));info.addView(text("Your local records stay available\nwhile cloud sync is disconnected.",12f,sub),LinearLayout.LayoutParams(0,-2,1f));info.addView(art(R.drawable.pdf_sync_cat,70),LinearLayout.LayoutParams(dp(72),dp(70)));add(info,92)
    }
    private fun activityLog(){
        header("Care Circle Activity Log","Track who did what and when.",back="Care Circle Access")
        val filters=listOf("All Activities","Care Logs","Documents","Reminders","Profile")
        var selected="All Activities";val chooseFilter=button(selected,true){ };add(chooseFilter,48,16)
        val results=col();add(results)
        data class Entry(val title:String,val detail:String,val by:String,val kind:String,val type:String,val yesterday:Boolean=false)
        val entries=listOf(Entry("Logged Feeding","8:00 AM · 1/2 cup kibble","Mom","Feeding","Care Logs"),Entry("Logged Medication","12:00 PM · 1 tablet","You","Medication","Care Logs"),Entry("Added Document","2:15 PM · Vet Receipt","Ate","Document","Documents"),Entry("Updated Reminder","6:30 PM · Changed time","Jam","Bell","Reminders",true),Entry("Added Care Log","5:00 PM · Playtime","Ate","Playtime","Care Logs",true),Entry("Edited Pet Profile","10:20 AM · Updated weight","You","Cat","Profile",true))
        fun draw(){results.removeAllViews();listOf(false,true).forEach{yesterday->val matching=entries.filter{it.yesterday==yesterday&&(selected=="All Activities"||it.type==selected)};if(matching.isNotEmpty()){results.addView(section(if(yesterday)"Yesterday · Wed, Sep 24, 2026"else "Today · Thu, Sep 25, 2026"),LinearLayout.LayoutParams(-1,dp(38)).apply{topMargin=dp(if(yesterday)12 else 0)})
            matching.forEach{e->val r=row().apply{background=shape();pad(this,12,9);setOnClickListener{preview("Sample caregiver activity. Live attribution requires shared accounts.")}};r.addView(icon(e.kind,30),LinearLayout.LayoutParams(dp(34),dp(38)).apply{rightMargin=dp(12)});val copy=col();copy.addView(text(e.title,14f,ink,700));copy.addView(text("${e.detail}\nby ${e.by}",12f,sub));r.addView(copy,LinearLayout.LayoutParams(0,-2,1f));r.addView(portrait(caregivers.first{it.first==e.by}.third,38),LinearLayout.LayoutParams(dp(38),dp(38)).apply{rightMargin=dp(10)});r.addView(icon("More",20));results.addView(r,LinearLayout.LayoutParams(-1,dp(78)).apply{bottomMargin=dp(5)})}
        }}}
        chooseFilter.setOnClickListener{AlertDialog.Builder(a).setTitle("Filter activities").setItems(filters.toTypedArray()){_,i->selected=filters[i];chooseFilter.text=selected;draw()}.show()};draw();demoLabel("Sample caregiver activity")
    }
    private fun privacy(){
        header("QR Information Privacy","Choose what information to share\nwhen $name's QR code is scanned.",back="Pet ID QR")
        add(idCard(true),178);add(button("Preview QR Code",true){go("Pet ID QR")},42,16);sectionLabel("Information to Share")
        val items=listOf(Triple("Pets","Pet Name",true),Triple("Photo","Photo",true),Triple("Cat","Species & Breed",true),Triple("Care","Age / Birthday",false),Triple("Phone","Owner Contact Number",true),Triple("Mail","Owner Email",false),Triple("Medical","Medical Information",true),Triple("Vet Visit","Veterinarian",true))
        items.forEach{(kind,label,default)->add(toggleRow(kind,label,when(label){"Medical Information"->"Allergies, medications (basic)";"Veterinarian"->"Clinic name and contact";else->null},default,"privacy:$label"),if(label in setOf("Medical Information","Veterinarian"))64 else 44,2)}
        demoLabel("Privacy preferences are local · nothing is published")
    }
    private fun more(){
        header("More to care for.","Everything you need, right where you expect it.",back="Home")
        listOf(Triple("Person","My Profile","Profile"),Triple("Document","Pet Records","Records"),Triple("Search","Search Records","Browse Records"),Triple("Bell","Reminders","Reminders"),Triple("Care","Care Calendar","Calendar"),Triple("Pets","Care Circle","Care Circle"),Triple("Link","Sitter Mode","Sitter Mode"),Triple("Lock","Care Circle Access","Care Circle Access"),Triple("Notes","Activity Log","Activity Log"),Triple("QR","Pet ID QR","Pet ID QR"),Triple("Emergency","Emergency Mode","Emergency"),Triple("Cloud","Offline & Sync","Offline & Sync"),Triple("Lock","QR Information Privacy","QR Privacy"),Triple("Wave","Sound Event Journal","Sound Journal"),Triple("Notes","Care History","Care History")).forEach{(kind,label,target)->add(infoRow(kind,label){go(target)},58,5)}
    }
}
