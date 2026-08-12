import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    kotlin("kapt")
}

// Keeps the Google Maps API key out of source control: reads app/secrets.properties (gitignored)
// and falls back to the checked-in placeholder in app/local.defaults.properties otherwise, so a
// fresh checkout still builds (Maps just won't work until you add a real key - see SETUP.md).
val secretsProperties = Properties().apply {
    val secretsFile = file("secrets.properties")
    val defaultsFile = file("local.defaults.properties")
    when {
        secretsFile.exists() -> secretsFile.inputStream().use { load(it) }
        defaultsFile.exists() -> defaultsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.amchiyatri.rider"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.amchiyatri.rider"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["MAPS_API_KEY"] = secretsProperties.getProperty("MAPS_API_KEY", "DEFAULT_API_KEY")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// The Firebase Gradle plugin hard-fails the build if app/google-services.json is missing, which
// would break `git clone && build` for anyone who hasn't set up Firebase yet. Applying it only
// when the file exists keeps the Fake* repositories working out of the box; see SETUP.md.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

dependencies {
    // Core / Kotlin
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    // Local persistence for preferences (language choice, saved places, session)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Firebase: phone-OTP auth + Firestore (live ride state) + Functions (dispatch simulator,
    // Razorpay order creation/verification). Versions are pinned by the BOM.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    // Google Maps: live map, device location, place autocomplete, real routing.
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.libraries.places:places:3.5.0")

    // Directions API is a plain REST endpoint (no first-party Android SDK).
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Razorpay Checkout (UPI/cards/wallets). Order creation + signature verification happen in
    // Cloud Functions (functions/) - the secret key must never live in the app.
    implementation("com.razorpay:checkout:1.6.40")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
