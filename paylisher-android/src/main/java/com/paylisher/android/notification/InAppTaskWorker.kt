package com.paylisher.android.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paylisher.android.PaylisherAndroid

class InAppTaskWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Retrieve data passed from onMessageReceived
        val notificationData = NotificationInAppData(
//            title = inputData.getString("title"),
//            message = inputData.getString("body"),
//            imageUrl = inputData.getString("imageUrl"),
//
//            actionText = inputData.getString("actionText"),
//            actionUrl = inputData.getString("actionUrl"),
            native = inputData.getString("native")?.let {
                PaylisherAndroid.gson.fromJson(it, object : TypeToken<InAppNative>() {}.type)
            },

            // Below properties not set | no need
            condition = Gson().fromJson(
                inputData.getString("condition")
                    ?: throw IllegalArgumentException("Condition is required"),
                object : TypeToken<Condition>() {}.type
            ),

            defaultLang = inputData.getString("defaultLang"),
            layoutType = inputData.getString("layoutType") ?: "none",
            layouts = inputData.getString("layouts")?.let {
                PaylisherAndroid.gson.fromJson(
                    it,
                    object : TypeToken<List<InAppMessagingLayout>>() {}.type
                )
            }
        )
        Log.d("FCM | InAppTaskWorker", "InApp: ${notificationData.toString()}")

        // Use NotificationHelper or InAppMessageHelper to display the in-app message
        FcmMessagingService.Companion.NotificationConfig.mainActivity?.get()
            ?.let { activity ->
                val helper = InAppMessageHelper()

                when (notificationData.layoutType) {
                    InAppLayoutType.BANNER.value -> {
                        helper.showCustomInAppMessageBanner(activity, notificationData)
                    }

                    InAppLayoutType.MODAL.value -> {
                        helper.showCustomInAppMessageModal(activity, notificationData)
                    }

                    InAppLayoutType.FULLSCREEN.value -> {
                        helper.showCustomInAppMessageFullscreen(activity, notificationData)
                    }

                    // CAROUSEL
                    InAppLayoutType.MODAL_CAROUSEL.value -> {
                        helper.showCustomInAppMessageModalCarousel(activity, notificationData)
                    }

                    InAppLayoutType.FULLSCREEN_CAROUSEL.value -> {
                        helper.showCustomInAppMessageFullscreenCarousel(activity, notificationData)
                    }

                    else -> {
                        if (notificationData.native != null) {
                            helper.showCustomInAppMessage(
                                activity,
                                notificationData.native,
                                notificationData.defaultLang
                            )
                        }
                    }
                }
            } ?: Log.e("FCM | InAppTaskWorker", "Main Activity reference is null")

        return Result.success()
    }
}