package com.paylisher.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.GsonBuilder
import com.paylisher.Paylisher
import com.paylisher.PaylisherInterface
import com.paylisher.SDK_VERSION
import com.paylisher.android.db.NotificationStatus
import com.paylisher.android.db.NotificationType
import com.paylisher.android.db.PaylisherDatabase
import com.paylisher.android.internal.MainHandler
import com.paylisher.android.internal.PaylisherActivityLifecycleCallbackIntegration
import com.paylisher.android.internal.PaylisherAndroidContext
import com.paylisher.android.internal.PaylisherAndroidLogger
import com.paylisher.android.internal.PaylisherAndroidNetworkStatus
import com.paylisher.android.internal.PaylisherAppInstallIntegration
import com.paylisher.android.internal.PaylisherLifecycleObserverIntegration
import com.paylisher.android.internal.PaylisherSharedPreferences
import com.paylisher.android.internal.appContext
import com.paylisher.android.notification.Condition
import com.paylisher.android.notification.InAppLayoutBlock
import com.paylisher.android.notification.InAppTaskWorker
import com.paylisher.android.notification.NotificationInAppData
import com.paylisher.android.notification.FcmMessagingService
import com.paylisher.android.notification.NotificationPushData
import com.paylisher.android.notification.NotificationTaskWorker
import com.paylisher.android.notification.helpers.InAppLayoutBlockDeserializer
import com.paylisher.android.replay.PaylisherReplayIntegration
import com.paylisher.android.replay.internal.PaylisherLogCatIntegration
import com.paylisher.internal.PaylisherNoOpLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Main entrypoint for the Android SDK
 * Use the setup method to set a global and singleton instance
 * Or use the with method that returns an instance that you can hold and pass it around
 */
