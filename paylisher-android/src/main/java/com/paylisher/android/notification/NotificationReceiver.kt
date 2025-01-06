package com.paylisher.android.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.paylisher.Paylisher

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationId = intent?.getIntExtra("notificationId", -1) ?: return

//        Log.d("FCM Receiver", "Dismiss Notification -> $notificationId")

        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            Paylisher.capture(
                "notificationDismiss",
                properties = mapOf("via" to "receiver")
            )
        }
    }
}

class NotificationCopyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val textToCopy = intent?.getStringExtra("copyText")

//        Log.d("FCM Receiver", "Copy to Clipboard -> $textToCopy")

        if (!textToCopy.isNullOrEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard: $textToCopy", Toast.LENGTH_SHORT).show()
        }
    }
}
