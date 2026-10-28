package com.purrfolio.mco1

import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.purrfolio.mco1.databinding.FloatingBottomNavBinding

/** Reference-specific presentation. Persistence and existing forms remain owned by MainActivity. */
class ReferenceScreens(private val a: Activity, private val pet: org.json.JSONObject, private val go: (String)->Unit, private val choose: ()->Unit) {
    private val name get()=pet.optString("name","Mochi")
    companion object { val destinations=setOf("Welcome","Home","Pet Detail","Care","Pet Timeline","Sitter Mode","Pet Passport") }
    private val night=a.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private fun color(s:String)=Color.parseColor(s)
    private val bg=color(if(night)"#101014" else "#FCF9FF")
    private val surface=color(if(night)"#1C1B26" else "#F2EBF7")
    private val elevated=color(if(night)"#24222F" else "#EBE0F3")
    private val lavender=color("#D8B4F8")
    private val primary=color(if(night)"#E1BEFC" else "#8051A8")
    private val ink=color(if(night)"#F8F2FF" else "#292130")
    private val sub=color(if(night)"#BDB3CD" else "#74667F")
    private val outline=color(if(night)"#373243" else "#D9CCE5")
    private val mint=color("#91E5C8")
    private val pink=color("#F3B6D7")
    private val yellow=color("#FFD18C")
    private val blue=color("#9BCBFF")
    private val fredoka=ResourcesCompat.getFont(a,R.font.fredoka)!!
    private val nunito=ResourcesCompat.getFont(a,R.font.nunito)!!
    private val photo=BitmapFactory.decodeResource(a.resources,R.drawable.purrfolio_cat_hero)
    private lateinit var content: LinearLayout
    private fun dp(n:Int)=(n*a.resources.displayMetrics.density+.5f).toInt()
    private fun col()=LinearLayout(a).apply {orientation=LinearLayout.VERTICAL}
    private fun row()=LinearLayout(a).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    private fun pad(v:View,l:Int,t:Int=l,r:Int=l,b:Int=t){v.setPadding(dp(l),dp(t),dp(r),dp(b))}
    private fun shape(c:Int,r:Int=22,border:Boolean=false)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();if(border)setStroke(dp(1),outline)}
    private fun text(s:String,size:Float=14f,c:Int=ink,weight:Int=400,display:Boolean=false)=TextView(a).apply{
        text=s;textSize=size;setTextColor(c);includeFontPadding=false
        BrandType.apply(this, display, weight)
        setLineSpacing(dp(2).toFloat(),1f)
    }
    private fun title(s:String,size:Float=28f)=text(s,size,primary,if(size<=22f || size>=30f)600 else 500,true)
    private fun spacer(h:Int){content.addView(View(a),LinearLayout.LayoutParams(1,dp(h)))}
    private fun add(v:View,h:Int=-2,bottom:Int=0){content.addView(v,LinearLayout.LayoutParams(-1,if(h<0)h else dp(h)).apply{bottomMargin=dp(bottom)})}
    private fun flexible(min:Int=8){content.addView(View(a),LinearLayout.LayoutParams(1,dp(min),1f))}
    private fun icon(kind:String,size:Int=30,tint:Int=primary)=CareArtwork(a,kind,tint).apply{layoutParams=LinearLayout.LayoutParams(dp(size),dp(size));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO}
    private fun action(kind:String,description:String,callback:()->Unit):FrameLayout=FrameLayout(a).apply{
        contentDescription=description;isFocusable=true;setOnClickListener{callback()}
        addView(icon(kind,26),FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));layoutParams=LinearLayout.LayoutParams(dp(48),dp(48))
    }
    private fun button(s:String,secondary:Boolean=false,callback:()->Unit)=text(s,17f,if(secondary)primary else color("#201528"),700).apply{
        gravity=Gravity.CENTER;background=shape(if(secondary)elevated else lavender,32,secondary);minHeight=dp(52);setOnClickListener{callback()};isFocusable=true
    }
    private fun preview(s:String){AlertDialog.Builder(a).setTitle("Frontend preview").setMessage(s).setPositiveButton("OK",null).show()}
    private fun header(s:String,extra:String?=null,back:String="Home"){
        val r=row();r.addView(action("Back","Back"){go(back)})
        r.addView(title(s).apply{gravity=Gravity.CENTER;maxLines=1;if(s.contains("⌄"))setOnClickListener{choose()};if(s.startsWith("•"))text=android.text.SpannableString(s).apply{setSpan(android.text.style.ForegroundColorSpan(mint),0,1,0)}},LinearLayout.LayoutParams(0,dp(56),1f))
        r.addView(if(extra!=null)action(extra,if(extra=="More")"Pet options" else "Share passport"){if(extra=="More")petOptions() else go("Pet ID QR")} else View(a),LinearLayout.LayoutParams(dp(48),dp(48)))
        add(r,64,12)
    }
    private fun portrait(size:Int):FrameLayout=FrameLayout(a).apply{
        background=shape(primary,size,true);pad(this,3)
        val bitmap=Bitmap.createBitmap(photo,(photo.width*.18f).toInt(),(photo.height*.22f).toInt(),(photo.width*.65f).toInt(),(photo.height*.65f).toInt())
        addView(ImageView(a).apply{setImageBitmap(bitmap);scaleType=ImageView.ScaleType.CENTER_CROP;background=shape(surface,size);clipToOutline=true;contentDescription="$name portrait"},FrameLayout.LayoutParams(-1,-1))
        layoutParams=LinearLayout.LayoutParams(dp(size),dp(size))
    }
    fun build(screen:String):View {
        WindowCompat.setDecorFitsSystemWindows(a.window,false)
        WindowCompat.getInsetsController(a.window,a.window.decorView).apply{isAppearanceLightStatusBars=!night;isAppearanceLightNavigationBars=!night}
        a.window.statusBarColor=bg;a.window.navigationBarColor=bg
        val root=col().apply{setBackgroundColor(bg)}
        ViewCompat.setOnApplyWindowInsetsListener(root){v,insets->val safe=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);insets}
        val scroll=ScrollView(a).apply{isFillViewport=true;isVerticalScrollBarEnabled=false;clipToPadding=false}
        content=col().apply{pad(this,22,if(screen=="Welcome")0 else 10,22,12)}
        scroll.addView(content,ViewGroup.LayoutParams(-1,-2));root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f).apply{if(screen=="Care")topMargin=dp(38)})
        when(screen){"Home"->home();"Pet Detail"->overview();"Care"->logCare();"Pet Timeline"->timeline();"Sitter Mode"->sitter();"Pet Passport"->passport()}
        if(screen!="Welcome"&&screen!="Care")root.addView(dock(screen),LinearLayout.LayoutParams(-1,dp(78)).apply{setMargins(dp(8),0,dp(8),dp(5))})
        ViewCompat.requestApplyInsets(root);return root
    }
    fun dock(screen:String):View{
        val binding=FloatingBottomNavBinding.inflate(a.layoutInflater)
        val active=when(screen){"Home"->"Home";"Pet Detail","Pets"->"Pets";"Profile","Account Settings","Notifications","More"->"More";else->"Care"}
        listOf(
            Triple("Home",binding.navHome,"Home"),
            Triple("Pets",binding.navPets,"Pets"),
            Triple("Care",binding.navCare,"Pet Timeline"),
            Triple("More",binding.navMore,"More")
        ).forEach{(label,cell,target)->
            val selected=label==active
            cell.background=if(selected)GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(elevated,if(night)color("#17131E") else surface)).apply{cornerRadius=dp(22).toFloat()}else null
            (cell.getChildAt(0) as CareArtwork).setArtwork(label,if(selected)primary else sub)
            (cell.getChildAt(1) as TextView).apply{setTextColor(if(selected)ink else sub);BrandType.apply(this,false,600)}
            cell.setOnClickListener{go(target)}
        }
        binding.navLogCare.setOnClickListener{go("Care")}
        return binding.root
    }
    private fun week(capsules:Boolean){
        val r=row();val colors=listOf(yellow,sub,blue,lavender,pink,lavender,mint)
        listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEachIndexed{i,d->
            val cell=col().apply{gravity=Gravity.CENTER;if(capsules||i==3)background=shape(if(i==3) {if(capsules)lavender else elevated} else surface,18);pad(this,0,7);contentDescription="$d, September ${22+i}, sample schedule";setOnClickListener{if(i!=3)preview("This reference schedule shows Thursday, September 25. Your saved entries are available in Care History.")}}
            cell.addView(text(d,12f,if(i==3) {if(capsules)color("#392446") else ink} else sub,if(i==3)800 else 400).apply{gravity=Gravity.CENTER})
            if(capsules)cell.addView(text("${22+i}",19f,if(i==3)color("#292130") else ink,800).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,dp(30)))
            else{val circle=FrameLayout(a).apply{background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(if(i==3)primary else bg);setStroke(dp(3),colors[i])};if(i==3)addView(icon("Pets",17,color("#48305C")),FrameLayout.LayoutParams(dp(17),dp(17),Gravity.CENTER))};cell.addView(circle,LinearLayout.LayoutParams(dp(25),dp(25)).apply{topMargin=dp(8)})}
            r.addView(cell,LinearLayout.LayoutParams(0,dp(if(capsules)69 else 64),1f).apply{setMargins(dp(2),0,dp(2),0)})
        };add(r,if(capsules)72 else 66,18)
    }
    private val tasks=listOf(Triple("Breakfast","8:00 AM","1/2 cup kibble"),Triple("Vitamins","12:00 PM","1 tablet"),Triple("Playtime","5:00 PM","20 mins"),Triple("Dinner","7:00 PM","1/2 cup kibble"))
    private fun careRow(index:Int,timeline:Boolean=false):View{
        val (label,time,detail)=if(index==4)Triple("Vet Visit","10:00 AM","Paws Animal Clinic") else tasks[index]
        val kind=when(index){0,3->"Feeding";1->"Medication";2->"Playtime";else->"Vet Visit"}
        val r=row().apply{background=shape(surface,22);pad(this,16,10);setOnClickListener{if(index==4)go("Reminders") else go("Quick Log")};contentDescription="$label, $time, $detail, sample care task"}
        r.addView(icon(kind,40),LinearLayout.LayoutParams(dp(40),dp(42)).apply{rightMargin=dp(18)})
        val labels=col();labels.addView(text(label,16f,ink,800));labels.addView(text(if(timeline)"$time\n$detail" else "$time · $detail",13f,sub).apply{setLineSpacing(dp(1).toFloat(),1f)},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
        r.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
        val check=FrameLayout(a).apply{if(index!=4)background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(if(index==0)mint else Color.TRANSPARENT);if(index!=0)setStroke(dp(2),color("#776B92"))};if(index==0)addView(icon("Check",22,color("#173A32")),FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));if(index==4)addView(icon("Chevron",25),FrameLayout.LayoutParams(-1,-1))}
        r.addView(check,LinearLayout.LayoutParams(dp(28),dp(28)));return r
    }
    private fun home(){
        spacer(10);val r=row();r.addView(title("Good morning,\n$name!",32f),LinearLayout.LayoutParams(0,-2,1f));r.addView(portrait(76).apply{setOnClickListener{go("Pet Detail")}});add(r,90,8)
        add(text("Thu, Sep 25, 2026",15f,sub),25,15);week(false)
        val summary=row().apply{background=shape(surface);pad(this,18,12);setOnClickListener{go("Pet Timeline")}}
        val labels=col();labels.addView(title("Today's Care",20f));labels.addView(text("3 of 5 done",16f,primary,700),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)});summary.addView(labels,LinearLayout.LayoutParams(0,-2,1f));summary.addView(action("Arrow","Open care timeline"){go("Pet Timeline")}.apply{background=shape(elevated,30)})
        add(summary,76,7);tasks.indices.forEach{add(careRow(it),72,5)};flexible(12)
    }
    private fun petOptions(){AlertDialog.Builder(a).setTitle(name).setItems(arrayOf("Switch pet","Edit pet profile","Pet Passport","Sitter Mode","Care History","Care Circle","Pet Records")){_,i->when(i){0->choose();1->go("Edit Pet Profile");2->go("Pet Passport");3->go("Sitter Mode");4->go("Care History");5->go("Care Circle");6->go("Records")}}.show()}
    private fun overviewHeader(){
        val header=FrameLayout(a)
        header.addView(action("Back","Back"){go("Home")},FrameLayout.LayoutParams(dp(48),dp(48),Gravity.START or Gravity.CENTER_VERTICAL))
        val nameAndChevron=row().apply{gravity=Gravity.CENTER_VERTICAL;contentDescription="Choose pet";setOnClickListener{choose()}}
        nameAndChevron.addView(title(name).apply{gravity=Gravity.CENTER_VERTICAL;maxLines=1},LinearLayout.LayoutParams(-2,dp(56)))
        val chevron=object:View(a){
            private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=primary;style=Paint.Style.STROKE;strokeWidth=dp(2).toFloat();strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
            override fun onDraw(canvas:Canvas){super.onDraw(canvas);val centerY=height/2f-dp(1);val half=dp(5).toFloat();val drop=dp(4).toFloat();val path=Path().apply{moveTo(width/2f-half,centerY-drop/2f);lineTo(width/2f,centerY+drop/2f);lineTo(width/2f+half,centerY-drop/2f)};canvas.drawPath(path,paint)}
        }.apply{importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO}
        nameAndChevron.addView(chevron,LinearLayout.LayoutParams(dp(18),dp(18)).apply{leftMargin=dp(7)})
        header.addView(nameAndChevron,FrameLayout.LayoutParams(-2,dp(56),Gravity.CENTER))
        header.addView(action("More","Pet options"){petOptions()},FrameLayout.LayoutParams(dp(48),dp(48),Gravity.END or Gravity.CENTER_VERTICAL))
        add(header,64,12)
    }
    private fun overview(){
        overviewHeader();add(PetRings(a,photo,!night).apply{contentDescription="Sample wellness rings: energy good, food 3 of 4, water 2 of 3, mood great"},330,12)
        val metrics=row();listOf(Triple("Energy","Good",mint),Triple("Food","3/4",primary),Triple("Water","2/3",blue),Triple("Mood","Great",mint)).forEach{(s,value,c)->
            val cell=col().apply{gravity=Gravity.CENTER}
            cell.addView(icon(s,34),LinearLayout.LayoutParams(dp(34),dp(34)))
            cell.addView(text(s,12f,ink,600).apply{gravity=Gravity.CENTER;textAlignment=View.TEXT_ALIGNMENT_CENTER},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})
            cell.addView(text(value,18f,if(night)c else if(c==mint)color("#28755F") else if(c==blue)color("#396B9C") else primary,800).apply{gravity=Gravity.CENTER;textAlignment=View.TEXT_ALIGNMENT_CENTER},LinearLayout.LayoutParams(-1,-2))
            metrics.addView(cell,LinearLayout.LayoutParams(0,dp(90),1f))
        };add(metrics,92,18)
        spacer(10)
        val card=row().apply{background=shape(surface,22,true);pad(this,20,18)};val copy=col();copy.addView(title("Insights",20f));copy.addView(text("$name seems more playful\ntoday! +20% playtime\ncompared to last week.",15f,sub),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
        card.addView(copy,LinearLayout.LayoutParams(0,-2,1f));val side=col();side.addView(icon("Wave",50).apply{background=shape(elevated,30)});side.addView(action("Arrow","View care history"){go("Care History")});card.addView(side);card.contentDescription="Illustrative insights preview. No health analysis is connected.";card.setOnClickListener{preview("These wellness rings and insights are reference examples. Open Care History to view your saved entries.")};add(card,150,0)
    }
    private fun logCare(){
        spacer(4);content.background=shape(surface,28,true)
        val handle=View(a).apply{background=shape(sub,4)};content.addView(handle,LinearLayout.LayoutParams(dp(40),dp(4)).apply{gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=dp(4)})
        val h=row();h.addView(action("Close","Close log care"){go("Home")});h.addView(title("Log Care",27f).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(52),1f));h.addView(View(a),LinearLayout.LayoutParams(dp(48),1));add(h,56,12)
        val circleSize=minOf(116,((a.resources.displayMetrics.widthPixels/a.resources.displayMetrics.density-44-48)/2).toInt())
        val categories=listOf("Feeding","Medication","Water","Playtime","Vet Visit","Grooming")
        categories.chunked(2).forEach{pair->val r=row().apply{gravity=Gravity.CENTER};pair.forEach{s->val cell=col().apply{gravity=Gravity.CENTER;background=shape(if(night)color("#201E29") else elevated,80,true);contentDescription="Log $s";setOnClickListener{go("Quick Log:$s")}}
            cell.addView(icon(s,43));cell.addView(text(s,14f,ink,800),LinearLayout.LayoutParams(-2,-2).apply{topMargin=dp(9)})
            r.addView(cell,LinearLayout.LayoutParams(dp(circleSize),dp(circleSize)).apply{setMargins(dp(12),0,dp(12),0)})};add(r,circleSize,12)}
        spacer(6)
        val voice=col().apply{gravity=Gravity.CENTER;background=shape(elevated,30,true);pad(this,14,20)};voice.addView(title("Quick Log with Voice",21f));voice.addView(text("Just say what happened.",14f,sub),LinearLayout.LayoutParams(-2,-2).apply{topMargin=dp(5)})
        val wave=row().apply{gravity=Gravity.CENTER};wave.addView(icon("Wave",49));val mic=FrameLayout(a).apply{background=shape(lavender,60);contentDescription="Open voice logging";setOnClickListener{go("Voice Log")};addView(icon("Mic",53,color("#443057")),FrameLayout.LayoutParams(dp(53),dp(53),Gravity.CENTER))};wave.addView(mic,LinearLayout.LayoutParams(dp(100),dp(100)).apply{setMargins(dp(20),0,dp(20),0)});wave.addView(icon("Wave",49));voice.addView(wave,LinearLayout.LayoutParams(-1,dp(100)).apply{topMargin=dp(18)});add(voice,230)
    }
    private fun timeline(){
        header("Care Timeline");week(true);add(text("Today, Sep 25, 2026",17f,primary,600,true),28,8)
        fun entry(i:Int,c:Int,last:Boolean=false){val r=row();val rail=object:View(a){val p=Paint(Paint.ANTI_ALIAS_FLAG);override fun onDraw(canvas:Canvas){p.color=c;p.alpha=130;p.strokeWidth=dp(2).toFloat();canvas.drawLine(width/2f,0f,width/2f,height.toFloat(),p);p.alpha=255;canvas.drawCircle(width/2f,height*.38f,dp(9).toFloat(),p)}}
            r.addView(rail,LinearLayout.LayoutParams(dp(25),-1));r.addView(careRow(i,true),LinearLayout.LayoutParams(0,dp(84),1f).apply{leftMargin=dp(8);bottomMargin=dp(if(last)0 else 7)});add(r,if(last)84 else 91)}
        entry(0,mint);entry(1,yellow);entry(2,pink);entry(3,yellow,true);spacer(22);add(text("Tomorrow, Sep 26, 2026",17f,primary,600,true),28,8);entry(4,lavender,true);flexible(8)
    }
    private fun sitter(){
        header("• Sitter Mode");add(ImageView(a).apply{setImageResource(R.drawable.sitter_friends);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription="Illustration of a cat and dog together"},120,12)
        add(text("Share a link with your sitter so they can\nsee exactly what to feed and when —\nno app, no login.",15f,sub,600).apply{gravity=Gravity.CENTER},68,18)
        val box=col().apply{background=shape(surface,22,true);pad(this,18,14)};val r=row();r.addView(icon("Link",30),LinearLayout.LayoutParams(dp(38),dp(38)));r.addView(title("Sitter Link",21f),LinearLayout.LayoutParams(0,-2,1f))
        val toggle=Switch(a).apply{isChecked=true;showText=false;switchMinWidth=dp(52);thumbDrawable=shape(Color.WHITE,20).apply{setSize(dp(28),dp(28))};trackDrawable=shape(lavender,20).apply{setSize(dp(52),dp(30))};thumbTintList=android.content.res.ColorStateList.valueOf(Color.WHITE);trackTintList=android.content.res.ColorStateList.valueOf(lavender);contentDescription="Sitter link preview switch";setOnCheckedChangeListener{_,_->preview("Sitter sharing is a frontend preview. No public link or access permissions have been created.")}};r.addView(toggle);box.addView(r)
        val url=row().apply{background=shape(bg,16,true);pad(this,14,8)};url.addView(text("purrfolio.app/sit/mochi",15f,sub,700),LinearLayout.LayoutParams(0,-2,1f));url.addView(action("More","Sitter link details"){preview("Example URL only. Public sitter links are not connected.")});box.addView(url,LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(8)})
        box.addView(text("Preview link · sharing not connected",13f,sub),LinearLayout.LayoutParams(-1,dp(32)).apply{topMargin=dp(10)});box.addView(button("Copy Link"){preview("This is a sample link. Copying a working sitter invitation requires account and access-control integration.")},LinearLayout.LayoutParams(-1,dp(50)));add(box,234,10)
        val list=col().apply{background=shape(surface,22);pad(this,0,2)}
        listOf(Triple("Eye","What they can see","Care Circle Access"),Triple("Feeding","Feeding instructions","Edit Pet Profile"),Triple("Medication","Medications","Pet Passport"),Triple("Notes","Important notes","Edit Pet Profile")).forEachIndexed{i,(icon,label,target)->list.addView(infoRow(icon,label,null){go(target)},LinearLayout.LayoutParams(-1,dp(52)));if(i<3)list.addView(View(a).apply{setBackgroundColor(outline)},LinearLayout.LayoutParams(-1,dp(1)))};add(list);flexible(6)
    }
    private fun infoRow(kind:String,label:String,value:String?,callback:()->Unit):View{
        val r=row().apply{pad(this,18,6);setOnClickListener{callback()};isFocusable=true}
        r.addView(icon(kind,34),LinearLayout.LayoutParams(dp(37),dp(38)).apply{rightMargin=dp(16)});val labels=col();labels.addView(text(label,16f,ink,800));if(value!=null)labels.addView(text(value,14f,sub),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(3)})
        r.addView(labels,LinearLayout.LayoutParams(0,-2,1f));r.addView(icon("Chevron",22,sub));return r
    }
    private fun petSummary():String {
        val species=pet.optString("species","Pet")
        val age=try { val born=java.util.Calendar.getInstance().apply{time=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).parse(pet.optString("birthday"))!!};val now=java.util.Calendar.getInstance();now.get(java.util.Calendar.YEAR)-born.get(java.util.Calendar.YEAR)-if(now.get(java.util.Calendar.DAY_OF_YEAR)<born.get(java.util.Calendar.DAY_OF_YEAR))1 else 0 } catch(_:Exception){-1}
        return if(age>=0) "$species · $age years old" else species
    }
    private fun passport(){
        header("Pet Passport","Share");val identity=row().apply{background=shape(surface,22,true);pad(this,15,12)};identity.addView(portrait(76),LinearLayout.LayoutParams(dp(76),dp(76)).apply{rightMargin=dp(16)})
        val labels=col();labels.addView(title(name,29f));labels.addView(text(petSummary(),14f,sub,600),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(7)});identity.addView(labels,LinearLayout.LayoutParams(0,-2,1f));identity.addView(action("Edit","Edit pet profile"){go("Edit Pet Profile")});add(identity,108,12)
        val list=col().apply{background=shape(surface,22)}
        val rows=listOf(Triple("Vaccinations","Vaccinations","No records added"),Triple("Medication","Medications","Not provided"),Triple("Allergies","Allergies","Not provided"),Triple("Vet Visit","Veterinarian","Not provided"),Triple("Emergency","Emergency Contact","Not provided"),Triple("Microchip","Microchip","Not provided"),Triple("Weight","Weight","Not provided"))
        rows.forEachIndexed{i,(kind,label,value)->list.addView(infoRow(kind,label,value){go(if(i==0)"Vaccination Card" else "Edit Pet Profile")},LinearLayout.LayoutParams(-1,dp(66)));if(i<6)list.addView(View(a).apply{setBackgroundColor(outline)},LinearLayout.LayoutParams(-1,dp(1)))};add(list,-2,12)
        val share=row().apply{gravity=Gravity.CENTER;background=shape(lavender,36);setOnClickListener{go("Pet ID QR")}};share.addView(icon("QR",28,color("#201528")),LinearLayout.LayoutParams(dp(28),dp(28)).apply{rightMargin=dp(12)});share.addView(text("Share Passport",18f,color("#201528"),700));add(share,58);flexible(0)
    }
}
