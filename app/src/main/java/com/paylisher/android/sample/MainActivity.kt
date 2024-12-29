package com.paylisher.android.sample

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import com.paylisher.Paylisher
import com.paylisher.PaylisherOnFeatureFlags
import com.paylisher.PaylisherPropertiesSanitizer
import com.paylisher.android.PaylisherAndroid
import com.paylisher.android.PaylisherAndroidConfig
import com.paylisher.android.sample.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        const val PAYLISHER_API_KEY = "phc_test"
        const val PAYLISHER_HOST = "https://test.paylisher.com"

        fun paylisherIdentify(
            distinctId: String?,
            email: String?,
            name: String?,
            token: String?,
            gender: String?
        ) {
            println("paylisher Identify $name | $email | $gender | $token")

            if (!distinctId.isNullOrEmpty()
                && !name.isNullOrEmpty() && !email.isNullOrEmpty()
                && !token.isNullOrEmpty() && !gender.isNullOrEmpty()
            ) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date()) // Get current date as a string

                // You should call `identify` as soon as you're able to.
                // Typically, this is every time your app loads for the first time as well as directly after your user logs in.
                // This ensures that events sent during your user's sessions are correctly associated with them.
                // When you call `identify`, all previously tracked anonymous events will be linked to the user.
                Paylisher.identify(
                    distinctId = distinctId,
                    userProperties = mapOf(
                        "token" to token,
//                        "fcm_token" to token,

                        "email" to email,
                        "name" to name,
                        "gender" to gender,
                    ),
                    userPropertiesSetOnce = mapOf(
                        "firstLogin" to currentDate
                    )
                )
            } else {
                // Handle invalid inputs, e.g., log error or throw exception
                println("Invalid input: One or more fields are null or empty.")
            }
        }
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        // Check for notification and location permissions if running on Android 13 or above
        getPermissions()

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

        //----------------------------------------------------------------------
        val company = "Paylisher"
//        val gender = "male" // female

//        val name = "Testy Cat"
//        val email = "emulator@testy.com"
//        val alias = "Cat"

        //----------------------------------------------------------------------
        //----------------------------------------------------------------------
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val alias = sharedPreferences.getString("alias", "") ?: ""

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                println("Firebase | Fetching FCM token failed: ${task.exception?.message}")
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Save the token to SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("fcm_token", token)
            editor.apply()

            val name = sharedPreferences.getString("name", "") ?: ""
            val email = sharedPreferences.getString("email", "") ?: ""
            val gender = sharedPreferences.getString("gender", "") ?: ""

            // can get this warning if already identified -> "already identified with id: <distinctId>"
            paylisherIdentify(email, email, name, token, gender)

            println("Firebase | FCM token saved successfully: $token")
        }
        //----------------------------------------------------------------------
        //----------------------------------------------------------------------
        Paylisher.group(type = "company", key = company)

        /**
         * Create an alias for the current user.
         */
        Paylisher.alias(alias)


//        Paylisher.distinctId()
//        Paylisher.reset()

        //----------------------------------------------------------------------
        //----------------------------------------------------------------------
//        val userProps = mutableMapOf<String, Any>()
//        userProps["Email"] = "testy@testy.com";
//        userProps["Username"] = "Testy";

//        // email Email name Name username Username UserName
//        Paylisher.capture(
//            "Person",
//            userProperties = userProps,
//            groups = mapOf("name" to "Testy Group")
//        )

//        Paylisher.screen(
//            screenTitle = "Main", properties = mapOf(
//                "background" to "blue",
//                "hero" to "superCat"
//            )
//        )

        val apiAvailability = GoogleApiAvailability.getInstance()
        val status = apiAvailability.isGooglePlayServicesAvailable(this)
        if (status != ConnectionResult.SUCCESS) {
            apiAvailability.getErrorDialog(this, status, 2404)?.show()
        }

        // for Deep link
        handleIntent(intent)
    }


    private fun getPermissions() {
        // Check for notification and location permissions if running on Android 13 or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionsNeeded = mutableListOf<String>()

            // Check for notification permission
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }

            // Check for fine location permission
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }

            // Check for background location permission only if fine location is granted
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            // Request any permissions that are missing
            if (permissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this, permissionsNeeded.toTypedArray(),
                    101
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    // for Deep link
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val data: Uri? = intent.data
            data?.let {
                // Handle the deep link data, e.g., extract path and query parameters
                val path = it.path
                val queryParams = it.queryParameterNames

                // Custom inApp Message on click deepLink test
                // Perform actions based on the path or query parameters
                if (path == "/promo") {
                    // Use NavController to navigate to SecondFragment
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    navController.navigate(R.id.action_FirstFragment_to_SecondFragment)
                }

                // Push notification on click deepLink test
                // Here you can extract data from the URI and navigate to the specific screen
                if (it.scheme == "myapp" && it.host == "offer") {
                    val offerId = it.lastPathSegment
                    // Navigate to the screen with the offer ID
                    navigateToOfferScreen(offerId)
                }
            }
        }
    }

    private fun navigateToOfferScreen(offerId: String?) {
        // Logic to navigate to the specific screen

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navController.navigate(R.id.action_FirstFragment_to_TestFragment)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}