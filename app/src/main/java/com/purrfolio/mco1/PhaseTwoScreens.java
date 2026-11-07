package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.purrfolio.mco1.databinding.ScreenPhaseTwoBinding;
import com.purrfolio.mco1.databinding.PhaseTwoInfoRowBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.json.JSONObject;

/** Native secondary screens and forms. */
public class PhaseTwoScreens {
    public static final Set<String> destinations=new HashSet<>(Arrays.asList(
            "Create Account","Log In","Reset Password","Profile","Personal Information",
            "Account Settings","Notifications","Add Your Pet","Edit Pet Profile","Pets","Care Circle",
            "Voice Log","Sound Journal","Records","Paperless Inbox","Vaccination Card","Browse Records",
            "Reminders","Calendar","Pet ID QR","Emergency","Offline & Sync","Care Circle Access",
            "Activity Log","QR Privacy","More"));
    private static final Set<String> CARE_KINDS=new HashSet<>(Arrays.asList("Feeding","Medication","Water",
            "Playtime","Vet Visit","Grooming","Home","Pets","Care","Notes","More","Plus","Back",
            "Close","Arrow","Chevron","Check","Energy","Food","Mood","Mic","Wave","Link","Eye",
            "Share","QR","Microchip","Edit","Heart","Weight","Emergency","Google","Apple","Facebook"));
    protected final Activity a;
    protected final JSONObject pet;
    protected final List<JSONObject> pets,logs,reminders;
    protected final Consumer<String> go;
    protected final Runnable choose,recognizeVoice,toggleSoundRecording,addReminder,login,goBack;
    protected final Consumer<Integer> selectPet;
    protected final BiConsumer<Map<String,String>,Boolean> savePet;
    protected final BiConsumer<String,String> saveLog;
    protected final Consumer<String> captureVoice;
    protected final Supplier<List<JSONObject>> soundRows;
    protected final Consumer<JSONObject> playRecording,toggleReminder;
    protected final Consumer<Boolean> onPetPhoto;
    protected final String pendingPetPhotoUri;
    protected final SharedPreferences prefs;
    protected final boolean night;
    protected final int bg,surface,elevated,primary,lavender,ink,sub,outline,pink,mint,coral,yellow,blue;
    protected LinearLayout body;
    protected String activeScreen="";

