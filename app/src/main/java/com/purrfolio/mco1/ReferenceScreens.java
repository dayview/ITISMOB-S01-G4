package com.purrfolio.mco1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.purrfolio.mco1.databinding.FloatingBottomNavBinding;
import com.purrfolio.mco1.databinding.ScreenReferenceBinding;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONObject;

/** Reference screen presentation; persistence remains in MainActivity. */
public class ReferenceScreens {
    public static final Set<String> destinations=new HashSet<>(Arrays.asList("Home","Pet Detail","Care","Pet Timeline","Sitter Mode","Pet Passport"));
    private final Activity a;
    private final JSONObject pet;
    private final Consumer<String> go;
    private final Runnable choose,onPetPhoto,onBack;
    private final boolean night;
    private final int bg,surface,elevated,lavender,primary,ink,sub,outline,mint,pink,yellow,blue;
    private Bitmap photo;
    private LinearLayout content;

    public ReferenceScreens(Activity a,JSONObject pet,Consumer<String> go,Runnable choose,
            Runnable onPetPhoto,Runnable onBack){
        this.a=a;this.pet=pet;this.go=go;this.choose=choose;this.onPetPhoto=onPetPhoto;this.onBack=onBack;
        night=(a.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        bg=color(night?"#101014":"#FCF9FF");surface=color(night?"#1C1B26":"#F2EBF7");
        elevated=color(night?"#24222F":"#EBE0F3");lavender=color("#D8B4F8");
        primary=color(night?"#E1BEFC":"#8051A8");ink=color(night?"#F8F2FF":"#292130");
        sub=color(night?"#BDB3CD":"#74667F");outline=color(night?"#373243":"#D9CCE5");
        mint=color("#91E5C8");pink=color("#F3B6D7");yellow=color("#FFD18C");blue=color("#9BCBFF");
    }
    public ReferenceScreens(Activity a,JSONObject pet,Consumer<String> go,Runnable choose){
        this(a,pet,go,choose,()->{},()->go.accept("Home"));
    }
    private String name(){return pet.optString("name","Mochi");}
    private int color(String hex){return Color.parseColor(hex);}
    private int dp(int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout col(){LinearLayout view=new LinearLayout(a);view.setOrientation(LinearLayout.VERTICAL);return view;}
    private LinearLayout row(){LinearLayout view=new LinearLayout(a);view.setOrientation(LinearLayout.HORIZONTAL);view.setGravity(Gravity.CENTER_VERTICAL);return view;}
    private void pad(View v,int l,int t,int r,int b){v.setPadding(dp(l),dp(t),dp(r),dp(b));}
    private void pad(View v,int l,int t){pad(v,l,t,l,t);}
    private GradientDrawable shape(int color,int radius,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border)d.setStroke(dp(1),outline);return d;}
    private GradientDrawable shape(int color,int radius){return shape(color,radius,false);}
    private GradientDrawable shape(int color){return shape(color,22,false);}
    private TextView text(String value,float size,int color,int weight,boolean display){TextView t=new TextView(a);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);
        BrandType.apply(t,display,weight);t.setLineSpacing(dp(2),1f);return t;}
    private TextView text(String value,float size,int color,int weight){return text(value,size,color,weight,false);}
    private TextView text(String value,float size,int color){return text(value,size,color,400,false);}
    private TextView title(String value,float size){return text(value,size,primary,size<=22||size>=30?600:500,true);}
    private TextView title(String value){return title(value,28);}
    private void spacer(int height){content.addView(new View(a),new LinearLayout.LayoutParams(1,dp(height)));}
    private void add(View view,int height,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));p.bottomMargin=dp(bottom);content.addView(view,p);}
    private void add(View view,int height){add(view,height,0);}
    private void add(View view){add(view,-2,0);}
    private void flexible(int minimum){content.addView(new View(a),new LinearLayout.LayoutParams(1,dp(minimum),1));}
    private CareArtwork icon(String kind,int size,int tint){CareArtwork view=new CareArtwork(a,kind,tint);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return view;}
    private CareArtwork icon(String kind,int size){return icon(kind,size,primary);}
    private FrameLayout action(String kind,String description,Runnable callback){FrameLayout view=new FrameLayout(a);
        view.setContentDescription(description);view.setFocusable(true);view.setOnClickListener(v->callback.run());
        view.addView(icon(kind,26),new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));return view;}
    private TextView button(String label,boolean secondary,Runnable callback){TextView view=text(label,17,secondary?primary:color("#201528"),700);
        view.setGravity(Gravity.CENTER);view.setBackground(shape(secondary?elevated:lavender,32,secondary));
        view.setMinHeight(dp(52));view.setOnClickListener(v->callback.run());view.setFocusable(true);return view;}
    private void preview(String message){new AlertDialog.Builder(a).setTitle("Frontend preview").setMessage(message).setPositiveButton("OK",null).show();}
    private Bitmap photo(){if(photo!=null)return photo;String uri=pet.optString("photoUri");
        if(!uri.isEmpty())try(InputStream stream=a.getContentResolver().openInputStream(Uri.parse(uri))){if(stream!=null)photo=BitmapFactory.decodeStream(stream);}catch(Exception ignored){}
        if(photo==null)photo=BitmapFactory.decodeResource(a.getResources(),R.drawable.purrfolio_cat_hero);return photo;}
    private void header(String label,String extra){LinearLayout r=row();r.addView(action("Back","Back",()->onBack.run()));
        TextView heading=title(label);heading.setGravity(Gravity.CENTER);heading.setMaxLines(1);
        if(label.contains("⌄"))heading.setOnClickListener(v->choose.run());
        if(label.startsWith("•")){SpannableString span=new SpannableString(label);span.setSpan(new ForegroundColorSpan(mint),0,1,0);heading.setText(span);}
        r.addView(heading,new LinearLayout.LayoutParams(0,dp(56),1));
        r.addView(extra==null?new View(a):action(extra,extra.equals("More")?"Pet options":"Share passport",
                ()->{if(extra.equals("More"))petOptions();else go.accept("Pet ID QR");}),new LinearLayout.LayoutParams(dp(48),dp(48)));
        add(r,64,12);}
    private void header(String label){header(label,null);}
    private FrameLayout portrait(int size){FrameLayout frame=new FrameLayout(a);frame.setBackground(shape(primary,size,true));pad(frame,3,3);
        Bitmap source=photo();Bitmap cropped=Bitmap.createBitmap(source,(int)(source.getWidth()*.18f),(int)(source.getHeight()*.22f),
                (int)(source.getWidth()*.65f),(int)(source.getHeight()*.65f));
        ImageView image=new ImageView(a);image.setImageBitmap(cropped);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(shape(surface,size));image.setClipToOutline(true);image.setContentDescription(name()+" portrait");
        frame.addView(image,new FrameLayout.LayoutParams(-1,-1));frame.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return frame;}
    public View build(String screen){
        WindowCompat.setDecorFitsSystemWindows(a.getWindow(),false);
        WindowCompat.getInsetsController(a.getWindow(),a.getWindow().getDecorView()).setAppearanceLightStatusBars(!night);
        WindowCompat.getInsetsController(a.getWindow(),a.getWindow().getDecorView()).setAppearanceLightNavigationBars(!night);
        a.getWindow().setStatusBarColor(bg);a.getWindow().setNavigationBarColor(bg);
        ScreenReferenceBinding binding=ScreenReferenceBinding.inflate(a.getLayoutInflater());
        LinearLayout root=binding.referenceRoot;root.setBackgroundColor(bg);content=binding.referenceContent;
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{androidx.core.graphics.Insets safe=insets.getInsets(
                WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});
        if(screen.equals("Care")){((LinearLayout.LayoutParams)binding.referenceScroll.getLayoutParams()).topMargin=dp(38);
            binding.dockContainer.setVisibility(View.GONE);}
        switch(screen){case "Home":home();break;case "Pet Detail":overview();break;case "Care":logCare();break;
            case "Pet Timeline":timeline();break;case "Sitter Mode":sitter();break;case "Pet Passport":passport();break;}
        if(!screen.equals("Care"))binding.dockContainer.addView(dock(screen));
        ViewCompat.requestApplyInsets(root);return root;
    }
    public View dock(String screen){FloatingBottomNavBinding binding=FloatingBottomNavBinding.inflate(a.getLayoutInflater());bindDock(binding,screen);return binding.getRoot();}
    public void bindDock(FloatingBottomNavBinding binding,String screen){
        String active=screen.equals("Home")?"Home":screen.equals("Pet Detail")||screen.equals("Pets")?"Pets":
                Arrays.asList("Profile","Account Settings","Notifications","More").contains(screen)?"More":"Care";
        bindDockCell(binding.navHome,"Home","Home",active);
        bindDockCell(binding.navPets,"Pets","Pets",active);
        bindDockCell(binding.navCare,"Care","Pet Timeline",active);
        bindDockCell(binding.navMore,"More","More",active);
        binding.navLogCare.setOnClickListener(v->go.accept("Care"));
    }
    private void bindDockCell(LinearLayout cell,String label,String target,String active){boolean selected=label.equals(active);
        if(selected){GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{elevated,night?color("#17131E"):surface});background.setCornerRadius(dp(22));cell.setBackground(background);}
        else cell.setBackground(null);
        ((CareArtwork)cell.getChildAt(0)).setArtwork(label,selected?primary:sub);
        TextView caption=(TextView)cell.getChildAt(1);caption.setTextColor(selected?ink:sub);BrandType.apply(caption,false,600);
        cell.setOnClickListener(v->go.accept(target));
    }
    private void week(boolean capsules){LinearLayout r=row();int[] colors={yellow,sub,blue,lavender,pink,lavender,mint};String[] days={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        for(int i=0;i<7;i++){final int index=i;LinearLayout cell=col();cell.setGravity(Gravity.CENTER);
            if(capsules||i==3)cell.setBackground(shape(i==3?(capsules?lavender:elevated):surface,18));pad(cell,0,7);
            cell.setContentDescription(days[i]+", September "+(22+i)+", sample schedule");
            cell.setOnClickListener(v->{if(index!=3)preview("This reference schedule shows Thursday, September 25. Your saved entries are available in Care History.");});
            TextView day=text(days[i],12,i==3?(capsules?color("#392446"):ink):sub,i==3?800:400);day.setGravity(Gravity.CENTER);cell.addView(day);
            if(capsules){TextView date=text(String.valueOf(22+i),19,i==3?color("#292130"):ink,800);date.setGravity(Gravity.CENTER);
                cell.addView(date,new LinearLayout.LayoutParams(-1,dp(30)));}
            else{FrameLayout circle=new FrameLayout(a);GradientDrawable d=new GradientDrawable();d.setShape(GradientDrawable.OVAL);
                d.setColor(i==3?primary:bg);d.setStroke(dp(3),colors[i]);circle.setBackground(d);
                if(i==3)circle.addView(icon("Pets",17,color("#48305C")),new FrameLayout.LayoutParams(dp(17),dp(17),Gravity.CENTER));
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(25),dp(25));cp.topMargin=dp(8);cell.addView(circle,cp);}
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(capsules?69:64),1);lp.setMargins(dp(2),0,dp(2),0);r.addView(cell,lp);
        }add(r,capsules?72:66,18);
    }
    private static final String[][] TASKS={{"Breakfast","8:00 AM","1/2 cup kibble"},{"Vitamins","12:00 PM","1 tablet"},
            {"Playtime","5:00 PM","20 mins"},{"Dinner","7:00 PM","1/2 cup kibble"}};
    private View careRow(int index,boolean timeline){String[] task=index==4?new String[]{"Vet Visit","10:00 AM","Paws Animal Clinic"}:TASKS[index];
        String kind=index==0||index==3?"Feeding":index==1?"Medication":index==2?"Playtime":"Vet Visit";
        LinearLayout r=row();r.setBackground(shape(surface,22));pad(r,16,10);
        r.setOnClickListener(v->go.accept(index==4?"Reminders":"Quick Log"));
        r.setContentDescription(task[0]+", "+task[1]+", "+task[2]+", sample care task");
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(40),dp(42));ip.rightMargin=dp(18);r.addView(icon(kind,40),ip);
        LinearLayout labels=col();labels.addView(text(task[0],16,ink,800));
        TextView detail=text(timeline?task[1]+"\n"+task[2]:task[1]+" · "+task[2],13,sub);
        detail.setLineSpacing(dp(1),1);LinearLayout.LayoutParams dp2=new LinearLayout.LayoutParams(-1,-2);dp2.topMargin=dp(4);labels.addView(detail,dp2);
        r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout check=new FrameLayout(a);
        if(index!=4){GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);
            circle.setColor(index==0?mint:Color.TRANSPARENT);if(index!=0)circle.setStroke(dp(2),color("#776B92"));check.setBackground(circle);}
        if(index==0)check.addView(icon("Check",22,color("#173A32")),new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));
        if(index==4)check.addView(icon("Chevron",25),new FrameLayout.LayoutParams(-1,-1));
        r.addView(check,new LinearLayout.LayoutParams(dp(28),dp(28)));return r;
    }
    private void home(){
        spacer(10);LinearLayout r=row();r.addView(title("Good morning,\n"+name()+"!",32),new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout portrait=portrait(76);portrait.setOnClickListener(v->go.accept("Pet Detail"));r.addView(portrait);add(r,90,8);
        add(text("Thu, Sep 25, 2026",15,sub),25,15);week(false);
        LinearLayout summary=row();summary.setBackground(shape(surface));pad(summary,18,12);summary.setOnClickListener(v->go.accept("Pet Timeline"));
        LinearLayout labels=col();labels.addView(title("Today's Care",20));TextView progress=text("3 of 5 done",16,primary,700);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=dp(4);labels.addView(progress,pp);
        summary.addView(labels,new LinearLayout.LayoutParams(0,-2,1));FrameLayout arrow=action("Arrow","Open care timeline",()->go.accept("Pet Timeline"));
        arrow.setBackground(shape(elevated,30));summary.addView(arrow);add(summary,76,7);
        for(int i=0;i<TASKS.length;i++)add(careRow(i,false),72,5);flexible(12);
    }
    private void petOptions(){String[] options={"Pet profile","Switch pet","Edit pet profile","Pet Passport","Sitter Mode","Care History","Care Circle","Pet Records"};
        String[] targets={"Pet Profile",null,"Edit Pet Profile","Pet Passport","Sitter Mode","Care History","Care Circle","Records"};
        new AlertDialog.Builder(a).setTitle(name()).setItems(options,(dialog,index)->{if(targets[index]==null)choose.run();else go.accept(targets[index]);}).show();}
    private void overviewHeader(){FrameLayout header=new FrameLayout(a);
        header.addView(action("Back","Back",()->onBack.run()),new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.START|Gravity.CENTER_VERTICAL));
        LinearLayout chooser=row();chooser.setGravity(Gravity.CENTER_VERTICAL);chooser.setContentDescription("Choose pet");chooser.setOnClickListener(v->choose.run());
        TextView name=title(name());name.setGravity(Gravity.CENTER_VERTICAL);name.setMaxLines(1);chooser.addView(name,new LinearLayout.LayoutParams(-2,dp(56)));
        View chevron=new View(a){final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas){paint.setColor(primary);paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
                float centerY=getHeight()/2f-dp(1),half=dp(5),drop=dp(4);
                Path path=new Path();path.moveTo(getWidth()/2f-half,centerY-drop/2);path.lineTo(getWidth()/2f,centerY+drop/2);
                path.lineTo(getWidth()/2f+half,centerY-drop/2);canvas.drawPath(path,paint);}};
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(18),dp(18));cp.leftMargin=dp(7);chooser.addView(chevron,cp);
        header.addView(chooser,new FrameLayout.LayoutParams(-2,dp(56),Gravity.CENTER));
        header.addView(action("More","Pet options",this::petOptions),new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.END|Gravity.CENTER_VERTICAL));
        add(header,64,12);
    }
    private void overview(){overviewHeader();PetRings rings=new PetRings(a,photo(),!night,()->onPetPhoto.run());
        rings.setContentDescription("Pet wellness rings. Tap the pet photo to edit, change, or remove it.");add(rings,330,12);
        LinearLayout metrics=row();String[] kinds={"Energy","Food","Water","Mood"},values={"Good","3/4","2/3","Great"};
        int[] colors={mint,primary,blue,mint};
        for(int i=0;i<4;i++){LinearLayout cell=col();cell.setGravity(Gravity.CENTER);
            cell.addView(icon(kinds[i],34),new LinearLayout.LayoutParams(dp(34),dp(34)));
            TextView label=text(kinds[i],12,ink,600);label.setGravity(Gravity.CENTER);label.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(6);cell.addView(label,lp);
            int valueColor=night?colors[i]:colors[i]==mint?color("#28755F"):colors[i]==blue?color("#396B9C"):primary;
            TextView value=text(values[i],18,valueColor,800);value.setGravity(Gravity.CENTER);value.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);cell.addView(value);
            metrics.addView(cell,new LinearLayout.LayoutParams(0,dp(90),1));}
        add(metrics,92,18);spacer(10);
        LinearLayout card=row();card.setBackground(shape(surface,22,true));pad(card,20,18);
        LinearLayout copy=col();copy.addView(title("Insights",20));TextView description=text(name()+" seems more playful\ntoday! +20% playtime\ncompared to last week.",15,sub);
        LinearLayout.LayoutParams dp2=new LinearLayout.LayoutParams(-1,-2);dp2.topMargin=dp(8);copy.addView(description,dp2);
        card.addView(copy,new LinearLayout.LayoutParams(0,-2,1));LinearLayout side=col();side.setGravity(Gravity.CENTER_VERTICAL);
        CareArtwork wave=icon("Wave",50);wave.setBackground(shape(elevated,30));side.addView(wave);card.addView(side);
        card.setContentDescription("Illustrative insights preview. No health analysis is connected.");
        card.setOnClickListener(v->preview("These wellness rings and insights are reference examples. Open Care History to view your saved entries."));
        add(card,150);
    }
    private void logCare(){spacer(4);content.setBackground(shape(surface,28,true));
        View handle=new View(a);handle.setBackground(shape(sub,4));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(40),dp(4));
        hp.gravity=Gravity.CENTER_HORIZONTAL;hp.bottomMargin=dp(4);content.addView(handle,hp);
        LinearLayout heading=row();heading.addView(action("Close","Close log care",()->onBack.run()));
        TextView text=title("Log Care",27);text.setGravity(Gravity.CENTER);heading.addView(text,new LinearLayout.LayoutParams(0,dp(52),1));
        heading.addView(new View(a),new LinearLayout.LayoutParams(dp(48),1));add(heading,56,12);
        int circleSize=Math.min(116,(int)((a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density-44-48)/2));
        String[] categories={"Feeding","Medication","Water","Playtime","Vet Visit","Grooming"};
        for(int i=0;i<6;i+=2){LinearLayout r=row();r.setGravity(Gravity.CENTER);
            for(int j=i;j<i+2;j++){String category=categories[j];LinearLayout cell=col();cell.setGravity(Gravity.CENTER);
                cell.setBackground(shape(night?color("#201E29"):elevated,80,true));cell.setContentDescription("Log "+category);
                cell.setOnClickListener(v->go.accept("Quick Log:"+category));cell.addView(icon(category,43));
                LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(-2,-2);labelParams.topMargin=dp(9);
                cell.addView(text(category,14,ink,800),labelParams);
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(circleSize),dp(circleSize));cp.setMargins(dp(12),0,dp(12),0);r.addView(cell,cp);}
            add(r,circleSize,12);}
        spacer(6);LinearLayout voice=col();voice.setGravity(Gravity.CENTER);voice.setBackground(shape(elevated,30,true));pad(voice,14,20);
        TextView voiceTitle=title("Quick Log with Voice",21);voiceTitle.setGravity(Gravity.CENTER);voiceTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);voice.addView(voiceTitle);
        LinearLayout.LayoutParams vlp=new LinearLayout.LayoutParams(-2,-2);vlp.topMargin=dp(5);voice.addView(text("Just say what happened.",14,sub),vlp);
        LinearLayout wave=row();wave.setGravity(Gravity.CENTER);wave.addView(icon("Wave",49));
        FrameLayout mic=new FrameLayout(a);mic.setBackground(shape(lavender,60));mic.setContentDescription("Open voice logging");mic.setOnClickListener(v->go.accept("Voice Log"));
        mic.addView(icon("Mic",53,color("#443057")),new FrameLayout.LayoutParams(dp(53),dp(53),Gravity.CENTER));
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(100),dp(100));mp.setMargins(dp(20),0,dp(20),0);wave.addView(mic,mp);wave.addView(icon("Wave",49));
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(-1,dp(100));wp.topMargin=dp(18);voice.addView(wave,wp);add(voice,230);
    }
    private void timelineEntry(int index,int color,boolean last){LinearLayout r=row();
        View rail=new View(a){final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c){paint.setColor(color);paint.setAlpha(130);paint.setStrokeWidth(dp(2));
                c.drawLine(getWidth()/2f,0,getWidth()/2f,getHeight(),paint);paint.setAlpha(255);
                c.drawCircle(getWidth()/2f,getHeight()*.38f,dp(9),paint);}};
        r.addView(rail,new LinearLayout.LayoutParams(dp(25),-1));
        LinearLayout.LayoutParams task=new LinearLayout.LayoutParams(0,dp(84),1);task.leftMargin=dp(8);task.bottomMargin=dp(last?0:7);
        r.addView(careRow(index,true),task);add(r,last?84:91);
    }
    private void timeline(){header("Care Timeline");week(true);add(text("Today, Sep 25, 2026",17,primary,600,true),28,8);
        timelineEntry(0,mint,false);timelineEntry(1,yellow,false);timelineEntry(2,pink,false);timelineEntry(3,yellow,true);
        spacer(22);add(text("Tomorrow, Sep 26, 2026",17,primary,600,true),28,8);timelineEntry(4,lavender,true);flexible(8);}
    private View infoRow(String kind,String label,String value,Runnable callback){LinearLayout r=row();pad(r,18,6);r.setOnClickListener(v->callback.run());r.setFocusable(true);
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(37),dp(38));iconParams.rightMargin=dp(16);r.addView(icon(kind,34),iconParams);
        LinearLayout labels=col();labels.addView(text(label,16,ink,800));
        if(value!=null){LinearLayout.LayoutParams detail=new LinearLayout.LayoutParams(-1,-2);detail.topMargin=dp(3);labels.addView(text(value,14,sub),detail);}
        r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));r.addView(icon("Chevron",22,sub));return r;}
    private void sitter(){header("• Sitter Mode");ImageView illustration=new ImageView(a);
        illustration.setImageResource(R.drawable.sitter_friends);illustration.setScaleType(ImageView.ScaleType.FIT_CENTER);
        illustration.setContentDescription("Illustration of a cat and dog together");add(illustration,120,12);
        TextView introduction=text("Share a link with your sitter so they can\nsee exactly what to feed and when —\nno app, no login.",15,sub,600);
        introduction.setGravity(Gravity.CENTER);add(introduction,68,18);
        LinearLayout box=col();box.setBackground(shape(surface,22,true));pad(box,18,14);
        LinearLayout heading=row();heading.addView(icon("Link",30),new LinearLayout.LayoutParams(dp(38),dp(38)));
        heading.addView(title("Sitter Link",21),new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(a);toggle.setChecked(true);toggle.setShowText(false);toggle.setSwitchMinWidth(dp(52));
        GradientDrawable thumb=shape(Color.WHITE,20);thumb.setSize(dp(28),dp(28));toggle.setThumbDrawable(thumb);
        GradientDrawable track=shape(lavender,20);track.setSize(dp(52),dp(30));toggle.setTrackDrawable(track);
        toggle.setThumbTintList(ColorStateList.valueOf(Color.WHITE));toggle.setTrackTintList(ColorStateList.valueOf(lavender));
        toggle.setContentDescription("Sitter link preview switch");toggle.setOnCheckedChangeListener((button,checked)->
                preview("Sitter sharing is a frontend preview. No public link or access permissions have been created."));
        heading.addView(toggle);box.addView(heading);
        LinearLayout url=row();url.setBackground(shape(bg,16,true));pad(url,14,8);
        url.addView(text("purrfolio.app/sit/mochi",15,sub,700),new LinearLayout.LayoutParams(0,-2,1));
        url.addView(action("More","Sitter link details",()->preview("Example URL only. Public sitter links are not connected.")));
        LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(-1,dp(50));up.topMargin=dp(8);box.addView(url,up);
        LinearLayout.LayoutParams hint=new LinearLayout.LayoutParams(-1,dp(32));hint.topMargin=dp(10);
        box.addView(text("Preview link · sharing not connected",13,sub),hint);
        box.addView(button("Copy Link",false,()->preview("This is a sample link. Copying a working sitter invitation requires account and access-control integration.")),
                new LinearLayout.LayoutParams(-1,dp(50)));add(box,234,10);
        LinearLayout list=col();list.setBackground(shape(surface,22));pad(list,0,2);
        String[][] rows={{"Eye","What they can see","Care Circle Access"},{"Feeding","Feeding instructions","Edit Pet Profile"},
                {"Medication","Medications","Pet Passport"},{"Notes","Important notes","Edit Pet Profile"}};
        for(int i=0;i<rows.length;i++){String[] item=rows[i];list.addView(infoRow(item[0],item[1],null,()->go.accept(item[2])),new LinearLayout.LayoutParams(-1,dp(52)));
            if(i<rows.length-1){View divider=new View(a);divider.setBackgroundColor(outline);list.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}}
        add(list);flexible(6);
    }
    private String petSummary(){String species=pet.optString("species","Pet");int age=-1;
        try{Calendar born=Calendar.getInstance();born.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(pet.optString("birthday")));
            Calendar now=Calendar.getInstance();age=now.get(Calendar.YEAR)-born.get(Calendar.YEAR)-
                    (now.get(Calendar.DAY_OF_YEAR)<born.get(Calendar.DAY_OF_YEAR)?1:0);}catch(Exception ignored){}
        return age>=0?species+" · "+age+" years old":species;
    }
    private void passport(){header("Pet Passport","Share");LinearLayout identity=row();identity.setBackground(shape(surface,22,true));pad(identity,15,12);
        LinearLayout.LayoutParams photoParams=new LinearLayout.LayoutParams(dp(76),dp(76));photoParams.rightMargin=dp(16);identity.addView(portrait(76),photoParams);
        LinearLayout labels=col();labels.addView(title(name(),29));LinearLayout.LayoutParams summary=new LinearLayout.LayoutParams(-1,-2);summary.topMargin=dp(7);
        labels.addView(text(petSummary(),14,sub,600),summary);identity.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        identity.addView(action("Edit","Edit pet profile",()->go.accept("Edit Pet Profile")));add(identity,108,12);
        LinearLayout list=col();list.setBackground(shape(surface,22));
        String[][] rows={{"Vaccinations","Vaccinations","No records added"},{"Medication","Medications","Not provided"},
                {"Allergies","Allergies","Not provided"},{"Vet Visit","Veterinarian","Not provided"},
                {"Emergency","Emergency Contact","Not provided"},{"Microchip","Microchip","Not provided"},
                {"Weight","Weight","Not provided"}};
        for(int i=0;i<rows.length;i++){String[] item=rows[i];String target=i==0?"Vaccination Card":"Edit Pet Profile";
            list.addView(infoRow(item[0],item[1],item[2],()->go.accept(target)),new LinearLayout.LayoutParams(-1,dp(66)));
            if(i<rows.length-1){View divider=new View(a);divider.setBackgroundColor(outline);list.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}}
        add(list,-2,12);
        LinearLayout share=row();share.setGravity(Gravity.CENTER);share.setBackground(shape(lavender,36));share.setOnClickListener(v->go.accept("Pet ID QR"));
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(28),dp(28));iconParams.rightMargin=dp(12);
        share.addView(icon("QR",28,color("#201528")),iconParams);share.addView(text("Share Passport",18,color("#201528"),700));
        add(share,58);flexible(0);
    }
}
