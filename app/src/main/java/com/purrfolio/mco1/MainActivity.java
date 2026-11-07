package com.purrfolio.mco1;

import android.Manifest;
import com.purrfolio.mco1.care.CareController;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.purrfolio.mco1.databinding.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/** Coordinates screen navigation, local state, and XML-backed screen controllers. */
public class MainActivity extends AppCompatActivity {
    private static final class SafeJson extends JSONObject {
        SafeJson() { super(); }
        SafeJson(String source) { super(); try { JSONObject parsed=new JSONObject(source);
            java.util.Iterator<String> keys=parsed.keys();while(keys.hasNext()){String key=keys.next();put(key,parsed.opt(key));}
        } catch(JSONException error){throw new IllegalStateException(error);} }
        @Override public SafeJson put(String key,Object value) {
            try { super.put(key,value);return this; } catch(JSONException error){throw new IllegalStateException(error);}
        }
    }
    private static JSONObject put(JSONObject object,String key,Object value){try{return object.put(key,value);}
        catch(JSONException error){throw new IllegalStateException(error);}}
    private String screen="Welcome",pendingCareCategory="Feeding",petPhotoPickerTarget="Pet Detail";
    private final ArrayDeque<String> screenHistory=new ArrayDeque<>();
    private int petIndex;
    private SharedPreferences preferences;
    private final List<JSONObject> pets=new ArrayList<>(),logs=new ArrayList<>(),reminders=new ArrayList<>(),legacyLogs=new ArrayList<>();
    private CareController care;
    private boolean night;
    private String pendingNewPetPhotoUri;
    private final ActivityResultLauncher<Intent> speechResult=registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),result->{
                ArrayList<String> words=result.getData()==null?null:result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if(words!=null&&!words.isEmpty())captureVoice(words.get(0));else note("No speech was recognized.");
            });
    private final ActivityResultLauncher<String> audioPermission=registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),granted->{
                if(granted)startSoundRecording();else note("Microphone permission is needed to record audio.");
            });
    private final ActivityResultLauncher<String[]> petPhotoPicker=registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),uri->{
                if(uri==null)return;
                try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
                if("Add Your Pet".equals(petPhotoPickerTarget))pendingNewPetPhotoUri=uri.toString();
                else{put(selectedPet(),"photoUri",uri.toString());save();}
                render(petPhotoPickerTarget);
            });

    @Override protected void onCreate(Bundle state){
        AppTheme.applySaved(this);super.onCreate(state);
        preferences=getSharedPreferences("purrfolio_demo",MODE_PRIVATE);
        night=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        restore();
        care=new CareController(this,new CareController.Host(){
            public JSONObject selectedPet(){MainActivity.this.petId(MainActivity.this.selectedPet());return MainActivity.this.selectedPet();}
            public List<JSONObject> pets(){return pets;}
            public void go(String destination){render(destination);}
            public void refreshCareScreens(){if(care!=null&&!isFinishing()&&!isDestroyed()&&
                    (screen.equals("Care History")||screen.equals("Sound Journal")))render(screen,false);}
            public void header(LegacyScreenHeaderBinding b,String title){bindLegacyHeader(b,title);}
            public void dock(FloatingBottomNavBinding b,String destination){bindCareDock(b,destination);}
            public void insets(View root){applyLegacyInsets(root);}
            public void persistDeviceState(){save();}
            public void requestMicrophone(){audioPermission.launch(Manifest.permission.RECORD_AUDIO);}
        },logs,legacyLogs);
        if(state!=null){ArrayList<String> history=state.getStringArrayList("screenHistory");if(history!=null)screenHistory.addAll(history);
            pendingNewPetPhotoUri=state.getString("pendingNewPetPhotoUri");
            petPhotoPickerTarget=state.getString("petPhotoPickerTarget","Pet Detail");}
        String destination=state==null?null:state.getString("screen");
        if(destination==null)destination=(getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0?
                getIntent().getStringExtra("preview_screen"):null;
        render(destination==null?"Welcome":destination,false);
    }
    @Override protected void onSaveInstanceState(Bundle out){
        out.putString("screen",screen);out.putStringArrayList("screenHistory",new ArrayList<>(screenHistory));
        out.putString("pendingNewPetPhotoUri",pendingNewPetPhotoUri);
        out.putString("petPhotoPickerTarget",petPhotoPickerTarget);super.onSaveInstanceState(out);
    }
    private JSONObject selectedPet(){return pets.get(Math.min(Math.max(petIndex,0),pets.size()-1));}
    private String petId(JSONObject pet){if(!pet.has("id")){put(pet,"id",UUID.randomUUID().toString());save();}return pet.optString("id");}
    private String entryPetId(JSONObject entry){String id=entry.optString("petId");if(!id.isEmpty())return id;
        for(JSONObject pet:pets)if(pet.optString("name").equals(entry.optString("pet")))return petId(pet);return "";}
    private String petName(){return selectedPet().optString("name","Mochi");}
    private void note(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private void editPetPhoto(boolean addingPet){
        String target=addingPet?"Add Your Pet":screen;
        String photoUri=addingPet?pendingNewPetPhotoUri:selectedPet().optString("photoUri");
        boolean hasPhoto=photoUri!=null&&!photoUri.isEmpty();
        String[] options=hasPhoto?new String[]{"Choose from device","Remove photo","Cancel"}:
                new String[]{"Choose from device","Cancel"};
        new AlertDialog.Builder(this).setTitle(addingPet?"Pet photo":petName()+"'s photo").setItems(options,(dialog,index)->{
            if(index==0){petPhotoPickerTarget=target;petPhotoPicker.launch(new String[]{"image/*"});}
            else if(index==1&&hasPhoto){try{getContentResolver().releasePersistableUriPermission(Uri.parse(photoUri),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
                if(addingPet)pendingNewPetPhotoUri=null;else{selectedPet().remove("photoUri");save();}render(target);}
        }).show();
    }
    private void navigateBack(){if(!screenHistory.isEmpty()){render(screenHistory.removeLast(),false);return;}
        String target;
        switch(screen){
            case "Create Account":case "Log In":case "Reset Password":target="Welcome";break;
            case "Pet Detail":case "Edit Pet Profile":case "Add Your Pet":target="Pets";break;
            case "Quick Log":case "Voice Log":case "Sound Journal":case "Care History":target="Care";break;
            case "Paperless Inbox":case "Pet Timeline":case "Pet Passport":case "Browse Records":case "Vaccination Card":target="Records";break;
            case "Personal Information":case "Account Settings":case "Notifications":target="Profile";break;
            case "App Theme":target="Account Settings";break;
            default:target="More";
        }render(target,false);
    }
    private void render(String destination){render(destination,true);}
    private void render(String destination,boolean recordHistory){
        if(destination.startsWith("Quick Log:")){pendingCareCategory=destination.substring("Quick Log:".length());render("Quick Log",recordHistory);return;}
        if("Add Your Pet".equals(screen)&&!"Add Your Pet".equals(destination))pendingNewPetPhotoUri=null;
        if(recordHistory&&!destination.equals(screen))screenHistory.addLast(screen);screen=destination;
        if(care!=null)care.watch();
        switch(destination){case "Welcome":showWelcome();return;case "App Theme":showAppTheme();return;
            case "Quick Log":showQuickLog();return;case "Care History":showCareHistory();return;}
        if(ReferenceScreens.destinations.contains(destination)){
            View view=new ReferenceScreens(this,selectedPet(),target->{render(target);},
                    ()->{choosePet();},()->{editPetPhoto(false);},
                    ()->{navigateBack();}).build(destination);
            setContentView(view);ViewCompat.requestApplyInsets(view);return;
        }
        if(PhaseTwoScreens.destinations.contains(destination)){
            View view=new PhaseTwoScreens(this,selectedPet(),pets,logs,reminders,
                    target->{render(target);},()->{choosePet();},
                    index->{petIndex=index;render("Pet Detail");},
                    (values,adding)->{JSONObject updated=adding?new SafeJson():new SafeJson(selectedPet().toString());
                        for(String key:new String[]{"name","species","breed","birthday","sex","notes","photoUri"})
                            if(values.containsKey(key))put(updated,key,values.get(key));
                        if(adding){pets.add(updated);petIndex=pets.size()-1;}else{put(updated,"id",petId(selectedPet()));pets.set(petIndex,updated);}
                        save();note("Pet profile saved on this device.");render("Pet Detail");},
                    (kind,detail)->care.saveText(kind,detail),
                    ()->{recognizeVoice();},transcript->{captureVoice(transcript);},
                    ()->{toggleSoundRecording();},this::soundRows,
                    entry->{playRecording(entry);},()->{addReminder();},
                    reminder->{put(reminder,"done",!reminder.optBoolean("done"));save();render("Reminders");},
                    ()->{render("Home");},()->{navigateBack();},
                    pendingNewPetPhotoUri,adding->{editPetPhoto(adding);}).build(destination);
            setContentView(view);ViewCompat.requestApplyInsets(view);return;
        }
        render("More");
    }
    private void applyLegacyInsets(View root){
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        getWindow().setStatusBarColor(getColor(R.color.background));getWindow().setNavigationBarColor(getColor(R.color.background));
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(!night);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(!night);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|
                    WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});
        ViewCompat.requestApplyInsets(root);
    }
    private void bindLegacyHeader(LegacyScreenHeaderBinding binding,String title){
        binding.screenTitle.setText(title);binding.petChooser.setText(petName()+" ⌄");
        BrandType.apply(binding.backButton,true,600);BrandType.apply(binding.screenTitle,true,600);
        BrandType.apply(binding.petChooser,false,700);
        binding.backButton.setOnClickListener(v->navigateBack());binding.petChooser.setOnClickListener(v->choosePet());
    }
    private void bindCareDock(FloatingBottomNavBinding binding,String screen){
        new ReferenceScreens(this,selectedPet(),target->{render(target);},
                ()->{choosePet();},()->{editPetPhoto(false);},
                ()->{navigateBack();}).bindDock(binding,screen);
    }
    private void showQuickLog(){care.category(pendingCareCategory);care.quickLog();}
    private void showCareHistory(){care.history();}
    private void recognizeVoice(){
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Describe your pet's care");
        try{speechResult.launch(intent);}catch(Exception error){note("Speech recognition is unavailable on this device.");}
    }
    private void captureVoice(String transcript){care.captureVoice(transcript);}
    private List<JSONObject> soundRows(){return care.sounds();}
    private void toggleSoundRecording(){care.toggleRecording();}
    private void startSoundRecording(){care.startRecording();}
    private void playRecording(JSONObject entry){care.play(entry);}
    @Override protected void onStart(){super.onStart();if(care!=null)care.onStart();}
    @Override protected void onStop(){if(care!=null)care.onStop();super.onStop();}
    @Override protected void onDestroy(){if(care!=null)care.onDestroy();super.onDestroy();}
    private void setSystemBars(int color){WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        getWindow().setStatusBarColor(getColor(color));getWindow().setNavigationBarColor(getColor(color));
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(!night);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(!night);}
    private void showWelcome(){setSystemBars(R.color.welcome_background);
        ScreenWelcomeBinding binding=ScreenWelcomeBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        if(night)binding.welcomeLogo.clearColorFilter();
        else binding.welcomeLogo.setColorFilter(getColor(R.color.welcome_primary),PorterDuff.Mode.SRC_IN);
        ViewCompat.setOnApplyWindowInsetsListener(binding.welcomeRoot,(view,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|
                    WindowInsetsCompat.Type.displayCutout());view.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});
        ViewCompat.requestApplyInsets(binding.welcomeRoot);
        BrandType.apply(binding.getStartedButton,false,700);BrandType.apply(binding.logInButton,false,700);
        BrandType.apply(binding.continueWithLabel,false,400);
        binding.getStartedButton.setOnClickListener(v->render("Create Account"));
        binding.logInButton.setOnClickListener(v->render("Log In"));
        social(binding.googleButton,"Google");social(binding.appleButton,"Apple");social(binding.facebookButton,"Facebook");
    }
    private void social(View view,String provider){view.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("Frontend preview").setMessage(provider+" sign-in requires authentication integration.")
            .setPositiveButton("OK",null).show());}
    private void showAppTheme(){setSystemBars(R.color.welcome_background);
        ScreenAppThemeBinding binding=ScreenAppThemeBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.themeRoot,(view,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|
                    WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});
        ViewCompat.requestApplyInsets(binding.themeRoot);
        BrandType.apply(binding.themeHeading,true,600);BrandType.apply(binding.themeSubtitle,false,400);
        binding.themeBack.setOnClickListener(v->navigateBack());
        String selected=AppTheme.selected(this);
        themeOption(binding.lightOption,selected,AppTheme.LIGHT,"Light Mode","Always use PURRFOLIO's light theme.");
        themeOption(binding.darkOption,selected,AppTheme.DARK,"Dark Mode","Always use PURRFOLIO's dark theme.");
        themeOption(binding.systemOption,selected,AppTheme.SYSTEM,"System Default","Follow your device appearance.");
        FloatingBottomNavBinding nav=binding.bottomNav;
        nav.navHome.setOnClickListener(v->render("Home"));nav.navPets.setOnClickListener(v->render("Pets"));
        nav.navLogCare.setOnClickListener(v->render("Care"));nav.navCare.setOnClickListener(v->render("Pet Timeline"));
        nav.navMore.setOnClickListener(v->render("More"));
        for(View cell:new View[]{nav.navHome,nav.navPets,nav.navCare,nav.navMore})
            if(cell instanceof android.view.ViewGroup&&((android.view.ViewGroup)cell).getChildAt(1) instanceof TextView)
                BrandType.apply((TextView)((android.view.ViewGroup)cell).getChildAt(1),false,600);
    }
    private void themeOption(ThemeOptionBinding view,String selected,String choice,String title,String description){
        boolean checked=selected.equals(choice);view.optionTitle.setText(title);view.optionDescription.setText(description);
        BrandType.apply(view.optionTitle,false,700);BrandType.apply(view.optionDescription,false,400);
        view.getRoot().setBackgroundResource(checked?R.drawable.theme_option_selected:R.drawable.theme_option_normal);
        view.optionTitle.setTextColor(getColor(checked?R.color.screen_primary:R.color.screen_text));
        view.optionRadio.setChecked(checked);
        view.getRoot().setContentDescription(title+", "+(checked?"selected":"not selected")+". "+description);
        view.getRoot().setOnClickListener(v->{if(!checked){AppTheme.select(this,choice);render("App Theme");}});
    }
    private void choosePet(){String[] names=new String[pets.size()];for(int i=0;i<pets.size();i++)names[i]=pets.get(i).optString("name");
        new AlertDialog.Builder(this).setTitle("Choose a pet").setItems(names,(d,index)->{petIndex=index;render(screen);})
                .setPositiveButton("Add pet",(d,w)->render("Add Your Pet"))
                .setNegativeButton("Cancel",null).show();}
    private void addReminder(){DialogAddReminderBinding binding=DialogAddReminderBinding.inflate(getLayoutInflater());
        new AlertDialog.Builder(this).setTitle("New care reminder").setView(binding.getRoot())
                .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
                    String title=binding.reminderTitle.getText().toString().trim();
                    String when=binding.reminderWhen.getText().toString().trim();
                    if(title.isEmpty()||when.isEmpty()){note("Both fields are required");return;}
                    reminders.add(0,new SafeJson().put("title",title).put("when",when).put("pet",petName()).put("done",Boolean.FALSE));
                    save();render("Reminders");}).show();}
    private void pull(String key,List<JSONObject> output)throws Exception{
        JSONArray array=new JSONArray(preferences.getString(key,"[]"));
        for(int i=0;i<array.length();i++)output.add(array.getJSONObject(i));}
    private void restore(){try{pull("pets",pets);pull("logs",legacyLogs);pull("reminders",reminders);}
        catch(Exception error){pets.clear();legacyLogs.clear();reminders.clear();}
        for(JSONObject pet:pets)if(!pet.has("id"))put(pet,"id",UUID.randomUUID().toString());
        for(JSONObject entry:legacyLogs){if(!entry.has("id"))put(entry,"id",UUID.randomUUID().toString());
            if(!entry.has("petId"))for(JSONObject pet:pets)if(pet.optString("name").equals(entry.optString("pet"))){
                put(entry,"petId",pet.optString("id"));break;}}
        if(!pets.isEmpty()||!legacyLogs.isEmpty())save();
        if(pets.isEmpty()){
            pets.add(new SafeJson().put("name","Mochi").put("species","Cat").put("breed","Mixed Breed")
                    .put("birthday","2024-09-26").put("sex","Female").put("notes","Playful, loves treats and sunny spots."));
            pets.add(new SafeJson().put("name","Bailey").put("species","Dog").put("breed","Golden Retriever")
                    .put("birthday","2022-05-13").put("sex","Male").put("notes","Enjoys long walks."));save();}}
    private String json(List<JSONObject> items){JSONArray array=new JSONArray();for(JSONObject item:items)array.put(item);return array.toString();}
    private void save(){if(preferences==null)return;preferences.edit().putString("pets",json(pets)).putString("logs",json(legacyLogs))
            .putString("reminders",json(reminders)).apply();}
    @Override public void onBackPressed(){if(screenHistory.isEmpty()&&(screen.equals("Home")||screen.equals("Welcome")))super.onBackPressed();
        else navigateBack();}
}
