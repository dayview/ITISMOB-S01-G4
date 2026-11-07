package com.purrfolio.mco1.care;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Source;
import com.purrfolio.mco1.BuildConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** These tests can only address the isolated demo Firebase emulators. */
@RunWith(AndroidJUnit4.class)
public class CareRepositoryIntegrationTest {
    private FirebaseCareServices services;
    private FirebaseCareRepository repository;
    private String email,uid;
    private static final String PASSWORD="Emulator-Test-123!";
    private <T>T await(Task<T> task)throws Exception{return Tasks.await(task,25,TimeUnit.SECONDS);}
    @Before public void setUp()throws Exception {
        assumeTrue("Run with -PcareUseEmulators=true",BuildConfig.DEBUG&&BuildConfig.CARE_USE_EMULATORS);
        services=FirebaseCareServices.get(ApplicationProvider.getApplicationContext());
        services.auth.signOut();email="care-"+UUID.randomUUID()+"@example.test";
        uid=await(services.auth.createUserWithEmailAndPassword(email,PASSWORD)).getUser().getUid();
        repository=new FirebaseCareRepository(services);
    }
    private CareRecord text(String pet,String source,String amount,String transcript){String id=UUID.randomUUID().toString();
        return new CareRecord(id,pet,pet.equals("miso")?"Miso":"Bailey","Feeding","One cup of kibble",source,
                amount,transcript,System.currentTimeMillis()-1000,0,"","ready",1);}
    @Test public void textVoiceHistoryEditDeleteAndAccountIsolation()throws Exception{
        CareRecord quick=text("miso","text","one cup","");await(repository.create(quick));
        CareRecord voice=text("miso","voice","one cup","Fed Miso one cup at 7:30 AM");await(repository.create(voice));
        CareRecord otherPet=text("bailey","text","one cup","");await(repository.create(otherPet));
        CountDownLatch received=new CountDownLatch(1);AtomicInteger count=new AtomicInteger();
        com.google.firebase.firestore.ListenerRegistration listener=repository.observe("miso",50,new FirebaseCareRepository.Observer(){
            public void changed(java.util.List<CareRecord> records,boolean cached,boolean more){if(!cached){count.set(records.size());received.countDown();}}
            public void failed(Exception error){received.countDown();}
        });
        assertTrue(received.await(15,TimeUnit.SECONDS));listener.remove();assertEquals(2,count.get());
        CareRecord saved=await(repository.get(quick.id));assertEquals(1,saved.revision);
        await(repository.edit(saved,"Feeding","Corrected note","two cups",saved.occurredAt-60000));
        CareRecord edited=await(repository.get(saved.id));assertEquals(2,edited.revision);assertEquals("two cups",edited.amount);
        try{await(repository.edit(saved,"Feeding","Stale note","one cup",saved.occurredAt));fail("Stale edit accepted");}catch(java.util.concurrent.ExecutionException expected){}
        services.auth.signOut();await(services.auth.signInWithEmailAndPassword(email,PASSWORD));
        assertEquals("Corrected note",await(repository.get(saved.id)).text);
        services.auth.signOut();await(services.auth.createUserWithEmailAndPassword("other-"+UUID.randomUUID()+"@example.test",PASSWORD));
        assertNull(await(repository.get(saved.id)));
        try{await(services.db.collection("users").document(uid).collection("careLogs").document(saved.id).get(Source.SERVER));fail("Foreign read accepted");}
        catch(java.util.concurrent.ExecutionException expected){}
        services.auth.signOut();await(services.auth.signInWithEmailAndPassword(email,PASSWORD));
        await(repository.delete(edited));assertNull(await(repository.get(saved.id)));
        await(repository.delete(await(repository.get(voice.id))));await(repository.delete(await(repository.get(otherPet.id))));
    }
    @Test public void localAudioMetadataIsStoredWithoutUploadingBytes()throws Exception{
        String id=UUID.randomUUID().toString(),device=UUID.randomUUID().toString();
        CareRecord draft=new CareRecord(id,"miso","Miso","Observation","A cough","audio","","",
                System.currentTimeMillis()-2000,2000,device,"ready",1);
        await(repository.create(draft));await(repository.create(draft));
        CareRecord saved=await(repository.get(id));assertEquals("ready",saved.status);assertEquals(1,saved.revision);
        assertEquals(device,saved.audioDeviceId);assertEquals(2000,saved.durationMs);
        com.google.firebase.firestore.DocumentSnapshot raw=await(services.db.collection("users").document(uid)
                .collection("careLogs").document(id).get(Source.SERVER));
        assertFalse(raw.contains("audioFile"));assertFalse(raw.contains("audioPath"));
        await(repository.delete(saved));assertNull(await(repository.get(id)));
    }
}
