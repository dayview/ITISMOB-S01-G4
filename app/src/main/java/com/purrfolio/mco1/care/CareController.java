package com.purrfolio.mco1.care;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.purrfolio.mco1.BrandType;
import com.purrfolio.mco1.CareCapture;
import com.purrfolio.mco1.databinding.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** Care-only Firebase UI coordinator; unrelated prototype features keep their existing behavior. */
public final class CareController {
    public interface Host {
        JSONObject selectedPet();
        List<JSONObject> pets();
        void go(String screen);
        void refreshCareScreens();
        void header(LegacyScreenHeaderBinding header,String title);
        void dock(FloatingBottomNavBinding dock,String screen);
        void insets(View root);
        void persistDeviceState();
        void requestMicrophone();
    }
    private final AppCompatActivity a;
    private final Host host;
    private final FirebaseCareServices services;
    private final FirebaseCareRepository repository;
    private final List<JSONObject> cloud,legacy;
    private final SharedPreferences drafts;
    private final String deviceId;
    private boolean signingIn;
    private final java.util.Set<String> importSkipped=new java.util.HashSet<>();
    private ListenerRegistration subscription;
    private String watchKey="",error="";
    private int limit=50;
    private boolean cached=true,loading,more,started;
    private final FirebaseAuth.AuthStateListener authListener;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private MediaRecorder recorder;
    private MediaPlayer player;
    private File recording;
    private long recordingAt;
    private JSONObject recordingPet;
    private String recordingOwner;
    private final Runnable recordingLimit=()->{if(recorder!=null){note("The 60-second recording limit was reached.");stopRecording();}};

