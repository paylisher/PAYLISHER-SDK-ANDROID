package com.paylisher.android.notification

import android.app.Activity
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.paylisher.Paylisher
import com.paylisher.android.PaylisherAndroid
import com.paylisher.android.db.NotificationEntity
import com.paylisher.android.db.NotificationStatus
import com.paylisher.android.db.NotificationType
import com.paylisher.android.db.PaylisherDatabase
import com.paylisher.android.notification.geofence.GeofenceManager
import com.paylisher.android.notification.helpers.InAppLayoutBlockDeserializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Date

class FcmMessagingService : FirebaseMessagingService() {
    // https://github.com/firebase/snippets-android/blob/0a133da3aff2742054a1a14967215a3e9518d718/messaging/app/src/main/java/com/google/firebase/example/messaging/kotlin/MyFirebaseMessagingService.kt#L89

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
//        Log.d(TAG, "From: ${remoteMessage.from} type: ${remoteMessage.data["type"]}")

        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(InAppLayoutBlock::class.java, InAppLayoutBlockDeserializer())
            .create()

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            val db = PaylisherDatabase.getInstance(applicationContext)

            Paylisher.capture(
                "notificationReceived",
                properties = mapOf(
                    "type" to (remoteMessage.data["type"] ?: "")
                )
            )


