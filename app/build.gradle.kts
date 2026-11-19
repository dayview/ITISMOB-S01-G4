plugins {
    id("com.android.application")
    // Google services Gradle plugin
    id("com.google.gms.google-services")
}
configurations.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}
android { namespace = "com.purrfolio.mco1"; compileSdk = 35
    defaultConfig { applicationId = "com.purrfolio.mco1"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    buildFeatures { viewBinding = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
    implementation("androidx.core:core:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Firebase (free Spark plan). Kept at BoM 33.7.0, the version tested on the Kotlin branch.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
