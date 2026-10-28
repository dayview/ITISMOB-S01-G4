plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.purrfolio.mco1"; compileSdk = 35
    defaultConfig { applicationId = "com.purrfolio.mco1"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies { implementation("androidx.core:core:1.13.0"); implementation("androidx.appcompat:appcompat:1.7.0") }
