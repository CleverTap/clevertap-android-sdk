package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.CleverTapAPI.isAppForeground;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import com.clevertap.android.sdk.events.BaseEventQueueManager;
import java.util.List;
import java.util.concurrent.Future;
import org.json.JSONObject;

class LocationManager extends BaseLocationManager {

    private int lastLocationPingTime = 0;

    private int lastLocationPingTimeForGeofence = 0;

    private final BaseEventQueueManager mBaseEventQueueManager;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final CoreMetaData mCoreMetaData;

    LocationManager(Context context,
            CleverTapInstanceConfig config,
            CoreMetaData coreMetaData,
            BaseEventQueueManager baseEventQueueManager) {
        mContext = context;
        mConfig = config;
        mCoreMetaData = coreMetaData;
        mBaseEventQueueManager = baseEventQueueManager;
    }

    @SuppressLint("MissingPermission")
    @Override
    public Location _getLocation() {
        try {
            android.location.LocationManager lm = (android.location.LocationManager) mContext
                    .getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                Logger.debug("Location Manager is null.");
                return null;
            }
            List<String> providers = lm.getProviders(true);
            Location bestLocation = null;
            Location l = null;
            for (String provider : providers) {
                try {
                    l = lm.getLastKnownLocation(provider);
                } catch (SecurityException e) {
                    //no-op
                    Logger.verbose("Location security exception", e);
                }

                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }

            return bestLocation;
        } catch (Throwable t) {
            Logger.verbose("Couldn't get user's location", t);
            return null;
        }
    }

    @Override
    Future<?> _setLocation(Location location) {
        if (location == null) {
            return null;
        }

        mCoreMetaData.setLocationFromUser(location);
        Logger.verbose(mConfig.getAccountId(),
                "Location updated (" + location.getLatitude() + ", " + location.getLongitude() + ")");

        // only queue the location ping if we are in the foreground
        if (!mCoreMetaData.isLocationForGeofence() && !isAppForeground()) {
            return null;
        }

        // Queue the ping event to transmit location update to server
        // min 10 second interval between location pings
        final int now = getNow();
        Future<?> future = null;

        if (mCoreMetaData.isLocationForGeofence() && now > (lastLocationPingTimeForGeofence
                + Constants.LOCATION_PING_INTERVAL_IN_SECONDS)) {

            future = mBaseEventQueueManager.queueEvent(mContext, new JSONObject(), Constants.PING_EVENT);
            setLastLocationPingTimeForGeofence(now);
            Logger.verbose(mConfig.getAccountId(),
                    "Queuing location ping event for geofence location (" + location.getLatitude() + ", " + location
                            .getLongitude() + ")");

        } else if (!mCoreMetaData.isLocationForGeofence() && now > (lastLocationPingTime
                + Constants.LOCATION_PING_INTERVAL_IN_SECONDS)) {

            future = mBaseEventQueueManager.queueEvent(mContext, new JSONObject(), Constants.PING_EVENT);
            setLastLocationPingTime(now);
            Logger.verbose(mConfig.getAccountId(),
                    "Queuing location ping event for location (" + location.getLatitude() + ", " + location
                            .getLongitude() + ")");
        }

        return future;
    }

    int getLastLocationPingTime() {
        return lastLocationPingTime;
    }

    void setLastLocationPingTime(final int lastLocationPingTime) {
        this.lastLocationPingTime = lastLocationPingTime;
    }

    int getLastLocationPingTimeForGeofence() {
        return lastLocationPingTimeForGeofence;
    }

    void setLastLocationPingTimeForGeofence(final int lastLocationPingTimeForGeofence) {
        this.lastLocationPingTimeForGeofence = lastLocationPingTimeForGeofence;
    }

    int getNow() {
        return (int) (System.currentTimeMillis() / 1000);
    }

}
