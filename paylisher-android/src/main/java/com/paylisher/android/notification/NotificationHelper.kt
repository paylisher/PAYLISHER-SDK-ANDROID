package com.paylisher.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.paylisher.Paylisher
import com.paylisher.android.R
import com.paylisher.android.notification.helpers.InAppLocalize.Companion.localize
import java.io.IOException
import java.net.URL

class NotificationHelper(
    private val context: Context
) {

    companion object {
        private const val TAG = "FCM | PUSH"

        // In your SDK library (MyFirebaseMessagingService or related class)
        object NotificationConfig {
            var notificationIntentClass: Class<*>? = null
        }

        fun setNotificationIntentClass(activityClass: Class<*>) {
            NotificationConfig.notificationIntentClass = activityClass
        }

        fun getInstance(context: Context): NotificationHelper {
            return NotificationHelper(context)
        }
    }

    fun showNotification(data: NotificationPushData) {
        val title = data.title?.localize(data.defaultLang) ?: "Title"
        val message = data.message?.localize(data.defaultLang) ?: "Body"
        val imageUrl = data.imageUrl
        val iconUrl = data.iconUrl

        // if has action url (deeplink or externalURL) -> else open main
        val pendingIntent: PendingIntent = if (data.action != null) {
            // Create an intent with the deep link URI
            val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data.action)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            // Create a pending intent for the notification click action
            PendingIntent.getActivity(
                context, 0, deepLinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val intentClass = NotificationConfig.notificationIntentClass ?: context::class.java
            val intent = Intent(context, intentClass)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Load images if URLs are provided
        val bmp = imageUrl?.let { loadBitmapFromUrl(it) }
        val icon = iconUrl?.let { loadBitmapFromUrl(it) }

        val channelId = if (data.silent) "silent_notification_channel" else "notification_channel"
        val builder = NotificationCompat.Builder(context, channelId)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.notification_small_icon) // Use a fixed small icon
            .setContentTitle(title ?: "Notification")
            .setContentText(message ?: "")
            .setContentIntent(pendingIntent)
            .setPriority(if (data.silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)

        if (!data.silent) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(defaultSoundUri)
        }

        // Set large icon if available
        icon?.let { builder.setLargeIcon(it) } // Set the large icon with the bitmap

        // Use BigPictureStyle if bitmap is available
        bmp?.let { builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(it)) }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channels if they don’t already exist
        createNotificationChannels(notificationManager)

        val notificationId = (0..Int.MAX_VALUE).random()

        data.buttons?.forEach { btn ->
//            println("FCM, label -> ${btn.label} action -> ${btn.action}")

            when {
                btn.action == "dismiss" -> {
                    // Handle dismiss action
                    val dismissIntent =
                        Intent(context, NotificationDismissReceiver::class.java).apply {
                            putExtra("notificationId", notificationId)
                        }
                    val dismissPendingIntent = PendingIntent.getBroadcast(
                        context, btn.label.hashCode(), dismissIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, btn.label.localize(data.defaultLang), dismissPendingIntent)
                }

                btn.action.startsWith("copy") -> {
                    val textToCopy = btn.action.removePrefix("copy:").trim()
                    val copyIntent = Intent(context, NotificationCopyReceiver::class.java).apply {
                        putExtra("copyText", textToCopy)
                    }
                    val copyPendingIntent = PendingIntent.getBroadcast(
                        context, btn.label.hashCode(), copyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, btn.label.localize(data.defaultLang), copyPendingIntent)
                }

                else -> {
                    // Default action: open a browser or handle deep links
                    val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(btn.action)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val btnPendingIntent = PendingIntent.getActivity(
                        context, btn.label.hashCode(), deepLinkIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    builder.addAction(0, btn.label.localize(data.defaultLang), btnPendingIntent)

                    // Create a custom dismiss action that will cancel the notification after clicking
                    val dismissNotificationIntent =
                        Intent(context, NotificationDismissReceiver::class.java).apply {
                            putExtra("notificationId", notificationId)
                        }

                    // This will trigger the NotificationDismissReceiver and close the notification when clicked
                    val dismissPendingIntent = PendingIntent.getBroadcast(
                        context, notificationId, dismissNotificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Set the delete intent to cancel the notification
                    builder.setDeleteIntent(dismissPendingIntent)

                    // Set the notification
                    notificationManager.notify(notificationId, builder.build())

                }
            }
        }

        notificationManager.notify(notificationId, builder.build())

        Paylisher.capture(
            "notificationOpen",
            properties = mapOf(
                "title" to (data.title ?: "title")
            )
        )

        Log.d(TAG, "Notification sent with ID: $notificationId")
    }

    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val regularChannelId = "notification_channel"
            val silentChannelId = "silent_notification_channel"

            if (notificationManager.getNotificationChannel(regularChannelId) == null) {
                val regularChannel = NotificationChannel(
                    regularChannelId, "Notifications", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                notificationManager.createNotificationChannel(regularChannel)
            }

            if (notificationManager.getNotificationChannel(silentChannelId) == null) {
                val silentChannel = NotificationChannel(
                    silentChannelId, "Silent Notifications", NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null) // No sound for silent notifications
                }
                notificationManager.createNotificationChannel(silentChannel)
            }
        }
    }

    private fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            BitmapFactory.decodeStream(URL(url).openStream())
        } catch (e: IOException) {
            Log.e(TAG, "Error loading image from URL: $url", e)
            null
        }
    }
}

