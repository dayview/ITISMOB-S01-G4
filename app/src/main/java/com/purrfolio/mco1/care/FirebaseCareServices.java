package com.purrfolio.mco1.care;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.purrfolio.mco1.BuildConfig;

/** Both services use the same app and identity; emulators never address the live project. */
public final class FirebaseCareServices {
    public final FirebaseAuth auth;
    public final FirebaseFirestore db;
    private static FirebaseCareServices instance;
    public static synchronized FirebaseCareServices get(Context context) {
        if(instance==null) instance=new FirebaseCareServices(context.getApplicationContext());
        return instance;
    }
    private FirebaseCareServices(Context context) {
        FirebaseApp app=FirebaseApp.getInstance();
        if(BuildConfig.DEBUG && BuildConfig.CARE_USE_EMULATORS) {
            FirebaseOptions options=new FirebaseOptions.Builder()
                    .setApplicationId(app.getOptions().getApplicationId())
                    .setApiKey("fake-api-key").setProjectId("demo-purrfolio-care")
                    .build();
            app=FirebaseApp.initializeApp(context,options,"care-emulator");
        }
        auth=FirebaseAuth.getInstance(app);db=FirebaseFirestore.getInstance(app);
        if(BuildConfig.DEBUG && BuildConfig.CARE_USE_EMULATORS) {
            auth.useEmulator("10.0.2.2",9099);
            db.useEmulator("10.0.2.2",8080);
        }
    }
}
