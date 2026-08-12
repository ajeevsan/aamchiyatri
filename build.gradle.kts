// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    // Reads app/google-services.json and wires up Firebase. Applied conditionally in
    // app/build.gradle.kts so the project still builds (against the Fake* repositories) before
    // you've dropped in your own google-services.json.
    id("com.google.gms.google-services") version "4.5.0" apply false
}
