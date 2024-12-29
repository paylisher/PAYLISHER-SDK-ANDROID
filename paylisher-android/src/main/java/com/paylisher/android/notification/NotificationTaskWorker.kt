package com.paylisher.android.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.reflect.TypeToken
import com.paylisher.android.PaylisherAndroid

class NotificationTaskWorker(
    appContext: Context, workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
//        Log.d("FCM Worker", "silent: ${inputData.getString("silent")}")
//        Log.d("FCM Worker", "buttons: ${inputData.getString("buttons")}")

        val notificationData = NotificationPushData(
            title = PaylisherAndroid.gson.fromJson(
                inputData.getString("title"), object : TypeToken<Map<String, String>>() {}.type
            ),
            message = PaylisherAndroid.gson.fromJson(
                inputData.getString("message"), object : TypeToken<Map<String, String>>() {}.type
            ),
//            title = inputData.getString("title"),
//            message = inputData.getString("message"),

            imageUrl = inputData.getString("imageUrl"),
            iconUrl = inputData.getString("iconUrl"),

            action = inputData.getString("action"),
            silent = inputData.getString("silent")?.toBoolean() ?: false,

            buttons = inputData.getString("buttons")?.let {
                PaylisherAndroid.gson.fromJson(
                    it, object : TypeToken<List<PushButton>>() {}.type
                )
            },

            defaultLang = inputData.getString("defaultLang"),

            // Below properties not set | no need
            condition = Condition(
                target = "",
                displayTime = 0,
                expireDate = 0,
                delay = 0
            ),
            geofence = null
        )
        Log.d("FCM NotificationTaskWorker", "Push: ${notificationData.toString()}")

        // Use NotificationHelper to display the notification
        val notificationHelper = NotificationHelper.getInstance(applicationContext)
        notificationHelper.showNotification(notificationData)

        return Result.success()
    }
}
