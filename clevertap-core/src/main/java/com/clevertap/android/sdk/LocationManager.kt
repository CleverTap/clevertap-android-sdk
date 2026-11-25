package com.clevertap.android.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.clevertap.android.sdk.CleverTapAPI.isAppForeground
import com.clevertap.android.sdk.events.BaseEventQueueManager
import java.util.concurrent.Future
import org.json.JSONObject

internal open class LocationManager(
    private val mContext: Context,
    private val mConfig: CleverTapInstanceConfig,
    private val mCoreMetaData: CoreMetaData,
    private val mBaseEventQueueManager: BaseEventQueueManager
) : BaseLocationManager() {

    var lastLocationPingTime: Int = 0

    var lastLocationPingTimeForGeofence: Int = 0

    private val mLogger: Logger = mConfig.logger

    @SuppressLint("MissingPermission")
    override fun _getLocation(): Location? {
        try {
            val lm = mContext
                .getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager?
            if (lm == null) {
                Logger.d("Location Manager is null.")
                return null
            }
            val providers = lm.getProviders(true)
            var bestLocation: Location? = null
            var l: Location? = null
            for (provider in providers) {
                try {
                    l = lm.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    //no-op
                    Logger.v("Location security exception", e)
                }

                if (l == null) {
                    continue
                }
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            return bestLocation
        } catch (t: Throwable) {
            Logger.v("Couldn't get user's location", t)
            return null
        }
    }

    override fun _setLocation(location: Location?): Future<*>? {
        if (location == null) {
            return null
        }

        mCoreMetaData.locationFromUser = location
        mLogger.verbose(
            mConfig.accountId,
            "Location updated (" + location.latitude + ", " + location.longitude + ")"
        )

        // only queue the location ping if we are in the foreground
        if (!mCoreMetaData.isLocationForGeofence && !isAppForeground()) {
            return null
        }

        // Queue the ping event to transmit location update to server
        // min 10 second interval between location pings
        val now = this.now
        var future: Future<*>? = null

        if (mCoreMetaData.isLocationForGeofence && now > (lastLocationPingTimeForGeofence
                    + Constants.LOCATION_PING_INTERVAL_IN_SECONDS)
        ) {
            future = mBaseEventQueueManager.queueEvent(mContext, JSONObject(), Constants.PING_EVENT)
            lastLocationPingTimeForGeofence = now
            mLogger.verbose(
                mConfig.accountId,
                "Queuing location ping event for geofence location (" + location.latitude + ", " + location
                    .longitude + ")"
            )
        } else if (!mCoreMetaData.isLocationForGeofence && now > (lastLocationPingTime
                    + Constants.LOCATION_PING_INTERVAL_IN_SECONDS)
        ) {
            future = mBaseEventQueueManager.queueEvent(mContext, JSONObject(), Constants.PING_EVENT)
            lastLocationPingTime = now
            mLogger.verbose(
                mConfig.accountId,
                "Queuing location ping event for location (" + location.latitude + ", " + location
                    .longitude + ")"
            )
        }

        return future
    }

    open val now: Int
        get() = (System.currentTimeMillis() / 1000).toInt()
}