            // Push Notification
            if (remoteMessage.data["type"] == NotificationType.PUSH) {
                val dataPayloadJson = JSONObject(remoteMessage.data as Map<*, *>).toString()
                Log.d(TAG, "Notification Push Data: $dataPayloadJson")

                NotificationConfig.notificationIntentClass?.let {
                    NotificationHelper.setNotificationIntentClass(it)
                }

                // Check if the message contains notification data
                val notificationRequest = OneTimeWorkRequestBuilder<NotificationTaskWorker>()
                    .setInputData(
                        workDataOf(
//                                "title" to it.title,
//                                "message" to it.body,
                            "title" to remoteMessage.data["title"],
                            "message" to remoteMessage.data["message"],

                            "imageUrl" to remoteMessage.data["imageUrl"],
//                                "imageUrl" to it.imageUrl?.toString(),

                            "iconUrl" to remoteMessage.data["iconUrl"],

                            "action" to remoteMessage.data["action"],
                            "silent" to remoteMessage.data["silent"],

                            "defaultLang" to remoteMessage.data["defaultLang"],
                            "buttons" to remoteMessage.data["buttons"],
//                                "buttons" to buttons,
                        )
                    )
                    .build()

                WorkManager.getInstance(applicationContext).enqueue(notificationRequest)
            }
            // Custom InApp Message
            else if (remoteMessage.data["type"] == NotificationType.IN_APP) {
                val dataPayloadJson = JSONObject(remoteMessage.data as Map<*, *>).toString()
                Log.d(TAG, "Notification InApp Data: $dataPayloadJson")

                val data = remoteMessage.data
                val condition = gson.fromJson(data["condition"], Condition::class.java)
//                Log.d(TAG, "condition: ${Gson().toJson(condition)}")

                var native: InAppNative? = null
                if (!data["native"].isNullOrEmpty()) {
                    native = gson.fromJson(data["native"], InAppNative::class.java)
                }

                var inAppLayouts: List<InAppMessagingLayout>? = null
                if (!data["layouts"].isNullOrEmpty()) {
                    val listType = object : TypeToken<List<InAppMessagingLayout>>() {}.type
                    inAppLayouts = gson.fromJson(data["layouts"], listType)
                }
//                Log.d(TAG, "layouts: ${gson.toJson(inAppLayouts)}")

                val notificationData = NotificationInAppData(
                    native = native,
                    condition = condition,

                    defaultLang = data["defaultLang"],
                    layoutType = data["layoutType"]?.let { data["layoutType"] } ?: "none",
                    layouts = inAppLayouts
                )

                val payloadJson: String = gson.toJson(notificationData)
//                println("FCM payloadJson: $payloadJson")

                val expire = notificationData.condition.expireDate?.let { Date(it) }
//                if (expire != null) {
//                    println("Expiration Date: $expire")
//                } else {
//                    println("No expiration date set or date conversion failed")
//                }

                val notification = NotificationEntity(
                    type = NotificationType.IN_APP,
                    receivedDate = Date(),
                    expirationDate = expire,
                    payload = payloadJson,
                    status = NotificationStatus.UNREAD
                )

                CoroutineScope(Dispatchers.IO).launch {
                    db.NotificationDao().insert(notification)
                }
            }
            // ACTION_BASED
            else if (remoteMessage.data["type"] == NotificationType.ACTION_BASED) {
                val dataPayloadJson = JSONObject(remoteMessage.data as Map<*, *>).toString()
                Log.d(TAG, "Notification ACTION_BASED Data: $dataPayloadJson")

//                val notify = remoteMessage.notification
                val condition =
                    gson.fromJson(remoteMessage.data["condition"], Condition::class.java)
//                Log.d(TAG, "condition: ${gson.toJson(condition)}")

                var buttons: List<PushButton>? = null
                if (remoteMessage.data["buttons"] != null) {
                    val type = object : TypeToken<List<PushButton>>() {}.type
                    buttons = gson.fromJson(remoteMessage.data["buttons"], type)
                }

                val notificationData = NotificationPushData(
//                    title = notify?.title,
//                    message = notify?.body,
                    title = PaylisherAndroid.gson.fromJson(
                        remoteMessage.data["title"],
                        object : TypeToken<Map<String, String>>() {}.type
                    ),
                    message = PaylisherAndroid.gson.fromJson(
                        remoteMessage.data["message"],
                        object : TypeToken<Map<String, String>>() {}.type
                    ),

//                    imageUrl = notify?.imageUrl?.toString(),
                    imageUrl = remoteMessage.data["imageUrl"],

                    iconUrl = remoteMessage.data["iconUrl"],

                    action = remoteMessage.data["action"],
                    silent = remoteMessage.data["silent"]?.toBoolean() ?: false,

                    buttons = buttons,
                    defaultLang = remoteMessage.data["defaultLang"],

                    condition = condition,
                    geofence = null
                )
                val payloadJson: String = gson.toJson(notificationData)
//                Log.d(TAG, "ACTION_BASED: ${gson.toJson(payloadJson)}")

                val expire = notificationData.condition.expireDate?.let { Date(it) }

                val notification = NotificationEntity(
                    type = NotificationType.ACTION_BASED,
                    receivedDate = Date(),
                    expirationDate = expire,
                    payload = payloadJson,
                    status = NotificationStatus.UNREAD
                )

                CoroutineScope(Dispatchers.IO).launch {
                    db.NotificationDao().insert(notification)
                }
            }
            // GEOFENCE
            else if (remoteMessage.data["type"] == NotificationType.GEOFENCE) {
                val dataPayloadJson = JSONObject(remoteMessage.data as Map<*, *>).toString()
                Log.d(TAG, "Notification GEOFENCE Data: $dataPayloadJson")

//                val notify = remoteMessage.notification
                val condition =
                    gson.fromJson(remoteMessage.data["condition"], Condition::class.java)
                val geofence = gson.fromJson(remoteMessage.data["geofence"], Geofence::class.java)
//                Log.d(TAG, "geofence: ${gson.toJson(geofence)}")

                var buttons: List<PushButton>? = null
                if (remoteMessage.data["buttons"] != null) {
                    val type = object : TypeToken<List<PushButton>>() {}.type
                    buttons = gson.fromJson(remoteMessage.data["buttons"], type)
                }

                val notificationData = NotificationPushData(
//                    title = notify?.title,
//                    message = notify?.body,
                    title = PaylisherAndroid.gson.fromJson(
                        remoteMessage.data["title"],
                        object : TypeToken<Map<String, String>>() {}.type
                    ),
                    message = PaylisherAndroid.gson.fromJson(
                        remoteMessage.data["message"],
                        object : TypeToken<Map<String, String>>() {}.type
                    ),
//                    imageUrl = notify?.imageUrl?.toString(),
                    imageUrl = remoteMessage.data["imageUrl"],

                    iconUrl = remoteMessage.data["iconUrl"],

                    action = remoteMessage.data["action"],
                    silent = remoteMessage.data["silent"]?.toBoolean() ?: false,

                    buttons = buttons,
                    defaultLang = remoteMessage.data["defaultLang"],

                    condition = condition,
                    geofence = geofence
                )

                val payloadJson: String = gson.toJson(notificationData)
//                Log.d(TAG, "Notification GEOFENCE payloadJson: $payloadJson")

                val expire = notificationData.condition.expireDate?.let { Date(it) }

                val notification = NotificationEntity(
                    type = NotificationType.GEOFENCE,
                    receivedDate = Date(),
                    expirationDate = expire,
                    payload = payloadJson,
                    status = NotificationStatus.UNREAD
                )

                if (notificationData.geofence != null) {
                    val latitude = notificationData.geofence.latitude
                    val longitude = notificationData.geofence.longitude
                    val radius = notificationData.geofence.radius
                    val geofenceId = notificationData.geofence.geofenceId

                    // Check that the geofence data is valid before proceeding
                    if (latitude != null && longitude != null && radius != null && geofenceId != null) {
                        val geofenceManager = GeofenceManager.getInstance(applicationContext)

                        CoroutineScope(Dispatchers.IO).launch {
                            db.NotificationDao().insert(notification)

                            // TODO: where to add geofence ??? here is ok?
                            geofenceManager.addGeofence(latitude, longitude, radius, geofenceId)
                            geofenceManager.startGeofencing()
                        }
                    }
                }
            }
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    companion object {
        private var TAG = "FCM"

        // In your SDK library (MyFirebaseMessagingService or related class)
        object NotificationConfig {
            var notificationIntentClass: Class<*>? = null

            // Using WeakReference to avoid memory leaks
            var mainActivity: WeakReference<Activity>? = null
        }

        fun setNotificationIntentClass(activityClass: Class<*>) {
            NotificationConfig.notificationIntentClass = activityClass
        }

        fun setMainActivity(activity: Activity) {
            NotificationConfig.mainActivity = WeakReference(activity)
        }

    }
}