public class PaylisherAndroid private constructor() {
    public companion object {
        private var database: PaylisherDatabase? = null
        private val lock = Any()

        private val signalService = Paylisher.getSignal()
        private val scope = CoroutineScope(Dispatchers.Default) // Background coroutine scope

        private val processedSignals = mutableSetOf<Pair<String, Long>>()
        private const val signalTimeoutMs = 3000L // Timeout for considering duplicates
        private val processedNotifications = mutableMapOf<Int, Long>()
        private const val notificationTimeoutMs = 3600000L // Timeout in milliseconds (1 hour)

        private fun shouldProcessSignal(signal: String): Boolean {
            synchronized(processedSignals) {
                val currentTime = System.currentTimeMillis()

                // Clear outdated signals
                processedSignals.removeIf { it.second + signalTimeoutMs < currentTime }

                // Check and add current signal
                return if (processedSignals.any { it.first == signal }) {
                    false
                } else {
                    processedSignals.add(signal to currentTime)
                    true
                }
            }
        }

        private fun shouldProcessNotification(notificationId: Int): Boolean {
            synchronized(processedNotifications) {
                val currentTime = System.currentTimeMillis()

                // Clear outdated entries
                processedNotifications.entries.removeIf { (_, timestamp) ->
                    timestamp + notificationTimeoutMs < currentTime
                }

                // Check if the notification is already processed
                return if (processedNotifications.containsKey(notificationId)) {
                    false
                } else {
                    processedNotifications[notificationId] = currentTime
                    true
                }
            }
        }

        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(InAppLayoutBlock::class.java, InAppLayoutBlockDeserializer())
            .create()

        /**
         * Setup the SDK and set a global and singleton instance
         * @param T the type of the Config
         * @property context the Context
         * @property config the Config
         */
        public fun <T : PaylisherAndroidConfig> setup(
            context: Context,
            config: T,
        ) {
            synchronized(lock) {
                setAndroidConfig(context.appContext(), config)

                Paylisher.setup(config)
                Log.i("Paylisher", "v$SDK_VERSION")

                // Signal Service
                try {
                    scope.launch {
//                        signalService.signalFlow
//                            .distinctUntilChanged() // prevent double signals
//                            .collect { signal ->
//                                setNotification(context, signal)
//                                println("Received signal: $signal")
//                            }
                        signalService.signalFlow
                            .collect { signal ->
                                if (shouldProcessSignal(signal)) {
                                    setNotification(context, signal)
                                    println("Processed signal: $signal")
                                } else {
                                    println("Skipped duplicate signal: $signal")
                                }
                            }

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("Signal | An error occurred: ${e.message}")
                }

                // DB Initialization
                try {

                    // Initialize the database and store the instance
                    database = PaylisherDatabase.getInstance(context)
//                    database = Room.databaseBuilder(
//                        context.applicationContext,
//                        PaylisherDatabase::class.java,
//                        "paylisher-database"
//                    )
//                        // TODO use if ya changed db schema only
//                        // TODO: comment the line below after db schema done
//                        .fallbackToDestructiveMigration() // This wipes and recreates the database if no migration is provided
//                        .build()

                    CoroutineScope(Dispatchers.IO).launch {
                        if (database != null) {
//                            database!!.NotificationDao().deleteAll();
                            val pushesFence = database!!.NotificationDao().getUnreadGeofence()
                            println("FCM Database: ${pushesFence.count()} -> Geofence")
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    println("Database | An error occurred: ${e.message}")
                }

                try {

                    setupFirebase(context)

                } catch (e: IllegalStateException) {
                    // Handle Firebase initialization issue
                    e.printStackTrace()
                    println("Firebase | Firebase initialization failed: ${e.message}")
                } catch (e: Exception) {
                    // Handle other potential issues
                    e.printStackTrace()
                    println("Firebase | An error occurred: ${e.message}")
                }

                try {

                    // Setup lifecycle tracking with minimum API 29 requirement
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setupGlobalLifecycleTracking(context)
                    } else {
                        // Log or handle the case where API level is below 29
                        println("PaylisherSDK | Global lifecycle tracking is only supported on API 29 and above.")
                        // Optionally: Use fallback behavior or partial tracking methods if feasible
                    }

                } catch (e: Exception) {
                    // Handle other potential issues
                    e.printStackTrace()
                    println("Lifecycle | An error occurred: ${e.message}")
                }

            }
        }

        // Expose a method to get the database instance
        private fun getDatabase(): PaylisherDatabase? {
            return database
        }

        private fun toPushData(payload: String?): NotificationPushData {
            return gson.fromJson(payload, object : TypeToken<NotificationPushData>() {}.type)
        }

        private fun toInAppData(payload: String?): NotificationInAppData {
            return gson.fromJson(payload, object : TypeToken<NotificationInAppData>() {}.type)
        }

        private fun showInAppNotification(context: Context, id: Int, data: NotificationInAppData?) {
            if (data != null) {
                // val mainActivity = context as Activity
                val notificationRequest = OneTimeWorkRequestBuilder<InAppTaskWorker>()
                    .setInputData(
                        workDataOf(
                            "native" to gson.toJson(data.native),
                            "condition" to gson.toJson(data.condition),

                            "defaultLang" to data.defaultLang,
                            "layoutType" to data.layoutType,
                            "layouts" to gson.toJson(data.layouts),
                        )
                    )
                    .setInitialDelay((data.condition.delay ?: 0).toLong(), TimeUnit.MINUTES)
                    .build()

                val workManager = WorkManager.getInstance(context)
                workManager.enqueue(notificationRequest)

                // Launch a coroutine to poll WorkInfo status
                CoroutineScope(Dispatchers.IO).launch {
                    while (true) {
                        val workInfo = workManager.getWorkInfoById(notificationRequest.id).get()
                        if (workInfo != null) {
                            when (workInfo.state) {
                                WorkInfo.State.SUCCEEDED -> {
                                    Log.i("FCM NotificationWorker", "Success $id")
                                    // TODO: for test | un-comment below later
                                    val db = getDatabase()
                                    db?.NotificationDao()?.updateStatus(id, NotificationStatus.READ)

                                    // TODO: send event
//                                    Paylisher.capture("")
                                    break
                                }

                                WorkInfo.State.FAILED -> {
                                    Log.e("FCM NotificationWorker", "Notification task failed")
                                    break
                                }

                                else -> {
                                    // Continue waiting
                                }
                            }
                        }
                        delay(500) // Delay in milliseconds before checking again
                    }
                }
            }
        }

        private fun showPushNotification(context: Context, id: Int, data: NotificationPushData?) {
            if (data != null) {
//                Log.d("FCM  PA", "silent: ${data.silent}")

                // Create a unique ID for the WorkRequest to track its result
                val notificationRequest = OneTimeWorkRequestBuilder<NotificationTaskWorker>()
                    .setInputData(
                        workDataOf(
                            "title" to gson.toJson(data.title),
                            "message" to gson.toJson(data.message),

                            "imageUrl" to data.imageUrl?.toString(),
                            "iconUrl" to data.iconUrl?.toString(),

                            "action" to data.action,
                            "silent" to data.silent.toString(), // Convert boolean to string

                            "buttons" to gson.toJson(data.buttons),
                            "defaultLang" to data.defaultLang,
                        )
                    )
                    .setInitialDelay((data.condition.delay ?: 0).toLong(), TimeUnit.MINUTES)
                    .build()

                println("data.condition.delay, ${data.condition.delay}")

                val workManager = WorkManager.getInstance(context)
                workManager.enqueue(notificationRequest)

                // Launch a coroutine to poll WorkInfo status
                CoroutineScope(Dispatchers.IO).launch {
                    while (true) {
                        val workInfo = workManager.getWorkInfoById(notificationRequest.id).get()
                        if (workInfo != null) {
                            when (workInfo.state) {
                                WorkInfo.State.SUCCEEDED -> {
                                    Log.i("FCM NotificationWorker", "Success $id")
                                    val db = getDatabase()
                                    db?.NotificationDao()?.updateStatus(id, NotificationStatus.READ)

                                    // TODO: send event
//                                    Paylisher.capture("")
                                    break
                                }

                                WorkInfo.State.FAILED -> {
                                    Log.e("FCM NotificationWorker", "Notification task failed")
                                    break
                                }

                                else -> {
                                    // Continue waiting
                                }
                            }
                        }
                        delay(500) // Delay in milliseconds before checking again
                    }
                }
            }
        }

        private fun shouldDisplayMessage(condition: Condition): Boolean {
            val currentTime = System.currentTimeMillis()

            // Check `displayTime` condition
            if (condition.displayTime != null && currentTime < (condition.displayTime - (60 * 1000))) {
                // TODO: NOTE: there is 80sec difference -> test result => 1732170121809 / 1000 = 1732170121.809 < 1732170203204 / 1000 = 1732170203.204
                println("FCM shouldDisplayMessage | displayTime ct: $currentTime < ${condition.displayTime}")
                return false
            }

            // Check `expireDate` condition
            if (condition.expireDate != null && currentTime > condition.expireDate) {
                println("FCM shouldDisplayMessage | expireDate ct: $currentTime < ${condition.expireDate}")
                return false
            }

            return true
        }

        private fun setNotification(context: Context, target: String) {

            val db = getDatabase()

            CoroutineScope(Dispatchers.IO).launch {
                if (db != null) {
                    // Assuming `push` is a list of `NotificationEntity` objects from your query
                    val pushes = db.NotificationDao().getUnreadNotifications()
                    println("FCM Database: ${pushes.count()} -> target $target")

                    if (pushes.isEmpty()) {
                        return@launch // No notifications to process, early exit
                    }

                    pushes.forEach { item ->
                        if (item.type == NotificationType.PUSH) {
                            // TODO: Push type will be shown directly.. not from db
                        } else if (item.type == NotificationType.IN_APP) {
                            val data = toInAppData(item.payload);

                            if (shouldDisplayMessage(data.condition)) {
                                println("FCM DB IN_APP: payload ${item.payload}")

                                val targets = data.condition.target?.split(",") ?: listOf()

                                // Check if the target list contains the current target string
                                if ((data.condition.target.isNullOrEmpty() && target.contains("Main")) || targets.contains(
                                        target
                                    )
                                ) {
                                    if (!shouldProcessNotification(item.id)) {
                                        println("Skipping duplicate notification: ${item.id}")
                                        return@forEach
                                    }
                                    showInAppNotification(context, item.id, data)
                                }
                            }
                        } else if (item.type == NotificationType.ACTION_BASED) {
                            val data = toPushData(item.payload)

                            if (shouldDisplayMessage(data.condition)) {
                                println("FCM DB AB: payload ${item.payload}")

                                val targets = data.condition.target?.split(",") ?: listOf()

                                // Check if the target list contains the current target string
                                if ((data.condition.target.isNullOrEmpty() && target.contains("Main")) || targets.contains(
                                        target
                                    )
                                ) {
                                    if (!shouldProcessNotification(item.id)) {
                                        println("Skipping duplicate notification: ${item.id}")
                                        return@forEach
                                    }
                                    showPushNotification(context, item.id, data)
                                }
                            }
                        }
                    }
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun setupGlobalLifecycleTracking(context: Context) {
            if (context is androidx.appcompat.app.AppCompatActivity) {

                // Register ActivityLifecycleCallbacks to listen to Activity lifecycle events
                context.registerActivityLifecycleCallbacks(object :
                    Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(
                        activity: Activity,
                        savedInstanceState: Bundle?
                    ) {
                    }

                    override fun onActivityStarted(activity: Activity) {

                        setNotification(context, activity.localClassName)

                        // Send event when any Activity is started
                        Paylisher.screen(
                            screenTitle = activity.localClassName ?: "UnknownActivity",
                            properties = mapOf("timestamp" to System.currentTimeMillis())
                        )
                    }

                    override fun onActivityResumed(activity: Activity) {}
                    override fun onActivityPaused(activity: Activity) {}
                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivitySaveInstanceState(
                        activity: Activity,
                        outState: Bundle
                    ) {
                    }

                    override fun onActivityDestroyed(activity: Activity) {}
                })

                // Optional: Fragment tracking if the context is AppCompatActivity
                val appCompatActivity = context as androidx.appcompat.app.AppCompatActivity
                appCompatActivity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                    object : FragmentManager.FragmentLifecycleCallbacks() {
                        override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                            super.onFragmentStarted(fm, f)

                            setNotification(context, f.javaClass.simpleName)

                            Paylisher.screen(
                                screenTitle = f.javaClass.simpleName,
                                properties = mapOf("fragment_tag" to (f.tag ?: "Unknown"))
                            )
                        }
                    },
                    true
                )
            }
        }

        private fun setupFirebase(context: Context) {
            val appContext = context.applicationContext

            // Initialize Firebase
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }

            FirebaseMessaging.getInstance() // Assuming you are using Firebase Messaging

            // In the app's MainActivity or Application class
            FcmMessagingService.setNotificationIntentClass(context::class.java)
            // In your MainActivity or Application class
            FcmMessagingService.setMainActivity(context as Activity)

            // Get the FCM registration token (if needed)
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("Firebase | Fetching FCM token failed" + task.exception?.message)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result

                Paylisher.capture("FCM", userProperties = mapOf("token" to token))

                println("SDK | FCM Token: $token")
            }

//                    // In-App Messaging
//                    FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            // Get the Installation ID
//                            val installationId = task.result
//
//                            val props = mutableMapOf<String, Any>()
//                            props["installationId"] = installationId;
//                            Paylisher.capture("FIAM", properties = props)
//
//                            println("Firebase Installation ID: $installationId")
//                        } else {
//                            // Handle error
//                            println("Failed to get Installation ID: ${task.exception}")
//                        }
//                    }
        }

        /**
         * Setup the SDK and returns an instance that you can hold and pass it around
         * @param T the type of the Config
         * @property context the Context
         * @property config the Config
         */
        public fun <T : PaylisherAndroidConfig> with(
            context: Context,
            config: T,
        ): PaylisherInterface {
            setAndroidConfig(context.appContext(), config)
            return Paylisher.with(config)
        }

        private fun <T : PaylisherAndroidConfig> setAndroidConfig(
            context: Context,
            config: T,
        ) {
            config.logger =
                if (config.logger is PaylisherNoOpLogger) PaylisherAndroidLogger(config) else config.logger
            config.context = config.context ?: PaylisherAndroidContext(context, config)

            val legacyPath = context.getDir("app_paylisher-disk-queue", Context.MODE_PRIVATE)
            val path = File(context.cacheDir, "paylisher-disk-queue")
            val replayPath = File(context.cacheDir, "paylisher-disk-replay-queue")
            config.legacyStoragePrefix = config.legacyStoragePrefix ?: legacyPath.absolutePath
            config.storagePrefix = config.storagePrefix ?: path.absolutePath
            config.replayStoragePrefix = config.replayStoragePrefix ?: replayPath.absolutePath
            val preferences =
                config.cachePreferences ?: PaylisherSharedPreferences(context, config)
            config.cachePreferences = preferences
            config.networkStatus =
                config.networkStatus ?: PaylisherAndroidNetworkStatus(context)
            // Flutter SDK sets the sdkName and sdkVersion, so this guard is not to allow
            // the values to be overwritten again
            if (config.sdkName != "paylisher-flutter") {
                config.sdkName = "paylisher-android"
                config.sdkVersion = SDK_VERSION
//                config.sdkVersion = "24.10.0"
//                config.sdkVersion = BuildConfig.VERSION_NAME
            }

            val mainHandler = MainHandler()
            config.addIntegration(PaylisherReplayIntegration(context, config, mainHandler))
            config.addIntegration(PaylisherLogCatIntegration(config))
            if (context is Application) {
                if (config.captureDeepLinks || config.captureScreenViews || config.sessionReplay) {
                    config.addIntegration(
                        PaylisherActivityLifecycleCallbackIntegration(
                            context,
                            config
                        )
                    )
                }
            }
            if (config.captureApplicationLifecycleEvents) {
                config.addIntegration(PaylisherAppInstallIntegration(context, config))
            }
            config.addIntegration(
                PaylisherLifecycleObserverIntegration(
                    context,
                    config,
                    mainHandler
                )
            )
        }

        fun showNotification(context: Context, geofenceIds: List<String>, triggerVal: String) {
            val db = PaylisherAndroid.getDatabase()

            // trigger -> Entered or Exited
            CoroutineScope(Dispatchers.IO).launch {
                if (db != null) {
                    val pushes = db.NotificationDao().getUnreadGeofence()

                    pushes.forEach { item ->
                        val data = toPushData(item.payload)
                        println("FCM geofence: payload ${item.payload}")

                        // Check if the geofence ID in the notification matches any of the triggered geofence IDs
                        val notificationGeofenceId = data.geofence?.geofenceId
                        if (notificationGeofenceId != null
                            && (data.geofence.trigger.isNullOrEmpty() || data.geofence.trigger === triggerVal)
                            && geofenceIds.contains(notificationGeofenceId)
                        ) {
                            showPushNotification(context, item.id, data)
                            Paylisher.capture(
                                "notificationOpen",
                                properties = mapOf(
                                    "type" to "geofence",
                                    "title" to (data.title ?: "title")
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
