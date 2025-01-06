package com.paylisher.android.notification.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.paylisher.android.PaylisherAndroid

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        // Debug the intent and geofencing event itself
        println("Received intent: $intent")
        println("Geofencing event: $geofencingEvent")

        if (geofencingEvent?.hasError() == true) {
            // Handle error if needed
            println("Geofencing event error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        println("Geofence Transition: $geofenceTransition -> Triggered Geofences: $triggeringGeofences")

        val geofenceIds = triggeringGeofences?.map { it.requestId } ?: listOf()

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                triggerNotification(context, "Entered geofences: $geofenceIds")
                PaylisherAndroid.showNotification(context, geofenceIds, "Entered")

                // Stop geofencing after entering the geofence area
                // GeofenceManager.getInstance(context).stopGeofencing()
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                triggerNotification(context, "Exited geofences: $geofenceIds")
                PaylisherAndroid.showNotification(context, geofenceIds, "Exited")

                // Stop geofencing after existing the geofence area
                GeofenceManager.getInstance(context).stopGeofencing()
            }

            else -> {
                println("Unhandled geofence transition: $geofenceTransition")
            }
        }
    }

    private fun triggerNotification(context: Context, message: String) {
        // Implement notification logic here or provide a callback to the library user
        println("Geofences: $message")
//        Toast.makeText(context, message, Toast.LENGTH_SHORT).show() // TODO Remove later
    }
}
