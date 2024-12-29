package com.paylisher.android.notification.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val geofencesList = mutableListOf<Geofence>()

    // Companion object to hold the singleton instance
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: GeofenceManager? = null

        fun getInstance(context: Context): GeofenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GeofenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun addGeofence(latitude: Double, longitude: Double, radius: Float, geofenceId: String) {
        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
        geofencesList.add(geofence)
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofencesList)
            .build()
    }

    private fun getGeofencePendingIntent(): PendingIntent {
//        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
//        return PendingIntent.getBroadcast(
//            context,
//            0,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )

        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        return geofencePendingIntent;
    }

    fun startGeofencing() {
        if (geofencesList.isNotEmpty()) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }

            println("Geofences to be added: ${geofencesList.size}")
            geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener {
                    println("Geofences successfully added!")
                }
                .addOnFailureListener { e ->
                    println("Failed to add geofences: ${e.message}")
                    e.printStackTrace()
                }
        }
    }

    fun stopGeofencing() {
        geofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener {
            println("Geofences successfully removed")
            geofencesList.clear() // Clear list to avoid duplicates
        }
    }

}
