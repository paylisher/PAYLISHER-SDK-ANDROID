package com.paylisher.android.db

import androidx.annotation.StringDef

// Annotation for type with StringDef
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    NotificationType.PUSH,
    NotificationType.IN_APP,
    NotificationType.GEOFENCE,
    NotificationType.ACTION_BASED
)
annotation class NotificationType {
    companion object {
        const val PUSH: String = "PUSH"     // direct show
        const val IN_APP: String = "IN-APP"
        const val GEOFENCE: String = "GEOFENCE"
        const val ACTION_BASED: String = "ACTION-BASED"
    }
}