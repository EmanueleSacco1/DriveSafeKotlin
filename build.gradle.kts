buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Plugin Android Gradle
        classpath("com.android.tools.build:gradle:8.3.0")
        // Plugin Kotlin Gradle
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        // Plugin Google Services (per Firebase)
        classpath("com.google.gms:google-services:4.4.2")
        // Safe Args plugin per Navigation Component
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
