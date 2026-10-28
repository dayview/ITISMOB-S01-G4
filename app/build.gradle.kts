plugins {
    alias(libs.plugins.android.application)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.itismob.mco.purrfolio"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.itismob.mco.purrfolio"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:35.0.2"))
    // TODO: Add the dependencies for Firebase products you want to use when using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    // Add the dependencies for any other desired Firebase products
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    // Fore Images and QR Codes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // https://firebase.google.com/docs/android/setup#available-libraries
}
