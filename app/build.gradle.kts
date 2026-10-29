plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Google services Gradle plugin
    id("com.google.gms.google-services")
}

android { namespace = "com.purrfolio.mco1"; compileSdk = 35
    defaultConfig { applicationId = "com.purrfolio.mco1"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    buildFeatures { viewBinding = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7") // lifecycleScope

    // BoM 33.7.0 builds with Kotlin 2.0.21; 35.x needs a newer Kotlin.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // Task.await()
}
