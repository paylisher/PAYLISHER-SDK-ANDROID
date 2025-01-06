# Getting Started

## Paylisher Android SDK Integration Guide

Copy `google-services.json` in `\app` directory.  

Copy `paylisher-release.aar` in `\app\libs` directory.  
Copy `paylisher-android-release.aar` in `\app\libs` directory.  

### Basic Integration

#### Java

build.gradle (:root)

````
buildscript {
    ...
    dependencies {
        ...

        classpath 'com.google.gms:google-services:4.4.2' 
    }
} 
````

build.gradle (:app)

````
# (.aar file in app/libs)
plugins {
    ...
    
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}
 
dependencies {
   // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // Add Firebase products (no version needed as they are managed by the BoM)
    implementation 'com.google.firebase:firebase-messaging:24.1.0'
    implementation 'com.google.firebase:firebase-analytics:22.1.2'

    implementation(name: 'paylisher-release', ext: 'aar')
    implementation(name: 'paylisher-android-release', ext: 'aar')

    // Database dependencies
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")  // Ensure version compatibility

    implementation "com.squareup.curtains:curtains:1.2.5"

    //**

    ...
}
````

settings.gradle

```` 
dependencyResolutionManagement {
    repositories {
        ...
        flatDir {
            dirs 'app/libs'
        }
    }
}
```` 

#### Kotlin

build.gradle (:root)

```` 
plugins { 
    ...
    
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.2" apply false
}
````

##### build.gradle (:app)

```` 
plugins {
    ...

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

...
repositories {
..
    flatDir {
        dirs 'libs' // This will look for libraries in the 'libs' folder
    }
}
dependencies {
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // Add Firebase products (no version needed as they are managed by the BoM)
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    implementation(files("libs/paylisher-release.aar"))
    implementation(files("libs/paylisher-android-release.aar"))

    implementation ("com.squareup.curtains:curtains:1.2.5")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.11.0"))
    implementation("com.squareup.okhttp3:okhttp")

    ...
}
```` 

### Step 2. Initialize SDK

#### Java

````
public class
MainActivity extends AppCompatActivity {

    String PAYLISHER_API_KEY = "<<KEY>>";
    String PAYLISHER_HOST = "<<HOST>>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		// ...

        PaylisherAndroidConfig config = new PaylisherAndroidConfig(PAYLISHER_API_KEY, PAYLISHER_HOST);
        config.setDebug(true);
        config.setFlushAt(1);
        config.setCaptureDeepLinks(true);
        config.setCaptureApplicationLifecycleEvents(true);
        config.setCaptureScreenViews(true);
        config.setSessionReplay(true);
        config.setPreloadFeatureFlags(true);
        config.setOnFeatureFlags(() -> System.out.println("feature flags loaded"));
        config.setPropertiesSanitizer(properties -> {
//            properties.remove("$device_name"); // Uncomment and modify as needed
            return properties;
        });
        config.getSessionReplayConfig().setMaskAllTextInputs(false);
        config.getSessionReplayConfig().setMaskAllImages(false);
        config.getSessionReplayConfig().setCaptureLogcat(true);
        config.getSessionReplayConfig().setScreenshot(true);

        PaylisherAndroid.Companion.setup(this, config);
    }    
}

````

#### Kotlin

````

class MainActivity : AppCompatActivity() {

    companion object {
        const val PAYLISHER_API_KEY = "<<KEY>>"
        const val PAYLISHER_HOST = "<<HOST>>"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
		
		// ...

        val config =
            PaylisherAndroidConfig(apiKey = PAYLISHER_API_KEY, host = PAYLISHER_HOST).apply {
                debug = true
                flushAt = 1
                captureDeepLinks = true
                captureApplicationLifecycleEvents = true
                captureScreenViews = true
                sessionReplay = true
                preloadFeatureFlags = true
                onFeatureFlags = PaylisherOnFeatureFlags { print("feature flags loaded") }
                propertiesSanitizer =
                    PaylisherPropertiesSanitizer { properties ->
                        properties.apply {
//                    remove("\$device_name")
                        }
                    }
                sessionReplayConfig.maskAllTextInputs = true
                sessionReplayConfig.maskAllImages = false

                sessionReplayConfig.captureLogcat = true
                sessionReplayConfig.screenshot = true
            }
        PaylisherAndroid.setup(this, config)
    }

````
