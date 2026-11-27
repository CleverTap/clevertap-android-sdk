package com.clevertap.android.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RestrictTo

/**
 * A Factory to create [PendingIntent] which will be sent to OS to receive geofence updates
 * through [CTGeofenceReceiver] and location updates through [CTLocationUpdateReceiver]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PendingIntentFactory {

    /**
     * Type constant to create Location Update PendingIntent
     */
    const val PENDING_INTENT_LOCATION: Int = 1

    /**
     * Type constant to create Geofence Update PendingIntent
     */
    const val PENDING_INTENT_GEOFENCE: Int = 2

    /** Request code of the location update [PendingIntent]. */
    private const val LOCATION_UPDATE_REQUEST_CODE: Int = 10100111

    /** Request code of the geofence update [PendingIntent]. */
    private const val GEOFENCE_UPDATE_REQUEST_CODE: Int = 1001001

    /**
     * Creates [PendingIntent] based on type and flags provided by caller
     *
     * @param context           Application [Context]
     * @param pendingIntentType [PENDING_INTENT_LOCATION] or [PENDING_INTENT_GEOFENCE]
     * @param flags             [PendingIntent.FLAG_NO_CREATE] or [PendingIntent.FLAG_UPDATE_CURRENT]
     */
    @JvmStatic
    fun getPendingIntent(context: Context?, pendingIntentType: Int, flags: Int): PendingIntent? {
        var flags = flags
        if (context == null) {
            return null
        }

        val broadcastSenderRequestCode: Int
        val intent: Intent

        when (pendingIntentType) {
            PENDING_INTENT_LOCATION -> {
                intent = Intent(context.applicationContext, CTLocationUpdateReceiver::class.java)
                intent.setAction(CTGeofenceConstants.ACTION_LOCATION_RECEIVER)
                broadcastSenderRequestCode = LOCATION_UPDATE_REQUEST_CODE
            }

            PENDING_INTENT_GEOFENCE -> {
                intent = Intent(context.applicationContext, CTGeofenceReceiver::class.java)
                intent.setAction(CTGeofenceConstants.ACTION_GEOFENCE_RECEIVER)
                broadcastSenderRequestCode = GEOFENCE_UPDATE_REQUEST_CODE
            }

            else -> throw IllegalArgumentException("invalid pendingIntentType")
        }

        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            /*require mutable PendingIntent object for requesting device location information*/
            flags = flags or PendingIntent.FLAG_MUTABLE
        }

        return PendingIntent.getBroadcast(
            context.applicationContext, broadcastSenderRequestCode, intent,
            flags
        )
    }
}
