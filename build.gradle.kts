// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false


    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.2" apply false
//    id("com.gradle.plugin-publish") version "1.2.1"
    id("maven-publish")
}

group = "com.paylisher"
version = "1.0.0"

buildscript {
    repositories {
        google()  // Make sure this is included
        mavenCentral()
    }

    dependencies {
        // Add or update this line
        classpath("com.android.tools.build:gradle:8.6.1")
    }
}

