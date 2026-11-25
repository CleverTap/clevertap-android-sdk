package com.clevertap.android.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.Nullable;

/**
 * A Factory to create {@link PendingIntent} which will be sent to OS to receive geofence updates
 * through {@link CTGeofenceReceiver} and location updates through {@link CTLocationUpdateReceiver}
 */
class PendingIntentFactory {

    /**
     * Type constant to create Location Update PendingIntent
     */
    static final int PENDING_INTENT_LOCATION = 1;

    /**
     * Type constant to create Geofence Update PendingIntent
     */
    static final int PENDING_INTENT_GEOFENCE = 2;

    /**
     * Creates {@link PendingIntent} based on type and flags provided by caller
     *
     * @param context           Application {@link Context}
     * @param pendingIntentType {@link #PENDING_INTENT_LOCATION} or {@link #PENDING_INTENT_GEOFENCE}
     * @param flags             {@link PendingIntent#FLAG_NO_CREATE} or {@link PendingIntent#FLAG_UPDATE_CURRENT}
     */
    @Nullable
    static PendingIntent getPendingIntent(@Nullable Context context, int pendingIntentType, int flags) {

        if (context == null) {
            return null;
        }

        int broadcastSenderRequestCode;
        Intent intent;

        switch (pendingIntentType) {
            case PENDING_INTENT_LOCATION:
                intent = new Intent(context.getApplicationContext(), CTLocationUpdateReceiver.class);
                intent.setAction(CTGeofenceConstants.ACTION_LOCATION_RECEIVER);
                broadcastSenderRequestCode = 10100111;
                break;
            case PENDING_INTENT_GEOFENCE:
                intent = new Intent(context.getApplicationContext(), CTGeofenceReceiver.class);
                intent.setAction(CTGeofenceConstants.ACTION_GEOFENCE_RECEIVER);
                broadcastSenderRequestCode = 1001001;
                break;
            default:
                throw new IllegalArgumentException("invalid pendingIntentType");
        }

        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            /*require mutable PendingIntent object for requesting device location information*/
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        return PendingIntent.getBroadcast(context.getApplicationContext(), broadcastSenderRequestCode, intent,
                flags);

    }

}
