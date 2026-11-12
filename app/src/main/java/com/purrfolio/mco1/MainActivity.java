package com.purrfolio.mco1;

import android.Manifest;
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

import com.purrfolio.mco1.databinding.ScreenCareCalendarBinding;
import com.purrfolio.mco1.databinding.ScreenDocumentsBinding;
import com.purrfolio.mco1.databinding.ScreenRemindersBinding;

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
    private final List<JSONObject> pets=new ArrayList<>(),logs=new ArrayList<>(),reminders=new ArrayList<>();
    private boolean night;
    private MediaRecorder recorder;
    private File recordingFile;
    private long recordingStartedAt;
    private MediaPlayer player;
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
        requestNotificationPermission();
        BackendHub.get().start(getApplicationContext(), petRefs());
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
    private SafeJson newLog(JSONObject pet,String kind,String detail){return newLog(pet,kind,detail,"text",System.currentTimeMillis());}
    private SafeJson newLog(JSONObject pet,String kind,String detail,String source){return newLog(pet,kind,detail,source,System.currentTimeMillis());}
    private SafeJson newLog(JSONObject pet,String kind,String detail,String source,long occurredAt){return new SafeJson()
            .put("id",UUID.randomUUID().toString()).put("petId",petId(pet)).put("pet",pet.optString("name"))
            .put("kind",kind).put("text",detail).put("source",source).put("occurredAt",Long.valueOf(occurredAt))
            .put("date",new SimpleDateFormat("MMM d, yyyy · h:mm a",Locale.getDefault()).format(new Date(occurredAt)));}
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
        switch(destination){case "Welcome":showWelcome();return;case "App Theme":showAppTheme();return;
            case "Quick Log":showQuickLog();return;case "Care History":showCareHistory();return;
            case "Reminders":showReminders();return; case "Calendar":showCareCalendar();return;
            case "Browse Records":showDocuments();return;}
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
                    (kind,detail)->{logs.add(0,newLog(selectedPet(),kind,detail));save();note("Care log saved on this device.");render("Care History");},
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
    private void showQuickLog(){
        ScreenQuickLogBinding binding=ScreenQuickLogBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        applyLegacyInsets(binding.quickLogRoot);bindLegacyHeader(binding.header,"Quick Log");bindCareDock(binding.bottomNav,"Quick Log");
        BrandType.apply(binding.eyebrow,false,700);BrandType.apply(binding.pageTitle,true,600);
        BrandType.apply(binding.pageSubtitle,false,400);BrandType.apply(binding.activityLabel,false,700);
        BrandType.apply(binding.saveButton,false,700);binding.pageSubtitle.setText("A quick record for "+petName()+".");
        List<String> categories=Arrays.asList("Feeding","Medication","Vitamins","Water","Playtime","Grooming","Vet Visit","Other");
        binding.categorySpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,categories));
        binding.categorySpinner.setSelection(Math.max(0,categories.indexOf(pendingCareCategory)));
        binding.saveButton.setOnClickListener(v->{String detail=binding.detailInput.getText().toString().trim();
            if(detail.isEmpty())note("Add a short care note first.");
            else{logs.add(0,newLog(selectedPet(),categories.get(binding.categorySpinner.getSelectedItemPosition()),detail));
                save();note("Care log saved on this device.");render("Care History");}});
    }
    private void showCareHistory(){
        ScreenCareHistoryBinding binding=ScreenCareHistoryBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        applyLegacyInsets(binding.careHistoryRoot);bindLegacyHeader(binding.header,"Care History");bindCareDock(binding.bottomNav,"Care History");
        BrandType.apply(binding.eyebrow,false,700);BrandType.apply(binding.pageTitle,true,600);
        BrandType.apply(binding.pageSubtitle,false,400);BrandType.apply(binding.newEntryButton,false,700);
        binding.pageSubtitle.setText("your saved care moments");
        String id=petId(selectedPet());List<JSONObject> filtered=new ArrayList<>();
        for(JSONObject entry:logs)if(entryPetId(entry).equals(id))filtered.add(entry);
        filtered.sort(Comparator.comparingLong((JSONObject e)->e.optLong("occurredAt",0)).reversed());
        if(filtered.isEmpty()){
            CareHistoryEmptyBinding empty=CareHistoryEmptyBinding.inflate(getLayoutInflater(),binding.entryContainer,false);
            BrandType.apply(empty.emptyTitle,false,700);BrandType.apply(empty.emptyDescription,false,400);
            binding.entryContainer.addView(empty.getRoot());
        }
        for(JSONObject entry:filtered){
            CareHistoryEntryBinding item=CareHistoryEntryBinding.inflate(getLayoutInflater(),binding.entryContainer,false);
            item.entryTitle.setText(entry.optString("kind"));
            item.entryDescription.setText(entry.optString("date")+" • "+entry.optString("text"));
            BrandType.apply(item.entryIcon,true,600);BrandType.apply(item.entryTitle,false,700);
            BrandType.apply(item.entryDescription,false,400);
            item.getRoot().setOnClickListener(v->{String[] actions=entry.optString("audioFile").isEmpty()?
                    new String[]{"Edit","Delete"}:new String[]{"Play recording","Edit","Delete"};
                new AlertDialog.Builder(this).setTitle(entry.optString("kind")).setMessage(entry.optString("text"))
                        .setItems(actions,(d,index)->{switch(actions[index]){case "Play recording":playRecording(entry);break;
                            case "Edit":editLog(entry);break;case "Delete":confirmDelete(entry);break;}})
                        .setNegativeButton("Close",null).show();});
            binding.entryContainer.addView(item.getRoot());
        }
        binding.newEntryButton.setOnClickListener(v->render("Quick Log"));
    }
    private void editLog(JSONObject entry){
        DialogEditCareEntryBinding binding=DialogEditCareEntryBinding.inflate(getLayoutInflater());
        String[] kinds={"Feeding","Medication","Vitamins","Water","Playtime","Grooming","Vet Visit","Other","Observation"};
        binding.category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,kinds));
        binding.category.setSelection(Math.max(0,Arrays.asList(kinds).indexOf(entry.optString("kind"))));
        binding.detail.setText(entry.optString("text"));
        new AlertDialog.Builder(this).setTitle("Edit care entry").setView(binding.getRoot())
                .setPositiveButton("Save",(d,w)->{String value=binding.detail.getText().toString().trim();
                    if(value.isEmpty()){note("Add a short care note first.");return;}
                    put(put(entry,"kind",binding.category.getSelectedItem().toString()),"text",value);
                    save();render("Care History");}).setNegativeButton("Cancel",null).show();
    }
    private void confirmDelete(JSONObject entry){
        new AlertDialog.Builder(this).setTitle("Delete care entry?")
                .setMessage("This removes the entry and its audio from this device.")
                .setPositiveButton("Delete",(d,w)->{String audio=entry.optString("audioFile");
                    if(!audio.isEmpty())new File(getFilesDir(),audio).delete();
                    logs.remove(entry);save();render("Care History");}).setNegativeButton("Cancel",null).show();
    }
    private void recognizeVoice(){
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Describe your pet's care");
        try{speechResult.launch(intent);}catch(Exception error){note("Speech recognition is unavailable on this device.");}
    }
    private void captureVoice(String transcript){
        List<String> petNames=new ArrayList<>();for(JSONObject pet:pets)petNames.add(pet.optString("name"));
        CareCapture.Draft draft=CareCapture.parse(transcript,petNames);
        DialogConfirmVoiceCareBinding binding=DialogConfirmVoiceCareBinding.inflate(getLayoutInflater());
        binding.transcript.setText("Heard: "+transcript);
        List<String> names=new ArrayList<>();names.add("Select pet");names.addAll(petNames);
        binding.pet.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        binding.pet.setSelection(Math.max(0,names.indexOf(draft.getPet())));
        String[] kinds={"Select activity","Feeding","Medication","Vitamins","Water","Playtime","Grooming","Vet Visit","Other"};
        binding.activity.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,kinds));
        binding.activity.setSelection(Math.max(0,Arrays.asList(kinds).indexOf(draft.getActivity())));
        binding.amount.setText(draft.getAmount()==null?"":draft.getAmount());
        binding.time.setText(draft.getTime()==null?"":draft.getTime());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Confirm care details").setView(binding.getRoot())
                .setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(binding.pet.getSelectedItemPosition()==0){note("Choose the pet.");return;}
            if(binding.activity.getSelectedItemPosition()==0){note("Choose the activity.");return;}
            if(binding.time.getText().toString().trim().isEmpty()){binding.time.setError("Enter the time");return;}
            JSONObject pet=pets.get(binding.pet.getSelectedItemPosition()-1);
            List<String> parts=new ArrayList<>();String amount=binding.amount.getText().toString().trim();
            if(!amount.isEmpty())parts.add(amount);parts.add("at "+binding.time.getText().toString().trim());parts.add(transcript);
            logs.add(0,newLog(pet,binding.activity.getSelectedItem().toString(),String.join(" · ",parts),"voice")
                    .put("transcript",transcript));save();dialog.dismiss();note("Care log saved on this device.");render("Care History");
        }));dialog.show();
    }
    private List<JSONObject> soundRows(){List<JSONObject> result=new ArrayList<>();String id=petId(selectedPet());
        for(JSONObject entry:logs)if(entryPetId(entry).equals(id)&&!entry.optString("audioFile").isEmpty())result.add(entry);return result;}
    private void toggleSoundRecording(){if(recorder!=null){stopSoundRecording();return;}
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            audioPermission.launch(Manifest.permission.RECORD_AUDIO);else startSoundRecording();}
    private void startSoundRecording(){File file=new File(getFilesDir(),"sound_"+UUID.randomUUID()+".m4a");
        try{MediaRecorder active=new MediaRecorder();active.setAudioSource(MediaRecorder.AudioSource.MIC);
            active.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);active.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            active.setOutputFile(file.getAbsolutePath());active.prepare();active.start();recorder=active;
            recordingFile=file;recordingStartedAt=System.currentTimeMillis();
            note("Recording started. Tap the button again to stop.");
        }catch(Exception error){if(recorder!=null)recorder.release();recorder=null;file.delete();note("Could not start audio recording.");}}
    private void stopSoundRecording(){File file=recordingFile;if(file==null)return;
        long duration=System.currentTimeMillis()-recordingStartedAt;boolean success;
        try{if(recorder!=null)recorder.stop();success=true;}catch(Exception error){success=false;}
        if(recorder!=null)recorder.release();recorder=null;recordingFile=null;
        if(!success||duration<1000){file.delete();note("Record at least one second of audio.");return;}
        EditText input=new EditText(this);input.setHint("What did you hear? (e.g. coughing)");
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Save sound observation").setView(input)
                .setPositiveButton("Save",null).setNegativeButton("Discard",(d,w)->file.delete())
                .setOnCancelListener(d->file.delete()).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String detail=input.getText().toString().trim();if(detail.isEmpty()){input.setError("Describe the sound");return;}
            logs.add(0,newLog(selectedPet(),"Observation",detail,"audio",recordingStartedAt)
                    .put("audioFile",file.getName()).put("durationMs",Long.valueOf(duration)));
            save();dialog.dismiss();render("Sound Journal");}));dialog.show();
    }
    private void playRecording(JSONObject entry){String audio=entry.optString("audioFile");
        File file=new File(getFilesDir(),audio.substring(audio.lastIndexOf('/')+1));
        if(!file.isFile()){note("Recording file is missing.");return;}
        if(player!=null)player.release();
        try{player=new MediaPlayer();player.setDataSource(file.getAbsolutePath());player.prepare();
            player.setOnCompletionListener(done->{done.release();player=null;});player.start();
        }catch(Exception error){note("Could not play this recording.");}}
    @Override protected void onDestroy(){if(recorder!=null)recorder.release();
        if(recordingFile!=null)recordingFile.delete();if(player!=null)player.release();super.onDestroy();}
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
    private void restore(){try{pull("pets",pets);pull("logs",logs);pull("reminders",reminders);}
        catch(Exception error){pets.clear();logs.clear();reminders.clear();}
        for(JSONObject pet:pets)if(!pet.has("id"))put(pet,"id",UUID.randomUUID().toString());
        for(JSONObject entry:logs){if(!entry.has("id"))put(entry,"id",UUID.randomUUID().toString());
            if(!entry.has("petId"))for(JSONObject pet:pets)if(pet.optString("name").equals(entry.optString("pet"))){
                put(entry,"petId",pet.optString("id"));break;}}
        if(!pets.isEmpty()||!logs.isEmpty())save();
        if(pets.isEmpty()){
            pets.add(new SafeJson().put("name","Mochi").put("species","Cat").put("breed","Mixed Breed")
                    .put("birthday","2024-09-26").put("sex","Female").put("notes","Playful, loves treats and sunny spots."));
            pets.add(new SafeJson().put("name","Bailey").put("species","Dog").put("breed","Golden Retriever")
                    .put("birthday","2022-05-13").put("sex","Male").put("notes","Enjoys long walks."));save();}}
    private String json(List<JSONObject> items){JSONArray array=new JSONArray();for(JSONObject item:items)array.put(item);return array.toString();}
    private void save(){if(preferences==null)return;preferences.edit().putString("pets",json(pets)).putString("logs",json(logs))
            .putString("reminders",json(reminders)).apply();}
    @Override public void onBackPressed(){if(screenHistory.isEmpty()&&(screen.equals("Home")||screen.equals("Welcome")))super.onBackPressed();
        else navigateBack();}

    private List<PetRef> petRefs(){
        List<PetRef> refs=new ArrayList<>();
        for(JSONObject pet:pets)refs.add(new PetRef(petId(pet),pet.optString("name","Pet")));
        return refs;}
    private PetRef currentPet(){
        if(pets.isEmpty())return new PetRef("","Pet");
        return new PetRef(petId(selectedPet()),petName());}
    private void requestNotificationPermission(){
        if(android.os.Build.VERSION.SDK_INT>=33&&androidx.core.content.ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
            androidx.core.app.ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},4001);}}
    private void showReminders(){
        ScreenRemindersBinding binding=ScreenRemindersBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        applyLegacyInsets(binding.remindersRoot);bindLegacyHeader(binding.header,"Reminders");bindCareDock(binding.bottomNav,"Reminders");
        new RemindersScreen(this,binding,petRefs(),currentPet()).bind();}
    private void showCareCalendar(){
        ScreenCareCalendarBinding binding=ScreenCareCalendarBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        applyLegacyInsets(binding.calendarRoot);bindLegacyHeader(binding.header,"Calendar");bindCareDock(binding.bottomNav,"Calendar");
        new CareCalendarScreen(this,binding,petRefs()).bind();}
    private void showDocuments(){
        ScreenDocumentsBinding binding=ScreenDocumentsBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        applyLegacyInsets(binding.documentsRoot);bindLegacyHeader(binding.header,"Documents");bindCareDock(binding.bottomNav,"Browse Records");
        new DocumentsScreen(this,binding,currentPet()).bind();}
}
