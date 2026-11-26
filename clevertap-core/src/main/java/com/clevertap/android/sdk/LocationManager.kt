package com.clevertap.android.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.clevertap.android.sdk.CleverTapAPI.Companion.isAppForeground
import com.clevertap.android.sdk.events.BaseEventQueueManager
import java.util.concurrent.Future
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class LocationManager(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val coreMetaData: CoreMetaData,
    private val baseEventQueueManager: BaseEventQueueManager
) : BaseLocationManager() {

    var lastLocationPingTime: Int = 0

    var lastLocationPingTimeForGeofence: Int = 0

    private val logger: Logger = config.logger

    @SuppressLint("MissingPermission")
    override fun findBestLocation(): Location? = try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager?
        if (lm == null) {
            Logger.d("Location Manager is null.")
            null
        } else {
            // The first location with the best (lowest) accuracy value wins, as in the Java loop.
            lm.getProviders(true)
                .mapNotNull { provider ->
                    try {
                        lm.getLastKnownLocation(provider)
                    } catch (e: SecurityException) {
                        //no-op
                        Logger.v("Location security exception", e)
                        null
                    }
                }
                .minByOrNull { it.accuracy }
        }
    } catch (t: Throwable) {
        Logger.v("Couldn't get user's location", t)
        null
    }

    override fun updateLocation(location: Location?): Future<*>? {
        if (location == null) {
            return null
        }

        coreMetaData.locationFromUser = location
        logger.verbose(config.accountId, "Location updated (${location.latitude}, ${location.longitude})")

        // only queue the location ping if we are in the foreground
        if (!coreMetaData.isLocationForGeofence && !isAppForeground()) {
            return null
        }

        // Queue the ping event to transmit location update to server
        // min 10 second interval between location pings
        val now = now
        val forGeofence = coreMetaData.isLocationForGeofence
        val lastPing = if (forGeofence) { lastLocationPingTimeForGeofence } else { lastLocationPingTime }
        if (now <= lastPing + Constants.LOCATION_PING_INTERVAL_IN_SECONDS) {
            return null
        }

        val future = baseEventQueueManager.queueEvent(context, JSONObject(), Constants.PING_EVENT)
        if (forGeofence) {
            lastLocationPingTimeForGeofence = now
            logger.verbose(
                config.accountId,
                "Queuing location ping event for geofence location (${location.latitude}, ${location.longitude})"
            )
        } else {
            lastLocationPingTime = now
            logger.verbose(
                config.accountId,
                "Queuing location ping event for location (${location.latitude}, ${location.longitude})"
            )
        }
        return future
    }

    open val now: Int
        get() = (System.currentTimeMillis() / 1000).toInt()
}
