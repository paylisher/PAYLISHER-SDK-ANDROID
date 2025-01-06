plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("kotlin-kapt") // Enables annotation processing with kapt
    id("signing")
}


android {
    namespace = "com.paylisher.android"
    compileSdk = 34

    defaultConfig {
        aarMetadata {
            minCompileSdk = 24
        }

        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.paylisher"
            artifactId = "paylisher-sdk-android"
            version = "1.0.1"

            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("paylisher-sdk-android")
                url.set("https://github.com/paylisher/paylisher-sdk-android")
                description.set("Paylisher SDK for Paylisher Android")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("paylisher")
                        name.set("Paylisher Team")
                        email.set("engineering@paylisher.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/paylisher/paylisher-sdk-android.git")
                    developerConnection.set("scm:git:ssh://github.com:paylisher/paylisher-sdk-android.git")
                    url.set("https://github.com/paylisher/paylisher-sdk-android")
                }
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["release"])
}


dependencies {
    // Import the Firebase BoM (see: https://firebase.google.com/docs/android/learn-more#bom)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // For an optimal experience using FCM, add the Firebase SDK
    // for Google Analytics. This is recommended, but not required.
    implementation("com.google.firebase:firebase-analytics")

    // Import Firebase Cloud Messaging library
    implementation("com.google.firebase:firebase-messaging")

    // Add the dependencies for the In-App Messaging
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-inappmessaging-display")

    // Database dependencies
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1") // Use kapt instead of annotationProcessor
    implementation("androidx.room:room-ktx:2.6.1")  // Ensure version compatibility

    implementation("com.google.code.gson:gson:2.10.1")  // Add this line

    // Geofence
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // runtime
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.6.2")
    implementation("com.squareup.curtains:curtains:1.2.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("com.paylisher:paylisher-sdk:1.0.1")
    //implementation(project(":paylisher"))
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
