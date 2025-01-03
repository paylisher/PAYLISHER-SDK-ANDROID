plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("kotlin-kapt") // Enables annotation processing with kapt
    id("signing")
}

android {
    namespace = "com.paylisher"
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

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.paylisher"
            artifactId = "paylisher-sdk"
            version = "1.0.1"

            afterEvaluate {
                from(components["release"])
            }
               pom {
                name.set("paylisher-sdk")
                url.set("https://github.com/paylisher/paylisher-sdk-android")
                description.set("Paylisher SDK for Android")
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
    implementation(libs.firebase.common)
    compileOnly(libs.animal.sniffer.annotations)

    implementation("com.google.code.gson:gson:2.10.1")

    // do not upgrade to >= 4.12 otherwise it does not work with Kotlin 1.7
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.11.0"))
    implementation("com.squareup.okhttp3:okhttp")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
buildscript {

    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.6.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    }
}