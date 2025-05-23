package com.clevertap.android.geofence.fakes

import android.location.Location
import com.google.android.gms.location.Geofence

object GeofenceEventFake {

    fun getDoubleMatchingTriggeredGeofenceList(): MutableList<Geofence?> {
        return mutableListOf(
            Geofence.Builder()
                .setRequestId("310001")
                .setTransitionTypes(1)
                .setCircularRegion(19.092962, 72.849717, 400f)
                .setExpirationDuration(-1)
                .build(),
            Geofence.Builder()
                .setRequestId("310002")
                .setTransitionTypes(1)
                .setCircularRegion(19.092962, 72.849717, 400f)
                .setExpirationDuration(-1)
                .build()
        )
    }

    fun getNonMatchingTriggeredGeofenceList(): MutableList<Geofence?> {
        return mutableListOf(
            Geofence.Builder()
                .setRequestId("312201")
                .setTransitionTypes(1)
                .setCircularRegion(19.092962, 72.849717, 400f)
                .setExpirationDuration(-1)
                .build()
        )
    }

    fun getSingleMatchingTriggeredGeofenceList(): MutableList<Geofence?> {
        return mutableListOf(
            Geofence.Builder()
                .setRequestId("310001")
                .setTransitionTypes(1)
                .setCircularRegion(19.092962, 72.849717, 400f)
                .setExpirationDuration(-1)
                .build()
        )
    }

    fun getTriggeredLocation(): Location {
        val location = Location("")
        location.latitude = 19.092962
        location.longitude = 72.849717

        return location
    }
}
