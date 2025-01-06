package com.paylisher.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NotificationDao {
    // Insert a new notification
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM Notifications")
    suspend fun getAll(): List<NotificationEntity?>?

    // Get all unread notifications
    // @Query("SELECT * FROM Notifications WHERE status = 'UNREAD' AND EXPIRATION_DATE > :currentTime")
    // suspend fun getUnreadNotifications(currentTime: Long = System.currentTimeMillis()): List<NotificationEntity>

    // TODO: only get Type: inApp, actionBased
    @Query("SELECT * FROM Notifications WHERE status = 'UNREAD' AND EXPIRATION_DATE > :currentTime AND TYPE IN ('IN-APP', 'ACTION-BASED')")
    suspend fun getUnreadNotifications(currentTime: Long = System.currentTimeMillis()): List<NotificationEntity>

    // TODO: only get Type: geofence
    @Query("SELECT * FROM Notifications WHERE status = 'UNREAD' AND EXPIRATION_DATE > :currentTime AND TYPE = 'GEOFENCE'")
    suspend fun getUnreadGeofence(currentTime: Long = System.currentTimeMillis()): List<NotificationEntity>

    // Get the count of all notifications
    @Query("SELECT COUNT(_id) FROM Notifications")
    suspend fun getCount(): Long

    // Update notification status (e.g., mark as read)
    @Query("UPDATE Notifications SET STATUS = :status WHERE _id = :id")
    suspend fun updateStatus(id: Int, status: String)

    // Truncate the table (delete all records)
    @Query("DELETE FROM Notifications")
    suspend fun deleteAll()
}
