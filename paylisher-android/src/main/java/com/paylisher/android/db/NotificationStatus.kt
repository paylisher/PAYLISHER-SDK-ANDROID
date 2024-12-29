package com.paylisher.android.db

import androidx.annotation.StringDef

// Annotation for status with StringDef
@Retention(AnnotationRetention.SOURCE)
@StringDef(NotificationStatus.UNREAD, NotificationStatus.READ)
annotation class NotificationStatus {
    companion object {
        const val UNREAD: String = "UNREAD"
        const val READ: String = "READ"
//        const val DISMISSED: String = "DISMISSED"
    }
}