package com.purrfolio.mco1.care;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Care-only Firestore APIs. Audio bytes stay on the recording device. */
public final class FirebaseCareRepository {
    public interface Observer {
        void changed(List<CareRecord> records,boolean fromCache,boolean canLoadMore);
        void failed(Exception error);
    }
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    public FirebaseCareRepository(FirebaseCareServices services){auth=services.auth;db=services.db;}
    public String uid(){if(auth.getCurrentUser()==null)throw new IllegalStateException("Sign in to your care account first.");return auth.getCurrentUser().getUid();}
    private CollectionReference collection(String uid){return db.collection("users").document(uid).collection("careLogs");}
    public ListenerRegistration observe(String petId,int limit,Observer observer) {
        return collection(uid()).whereEqualTo("petId",petId).orderBy("occurredAt",Query.Direction.DESCENDING)
                .limit(limit).addSnapshotListener(MetadataChanges.INCLUDE,(snapshot,error)->{
                    if(error!=null){observer.failed(error);return;}if(snapshot==null)return;
                    try{List<CareRecord> records=new ArrayList<>();
                        for(DocumentSnapshot d:snapshot.getDocuments())records.add(CareRecord.fromDocument(d));
                        observer.changed(records,snapshot.getMetadata().isFromCache(),snapshot.size()>=limit);
                    }catch(Exception invalid){observer.failed(new IllegalStateException("A care record has invalid data. Please contact the project administrator.",invalid));}
                });
    }
    public Task<Void> create(CareRecord record) {
        DocumentReference ref=collection(uid()).document(record.id);
        return db.runTransaction(tx->{
            DocumentSnapshot existing=tx.get(ref);
            if(existing.exists()){
                CareRecord current=CareRecord.fromDocument(existing);
                if(!current.petId.equals(record.petId)||!current.text.equals(record.text)||
                        !current.source.equals(record.source)||!current.audioDeviceId.equals(record.audioDeviceId)||current.status.equals("deleting"))
                    throw new IllegalStateException("This entry ID already belongs to another record. Reload Care History.");
                return null;
            }
            Map<String,Object> fields=record.fields();fields.put("createdAt",FieldValue.serverTimestamp());
            fields.put("updatedAt",FieldValue.serverTimestamp());tx.set(ref,fields);return null;
        });
    }
    public Task<Void> edit(CareRecord original,String kind,String text,String amount,long occurredAt) {
        CareRecord validated=new CareRecord(original.id,original.petId,original.petName,kind,text,original.source,
                amount,original.transcript,occurredAt,original.durationMs,original.audioDeviceId,original.status,original.revision);
        DocumentReference ref=collection(uid()).document(original.id);
        return db.runTransaction(tx->{DocumentSnapshot current=tx.get(ref);
            if(!current.exists())throw new IllegalStateException("This entry was deleted. Reload Care History.");
            if(!"ready".equals(current.getString("status"))||current.getLong("revision")!=original.revision)
                throw new IllegalStateException("This entry changed on another device. Reload it before editing.");
            Map<String,Object> fields=new HashMap<>();fields.put("kind",validated.kind);fields.put("text",validated.text);
            fields.put("amount",validated.amount);fields.put("occurredAt",validated.occurredAt);
            fields.put("revision",original.revision+1);fields.put("updatedAt",FieldValue.serverTimestamp());
            tx.update(ref,fields);return null;
        });
    }
    public Task<Void> delete(CareRecord original) {
        DocumentReference ref=collection(uid()).document(original.id);
        return db.runTransaction(tx->{DocumentSnapshot current=tx.get(ref);if(!current.exists())return null;
            if(!"deleting".equals(current.getString("status"))){
                if(current.getLong("revision")!=original.revision)throw new IllegalStateException("This entry changed. Reload it before deleting.");
                tx.update(ref,"status","deleting","revision",original.revision+1,"updatedAt",FieldValue.serverTimestamp());
            }return null;
        }).continueWithTask(marked->marked.isSuccessful()?ref.delete():Tasks.forException(failure(marked)));
    }
    public Task<CareRecord> get(String id) {
        return collection(uid()).document(id).get(Source.SERVER).continueWithTask(task->{
            if(!task.isSuccessful())return Tasks.forException(failure(task));
            return Tasks.forResult(task.getResult().exists()?CareRecord.fromDocument(task.getResult()):null);
        });
    }
    public static Exception failure(Task<?> task){return task.getException()==null?new IllegalStateException("The care operation did not complete."):task.getException();}
}