    public PhaseTwoScreens(Activity a,JSONObject pet,List<JSONObject> pets,List<JSONObject> logs,
            List<JSONObject> reminders,Consumer<String> go,Runnable choose,
            Consumer<Integer> selectPet,BiConsumer<Map<String,String>,Boolean> savePet,
            BiConsumer<String,String> saveLog,Runnable recognizeVoice,
            Consumer<String> captureVoice,Runnable toggleSoundRecording,
            Supplier<List<JSONObject>> soundRows,Consumer<JSONObject> playRecording,
            Runnable addReminder,Consumer<JSONObject> toggleReminder,Runnable login,
            Runnable goBack,String pendingPetPhotoUri,Consumer<Boolean> onPetPhoto){
        this.a=a;this.pet=pet;this.pets=pets;this.logs=logs;this.reminders=reminders;
        this.go=go;this.choose=choose;this.selectPet=selectPet;this.savePet=savePet;this.saveLog=saveLog;
        this.recognizeVoice=recognizeVoice;this.captureVoice=captureVoice;
        this.toggleSoundRecording=toggleSoundRecording;this.soundRows=soundRows;
        this.playRecording=playRecording;this.addReminder=addReminder;this.toggleReminder=toggleReminder;
        this.login=login;this.goBack=goBack;this.pendingPetPhotoUri=pendingPetPhotoUri;this.onPetPhoto=onPetPhoto;
        prefs=a.getSharedPreferences("purrfolio_demo",Activity.MODE_PRIVATE);
        night=(a.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        bg=color(night?"#101014":"#FCF9FF");surface=color(night?"#1C1B26":"#F3EDF8");
        elevated=color(night?"#24222F":"#EDE2F5");primary=color(night?"#D8B4F8":"#8051A8");
        lavender=color("#D8B4F8");ink=color(night?"#F8F2FF":"#292130");
        sub=color(night?"#BDB3CD":"#72647D");outline=color(night?"#373243":"#DED1E8");
        pink=color("#F3B6D7");mint=color("#91E5C8");coral=color(night?"#FF8298":"#B32646");
        yellow=color("#FFD18C");blue=color("#9BCBFF");
    }
    protected String name(){return pet.optString("name","Mochi");}
    protected int color(String hex){return Color.parseColor(hex);}
    protected int dp(int value){return (int)(value*a.getResources().getDisplayMetrics().density+.5f);}
    protected LinearLayout col(){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);return v;}
    protected LinearLayout row(){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    protected void pad(View v,int l,int t,int r,int b){v.setPadding(dp(l),dp(t),dp(r),dp(b));}
    protected void pad(View v,int l,int t){pad(v,l,t,l,t);}
    protected GradientDrawable shape(int c,int r,boolean border){GradientDrawable d=new GradientDrawable();
        d.setColor(c);d.setCornerRadius(dp(r));if(border)d.setStroke(dp(1),outline);return d;}
    protected GradientDrawable shape(int c,int r){return shape(c,r,true);}
    protected GradientDrawable shape(){return shape(surface,20,true);}
    protected TextView text(String value,float size,int color,int weight,boolean display){TextView t=new TextView(a);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);
        BrandType.apply(t,display,weight);t.setLineSpacing(dp(2),1f);return t;}
    protected TextView text(String value,float size,int color,int weight){return text(value,size,color,weight,false);}
    protected TextView text(String value,float size,int color){return text(value,size,color,400,false);}
    protected TextView heading(String value,float size){return text(value,size,primary,600,true);}
    protected TextView heading(String value){return heading(value,30);}
    protected TextView section(String value){return text(value,19,primary,600,true);}
    protected View icon(String kind,int size,int tint){View view=CARE_KINDS.contains(kind)?
            new CareArtwork(a,kind,tint):new DetailIcon(a,kind,tint);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return view;}
    protected View icon(String kind,int size){return icon(kind,size,primary);}
    protected void add(View v,int height,int gap){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));p.bottomMargin=dp(gap);body.addView(v,p);}
    protected void add(View v,int height){add(v,height,0);}
    protected void add(View v){add(v,-2,0);}
    protected void gap(int height){add(new View(a),height);}
    protected void push(int minimum){body.addView(new View(a),new LinearLayout.LayoutParams(1,dp(minimum),1));}
    protected LinearLayout steps(int count){LinearLayout r=row();r.setGravity(Gravity.CENTER);
        for(int i=0;i<count;i++){if(i>0){View divider=new View(a);divider.setBackgroundColor(outline);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(22),dp(2));p.setMargins(dp(4),0,dp(4),0);r.addView(divider,p);}
            View dot=new View(a);dot.setBackground(shape(i==0?lavender:bg,12,true));
            r.addView(dot,new LinearLayout.LayoutParams(dp(i==0?16:12),dp(i==0?16:12)));}
        r.setContentDescription("Step 1 of "+count);return r;}
    protected TextView center(String value,float size,int color,int weight){TextView t=text(value,size,color,weight);t.setGravity(Gravity.CENTER);return t;}
    protected TextView center(String value){return center(value,14,sub,400);}
    protected FrameLayout action(String kind,String label,Runnable callback){FrameLayout frame=new FrameLayout(a);
        frame.setContentDescription(label);frame.setFocusable(true);frame.setOnClickListener(v->callback.run());
        int tint=kind.equals("Check")||kind.equals("Camera")?color("#201528"):kind.equals("Warning")?coral:primary;
        frame.addView(icon(kind,26,tint),new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));
        frame.setMinimumWidth(dp(48));frame.setMinimumHeight(dp(48));return frame;}
    protected TextView button(String label,boolean secondary,int tint,Runnable callback){TextView t=text(label,15,secondary?primary:color("#201528"),700);
        t.setGravity(Gravity.CENTER);t.setMinHeight(dp(52));t.setBackground(shape(secondary?elevated:tint,30,secondary));
        t.setFocusable(true);t.setOnClickListener(v->callback.run());return t;}
    protected TextView button(String label,Runnable callback){return button(label,false,lavender,callback);}
    protected TextView button(String label,boolean secondary,Runnable callback){return button(label,secondary,lavender,callback);}
    protected void preview(String message){new AlertDialog.Builder(a).setTitle("Frontend preview").setMessage(message).setPositiveButton("OK",null).show();}
    protected void notice(String message){Toast.makeText(a,message,Toast.LENGTH_LONG).show();}
    protected void demoLabel(String label){TextView t=text(label,11,sub,500);t.setGravity(Gravity.END);add(t,18,3);}
    protected void demoLabel(){demoLabel("Sample content");}
    protected void header(String title,String subtitle,String back,String extra,Runnable onExtra,float size,boolean left){
        FrameLayout panel=new FrameLayout(a);int height=title.length()>23?92:68;
        if(!left)panel.addView(action("Back","Back",()->goBack.run()),new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.TOP|Gravity.START));
        TextView heading=heading(title,size);if(title.equals("Emergency Mode"))heading.setTextColor(coral);
        heading.setGravity(left?Gravity.START|Gravity.CENTER_VERTICAL:Gravity.CENTER);heading.setMaxLines(2);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,dp(height-16));hp.topMargin=dp(16);if(left)hp.rightMargin=dp(50);panel.addView(heading,hp);
        if(extra!=null)panel.addView(action(extra,extra.equals("Plus")?"Add":extra,onExtra==null?()->{}:onExtra),
                new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.END));
        add(panel,height,subtitle==null?10:0);if(subtitle!=null)add(center(subtitle),-2,18);
    }
    protected void header(String title,String subtitle,String back){header(title,subtitle,back,null,null,30,false);}
    protected void header(String title){header(title,null,"More",null,null,30,false);}
    protected ImageView art(int resource,int height){ImageView view=new ImageView(a);
        view.setImageResource(resource);view.setScaleType(ImageView.ScaleType.FIT_CENTER);view.setAdjustViewBounds(true);
        view.setContentDescription("PURRFOLIO illustration from the supplied wireframe");
        view.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(height)));return view;}
    protected FrameLayout portrait(int resource,int size,int ring){FrameLayout frame=new FrameLayout(a);
        frame.setBackground(shape(ring,size,false));pad(frame,3,3);
        ImageView image=new ImageView(a);image.setImageResource(resource);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(shape(surface,size,false));image.setClipToOutline(true);image.setContentDescription("Pet portrait");
        frame.addView(image,new FrameLayout.LayoutParams(-1,-1));frame.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return frame;}
    protected FrameLayout portrait(int resource,int size){return portrait(resource,size,primary);}
    protected FrameLayout portrait(int size){return portrait(R.drawable.pdf_mochi,size,primary);}
    protected View portraitHero(int size,boolean decorated,boolean editable){FrameLayout frame=new FrameLayout(a);
        frame.addView(portrait(size),new FrameLayout.LayoutParams(dp(size),dp(size),Gravity.CENTER));
        if(editable){FrameLayout camera=action("Camera","Change photo",()->preview("Photo upload is not connected. The supplied image is shown for this frontend preview."));
            camera.setBackground(shape(lavender,30,false));camera.setTranslationX(dp((size-35)/2));camera.setTranslationY(dp((size-30)/2));
            frame.addView(camera,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.CENTER));}
        if(decorated){View heart=icon("Heart",26,pink);heart.setTranslationX(-dp((size+65)/2));heart.setTranslationY(-dp(15));
            frame.addView(heart,new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));
            View sparkle=icon("Sparkle",24);sparkle.setTranslationX(dp((size+65)/2));sparkle.setTranslationY(dp(20));
            frame.addView(sparkle,new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));}
        return frame;
    }
    protected View petPhotoEditor(String uri,int size,Runnable manage){FrameLayout frame=new FrameLayout(a);FrameLayout image=portrait(size);
        if(uri!=null&&!uri.isEmpty())try{((ImageView)image.getChildAt(0)).setImageURI(Uri.parse(uri));}catch(Exception ignored){}
        frame.addView(image,new FrameLayout.LayoutParams(dp(size),dp(size),Gravity.CENTER));
        FrameLayout camera=action("Camera",uri==null||uri.isEmpty()?"Add pet photo":"Manage pet photo",manage);
        camera.setBackground(shape(lavender,30,false));camera.setTranslationX(dp((size-35)/2));camera.setTranslationY(dp((size-30)/2));
        frame.addView(camera,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.CENTER));
        frame.setContentDescription(uri==null||uri.isEmpty()?"Pet portrait. Add a photo from this device.":
                "Saved pet photo. Tap to change or remove it.");return frame;}
    protected EditText field(String label,String value,String kind,String hint,boolean password,boolean multiline,
            List<String> dropdown,LinearLayout parent){
        LinearLayout box=row();box.setBackground(shape());pad(box,15,10);
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(30),dp(32));iconParams.rightMargin=dp(14);
        box.addView(icon(kind,26),iconParams);
        LinearLayout labels=col();labels.addView(text(label,12,sub));
        EditText entry=new EditText(a);entry.setText(value);entry.setHint(hint);entry.setTextSize(15);entry.setTextColor(ink);
        entry.setHintTextColor(sub);BrandType.apply(entry,false,500);entry.setBackground(null);entry.setPadding(0,dp(3),0,0);
        entry.setIncludeFontPadding(false);entry.setSingleLine(!multiline);entry.setContentDescription(label);
        int inputType=password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:
                label.contains("Email")?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
                label.contains("Phone")?InputType.TYPE_CLASS_PHONE:
                label.equals("Weight")?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL:
                multiline?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE:
                InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        entry.setInputType(inputType);
        if(multiline){entry.setMinLines(2);entry.setGravity(Gravity.TOP);}
        labels.addView(entry,new LinearLayout.LayoutParams(-1,-2));box.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        if(password){entry.setTransformationMethod(PasswordTransformationMethod.getInstance());
            box.addView(action("Eye","Show or hide password",()->{int start=entry.getSelectionStart();
                entry.setTransformationMethod(entry.getTransformationMethod()==null?PasswordTransformationMethod.getInstance():null);
                entry.setSelection(Math.max(start,0));}),new LinearLayout.LayoutParams(dp(40),dp(40)));}
        if(dropdown!=null){entry.setFocusable(false);Runnable pick=()->new AlertDialog.Builder(a).setTitle(label)
                .setItems(dropdown.toArray(new String[0]),(dialog,index)->entry.setText(dropdown.get(index))).show();
            box.setOnClickListener(v->pick.run());entry.setOnClickListener(v->pick.run());
            box.addView(action("Down","Choose "+label,pick),new LinearLayout.LayoutParams(dp(34),dp(40)));}
        if(label.contains("Birthday")||label.equals("Date of Birth")){entry.setFocusable(false);
            Runnable pick=()->{Calendar now=Calendar.getInstance();new DatePickerDialog(a,(view,year,month,day)->
                    entry.setText(String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day)),
                    now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();};
            entry.setOnClickListener(v->pick.run());box.setOnClickListener(v->pick.run());box.addView(icon("Down",22));}
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(multiline?102:70));params.bottomMargin=dp(8);
        (parent==null?body:parent).addView(box,params);return entry;
    }
    protected EditText field(String label,String value,String kind){return field(label,value,kind,"",false,false,null,null);}
    protected EditText field(String label,String value){return field(label,value,"Person","",false,false,null,null);}
    protected View infoRow(String kind,String label,String detail,String trailing,int tint,Runnable callback){
        PhaseTwoInfoRowBinding binding=PhaseTwoInfoRowBinding.inflate(a.getLayoutInflater());
        LinearLayout r=binding.infoRowRoot;r.setBackground(shape());pad(r,15,10);
        r.setFocusable(true);r.setOnClickListener(v->callback.run());
        binding.iconHost.addView(icon(kind,27,tint),new FrameLayout.LayoutParams(dp(27),dp(27),Gravity.CENTER));
        binding.title.setText(label);binding.title.setTextColor(tint==coral?coral:ink);
        BrandType.apply(binding.title,false,700);binding.title.setLineSpacing(dp(2),1f);
        if(detail!=null){binding.detail.setVisibility(View.VISIBLE);binding.detail.setText(detail);
            binding.detail.setTextColor(sub);BrandType.apply(binding.detail,false,400);
            binding.detail.setLineSpacing(dp(2),1f);}
        if(trailing!=null&&!trailing.isEmpty())binding.trailingHost.addView(icon(trailing,23),
                new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));
        else binding.trailingHost.setVisibility(View.GONE);
        return r;
    }
    protected View infoRow(String kind,String label,Runnable callback){return infoRow(kind,label,null,"Chevron",primary,callback);}
    protected View infoRow(String kind,String label,String detail,Runnable callback){return infoRow(kind,label,detail,"Chevron",primary,callback);}
    protected View toggleRow(String kind,String label,String description,boolean defaultValue,String key,boolean persist){
        LinearLayout r=row();r.setBackground(shape());pad(r,14,8);
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(30),dp(32));iconParams.rightMargin=dp(12);r.addView(icon(kind,27),iconParams);
        LinearLayout labels=col();labels.addView(text(label,14,ink,700));
        if(description!=null){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(5);labels.addView(text(description,11,sub),p);}
        r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(a);toggle.setContentDescription(label);toggle.setChecked(prefs.getBoolean(key,defaultValue));
        toggle.setShowText(false);toggle.setSplitTrack(false);toggle.setSwitchMinWidth(dp(46));
        GradientDrawable thumb=shape(Color.WHITE,20,false);thumb.setSize(dp(24),dp(24));toggle.setThumbDrawable(thumb);
        GradientDrawable track=shape(lavender,20,false);track.setSize(dp(46),dp(27));toggle.setTrackDrawable(track);
        toggle.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        toggle.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{lavender,outline}));
        toggle.setOnCheckedChangeListener((button,checked)->{if(persist)prefs.edit().putBoolean(key,checked).apply();});
        r.addView(toggle,new LinearLayout.LayoutParams(dp(50),dp(44)));return r;
    }
    protected View toggleRow(String kind,String label,String description){return toggleRow(kind,label,description,true,"setting:"+label,true);}
    protected void sectionLabel(String label,int gap){add(section(label),-2,gap);}
    protected void sectionLabel(String label){sectionLabel(label,12);}
    protected void social(){gap(16);LinearLayout divider=row();View left=new View(a),right=new View(a);
        left.setBackgroundColor(outline);right.setBackgroundColor(outline);
        divider.addView(left,new LinearLayout.LayoutParams(0,dp(1),1));divider.addView(center("or continue with",13,sub,400),new LinearLayout.LayoutParams(dp(136),dp(26)));
        divider.addView(right,new LinearLayout.LayoutParams(0,dp(1),1));add(divider,30,8);
        LinearLayout r=row();r.setGravity(Gravity.CENTER);
        for(String provider:new String[]{"Apple","Google","Facebook"}){int size=provider.equals("Apple")?30:34;
            FrameLayout cell=new FrameLayout(a);cell.setBackground(shape(surface,40,true));
            cell.setContentDescription("Continue with "+provider);cell.setOnClickListener(v->preview(provider+" sign-in is not connected."));
            cell.addView(icon(provider,size,primary),new FrameLayout.LayoutParams(dp(size),dp(size),Gravity.CENTER));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(58),dp(58));p.setMargins(dp(12),0,dp(12),0);r.addView(cell,p);}
        add(r,62,12);
    }
    protected View chips(List<String> labels,String selected,java.util.function.Consumer<String> onSelect){LinearLayout r=row();
        for(String label:labels){TextView chip=text(label,12,label.equals(selected)?color("#24192E"):ink,700);
            chip.setGravity(Gravity.CENTER);chip.setBackground(shape(label.equals(selected)?lavender:surface,24,true));pad(chip,10,9);
            chip.setFocusable(true);chip.setOnClickListener(v->onSelect.accept(label));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(38),1);p.setMargins(dp(2),0,dp(2),0);r.addView(chip,p);}
        return r;
    }
    protected View chips(List<String> labels,java.util.function.Consumer<String> onSelect){return chips(labels,labels.get(0),onSelect);}
    protected void createAccount(){
        LinearLayout top=row();top.addView(action("Back","Back",()->goBack.run()),new LinearLayout.LayoutParams(dp(44),dp(44)));
        top.addView(steps(3),new LinearLayout.LayoutParams(0,dp(44),1));top.addView(new View(a),new LinearLayout.LayoutParams(dp(44),1));add(top,48,12);
        TextView title=heading("Create Account",32);title.setGravity(Gravity.CENTER);add(title,48,4);
        add(center("Let's get to know you!",17,ink,600),28,24);
        field("Full Name","Gracezel Castrence");field("Email Address","youremail@dlsu.edu.ph","Mail");
        EditText password=field("Password","","Lock","",true,false,null,null);
        LinearLayout requirements=col();requirements.setBackground(shape());pad(requirements,18,12);
        String[] checks={"At least 8 characters","1 letter and 1 number","1 special character"};
        List<LinearLayout> rows=new ArrayList<>();
        for(String label:checks){LinearLayout r=row();LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(26),dp(26));ip.rightMargin=dp(12);
            r.addView(icon("Check",22),ip);r.addView(text(label,14,sub));requirements.addView(r,new LinearLayout.LayoutParams(-1,dp(32)));rows.add(r);}
        password.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void afterTextChanged(Editable value){}
            public void onTextChanged(CharSequence value,int start,int before,int count){String v=value.toString();
                boolean letter=false,digit=false,special=false;for(char c:v.toCharArray()){if(Character.isLetter(c))letter=true;
                    else if(Character.isDigit(c))digit=true;else special=true;}
                boolean[] ok={v.length()>=8,letter&&digit,special};for(int i=0;i<3;i++)rows.get(i).setAlpha(ok[i]?1f:.45f);}});
        add(requirements,124,20);add(button("Continue",()->go.accept("Add Your Pet")),52);social();push(16);
        TextView login=center("Already have an account?  Log In",14,sub,400);login.setOnClickListener(v->go.accept("Log In"));
        login.setMinHeight(dp(48));add(login,48);
    }
    protected void logIn(){header("Welcome back!","Good to see you again","Welcome",null,null,32,false);
        add(portraitHero(145,true,false),170,14);field("Email Address","youremail@dlsu.edu.ph","Person");
        field("Password","","Lock","",true,false,null,null);
        TextView forgot=text("Forgot password?",14,primary,700);forgot.setGravity(Gravity.END);forgot.setOnClickListener(v->go.accept("Reset Password"));
        forgot.setMinHeight(dp(40));add(forgot,40,8);add(button("Log In",()->login.run()),52);social();
        LinearLayout card=row();card.setBackground(shape());pad(card,12,12);card.setOnClickListener(v->go.accept("Create Account"));
        FrameLayout avatar=new FrameLayout(a);avatar.setContentDescription("Create a new account");
        avatar.addView(icon("Person",48,primary),new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER));
        card.addView(avatar,new LinearLayout.LayoutParams(dp(96),dp(72)));
        LinearLayout copy=col();copy.addView(section("New here?"));LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(-1,-2);p1.topMargin=dp(7);
        copy.addView(text("Create an account to start\nyour pet care journey.",13,sub),p1);
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(-1,-2);p2.topMargin=dp(12);
        copy.addView(text("Create Account  →",15,primary,700),p2);card.addView(copy,new LinearLayout.LayoutParams(0,-2,1));add(card,136);
    }
    protected void resetPassword(){header("Reset Password",null,"Log In",null,null,31,false);
        add(center("We'll send you a link to reset\nyour password."),44,12);
        field("Email Address","youremail@dlsu.edu.ph","Person");
        add(button("Send Reset Link",()->preview("Password recovery requires the existing authentication integration. No reset email has been sent.")),52,26);
        LinearLayout card=col();card.setBackground(shape());pad(card,18,18);card.addView(section("Didn't receive the email?"));
        for(String message:new String[]{"Check your spam folder","Make sure you entered the\ncorrect email","Try again in a few minutes"}){
            LinearLayout r=row();LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(32),dp(32));ip.rightMargin=dp(12);
            r.addView(icon("Check",25),ip);r.addView(text(message,14,sub));card.addView(r,new LinearLayout.LayoutParams(-1,dp(54)));}add(card);
    }
    protected void profile(){header("Profile",null,"More","Settings",()->go.accept("Account Settings"),30,true);
        add(portraitHero(130,true,true),150,8);
        add(center(prefs.getString("profile:name","Gracezel Castrence"),20,ink,700),28,3);
        add(center(prefs.getString("profile:email","youremail@dlsu.edu.ph"),13,sub,400),24,16);
        View complete=infoRow("Progress","Account Complete","4 of 5 completed",()->go.accept("Personal Information"));
        complete.setBackground(shape(elevated,20,true));add(complete,72,12);
        String[][] rows={{"Person","Personal Information","Personal Information"},{"Settings","Account Settings","Account Settings"},
                {"Bell","Notifications","Notifications"},{"Lock","Privacy & Security","QR Privacy"},
                {"Link","Linked Accounts","Account Settings"}};
        for(String[] item:rows)add(infoRow(item[0],item[1],()->go.accept(item[2])),56,5);
        gap(8);add(button("Log Out",()->go.accept("Welcome")),52);push(6);
    }
    protected void personalInformation(){header("Personal Information",null,"Profile");add(portraitHero(125,false,true),155,18);
        Map<String,EditText> fields=new LinkedHashMap<>();
        fields.put("name",field("Full Name",prefs.getString("profile:name","Gracezel Castrence")));
        fields.put("email",field("Email Address",prefs.getString("profile:email","youremail@dlsu.edu.ph"),"Mail"));
        fields.put("phone",field("Phone Number",prefs.getString("profile:phone","+63 912 345 6789"),"Phone"));
        fields.put("birthday",field("Date of Birth",prefs.getString("profile:birthday","2004-09-25"),"Care"));
        fields.put("country",field("Country / Region",prefs.getString("profile:country","Philippines"),"Location","",false,false,
                Arrays.asList("Philippines","Singapore","Malaysia","Other"),null));
        gap(20);add(button("Save Changes",()->{SharedPreferences.Editor editor=prefs.edit();
            for(Map.Entry<String,EditText> e:fields.entrySet())editor.putString("profile:"+e.getKey(),e.getValue().getText().toString());
            editor.apply();notice("Profile saved on this device.");go.accept("Profile");}),52);
    }
    protected void accountSettings(){header("Account Settings",null,"Profile");gap(12);
        add(infoRow("Lock","Change Password",()->go.accept("Reset Password")),66,8);
        add(infoRow("Moon","App Theme",AppTheme.label(a),()->go.accept("App Theme")),72,8);
        add(infoRow("Globe","Language","English",()->preview("English is the available language in this frontend.")),72,8);
        add(infoRow("Database","Data & Storage",()->go.accept("Offline & Sync")),66,24);
        sectionLabel("Account");add(infoRow("Trash","Delete Account",null,"Chevron",coral,
                ()->preview("Account deletion requires authenticated account services. No account or local records have been deleted.")),66);
    }
    protected void notifications(){header("Notifications",null,"Profile");sectionLabel("Care Reminders");
        add(toggleRow("Bell","Care Reminders","Medication, feeding, playtime, etc."),70,22);
        sectionLabel("Updates");add(toggleRow("Megaphone","App Updates","New features and improvements"),70,4);
        add(toggleRow("Star","Tips & Insights","Pet care tips and recommendations"),70,22);
        sectionLabel("Shared Care");add(toggleRow("Pets","Care Circle Activity","When someone updates a record"),70,22);
        sectionLabel("Marketing");add(toggleRow("Mail","Product News","Occasional updates and offers",false,"setting:Product News",true),70,14);
        add(art(R.drawable.pdf_notification_cat_transparent,76),76,4);
        add(center("We'll only notify you\nabout what matters.",14,ink,500),44);
    }
    public View build(String screen){
        activeScreen=screen;
        WindowCompat.setDecorFitsSystemWindows(a.getWindow(),false);
        WindowCompat.getInsetsController(a.getWindow(),a.getWindow().getDecorView()).setAppearanceLightStatusBars(!night);
        WindowCompat.getInsetsController(a.getWindow(),a.getWindow().getDecorView()).setAppearanceLightNavigationBars(!night);
        ScreenPhaseTwoBinding binding=ScreenPhaseTwoBinding.inflate(a.getLayoutInflater());
        LinearLayout root=binding.phaseTwoRoot;root.setBackgroundColor(bg);body=binding.phaseTwoBody;
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|
                    WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});
        switch(screen){
            case "Create Account":createAccount();break;case "Log In":logIn();break;
            case "Reset Password":resetPassword();break;case "Profile":profile();break;
            case "Personal Information":personalInformation();break;
            case "Account Settings":accountSettings();break;case "Notifications":notifications();break;
            case "Add Your Pet":petForm(false);break;case "Edit Pet Profile":petForm(true);break;case "Pets":myPets();break;
            case "Care Circle":careCircle(false);break;case "Care Circle Access":careCircle(true);break;
            case "Voice Log":voiceLog();break;case "Sound Journal":soundJournal();break;
            case "Records":case "Paperless Inbox":records();break;case "Vaccination Card":vaccination();break;
            case "Browse Records":recordsSearch();break;case "Reminders":reminders();break;case "Calendar":calendar();break;
            case "Pet ID QR":petId();break;case "Emergency":emergency();break;case "Offline & Sync":offline();break;
            case "Activity Log":activityLog();break;case "QR Privacy":privacy();break;case "More":more();break;
            default:throw new IllegalArgumentException("Screen not migrated: "+screen);
        }
        if(Arrays.asList("Create Account","Log In","Reset Password","Personal Information","Add Your Pet","Edit Pet Profile").contains(screen))
            binding.phaseTwoDock.setVisibility(View.GONE);
        else binding.phaseTwoDock.addView(new ReferenceScreens(a,pet,go,choose).dock(screen));
        root.setFocusableInTouchMode(true);root.requestFocus();ViewCompat.requestApplyInsets(root);return root;
    }
    protected void petForm(boolean edit){
        if(edit)header("Edit Pet Profile",null,"Pet Detail");
        else{LinearLayout top=row();top.addView(action("Back","Back",()->goBack.run()),new LinearLayout.LayoutParams(dp(44),dp(44)));
            top.addView(steps(4),new LinearLayout.LayoutParams(0,dp(44),1));
            TextView skip=text("Skip",14,primary,700);skip.setOnClickListener(v->go.accept("Home"));
            top.addView(skip,new LinearLayout.LayoutParams(dp(42),dp(44)));add(top,48,8);
            TextView title=heading("Add Your Pet");title.setGravity(Gravity.CENTER);add(title,44,4);
            add(center("Let's create their profile!",17,primary,600),28,10);}
        String photoUri=edit?pet.optString("photoUri"):pendingPetPhotoUri;
        add(petPhotoEditor(photoUri,edit?125:140,()->onPetPhoto.accept(!edit)),edit?145:166,12);
        Map<String,EditText> fields=new LinkedHashMap<>();
        fields.put("name",field("Pet Name",edit?name():"","Pets","Mochi",false,false,null,null));
        fields.put("species",field("Species",edit?pet.optString("species"):"Cat","Cat","",false,false,
                Arrays.asList("Cat","Dog","Rabbit","Other"),null));
        fields.put("breed",field("Breed",edit?pet.optString("breed"):"","Pets","Mixed Breed",false,false,null,null));
        fields.put("birthday",field("Birthday",edit?pet.optString("birthday"):"","Care","YYYY-MM-DD",false,false,null,null));
        EditText weight;
        if(edit){LinearLayout r=row(),left=col(),right=col();
            weight=field("Weight",prefs.getString("pet-ui-weight:"+name(),""),"Weight","kg",false,false,null,left);
            fields.put("sex",field("Sex",pet.optString("sex"),"Sex","",false,false,
                    Arrays.asList("Female","Male","Unknown"),right));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.rightMargin=dp(4);r.addView(left,lp);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);rp.leftMargin=dp(4);r.addView(right,rp);add(r);
            fields.put("notes",field("About "+name(),pet.optString("notes"),"Notes","",false,true,null,null));
        }else weight=field("Weight","","Weight","kg (optional)",false,false,null,null);
        gap(10);EditText finalWeight=weight;
        add(button(edit?"Save Changes":"Continue",()->{
            if(fields.get("name").getText().toString().trim().isEmpty()){
                fields.get("name").setError("Pet name is required");return;}
            Map<String,String> values=new LinkedHashMap<>();
            for(Map.Entry<String,EditText> entry:fields.entrySet())values.put(entry.getKey(),entry.getValue().getText().toString().trim());
            if(photoUri!=null&&!photoUri.isEmpty())values.put("photoUri",photoUri);
            prefs.edit().putString("pet-ui-weight:"+values.get("name"),finalWeight.getText().toString()).apply();
            savePet.accept(values,!edit);
        }),52);
    }
    protected void myPets(){header("My Pets",null,"Home","Plus",()->go.accept("Add Your Pet"),30,true);
        for(int i=0;i<pets.size();i++){final int index=i;JSONObject p=pets.get(i);
            String species=p.optString("species");
            petCard(p.optString("name"),species+" · "+(i==0?"2":"3")+" years old",
                    species.equals("Dog")?R.drawable.pdf_bailey:R.drawable.pdf_mochi,
                    p.optString("name").equals(name()),()->selectPet.accept(index));}
        if(pets.size()<4){petCard("Luna","1 year old · Rabbit · Demo",R.drawable.pdf_luna,false,
                ()->preview("Luna is a sample pet from the wireframe. Use Add Another Pet to create your own profile."));
            petCard("Simba","4 years old · Cat · Demo",R.drawable.pdf_simba,false,
                    ()->preview("Simba is a sample pet from the wireframe."));}
        gap(14);add(button("+  Add Another Pet",()->go.accept("Add Your Pet")),52);
    }
    protected void petCard(String label,String detail,int image,boolean selected,Runnable callback){
        LinearLayout r=row();GradientDrawable background=shape(selected?elevated:surface,24,true);
        if(selected)background.setStroke(dp(2),lavender);r.setBackground(background);pad(r,13,12);
        r.setOnClickListener(v->callback.run());
        LinearLayout.LayoutParams photo=new LinearLayout.LayoutParams(dp(82),dp(82));photo.rightMargin=dp(16);
        r.addView(portrait(image,82),photo);
        LinearLayout labels=col();labels.addView(heading(label,23));LinearLayout.LayoutParams d=new LinearLayout.LayoutParams(-1,-2);d.topMargin=dp(7);
        labels.addView(text(detail,13,sub),d);r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        r.addView(icon("Chevron",24));add(r,110,12);
    }
    private static final String[][] CAREGIVERS={{"You","Owner"},{"Mom","Family"},{"Ate","Sitter"},{"Jam","Family"}};
    private static final int[] CAREGIVER_IMAGES={R.drawable.pdf_owner,R.drawable.pdf_mom,R.drawable.pdf_ate,R.drawable.pdf_jam};
    protected void avatar(FrameLayout frame,String label,String role,int image,int size,float fraction,int top){
        LinearLayout view=col();view.setGravity(Gravity.CENTER);
        view.setContentDescription(label+", "+role+", sample caregiver");
        view.setOnClickListener(v->go.accept(label.equals(name())?"Pet Detail":"Care Circle Access"));
        view.addView(portrait(image,size,label.equals("Ate")||label.equals("Mom")?mint:primary));
        view.addView(center(label,14,ink,700),new LinearLayout.LayoutParams(-1,dp(21)));
        if(!role.isEmpty())view.addView(center(role,12,sub,400),new LinearLayout.LayoutParams(-1,dp(19)));
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(size+14),-2);p.topMargin=dp(top);frame.addView(view,p);view.setTag(fraction);
    }
    protected View circleDiagram(boolean access){FrameLayout frame=new FrameLayout(a);
        if(access){avatar(frame,"You","Owner",R.drawable.pdf_owner,60,.13f,6);
            avatar(frame,name(),"",R.drawable.pdf_mochi,96,.5f,6);
            avatar(frame,"Mom","Family",R.drawable.pdf_mom,60,.87f,6);
            avatar(frame,"Ate","Sitter",R.drawable.pdf_ate,60,.13f,125);
            avatar(frame,"Jam","Family",R.drawable.pdf_jam,60,.5f,140);
            LinearLayout invite=col();invite.setGravity(Gravity.CENTER);invite.setTag(.87f);
            invite.setOnClickListener(v->preview("Caregiver invitations require authenticated shared accounts."));
            FrameLayout plus=action("Plus","Invite caregiver",()->preview("Invitations are a frontend preview."));
            plus.setBackground(shape(surface,40,true));invite.addView(plus,new LinearLayout.LayoutParams(dp(64),dp(64)));
            invite.addView(center("Invite",14,ink,700));FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(74),dp(96));ip.topMargin=dp(127);frame.addView(invite,ip);
        }else{avatar(frame,name(),"",R.drawable.pdf_mochi,120,.5f,40);
            avatar(frame,"You","Owner",R.drawable.pdf_owner,54,.12f,8);
            avatar(frame,"Mom","Family",R.drawable.pdf_mom,54,.88f,8);
            avatar(frame,"Ate","Sitter",R.drawable.pdf_ate,54,.12f,120);
            avatar(frame,"Jam","Family",R.drawable.pdf_jam,54,.88f,120);}
        frame.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            for(int i=0;i<frame.getChildCount();i++){View child=frame.getChildAt(i);Object tag=child.getTag();
                if(tag instanceof Float)child.setTranslationX(v.getWidth()*(Float)tag-child.getWidth()/2f);}});
        frame.addView(icon("Heart",23,pink),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.TOP|Gravity.CENTER_HORIZONTAL));return frame;
    }
    protected void careCircle(boolean access){header("Care Circle",access?"People who help care for "+name()+".":"People who care for "+name(),
            "More",access?"Person":null,()->preview("Invitations require connected shared accounts."),30,false);
        if(!access)add(center("Share access so family and sitters\ncan help keep "+name()+" happy and healthy."),44,8);
        add(circleDiagram(access),access?250:235,10);
        if(access){add(chips(Arrays.asList("Access Control","Activity Log"),value->{if(value.equals("Activity Log"))go.accept("Activity Log");}),42,16);
            sectionLabel("Access Permissions");
            add(toggleRow("Document","View Profile & Records","Can view all pet information",true,"access:view",true),65,3);
            add(toggleRow("Edit","Add & Edit Care Logs","Can add and edit care activities",true,"access:logs",true),65,3);
            add(toggleRow("Bell","Manage Reminders","Can create and edit reminders",false,"access:reminders",true),65,3);
            add(toggleRow("Folder","Manage Documents","Can add and edit documents",false,"access:documents",true),65,8);
            add(button("Save Permissions",()->preview("Preferences are saved on this device as a preview. No caregiver access has been granted.")),50);
        }else{add(button("Invite Caregiver",()->preview("Sample Care Circle: invitations and shared access are not connected.")),50,20);
            sectionLabel("Care Circle (4) · Demo",10);
            for(int i=0;i<CAREGIVERS.length;i++){String label=CAREGIVERS[i][0],role=CAREGIVERS[i][1];
                LinearLayout r=row();r.setBackground(shape());pad(r,10,7);r.setOnClickListener(v->go.accept("Care Circle Access"));
                LinearLayout.LayoutParams photo=new LinearLayout.LayoutParams(dp(46),dp(46));photo.rightMargin=dp(14);
                r.addView(portrait(CAREGIVER_IMAGES[i],46),photo);
                LinearLayout labels=col();labels.addView(text(label,15,ink,700));
                LinearLayout.LayoutParams detail=new LinearLayout.LayoutParams(-1,-2);detail.topMargin=dp(5);
                labels.addView(text(role+" · "+(label.equals("You")?"Full access":label.equals("Ate")?"Today's care only":"Care + Records"),12,sub),detail);
                r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));r.addView(icon("Chevron",22));add(r,62,4);}}
    }
    protected void voiceLog(){header("Voice Log","Describe the care activity in your own words.","Care");
        add(new Waveform(a,primary),90,20);add(button("Record voice",()->recognizeVoice.run()),52,12);
        EditText transcript=field("Or type what happened","","Mic","Fed "+name()+" one cup at 7:30 AM",false,true,null,null);
        add(button("Continue with text",true,()->{String value=transcript.getText().toString().trim();
            if(value.isEmpty())notice("Describe the care activity first.");else captureVoice.accept(value);}),52);
    }
    protected void soundJournal(){header("Sound Event Journal","Record a sound and describe what you heard.","Care");
        add(new Waveform(a,primary,true),180,12);add(button("Start / stop recording",()->toggleSoundRecording.run()),52,18);
        sectionLabel("Recent Recordings");List<JSONObject> recordings=soundRows.get();
        if(recordings.isEmpty())add(center("No sound observations saved for "+name()+" yet."),60);
        for(JSONObject entry:recordings){long duration=entry.optLong("durationMs")/1000;
            add(infoRow("Play",entry.optString("text"),duration+"s · "+entry.optString("date"),"More",primary,
                    ()->playRecording.accept(entry)),70,5);}
        add(center("Tap a recording to play it. Edit or delete it from Care History."),52);
    }
    protected static final class SampleDocument {
        final String name,date,type,tint;
        SampleDocument(String name,String date,String type,String tint){this.name=name;this.date=date;this.type=type;this.tint=tint;}
    }
    protected final List<SampleDocument> documents=Arrays.asList(
            new SampleDocument("Vaccination Card","Sep 12, 2026","Vaccinations","#F3B6D7"),
            new SampleDocument("Vet Receipt","Aug 28, 2026","Documents","#D8B4F8"),
            new SampleDocument("Prescription","Aug 10, 2026","Medications","#FFD0BC"),
            new SampleDocument("Vet Visit Notes","Jul 15, 2026","Appointments","#9BCBFF"));
    protected View documentRow(SampleDocument d,boolean tag){LinearLayout r=row();r.setBackground(shape());pad(r,13,9);
        r.setOnClickListener(v->{if(d.name.equals("Vaccination Card"))go.accept("Vaccination Card");
            else preview(d.name+" is a sample document. No file is attached.");});
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(34),dp(36));iconParams.rightMargin=dp(14);
        r.addView(icon("Document",32,color(d.tint)),iconParams);
        LinearLayout copy=col();copy.addView(text(d.name,14,ink,700));
        LinearLayout.LayoutParams date=new LinearLayout.LayoutParams(-1,-2);date.topMargin=dp(5);
        copy.addView(text(d.date,12,sub),date);r.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        if(tag){String type=d.type.endsWith("s")?d.type.substring(0,d.type.length()-1):d.type;
            TextView label=text(type,10,primary,500);label.setBackground(shape(elevated,18,false));pad(label,7,5);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(7);r.addView(label,p);}
        r.addView(icon(tag?"Chevron":"More",20));return r;
    }
    protected void records(){header("Pet Records","Keep all of "+name()+"'s important\ndocuments in one place.","More");
        LinearLayout identity=row();identity.setBackground(shape(elevated,20,true));pad(identity,12,8);identity.setOnClickListener(v->choose.run());
        LinearLayout.LayoutParams portraitParams=new LinearLayout.LayoutParams(dp(58),dp(58));portraitParams.rightMargin=dp(16);
        identity.addView(portrait(58),portraitParams);
        LinearLayout copy=col();copy.addView(heading(name(),23));copy.addView(text(pet.optString("species")+" · 2 years old",12,sub));
        identity.addView(copy,new LinearLayout.LayoutParams(0,-2,1));identity.addView(icon("Down",24));add(identity,80,12);
        add(chips(Arrays.asList("All","Vaccinations","Medications","Receipts"),v->go.accept("Browse Records")),38,16);
        LinearLayout upload=col();upload.setBackground(shape(elevated,24,true));pad(upload,16,14);upload.setGravity(Gravity.CENTER);
        upload.addView(art(R.drawable.pdf_folder,118),new LinearLayout.LayoutParams(-1,dp(118)));
        TextView title=section("Scan or upload a document");title.setGravity(Gravity.CENTER);upload.addView(title,new LinearLayout.LayoutParams(-1,dp(32)));
        upload.addView(center("Vaccination cards, prescriptions,\nreceipts, and more."),new LinearLayout.LayoutParams(-1,dp(45)));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48));bp.topMargin=dp(8);
        upload.addView(button("Add Document",()->preview("Document upload requires file selection and storage integration. No sample file has been uploaded.")),bp);
        add(upload,278,18);
        LinearLayout h=row();h.addView(section("Recent Documents"),new LinearLayout.LayoutParams(0,-2,1));
        TextView all=text("See All",12,primary,700);all.setOnClickListener(v->go.accept("Browse Records"));h.addView(all);add(h,28,8);
        for(int i=0;i<2;i++)add(documentRow(documents.get(i),false),66,5);demoLabel("Sample documents");
    }
    protected void vaccination(){header("Vaccination Card","Added on Sep 12, 2026 · Sample document","Records","More",
            ()->preview("This document is a supplied wireframe sample."),30,false);
        LinearLayout card=col();card.setBackground(shape());pad(card,11,11);
        ImageView image=art(R.drawable.pdf_vaccine_record,330);
        image.setContentDescription("Sample vaccination record from the PDF; not a clinical record for this pet");
        card.addView(image,new LinearLayout.LayoutParams(-1,dp(330)));
        LinearLayout meta=row();meta.addView(icon("Document",32,pink),new LinearLayout.LayoutParams(dp(42),dp(48)));
        LinearLayout labels=col();labels.addView(text("Vaccination Card",15,ink,700));labels.addView(text("Sep 12, 2026 · 2.4 MB · PDF",12,sub));
        meta.addView(labels);card.addView(meta,new LinearLayout.LayoutParams(-1,dp(64)));add(card,-2,6);
        LinearLayout actions=col();actions.setBackground(shape());pad(actions,2,3);
        String[][] items={{"Expand","View Full Screen"},{"Download","Download"},{"Share","Share"},{"Edit","Rename"},{"Trash","Delete"}};
        for(int i=0;i<items.length;i++){final int index=i;String kind=items[i][0],label=items[i][1];
            View item=infoRow(kind,label,null,"",i>=3?coral:primary,()->{
                if(index==0){ImageView full=new ImageView(a);full.setImageResource(R.drawable.pdf_vaccine_record);
                    full.setAdjustViewBounds(true);full.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    new AlertDialog.Builder(a).setTitle("Sample vaccination record").setView(full).setPositiveButton("Close",null).show();}
                else preview(label+" requires an attached document. This is a visual sample; no file was changed or shared.");});
            item.setBackground(null);actions.addView(item,new LinearLayout.LayoutParams(-1,dp(46)));
            if(i<4){View divider=new View(a);divider.setBackgroundColor(outline);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));p.leftMargin=dp(16);p.rightMargin=dp(16);actions.addView(divider,p);}}
        add(actions);
    }
    protected void drawTypeSelection(ViewGroup container,String selected){for(int i=0;i<container.getChildCount();i++){
        View child=container.getChildAt(i);if(child instanceof ViewGroup)drawTypeSelection((ViewGroup)child,selected);
        else if(child instanceof TextView){TextView t=(TextView)child;boolean active=t.getText().toString().equals(selected);
            t.setBackground(shape(active?lavender:surface,24,true));t.setTextColor(active?color("#24192E"):ink);}}}
    protected void recordsSearch(){header("Records","Find specific documents, visits,\nmedications, and more.","Records");
        final String[] selectedType={"All"},selectedDate={"All"};
        LinearLayout search=row();EditText query=new EditText(a);query.setHint("Search records…");query.setContentDescription("Search records");
        query.setTextSize(15);BrandType.apply(query,false,400);query.setTextColor(ink);query.setHintTextColor(sub);
        query.setBackground(shape(surface,30,true));query.setSingleLine();pad(query,18,10);
        search.addView(query,new LinearLayout.LayoutParams(0,dp(48),1));
        FrameLayout filter=action("Filter","Filter records",()->notice("Choose a type or date below."));filter.setBackground(shape(elevated,28,true));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(48),dp(48));fp.leftMargin=dp(10);search.addView(filter,fp);add(search,48,22);
        sectionLabel("Filter by Type");LinearLayout typeControls=col();add(typeControls,-2,18);
        sectionLabel("Filter by Date");LinearLayout dateControls=col();add(dateControls,-2,20);
        TextView count=section("Recent Results (4)");add(count,28,8);LinearLayout results=col();add(results);demoLabel("Searchable sample documents");
        Runnable refresh=()->{results.removeAllViews();List<SampleDocument> matches=new ArrayList<>();
            String term=query.getText().toString().toLowerCase(Locale.ROOT);
            for(SampleDocument d:documents)if((selectedType[0].equals("All")||d.type.equals(selectedType[0]))&&
                    (term.isEmpty()||d.name.toLowerCase(Locale.ROOT).contains(term))&&
                    (!selectedDate[0].equals("This Month")||d.date.startsWith("Sep"))&&
                    (!selectedDate[0].equals("Custom")||d.date.startsWith("Sep 12")))matches.add(d);
            count.setText("Recent Results ("+matches.size()+")");
            for(SampleDocument d:matches){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(62));p.bottomMargin=dp(5);
                results.addView(documentRow(d,true),p);}
            if(matches.isEmpty())results.addView(center("No matching sample documents."));};
        drawRecordChips(typeControls,new String[][]{{"All","Vaccinations","Medications"},{"Appointments","Documents","Care Logs"}},
                selectedType,refresh,false);
        drawRecordChips(dateControls,new String[][]{{"All","This Month","Last 3 Months"},{"Last Year","Custom"}},
                selectedDate,refresh,true);
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void afterTextChanged(Editable s){}public void onTextChanged(CharSequence s,int start,int before,int count){refresh.run();}});
        refresh.run();
    }
    private void drawRecordChips(LinearLayout container,String[][] groups,String[] selected,Runnable refresh,boolean dates){
        container.removeAllViews();for(String[] labels:groups)container.addView(chips(Arrays.asList(labels),selected[0],value->{
            selected[0]=value;drawTypeSelection(container,value);
            if(dates&&value.equals("Custom"))notice("Showing the sample document dated Sep 12, 2026.");refresh.run();}),
                new LinearLayout.LayoutParams(-1,dp(42)));
    }
    private static final String[][] SCHEDULE={{"Breakfast","8:00 AM","Feeding"},{"Vitamins","12:00 PM","Medication"},
            {"Playtime","5:00 PM","Playtime"},{"Dinner","7:00 PM","Feeding"}};
    protected View scheduleRow(String label,String time,String kind,String detail,boolean complete,Runnable onClick){
        LinearLayout r=row();r.setBackground(shape());pad(r,14,8);r.setOnClickListener(v->onClick.run());
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(40),dp(40));iconParams.rightMargin=dp(14);
        r.addView(icon(kind,36),iconParams);
        LinearLayout texts=col();texts.addView(text(time,12,sub));texts.addView(text(label,16,ink,700));texts.addView(text(detail,12,sub));
        r.addView(texts,new LinearLayout.LayoutParams(0,-2,1));FrameLayout status=new FrameLayout(a);
        GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);
        circle.setColor(complete?mint:Color.TRANSPARENT);if(!complete)circle.setStroke(dp(2),outline);
        status.setBackground(circle);if(complete)status.addView(icon("Check",22,color("#264439")),new FrameLayout.LayoutParams(-1,-1));
        r.addView(status,new LinearLayout.LayoutParams(dp(27),dp(27)));return r;
    }
    protected void reminders(){header("Reminders","Never miss a moment\nof their care.","More","Plus",()->addReminder.run(),30,false);
        String filter=prefs.getString("ui:reminder-filter","All");
        View filters=chips(Arrays.asList("All","Medication","Feeding","Others"),value->goFilteredReminders(value));
        add(filters,38,18);drawTypeSelection((ViewGroup)filters,filter);
        sectionLabel("Today · Thu, Sep 25",10);
        for(int i=0;i<SCHEDULE.length;i++){String[] task=SCHEDULE[i];String label=task[0],time=task[1],kind=task[2];
            if(filter.equals("All")||filter.equals(kind)||(filter.equals("Others")&&kind.equals("Playtime"))){
                String amount=i==1?"1 tablet":i==2?"20 mins":"1/2 cup kibble";
                add(scheduleRow(label,time,kind,amount+" · "+name(),i==0,()->go.accept("Quick Log:"+kind)),73,5);}}
        gap(16);sectionLabel("Tomorrow · Fri, Sep 26",10);
        if(filter.equals("All")||filter.equals("Medication"))
            add(scheduleRow("Medication","8:00 AM","Medication","1 tablet · Luna (demo)",false,()->go.accept("Quick Log:Medication")),73,5);
        if(filter.equals("All")||filter.equals("Others"))
            add(scheduleRow("Vet Visit","6:00 PM","Vet Visit","Annual check-up · Bailey (demo)",false,()->addReminder.run()),73,5);
        demoLabel("Sample schedule");
        if(!reminders.isEmpty()){sectionLabel("Your reminders");for(JSONObject reminder:reminders)
            add(scheduleRow(reminder.optString("title"),reminder.optString("when"),"Care",reminder.optString("pet"),
                    reminder.optBoolean("done"),()->toggleReminder.accept(reminder)),73,5);}
    }
    protected void goFilteredReminders(String filter){prefs.edit().putString("ui:reminder-filter",filter).apply();go.accept("Reminders");}
    protected void calendar(){header("Care Calendar","See all of "+name()+"'s care at a glance.","More","Care",()->addReminder.run(),30,false);
        Calendar date=Calendar.getInstance();date.set(2026,Calendar.SEPTEMBER,25,12,0,0);
        LinearLayout calendar=col();add(calendar);LinearLayout scheduleBox=col();add(scheduleBox);
        Runnable[] render={null};render[0]=()->renderCalendar(date,calendar,scheduleBox,render[0]);
        render[0].run();gap(18);
        add(button("Add to Google Calendar",()->preview("This is a sample schedule. Google Calendar export is not connected.")),50,4);
        demoLabel("Sample schedule · dates are selectable");
    }
    private void renderCalendar(Calendar date,LinearLayout calendar,LinearLayout scheduleBox,Runnable render){
        calendar.removeAllViews();scheduleBox.removeAllViews();
        LinearLayout month=row();month.setBackground(shape());
        TextView label=center(new SimpleDateFormat("MMMM yyyy",Locale.US).format(date.getTime()),17,primary,700);
        month.addView(action("Back","Previous month",()->{date.add(Calendar.MONTH,-1);render.run();}),new LinearLayout.LayoutParams(dp(44),dp(44)));
        month.addView(label,new LinearLayout.LayoutParams(0,dp(44),1));
        month.addView(action("Chevron","Next month",()->{date.add(Calendar.MONTH,1);render.run();}),new LinearLayout.LayoutParams(dp(44),dp(44)));
        calendar.addView(month);
        LinearLayout weekdays=row();for(String day:new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"})
            weekdays.addView(center(day,12,sub,400),new LinearLayout.LayoutParams(0,dp(42),1));calendar.addView(weekdays);
        Calendar start=(Calendar)date.clone();start.add(Calendar.DAY_OF_MONTH,-(start.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY));
        for(int week=0;week<2;week++){LinearLayout r=row();for(int day=0;day<7;day++){
            Calendar cellDate=(Calendar)start.clone();cellDate.add(Calendar.DAY_OF_MONTH,week*7+day);
            boolean chosen=cellDate.get(Calendar.DAY_OF_YEAR)==date.get(Calendar.DAY_OF_YEAR)&&
                    cellDate.get(Calendar.YEAR)==date.get(Calendar.YEAR);
            LinearLayout cell=col();cell.setGravity(Gravity.CENTER);
            cell.setContentDescription(new SimpleDateFormat("MMMM d, yyyy",Locale.US).format(cellDate.getTime()));
            cell.setOnClickListener(v->{date.setTime(cellDate.getTime());render.run();});
            TextView number=center(String.valueOf(cellDate.get(Calendar.DAY_OF_MONTH)),15,chosen?color("#26192E"):ink,chosen?700:400);
            if(chosen)number.setBackground(shape(lavender,28,false));cell.addView(number,new LinearLayout.LayoutParams(dp(38),dp(38)));
            int[] colors={mint,yellow,pink,primary};View dot=new View(a);dot.setBackground(shape(colors[day%4],6,false));
            LinearLayout.LayoutParams dotParams=new LinearLayout.LayoutParams(dp(6),dp(6));dotParams.topMargin=dp(3);cell.addView(dot,dotParams);
            r.addView(cell,new LinearLayout.LayoutParams(0,dp(54),1));}calendar.addView(r);}
        TextView selected=center(new SimpleDateFormat("EEE, MMM d, yyyy",Locale.US).format(date.getTime()),16,ink,700);
        selected.setBackground(shape());selected.setGravity(Gravity.CENTER_VERTICAL);pad(selected,16,8);
        LinearLayout.LayoutParams selectedParams=new LinearLayout.LayoutParams(-1,dp(48));selectedParams.topMargin=dp(16);selectedParams.bottomMargin=dp(8);
        calendar.addView(selected,selectedParams);
        int[] railColors={mint,pink,yellow,yellow};
        for(int i=0;i<SCHEDULE.length;i++){String[] task=SCHEDULE[i];String kind=task[2];LinearLayout r=row();
            FrameLayout rail=new FrameLayout(a);View bar=new View(a);bar.setBackground(shape(railColors[i],2,false));
            rail.addView(bar,new FrameLayout.LayoutParams(dp(2),-1,Gravity.CENTER));
            View point=new View(a);point.setBackground(shape(railColors[i],12,false));
            rail.addView(point,new FrameLayout.LayoutParams(dp(12),dp(12),Gravity.CENTER));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(20),dp(76));rp.rightMargin=dp(8);r.addView(rail,rp);
            r.addView(text(task[1],11,sub),new LinearLayout.LayoutParams(dp(61),-2));
            String amount=i==2?"20 mins · "+name():i==1?"1 tablet · "+name():"1/2 cup · "+name();
            r.addView(scheduleRow(task[0],"",kind,amount,i==0,()->go.accept("Quick Log:"+kind)),new LinearLayout.LayoutParams(0,dp(68),1));
            scheduleBox.addView(r,new LinearLayout.LayoutParams(-1,dp(76)));}
    }
    protected View idCard(boolean horizontal){LinearLayout card=horizontal?row():col();
        if(!horizontal)card.setGravity(Gravity.CENTER);card.setBackground(new PetIdBackground(horizontal));pad(card,18,16);
        LinearLayout identity=col();identity.setGravity(horizontal?Gravity.START:Gravity.CENTER);
        identity.addView(portrait(R.drawable.pdf_mochi,horizontal?90:138,color("#D995DF")));
        LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(-2,-2);nameParams.topMargin=dp(5);
        identity.addView(text(name(),horizontal?23:27,color("#211626"),600,true),nameParams);
        TextView detail=text(pet.optString("species")+" · 2 years old",13,color("#33223D"));
        detail.setGravity(horizontal?Gravity.START:Gravity.CENTER);identity.addView(detail);
        if(horizontal){card.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
            card.addView(new DemoQr(a),new LinearLayout.LayoutParams(dp(112),dp(112)));}
        else{card.addView(identity);LinearLayout.LayoutParams qr=new LinearLayout.LayoutParams(dp(146),dp(146));qr.topMargin=dp(10);
            card.addView(new DemoQr(a),qr);LinearLayout.LayoutParams note=new LinearLayout.LayoutParams(-2,dp(28));note.topMargin=dp(5);
            card.addView(text("Demo QR · not scannable",12,color("#33223D"),700),note);}
        return card;
    }
    protected void petId(){header("Pet ID QR","A safer tomorrow for your furbaby.","More");
        add(idCard(false),398,10);add(button("Customize Info",()->go.accept("QR Privacy")),44,18);
        sectionLabel("Share or Download");LinearLayout actions=row();
        String[][] items={{"Share","Share"},{"Download","Download"},{"Print","Print"},{"Photo","Save to Photos"}};
        for(String[] item:items){String kind=item[0],label=item[1];LinearLayout cell=col();cell.setGravity(Gravity.CENTER);
            FrameLayout control=action(kind,label,()->preview("The QR card is a non-scannable demo. "+label+" is not connected to an exportable pet ID."));
            control.setBackground(shape(surface,40,true));cell.addView(control,new LinearLayout.LayoutParams(dp(58),dp(58)));
            cell.addView(center(label,11,ink,400),new LinearLayout.LayoutParams(-1,dp(27)));
            actions.addView(cell,new LinearLayout.LayoutParams(0,dp(86),1));}add(actions,86,14);
        LinearLayout info=row();info.setBackground(shape());pad(info,10,7);
        info.addView(art(R.drawable.pdf_id_cat,64),new LinearLayout.LayoutParams(dp(88),dp(64)));
        info.addView(text("You can choose what information\nis shown on "+name()+"'s ID card.",12,sub),new LinearLayout.LayoutParams(0,-2,1));
        add(info,80);
    }
    protected void emergency(){header("Emergency Mode","Quick access to important\ninformation when it matters.","More","Warning",
            ()->preview("Emergency details have not been configured in this prototype."),30,false);
        LinearLayout card=row();card.setBackground(shape(night?color("#302026"):color("#FBE5EA"),20,true));pad(card,14,14);
        LinearLayout.LayoutParams photoParams=new LinearLayout.LayoutParams(dp(102),dp(102));photoParams.rightMargin=dp(14);
        card.addView(portrait(R.drawable.pdf_mochi,102,coral),photoParams);
        LinearLayout copy=col();copy.addView(heading(name(),26));copy.addView(text(pet.optString("species")+" · 2 years old",13,sub));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(42));bp.topMargin=dp(10);
        copy.addView(button("EMERGENCY INFO",false,color("#FF8298"),()->preview("No emergency contact or clinical record is configured. Open Pet Passport to review available details.")),bp);
        card.addView(copy,new LinearLayout.LayoutParams(0,-2,1));add(card,142,12);
        add(infoRow("Medical","Medical Information","Allergies, medications, conditions","Chevron",coral,()->go.accept("Pet Passport")),68,6);
        add(infoRow("Vet Visit","Veterinarian","Contact not configured","Phone",coral,
                ()->preview("Add a verified veterinary contact before using this action.")),68,6);
        add(infoRow("Phone","Emergency Contact","Contact not configured","Phone",coral,()->go.accept("Personal Information")),68,6);
        add(infoRow("Pets","Care Circle","View your caregivers","Chevron",coral,()->go.accept("Care Circle")),68,6);
        add(infoRow("Document","Pet Passport","View and share full summary","Chevron",coral,()->go.accept("Pet Passport")),68,20);
        add(button("Share Emergency Info",false,color("#FF8298"),()->preview("Emergency sharing is not connected. No information has been shared.")),52);
    }
    protected void offline(){header("Offline & Sync","Keep recording, even without\ninternet connection.","More");
        add(art(R.drawable.pdf_offline_cat,112),112);
        add(infoRow("Wifi","You're offline","Your entries are saved locally.\nCloud sync is not connected.","",pink,
                ()->preview("This is the offline presentation state. Device network connectivity is not being measured.")),116,20);
        int pending=logs.isEmpty()?3:logs.size();LinearLayout h=row();
        h.addView(section("Pending Sync ("+pending+")"),new LinearLayout.LayoutParams(0,-2,1));
        h.addView(button("Sync Now",()->preview("Firebase synchronization is not connected. Local entries remain on this device.")),
                new LinearLayout.LayoutParams(dp(118),dp(40)));add(h,44,10);
        if(logs.isEmpty()){demoLabel("Offline preview · sample pending entries");
            String[][] sample={{"Feeding","Sep 25, 8:00 AM","Feeding"},{"Medication","Sep 25, 12:00 PM","Medication"},
                    {"Vet Visit","Sep 24, 4:00 PM","Vet Visit"}};
            for(String[] item:sample)add(infoRow(item[2],item[0],item[1]+" · "+name(),"Cloud",primary,
                    ()->preview("Sample pending entry; cloud sync is not connected.")),72,4);
        }else for(JSONObject entry:logs)add(infoRow("Notes",entry.optString("kind"),entry.optString("date"),"Cloud",primary,
                ()->go.accept("Care History")),72,4);
        gap(12);LinearLayout info=row();info.setBackground(shape());pad(info,14,10);
        info.addView(icon("Cloud",32),new LinearLayout.LayoutParams(dp(40),dp(42)));
        info.addView(text("Your local records stay available\nwhile cloud sync is disconnected.",12,sub),new LinearLayout.LayoutParams(0,-2,1));
        info.addView(art(R.drawable.pdf_sync_cat,70),new LinearLayout.LayoutParams(dp(72),dp(70)));add(info,92);
    }
    private static final class ActivityEntry {
        final String title,detail,by,kind,type;final boolean yesterday;
        ActivityEntry(String title,String detail,String by,String kind,String type,boolean yesterday){
            this.title=title;this.detail=detail;this.by=by;this.kind=kind;this.type=type;this.yesterday=yesterday;}
    }
    protected void activityLog(){header("Care Circle Activity Log","Track who did what and when.","Care Circle Access");
        List<String> filters=Arrays.asList("All Activities","Care Logs","Documents","Reminders","Profile");
        String[] selected={"All Activities"};TextView chooseFilter=button(selected[0],true,()->{});add(chooseFilter,48,16);
        LinearLayout results=col();add(results);
        List<ActivityEntry> entries=Arrays.asList(
                new ActivityEntry("Logged Feeding","8:00 AM · 1/2 cup kibble","Mom","Feeding","Care Logs",false),
                new ActivityEntry("Logged Medication","12:00 PM · 1 tablet","You","Medication","Care Logs",false),
                new ActivityEntry("Added Document","2:15 PM · Vet Receipt","Ate","Document","Documents",false),
                new ActivityEntry("Updated Reminder","6:30 PM · Changed time","Jam","Bell","Reminders",true),
                new ActivityEntry("Added Care Log","5:00 PM · Playtime","Ate","Playtime","Care Logs",true),
                new ActivityEntry("Edited Pet Profile","10:20 AM · Updated weight","You","Cat","Profile",true));
        Runnable draw=()->drawActivityLog(results,entries,selected[0]);
        chooseFilter.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("Filter activities")
                .setItems(filters.toArray(new String[0]),(dialog,index)->{
                    selected[0]=filters.get(index);chooseFilter.setText(selected[0]);draw.run();}).show());
        draw.run();demoLabel("Sample caregiver activity");
    }
    private void drawActivityLog(LinearLayout results,List<ActivityEntry> entries,String selected){
        results.removeAllViews();for(boolean yesterday:new boolean[]{false,true}){
            List<ActivityEntry> matching=new ArrayList<>();
            for(ActivityEntry e:entries)if(e.yesterday==yesterday&&
                    (selected.equals("All Activities")||e.type.equals(selected)))matching.add(e);
            if(matching.isEmpty())continue;
            LinearLayout.LayoutParams title=new LinearLayout.LayoutParams(-1,dp(38));title.topMargin=dp(yesterday?12:0);
            results.addView(section(yesterday?"Yesterday · Wed, Sep 24, 2026":"Today · Thu, Sep 25, 2026"),title);
            for(ActivityEntry entry:matching){LinearLayout r=row();r.setBackground(shape());pad(r,12,9);
                r.setOnClickListener(v->preview("Sample caregiver activity. Live attribution requires shared accounts."));
                LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(34),dp(38));iconParams.rightMargin=dp(12);
                r.addView(icon(entry.kind,30),iconParams);
                LinearLayout copy=col();copy.addView(text(entry.title,14,ink,700));
                copy.addView(text(entry.detail+"\nby "+entry.by,12,sub));r.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
                int image=R.drawable.pdf_owner;for(int i=0;i<CAREGIVERS.length;i++)
                    if(CAREGIVERS[i][0].equals(entry.by)){image=CAREGIVER_IMAGES[i];break;}
                LinearLayout.LayoutParams portraitParams=new LinearLayout.LayoutParams(dp(38),dp(38));portraitParams.rightMargin=dp(10);
                r.addView(portrait(image,38),portraitParams);r.addView(icon("More",20));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(78));p.bottomMargin=dp(5);results.addView(r,p);}
        }
    }
    protected void privacy(){header("QR Information Privacy","Choose what information to share\nwhen "+name()+"'s QR code is scanned.","Pet ID QR");
        add(idCard(true),178);add(button("Preview QR Code",true,()->go.accept("Pet ID QR")),42,16);
        sectionLabel("Information to Share");
        String[][] items={{"Pets","Pet Name","true"},{"Photo","Photo","true"},{"Cat","Species & Breed","true"},
                {"Care","Age / Birthday","false"},{"Phone","Owner Contact Number","true"},
                {"Mail","Owner Email","false"},{"Medical","Medical Information","true"},
                {"Vet Visit","Veterinarian","true"}};
        for(String[] item:items){String label=item[1];String description=label.equals("Medical Information")?
                "Allergies, medications (basic)":label.equals("Veterinarian")?"Clinic name and contact":null;
            add(toggleRow(item[0],label,description,Boolean.parseBoolean(item[2]),"privacy:"+label,true),
                    description==null?44:64,2);}
        demoLabel("Privacy preferences are local · nothing is published");
    }
    protected void more(){FrameLayout brandHeader=new FrameLayout(a);
        brandHeader.addView(action("Back","Back",()->goBack.run()),
                new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.START|Gravity.CENTER_VERTICAL));
        ImageView logo=new ImageView(a);logo.setImageResource(R.drawable.purrfolio_wordmark_alpha);
        logo.setColorFilter(primary,PorterDuff.Mode.SRC_IN);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("PURRFOLIO logo");brandHeader.addView(logo,new FrameLayout.LayoutParams(dp(190),dp(48),Gravity.CENTER));
        add(brandHeader,64,12);
        String[] labels={"Account","Care & Routines","Care Circle","Records & Safety"};
        String[][][] groups={
            {{"Person","My Profile","Profile"}},
            {{"Bell","Reminders","Reminders"},{"Care","Care Calendar","Calendar"},
                {"Wave","Sound Event Journal","Sound Journal"},{"Notes","Care History","Care History"}},
            {{"Pets","Care Circle","Care Circle"},{"Link","Sitter Mode","Sitter Mode"},
                {"Lock","Care Circle Access","Care Circle Access"},{"Notes","Activity Log","Activity Log"}},
            {{"Document","Pet Records","Records"},{"Search","Search Records","Browse Records"},
                {"QR","Pet ID QR","Pet ID QR"},{"Emergency","Emergency Mode","Emergency"},
                {"Cloud","Offline & Sync","Offline & Sync"},{"Lock","QR Information Privacy","QR Privacy"}}
        };
        for(int group=0;group<groups.length;group++){add(text(labels[group],13,sub,700),20,6);
            LinearLayout list=col();list.setBackground(shape(surface,18,true));pad(list,0,3);
            String[][] items=groups[group];for(int i=0;i<items.length;i++){String[] item=items[i];
                View view=infoRow(item[0],item[1],()->go.accept(item[2]));view.setBackground(null);
                list.addView(view,new LinearLayout.LayoutParams(-1,dp(54)));
                if(i<items.length-1){View divider=new View(a);divider.setBackgroundColor(outline);
                    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));p.leftMargin=dp(57);p.rightMargin=dp(12);
                    list.addView(divider,p);}}
            add(list,-2,group<groups.length-1?12:0);}
    }
}
