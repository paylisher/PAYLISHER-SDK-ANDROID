package com.paylisher.android.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int = 0,                              // Unique ID for each notification

    @NotificationType
    @ColumnInfo(name = "TYPE")
    var type: String = NotificationType.PUSH,     // Type of notification

    @ColumnInfo(name = "RECEIVED_DATE")
    var receivedDate: Date? = null,               // When notification was received

    @ColumnInfo(name = "EXPIRATION_DATE")
    var expirationDate: Date? = null,             // When notification expires

    @ColumnInfo(name = "PAYLOAD")
    var payload: String? = null,                  // JSON payload as a string

    @NotificationStatus
    @ColumnInfo(name = "STATUS")
    var status: String = NotificationStatus.UNREAD // Status: UNREAD, READ, //DISMISSED
)


