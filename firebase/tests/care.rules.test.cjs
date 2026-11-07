const {test,before,after,beforeEach}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {initializeTestEnvironment,assertSucceeds,assertFails}=require('@firebase/rules-unit-testing');
const {doc,setDoc,getDoc,updateDoc,deleteDoc,collection,query,where,orderBy,limit,startAfter,getDocs,serverTimestamp}=require('firebase/firestore');
let env;
const owner=()=>env.authenticatedContext('owner');
const stranger=()=>env.authenticatedContext('stranger');
const record=(id='entry',extra={})=>({id,petId:'miso',petName:'Miso',kind:'Feeding',text:'One cup of kibble',
  source:'text',amount:'one cup',transcript:'',occurredAt:Date.now()-1000,durationMs:0,audioDeviceId:'',
  status:'ready',revision:1,createdAt:serverTimestamp(),updatedAt:serverTimestamp(),...extra});
const entry=(db,id='entry',uid='owner')=>doc(db,`users/${uid}/careLogs/${id}`);
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-purrfolio-care',
  firestore:{rules:fs.readFileSync('firestore.rules','utf8')}});});
after(async()=>{await env.cleanup();});
beforeEach(async()=>{await env.clearFirestore();});

test('only authenticated owner can create, read, query, edit, or delete care records',async()=>{
  const db=owner().firestore();await assertSucceeds(setDoc(entry(db),record()));
  await assertSucceeds(getDoc(entry(db)));
  await assertFails(getDoc(entry(stranger().firestore())));
  await assertFails(getDoc(entry(env.unauthenticatedContext().firestore())));
  await assertFails(setDoc(entry(stranger().firestore(),'other'),record('other')));
  await assertFails(getDocs(collection(stranger().firestore(),'users/owner/careLogs')));
  await assertFails(updateDoc(entry(stranger().firestore()),{text:'tampered',revision:2,updatedAt:serverTimestamp()}));
  await assertFails(deleteDoc(entry(stranger().firestore())));
});
test('existing user-management root permissions are preserved without granting other collections',async()=>{
  const db=owner().firestore();await assertSucceeds(setDoc(doc(db,'users/owner'),{fullName:'Owner'}));
  await assertSucceeds(getDoc(doc(db,'users/owner')));
  await assertFails(getDoc(doc(stranger().firestore(),'users/owner')));
  await assertFails(setDoc(doc(db,'users/owner/unrelated/example'),{value:'not care'}));
});
test('schema rejects unsupported fields, invalid dates, malformed IDs and incomplete voice records',async()=>{
  const db=owner().firestore();
  for(const invalid of [{text:''},{text:'   \n   '},{petName:'   '},{kind:'Diagnosis'},{petId:''},{source:'other'},{occurredAt:0},
    {occurredAt:Date.now()+2*86400000},{admin:true},{revision:2},{createdAt:new Date(0)},
    {source:'voice',transcript:'fed Miso',amount:''},{source:'voice',transcript:'fed Miso',amount:'  '},{source:'voice',transcript:''},
    {source:'text',durationMs:2000},{source:'audio',status:'ready'},{id:'mismatch'}]){
      await assertFails(setDoc(entry(db),record('entry',invalid)));
  }
  await assertSucceeds(setDoc(entry(db),record('entry',{source:'voice',transcript:'Fed Miso one cup at 7:30 AM'})));
});
test('edits require next revision and cannot reassign pet, source, identity, or creation time',async()=>{
  const db=owner().firestore();await setDoc(entry(db),record());
  await assertSucceeds(updateDoc(entry(db),{text:'Corrected note',amount:'two cups',revision:2,updatedAt:serverTimestamp()}));
  await assertFails(updateDoc(entry(db),{text:'Stale edit',revision:2,updatedAt:serverTimestamp()}));
  for(const immutable of [{petId:'bailey'},{petName:'Bailey'},{source:'voice'},{createdAt:serverTimestamp()},{audioDeviceId:'foreign'}])
    await assertFails(updateDoc(entry(db),{...immutable,revision:3,updatedAt:serverTimestamp()}));
  await assertFails(deleteDoc(entry(db)));
  await assertSucceeds(updateDoc(entry(db),{status:'deleting',revision:3,updatedAt:serverTimestamp()}));
  await assertFails(updateDoc(entry(db),{text:'Revive',status:'ready',revision:4,updatedAt:serverTimestamp()}));
  await assertSucceeds(deleteDoc(entry(db)));
});
test('per-pet chronological history supports cursor pagination',async()=>{
  const db=owner().firestore();for(let i=0;i<4;i++)await setDoc(entry(db,`entry${i}`),record(`entry${i}`,{occurredAt:1000+i}));
  await setDoc(entry(db,'bailey'),record('bailey',{petId:'bailey',occurredAt:99999}));
  const base=collection(db,'users/owner/careLogs');
  const page=await getDocs(query(base,where('petId','==','miso'),orderBy('occurredAt','desc'),limit(2)));
  assert.deepEqual(page.docs.map(d=>d.id),['entry3','entry2']);
  const older=await getDocs(query(base,where('petId','==','miso'),orderBy('occurredAt','desc'),startAfter(page.docs[1]),limit(2)));
  assert.deepEqual(older.docs.map(d=>d.id),['entry1','entry0']);
});
test('local-audio metadata is owner scoped, bounded, and cannot expose file paths',async()=>{
  const db=owner().firestore();
  const audio=record('entry',{source:'audio',kind:'Observation',amount:'',durationMs:2500,audioDeviceId:'device-123'});
  await assertSucceeds(setDoc(entry(db),audio));
  await assertSucceeds(getDoc(entry(db)));
  await assertFails(getDoc(entry(stranger().firestore())));
  await assertFails(updateDoc(entry(db),{audioDeviceId:'another-device',revision:2,updatedAt:serverTimestamp()}));
  await assertFails(updateDoc(entry(db),{kind:'Feeding',revision:2,updatedAt:serverTimestamp()}));
  await updateDoc(entry(db),{text:'Corrected sound note',revision:2,updatedAt:serverTimestamp()});
  await updateDoc(entry(db),{status:'deleting',revision:3,updatedAt:serverTimestamp()});
  await assertSucceeds(deleteDoc(entry(db)));
  for(const extra of [{durationMs:999},{durationMs:60001},{audioDeviceId:''},{audioFile:'/private/file.m4a'},{audioPath:'public-url'}])
    await assertFails(setDoc(entry(db),{...audio,...extra}));
});
