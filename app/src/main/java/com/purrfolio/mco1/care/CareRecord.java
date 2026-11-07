package com.purrfolio.mco1.care;

import com.google.firebase.firestore.DocumentSnapshot;
import org.json.JSONObject;
import org.json.JSONException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Structured care payload. Display labels and the existing local JSON keys stay compatible. */
public final class CareRecord {
    public static final List<String> KINDS=Arrays.asList("Feeding","Medication","Vitamins","Water",
            "Playtime","Grooming","Vet Visit","Other","Observation");
    public static final long MAX_AUDIO_MS=60000;
    public static final long MAX_AUDIO_BYTES=5*1024*1024;
    public final String id,petId,petName,kind,text,source,amount,transcript,audioDeviceId,status;
    public final long occurredAt,durationMs,revision;
    public CareRecord(String id,String petId,String petName,String kind,String text,String source,
            String amount,String transcript,long occurredAt,long durationMs,String audioDeviceId,String status,long revision) {
        this.id=id;this.petId=petId;this.petName=petName;this.kind=kind;this.text=text.trim();this.source=source;
        this.amount=amount.trim();this.transcript=transcript.trim();this.occurredAt=occurredAt;
        this.durationMs=durationMs;this.audioDeviceId=audioDeviceId;this.status=status;this.revision=revision;
        if(!id.matches("[A-Za-z0-9_-]{1,128}") || !petId.matches("[A-Za-z0-9_-]{1,128}"))
            throw new IllegalArgumentException("Invalid care entry or pet ID.");
        if(petName.trim().isEmpty()||petName.length()>120||!KINDS.contains(kind)||this.text.isEmpty()||this.text.length()>4000)
            throw new IllegalArgumentException("Choose a pet and activity, and enter a note of up to 4,000 characters.");
        if(!Arrays.asList("text","voice","audio").contains(source)||occurredAt<=0||occurredAt>System.currentTimeMillis()+86400000L)
            throw new IllegalArgumentException("Enter a valid care time.");
        if(this.amount.length()>120||this.transcript.length()>4000)throw new IllegalArgumentException("The care details are too long.");
        if(source.equals("voice") && (this.transcript.isEmpty()||requiresAmount(kind)&&this.amount.isEmpty()))
            throw new IllegalArgumentException("Complete the voice activity amount before saving.");
        if(!Arrays.asList("ready","deleting").contains(status))throw new IllegalArgumentException("Invalid care record state.");
        if(source.equals("audio") && (!kind.equals("Observation")||durationMs<1000||durationMs>MAX_AUDIO_MS||!audioDeviceId.matches("[A-Za-z0-9_-]{1,128}")))
            throw new IllegalArgumentException("Sound observations must be between 1 and 60 seconds.");
        if(!source.equals("audio") && (durationMs!=0||!audioDeviceId.isEmpty()))
            throw new IllegalArgumentException("Only sound observations can contain an audio attachment.");
    }
    public static boolean requiresAmount(String kind) {
        return Arrays.asList("Feeding","Medication","Vitamins","Water").contains(kind);
    }
    public static CareRecord fromJson(JSONObject o,String uid) {
        String source=o.optString("source","text"),id=o.optString("id");
        String device=source.equals("audio")?o.optString("audioDeviceId"):"";
        return new CareRecord(id,o.optString("petId"),o.optString("pet"),o.optString("kind"),o.optString("text"),
                source,o.optString("amount"),o.optString("transcript"),o.optLong("occurredAt"),
                o.optLong("durationMs"),device,"ready",1);
    }
    public Map<String,Object> fields() {
        Map<String,Object> m=new HashMap<>();m.put("id",id);m.put("petId",petId);m.put("petName",petName);
        m.put("kind",kind);m.put("text",text);m.put("source",source);m.put("amount",amount);
        m.put("transcript",transcript);m.put("occurredAt",occurredAt);m.put("durationMs",durationMs);
        m.put("audioDeviceId",audioDeviceId);m.put("status",status);m.put("revision",revision);return m;
    }
    public static CareRecord fromDocument(DocumentSnapshot d) {
        return new CareRecord(d.getId(),s(d,"petId"),s(d,"petName"),s(d,"kind"),s(d,"text"),s(d,"source"),
                s(d,"amount"),s(d,"transcript"),n(d,"occurredAt"),n(d,"durationMs"),s(d,"audioDeviceId"),s(d,"status"),n(d,"revision"));
    }
    private static String s(DocumentSnapshot d,String key){String value=d.getString(key);return value==null?"":value;}
    private static long n(DocumentSnapshot d,String key){Long value=d.getLong(key);return value==null?0:value;}
    public JSONObject toJson() {
        try {JSONObject o=new JSONObject();for(Map.Entry<String,Object> e:fields().entrySet())o.put(e.getKey(),e.getValue());
            o.put("pet",petName);o.put("date",new SimpleDateFormat("MMM d, yyyy · h:mm a",Locale.getDefault()).format(new Date(occurredAt)));
            o.put("cloud",true);return o;
        }catch(JSONException error){throw new IllegalStateException(error);}
    }
}