    public CareController(AppCompatActivity activity,Host host,List<JSONObject> cloud,List<JSONObject> legacy) {
        this.a=activity;this.host=host;this.cloud=cloud;this.legacy=legacy;
        services=FirebaseCareServices.get(a);repository=new FirebaseCareRepository(services);
        drafts=a.getSharedPreferences("purrfolio_care_drafts",0);
        String installation=drafts.getString("installationId",null);
        if(installation==null){installation=UUID.randomUUID().toString();drafts.edit().putString("installationId",installation).apply();}
        deviceId=installation;
        authListener=auth->{stopPlayback();stopWatch();cloud.clear();limit=50;error="";importSkipped.clear();watch();reconcileAttachments();host.refreshCareScreens();};
    }
    public void onStart(){started=true;services.auth.addAuthStateListener(authListener);watch();}
    public void onStop(){started=false;services.auth.removeAuthStateListener(authListener);stopWatch();stopPlayback();
        if(recorder!=null)stopRecording();}
    public void onDestroy(){stopWatch();handler.removeCallbacks(recordingLimit);
        if(recorder!=null)recorder.release();if(recording!=null)recording.delete();stopPlayback();}
    private void stopPlayback(){if(player!=null){player.release();player=null;}}
    private String uid(){return services.auth.getCurrentUser()==null?"":services.auth.getCurrentUser().getUid();}
    private void stopWatch(){if(subscription!=null)subscription.remove();subscription=null;watchKey="";}
    public void watch(){
        if(!started||uid().isEmpty())return;
        String user=uid(),petId=petId(host.selectedPet()),key=user+":"+petId+":"+limit;
        if(watchKey.equals(key))return;stopWatch();cloud.clear();loading=true;error="";watchKey=key;
        subscription=repository.observe(petId,limit,new FirebaseCareRepository.Observer(){
            public void changed(List<CareRecord> records,boolean fromCache,boolean canLoadMore){
                if(!uid().equals(user)||!watchKey.equals(key))return;
                cloud.clear();for(CareRecord record:records){JSONObject json=record.toJson();put(json,"_ownerUid",user);
                    put(json,"audioAvailable",hasAudio(user,record));cloud.add(json);
                    if(record.status.equals("ready"))markSaved(user,record.id);}
                loading=false;cached=fromCache;more=canLoadMore;error="";host.refreshCareScreens();
            }
            public void failed(Exception exception){if(!uid().equals(user)||!watchKey.equals(key))return;
                loading=false;error=message(exception);host.refreshCareScreens();}
        });
    }
    private String petId(JSONObject pet){String id=pet.optString("id");if(id.isEmpty()){
        id=UUID.randomUUID().toString();put(pet,"id",id);host.persistDeviceState();}return id;}
    private void note(String message){Toast.makeText(a,message,Toast.LENGTH_LONG).show();}
    private boolean alive(){return !a.isFinishing()&&!a.isDestroyed();}
    private static void put(JSONObject object,String key,Object value){try{object.put(key,value);}catch(JSONException e){throw new IllegalStateException(e);}}
    private JSONObject newEntry(JSONObject pet,String kind,String text,String source,long at){
        JSONObject entry=new JSONObject();put(entry,"id",UUID.randomUUID().toString());put(entry,"petId",petId(pet));
        put(entry,"pet",pet.optString("name"));put(entry,"kind",kind);put(entry,"text",text.trim());put(entry,"source",source);
        put(entry,"_ownerUid",uid());put(entry,"occurredAt",at);put(entry,"durationMs",0);put(entry,"audioDeviceId",source.equals("audio")?deviceId:"");put(entry,"amount","");put(entry,"transcript","");return entry;
    }
    private CareRecord stored(JSONObject entry){
        if(!entry.optString("_ownerUid",uid()).equals(uid()))throw new IllegalStateException("This entry belongs to another care session. Reload Care History.");
        return new CareRecord(entry.optString("id"),entry.optString("petId"),entry.optString("pet"),
            entry.optString("kind"),entry.optString("text"),entry.optString("source"),entry.optString("amount"),entry.optString("transcript"),
            entry.optLong("occurredAt"),entry.optLong("durationMs"),entry.optString("audioDeviceId"),entry.optString("status","ready"),entry.optLong("revision",1));}
    private String message(Exception e){
        if(e.getCause() instanceof IllegalStateException)return e.getCause().getMessage();
        if(e instanceof FirebaseAuthException)return "Sign-in failed. Check your email and password, then retry.";
        if(e instanceof FirebaseFirestoreException){switch(((FirebaseFirestoreException)e).getCode()){
            case PERMISSION_DENIED:return "This account cannot access these care records. Sign in again, or contact the project administrator.";
            case UNAUTHENTICATED:return "Please sign in again.";
            case UNAVAILABLE:return "You're offline. Reconnect and retry; your current draft is kept.";
            case ABORTED:return "This entry changed during the request. Reload Care History and retry.";
            case FAILED_PRECONDITION:return "Care history is temporarily unavailable. Please retry shortly.";
            default:break;}}
        if(e instanceof IllegalArgumentException||e instanceof IllegalStateException)return e.getMessage()==null?"The care operation failed.":e.getMessage();
        return "The care service could not complete this request. Check your connection and retry.";
    }
    private void requireAccount(Runnable action){if(!uid().isEmpty()){action.run();return;}signIn(action);}
    private void signIn(Runnable after){if(signingIn)return;signingIn=true;DialogCareSignInBinding b=DialogCareSignInBinding.inflate(a.getLayoutInflater());
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Care account").setView(b.getRoot()).setPositiveButton("Sign in",null)
                .setNegativeButton("Cancel",null).create();
        dialog.setOnDismissListener(d->signingIn=false);
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String email=b.email.getText().toString().trim(),password=b.password.getText().toString();
            if(email.isEmpty()||password.isEmpty()){b.error.setText("Enter your existing account email and password.");return;}
            v.setEnabled(false);b.error.setText("Signing in…");
            services.auth.signInWithEmailAndPassword(email,password).addOnCompleteListener(a,task->{
                if(task.isSuccessful()){dialog.dismiss();after.run();}
                else{v.setEnabled(true);b.error.setText(message(FirebaseCareRepository.failure(task)));}});
        }));dialog.show();
    }
    private void account(){if(uid().isEmpty()){signIn(()->host.go("Care History"));return;}
        String email=services.auth.getCurrentUser().getEmail();
        new AlertDialog.Builder(a).setTitle("Care account").setMessage(email==null?"Signed in":email)
                .setPositiveButton("Sign out",(d,w)->{if(recorder!=null){note("Finish the current recording first.");return;}
                    stopPlayback();services.auth.signOut();cloud.clear();host.refreshCareScreens();})
                .setNegativeButton("Close",null).show();
    }
    public void quickLog(){ScreenQuickLogBinding b=ScreenQuickLogBinding.inflate(a.getLayoutInflater());a.setContentView(b.getRoot());
        host.insets(b.quickLogRoot);host.header(b.header,"Quick Log");host.dock(b.bottomNav,"Quick Log");
        BrandType.apply(b.eyebrow,false,700);BrandType.apply(b.pageTitle,true,600);BrandType.apply(b.pageSubtitle,false,400);
        BrandType.apply(b.activityLabel,false,700);BrandType.apply(b.saveButton,false,700);
        b.pageSubtitle.setText("A quick record for "+host.selectedPet().optString("name")+".");
        List<String> kinds=CareRecord.KINDS.subList(0,CareRecord.KINDS.size()-1);
        b.categorySpinner.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,kinds));
        b.categorySpinner.setSelection(Math.max(0,kinds.indexOf(pendingCategory)));
        b.saveButton.setOnClickListener(v->{String text=b.detailInput.getText().toString().trim();
            if(text.isEmpty()){b.detailInput.setError("Add a care note.");return;}
            JSONObject entry=newEntry(host.selectedPet(),kinds.get(b.categorySpinner.getSelectedItemPosition()),text,"text",System.currentTimeMillis());
            CareCapture.Draft parsed=CareCapture.parse(text,new ArrayList<>());put(entry,"amount",parsed.getAmount()==null?"":parsed.getAmount());
            save(entry,null,b.saveButton,()->host.go("Care History"));});
    }
    private String pendingCategory="Feeding";
    public void category(String category){pendingCategory=category;}
    public void saveText(String kind,String text){JSONObject entry=newEntry(host.selectedPet(),kind,text,"text",System.currentTimeMillis());save(entry,null,null,()->host.go("Care History"));}
    private void save(JSONObject entry,File file,View button,Runnable after){requireAccount(()->{
        final String user=uid();try{
            if(!entry.optString("_ownerUid",user).isEmpty()&&!entry.optString("_ownerUid",user).equals(user))
                throw new IllegalStateException("Your care account changed. Reopen this draft in its original account.");
            put(entry,"_ownerUid",user);
            CareRecord record=CareRecord.fromJson(entry,user);
            if(record.source.equals("audio")&&(file==null||!file.isFile()||file.length()==0||file.length()>CareRecord.MAX_AUDIO_BYTES))
                throw new IllegalArgumentException("The local recording is missing or exceeds 5 MB.");
            if(file!=null)storePending(user,entry);if(button!=null)button.setEnabled(false);
            repository.create(record).addOnCompleteListener(a,task->{
                if(button!=null)button.setEnabled(true);if(!uid().equals(user))return;
                if(task.isSuccessful()){markSaved(user,record.id);note(record.source.equals("audio")?"Observation saved. Audio stays on this device.":"Care entry saved to your account.");after.run();}
                else note(message(FirebaseCareRepository.failure(task)));
            });
        }catch(Exception e){if(button!=null)button.setEnabled(true);note(message(e));}
    });}
    private String status(){if(uid().isEmpty())return "Sign in to load and save private care records.";
        if(!error.isEmpty())return error;if(loading)return "Loading care records…";
        return cached?"Showing cached records. Reconnect to save changes.":"Care records are up to date for this account.";}
    public void history(){watch();ScreenCareHistoryBinding b=ScreenCareHistoryBinding.inflate(a.getLayoutInflater());a.setContentView(b.getRoot());
        host.insets(b.careHistoryRoot);host.header(b.header,"Care History");host.dock(b.bottomNav,"Care History");
        BrandType.apply(b.eyebrow,false,700);BrandType.apply(b.pageTitle,true,600);BrandType.apply(b.pageSubtitle,false,400);BrandType.apply(b.newEntryButton,false,700);
        b.careStatus.setText(status());b.careStatus.setOnClickListener(v->{stopWatch();watch();});
        b.careAccount.setText(uid().isEmpty()?"Sign in to care records":"Care account");b.careAccount.setOnClickListener(v->account());
        int count=0;for(JSONObject entry:legacy)if(entry.optString("petId").equals(petId(host.selectedPet())))count++;
        b.deviceEntries.setVisibility(count==0?View.GONE:View.VISIBLE);b.deviceEntries.setText("Import "+count+" device care entries");
        b.deviceEntries.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("Import device care entries?")
                .setMessage("These entries for the selected pet will be copied into the care account you sign in to. Other local data stays on this device.")
                .setPositiveButton("Import",(d,w)->requireAccount(()->{importSkipped.clear();importNext();})).setNegativeButton("Cancel",null).show());
        List<JSONObject> entries=visibleEntries();
        if(entries.isEmpty()){
            CareHistoryEmptyBinding empty=CareHistoryEmptyBinding.inflate(a.getLayoutInflater(),b.entryContainer,false);
            empty.emptyTitle.setText(uid().isEmpty()?"Sign in to view care records":"No saved care entries");
            empty.emptyDescription.setText(loading?"Loading…":"Add an entry, or import your existing device entries.");
            b.entryContainer.addView(empty.getRoot());}
        for(JSONObject entry:entries){CareHistoryEntryBinding row=CareHistoryEntryBinding.inflate(a.getLayoutInflater(),b.entryContainer,false);
            row.entryTitle.setText(entry.optString("kind"));String state=entry.optString("status","ready");
            String prefix=state.equals("ready")?"":state.equals("deleting")?"Deletion pending — tap to retry. ":"Save pending — tap to retry. ";
            if(entry.optString("source").equals("audio")&&!entry.optBoolean("audioAvailable"))prefix+="Recording is on the original device. ";
            row.entryDescription.setText(prefix+entry.optString("date",CareTime.date(entry.optLong("occurredAt")))+" • "+entry.optString("text"));
            BrandType.apply(row.entryIcon,true,600);BrandType.apply(row.entryTitle,false,700);BrandType.apply(row.entryDescription,false,400);
            row.getRoot().setOnClickListener(v->entryActions(entry));b.entryContainer.addView(row.getRoot());}
        b.loadMore.setVisibility(more?View.VISIBLE:View.GONE);b.loadMore.setOnClickListener(v->{limit+=50;watch();});
        b.newEntryButton.setOnClickListener(v->host.go("Quick Log"));
    }
    private List<JSONObject> visibleEntries(){List<JSONObject> result=new ArrayList<>(cloud);if(uid().isEmpty())return result;
        JSONObject pending=pending(uid());java.util.Iterator<String> keys=pending.keys();
        while(keys.hasNext()){String id=keys.next();JSONObject entry=pending.optJSONObject(id);if(entry==null||entry.optBoolean("_saved")||!entry.optString("petId").equals(petId(host.selectedPet())))continue;
            boolean exists=false;for(JSONObject remote:result)if(remote.optString("id").equals(id)){exists=true;break;}
            if(!exists){put(entry,"status","pending");put(entry,"revision",0);result.add(entry);}}
        result.sort((x,y)->Long.compare(y.optLong("occurredAt"),x.optLong("occurredAt")));return result;}
    private void entryActions(JSONObject entry){String status=entry.optString("status","ready");List<String> actions=new ArrayList<>();
        if(status.equals("ready")){if(entry.optString("source").equals("audio"))actions.add("Play recording");actions.add("Edit");}
        if(status.equals("pending"))actions.add("Retry save");actions.add("Delete");
        new AlertDialog.Builder(a).setTitle(entry.optString("kind")).setItems(actions.toArray(new String[0]),(d,index)->{
            switch(actions.get(index)){case "Play recording":play(entry);break;case "Edit":edit(entry);break;
                case "Retry save":JSONObject pending=pending(uid()).optJSONObject(entry.optString("id"));
                    File file=pending==null?null:local(pending.optString("audioFile"));
                    if(pending==null||file==null||!file.isFile()){note("The original recording is unavailable on this device. Delete this pending entry and record again.");break;}
                    if(pending.optString("text").trim().isEmpty())describeRecording(pending,file);
                    else save(pending,file,null,()->host.go("Care History"));break;case "Delete":confirmDelete(entry);break;}
        }).setNegativeButton("Close",null).show();
    }
    public void edit(JSONObject entry){requireAccount(()->{CareRecord original=stored(entry);
        DialogEditCareEntryBinding b=DialogEditCareEntryBinding.inflate(a.getLayoutInflater());
        b.category.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,CareRecord.KINDS));
        b.category.setSelection(CareRecord.KINDS.indexOf(original.kind));b.detail.setText(original.text);b.amount.setText(original.amount);
        b.date.setText(CareTime.date(original.occurredAt));b.time.setText(CareTime.time(original.occurredAt));
        if(original.source.equals("audio"))b.category.setEnabled(false);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Edit care entry").setView(b.getRoot()).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{long at=CareTime.parse(b.date.getText().toString(),b.time.getText().toString());String user=uid();
                Task<Void> request=repository.edit(original,b.category.getSelectedItem().toString(),b.detail.getText().toString(),b.amount.getText().toString(),at);
                v.setEnabled(false);request.addOnCompleteListener(a,task->{v.setEnabled(true);if(!uid().equals(user))return;
                    if(task.isSuccessful()){dialog.dismiss();note("Care entry updated.");host.go("Care History");}
                    else note(message(FirebaseCareRepository.failure(task)));});
            }catch(Exception e){note(message(e));}
        }));dialog.show();});}
    public void confirmDelete(JSONObject entry){new AlertDialog.Builder(a).setTitle("Delete care entry?")
        .setMessage("This removes the care entry from your account and its recording from this device. Audio files never upload to Firestore.")
        .setPositiveButton("Delete",(d,w)->requireAccount(()->delete(entry))).setNegativeButton("Cancel",null).show();}
    private void delete(JSONObject entry){String user=uid();String id=entry.optString("id");
        if(!entry.optString("_ownerUid",user).equals(user)){note("Your care account changed. Reload Care History.");return;}
        // A process may have stopped before a draft's metadata creation was acknowledged.
        repository.get(id).addOnCompleteListener(a,loaded->{if(!uid().equals(user))return;
            if(!loaded.isSuccessful()){note(message(FirebaseCareRepository.failure(loaded)));return;}
            if(loaded.getResult()==null){removePending(user,id,true);host.go("Care History");return;}
            CareRecord record=loaded.getResult();
            if(entry.optLong("revision")>0&&record.revision!=entry.optLong("revision")&&!record.status.equals("deleting")){
                note("This entry changed. Reload it before deleting.");return;}
            repository.delete(record).addOnCompleteListener(a,deleted->{if(!uid().equals(user))return;
                if(deleted.isSuccessful()){removePending(user,id,true);note("Care entry deleted.");host.go("Care History");}
                else note(message(FirebaseCareRepository.failure(deleted)));});
        });}
    private void importNext(){JSONObject next=null;String id=petId(host.selectedPet());
        for(JSONObject entry:legacy)if(entry.optString("petId").equals(id)&&!importSkipped.contains(entry.optString("id"))){next=entry;break;}
        if(next==null){note(importSkipped.isEmpty()?"Device care entries imported.":"Import finished. Skipped entries remain on this device.");host.go("Care History");return;}
        JSONObject entry=next;String user=uid();
        if(entry.optString("source","text").equals("voice")){CareCapture.Draft parsed=CareCapture.parse(entry.optString("transcript",entry.optString("text")),new ArrayList<>());
            if(entry.optString("amount").isEmpty())put(entry,"amount",parsed.getAmount()==null?"":parsed.getAmount());}
        try{
            if(entry.optString("source").equals("audio"))put(entry,"audioDeviceId",deviceId);
            CareRecord record=CareRecord.fromJson(entry,user);File audio=record.source.equals("audio")?local(entry.optString("audioFile")):null;
            if(record.source.equals("audio")){
                if(audio==null||!audio.isFile()||audio.length()==0||audio.length()>CareRecord.MAX_AUDIO_BYTES)throw new IllegalArgumentException("The device recording is missing or exceeds 5 MB.");
                protectAudioFile(audio);
                storePending(user,entry);
            }
            repository.create(record).addOnCompleteListener(a,task->{if(!uid().equals(user))return;
                if(task.isSuccessful()){markSaved(user,record.id);
                    if(record.source.equals("audio")&&new File(a.getNoBackupFilesDir(),entry.optString("audioFile")).isFile())
                        new File(a.getFilesDir(),entry.optString("audioFile")).delete();
                    legacy.remove(entry);host.persistDeviceState();importNext();}
                else note("Import stopped; remaining device entries are kept. "+message(FirebaseCareRepository.failure(task)));});
        }catch(Exception e){confirmLegacy(entry,message(e));}}
    private void confirmLegacy(JSONObject entry,String reason){
        DialogEditCareEntryBinding b=DialogEditCareEntryBinding.inflate(a.getLayoutInflater());
        b.category.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,CareRecord.KINDS));
        b.category.setSelection(Math.max(0,CareRecord.KINDS.indexOf(entry.optString("kind"))));
        b.detail.setText(entry.optString("text"));b.amount.setText(entry.optString("amount"));
        long at=entry.optLong("occurredAt",System.currentTimeMillis());b.date.setText(CareTime.date(at));b.time.setText(CareTime.time(at));
        if(entry.optString("source").equals("audio"))b.category.setEnabled(false);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Complete device entry")
                .setMessage(reason+" You can correct the details or keep this entry on the device.").setView(b.getRoot())
                .setPositiveButton("Import",null).setNegativeButton("Keep on device",(d,w)->{importSkipped.add(entry.optString("id"));importNext();}).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            put(entry,"kind",b.category.getSelectedItem().toString());put(entry,"text",b.detail.getText().toString().trim());
            put(entry,"amount",b.amount.getText().toString().trim());put(entry,"occurredAt",CareTime.parse(b.date.getText().toString(),b.time.getText().toString()));
            if(entry.optString("source").equals("voice")&&entry.optString("transcript").isEmpty())put(entry,"transcript",entry.optString("text"));
            CareRecord.fromJson(entry,uid());host.persistDeviceState();dialog.dismiss();importNext();
        }catch(Exception e){note(message(e));}}));dialog.show();
    }
    public void captureVoice(String transcript){if(transcript==null||transcript.trim().isEmpty()){note("No speech was recognized.");return;}
        List<String> petNames=new ArrayList<>();for(JSONObject pet:host.pets())petNames.add(pet.optString("name"));
        CareCapture.Draft draft=CareCapture.parse(transcript,petNames);
        DialogConfirmVoiceCareBinding b=DialogConfirmVoiceCareBinding.inflate(a.getLayoutInflater());b.transcript.setText("Heard: "+transcript);
        List<String> names=new ArrayList<>();names.add("Select pet");names.addAll(petNames);
        b.pet.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));b.pet.setSelection(Math.max(0,names.indexOf(draft.getPet())));
        List<String> kinds=new ArrayList<>();kinds.add("Select activity");kinds.addAll(CareRecord.KINDS.subList(0,CareRecord.KINDS.size()-1));
        b.activity.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,kinds));b.activity.setSelection(Math.max(0,kinds.indexOf(draft.getActivity())));
        b.amount.setText(draft.getAmount()==null?"":draft.getAmount());b.time.setText(draft.getTime()==null?"":draft.getTime());java.util.Calendar voiceDate=java.util.Calendar.getInstance();voiceDate.add(java.util.Calendar.DAY_OF_MONTH,draft.getDayOffset());b.date.setText(CareTime.date(voiceDate.getTimeInMillis()));
        Runnable prompt=()->{List<String> missing=new ArrayList<>();if(b.pet.getSelectedItemPosition()==0)missing.add("which pet");
            if(b.activity.getSelectedItemPosition()==0)missing.add("the activity");
            if(CareRecord.requiresAmount(b.activity.getSelectedItem().toString())&&b.amount.getText().toString().trim().isEmpty())missing.add("the amount");
            try{CareTime.parse(b.date.getText().toString(),b.time.getText().toString());}catch(Exception e){missing.add("the date/time including AM or PM");}
            b.missingPrompt.setText(missing.isEmpty()?"Please confirm these details before saving.":"Please tell me "+String.join(", ",missing)+".");};
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void afterTextChanged(Editable s){prompt.run();}public void onTextChanged(CharSequence s,int start,int before,int count){}};
        b.amount.addTextChangedListener(watcher);b.date.addTextChangedListener(watcher);b.time.addTextChangedListener(watcher);
        android.widget.AdapterView.OnItemSelectedListener selected=new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View view,int position,long id){prompt.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}};
        b.pet.setOnItemSelectedListener(selected);b.activity.setOnItemSelectedListener(selected);prompt.run();
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Confirm care details").setView(b.getRoot()).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(b.pet.getSelectedItemPosition()==0){note("Choose the pet.");return;}
            if(b.activity.getSelectedItemPosition()==0){note("Choose the activity.");return;}
            String kind=b.activity.getSelectedItem().toString(),amount=b.amount.getText().toString().trim();
            if(CareRecord.requiresAmount(kind)&&amount.isEmpty()){b.amount.setError("Enter the amount");b.amount.requestFocus();return;}
            try{long at=CareTime.parse(b.date.getText().toString(),b.time.getText().toString());
                String detail=(amount.isEmpty()?"":amount+" · ")+"at "+CareTime.time(at)+" · "+transcript;
                JSONObject entry=newEntry(host.pets().get(b.pet.getSelectedItemPosition()-1),kind,detail,"voice",at);
                put(entry,"amount",amount);put(entry,"transcript",transcript);
                save(entry,null,v,()->{dialog.dismiss();host.go("Care History");});
            }catch(Exception e){b.time.setError(message(e));}
        }));dialog.show();
    }
    public List<JSONObject> sounds(){List<JSONObject> result=new ArrayList<>();for(JSONObject entry:cloud)
        if(entry.optString("source").equals("audio")&&entry.optString("status").equals("ready"))result.add(entry);return result;}
    public void toggleRecording(){if(recorder!=null){stopRecording();return;}
        requireAccount(()->{if(ContextCompat.checkSelfPermission(a,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)host.requestMicrophone();else startRecording();});}
    public void startRecording(){if(uid().isEmpty()){requireAccount(this::startRecording);return;}if(recorder!=null)return;
        MediaRecorder active=new MediaRecorder();File file=new File(a.getNoBackupFilesDir(),"sound_"+UUID.randomUUID()+".m4a");
        try{active.setAudioSource(MediaRecorder.AudioSource.MIC);active.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            active.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);active.setAudioEncodingBitRate(64000);active.setAudioSamplingRate(22050);
            active.setMaxDuration((int)CareRecord.MAX_AUDIO_MS);active.setMaxFileSize(CareRecord.MAX_AUDIO_BYTES);
            active.setOutputFile(file.getAbsolutePath());active.setOnInfoListener((r,what,extra)->{
                if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED||what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)handler.post(recordingLimit);});
            active.prepare();active.start();recorder=active;recording=file;recordingAt=System.currentTimeMillis();
            recordingPet=new JSONObject(host.selectedPet().toString());recordingOwner=uid();handler.postDelayed(recordingLimit,CareRecord.MAX_AUDIO_MS);
            note("Recording started. Tap again to stop. Maximum 60 seconds.");
        }catch(Exception e){active.release();file.delete();recorder=null;recording=null;note("Could not start recording. Check microphone permission and try again.");}}
    private void stopRecording(){if(recorder==null)return;handler.removeCallbacks(recordingLimit);
        File file=recording;JSONObject pet=recordingPet;long at=recordingAt,duration=Math.min(System.currentTimeMillis()-at,CareRecord.MAX_AUDIO_MS);
        boolean valid=true;try{recorder.stop();}catch(Exception e){valid=false;}recorder.release();recorder=null;recording=null;
        if(!valid||file==null||duration<1000||file.length()==0){if(file!=null)file.delete();note("Record at least one second of audio.");return;}
        JSONObject entry=newEntry(pet,"Observation","","audio",at);put(entry,"_ownerUid",recordingOwner);
        put(entry,"durationMs",duration);put(entry,"audioFile",file.getName());
        storePending(recordingOwner,entry);
        if(started&&alive()&&uid().equals(recordingOwner))describeRecording(entry,file);
    }
    private void describeRecording(JSONObject entry,File file){
        EditText description=new EditText(a);description.setHint("What did you hear? (e.g. coughing)");
        description.setText(entry.optString("text"));
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Save sound observation").setView(description).setPositiveButton("Save",null)
                .setNegativeButton("Discard",(d,w)->confirmDelete(entry)).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String text=description.getText().toString().trim();if(text.isEmpty()){description.setError("Describe the sound");return;}
            put(entry,"text",text);description.setEnabled(false);
            save(entry,file,v,()->{dialog.dismiss();host.go("Sound Journal");});
        }));if(alive())dialog.show();
    }
    public void play(JSONObject entry){requireAccount(()->{try{CareRecord record=stored(entry);
        JSONObject attachment=pending(uid()).optJSONObject(record.id);
        File file=attachment==null?null:local(attachment.optString("audioFile"));
        if(!hasAudio(uid(),record)||file==null){note("This recording is stored only on the original device. Its observation is available here.");return;}
        stopPlayback();player=new MediaPlayer();player.setDataSource(file.getAbsolutePath());
        player.setOnPreparedListener(MediaPlayer::start);player.setOnCompletionListener(p->stopPlayback());
        player.setOnErrorListener((p,what,extra)->{stopPlayback();note("Could not play this local recording.");return true;});player.prepareAsync();
        }catch(Exception e){stopPlayback();note(message(e));}});}
    private boolean hasAudio(String user,CareRecord record){
        if(!record.source.equals("audio")||!deviceId.equals(record.audioDeviceId))return false;
        JSONObject attachment=pending(user).optJSONObject(record.id);
        File file=attachment==null?null:local(attachment.optString("audioFile"));return file!=null&&file.isFile();
    }
    private void markSaved(String user,String id){JSONObject all=pending(user),entry=all.optJSONObject(id);
        if(entry==null||entry.optBoolean("_saved"))return;put(entry,"_saved",true);put(all,id,entry);
        drafts.edit().putString("audio:"+user,all.toString()).apply();
    }
    private void reconcileAttachments(){if(uid().isEmpty())return;String user=uid();JSONObject all=pending(user);
        java.util.Iterator<String> ids=all.keys();while(ids.hasNext()){String id=ids.next();JSONObject entry=all.optJSONObject(id);
            if(entry==null||!entry.optBoolean("_saved"))continue;
            repository.get(id).addOnCompleteListener(task->{if(uid().equals(user)&&task.isSuccessful()&&task.getResult()==null)removePending(user,id,true);});
        }
    }
    private File local(String filename){if(filename==null||filename.isEmpty())return null;
        if(!filename.equals(new File(filename).getName()))return null;
        File privateFile=new File(a.getNoBackupFilesDir(),filename);
        return privateFile.isFile()?privateFile:new File(a.getFilesDir(),filename);}
    private void protectAudioFile(File file)throws java.io.IOException {
        File destination=new File(a.getNoBackupFilesDir(),file.getName());
        if(file.getCanonicalPath().equals(destination.getCanonicalPath()))return;
        try(java.io.InputStream input=new java.io.FileInputStream(file);
            java.io.OutputStream output=new java.io.FileOutputStream(destination)){
            byte[] bytes=new byte[8192];int count;while((count=input.read(bytes))!=-1)output.write(bytes,0,count);
        }
    }
    private JSONObject pending(String user){try{return new JSONObject(drafts.getString("audio:"+user,"{}"));}catch(JSONException e){return new JSONObject();}}
    private void storePending(String user,JSONObject entry){put(entry,"_saved",false);JSONObject all=pending(user);put(all,entry.optString("id"),entry);drafts.edit().putString("audio:"+user,all.toString()).apply();}
    private void removePending(String user,String id,boolean deleteFile){JSONObject all=pending(user);JSONObject entry=all.optJSONObject(id);
        if(entry==null)return;
        if(deleteFile){File file=local(entry.optString("audioFile"));if(file!=null)file.delete();}
        all.remove(id);drafts.edit().putString("audio:"+user,all.toString()).apply();}
}
