package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.CleverTapAPI.isAppForeground;

import android.content.Context;
import android.location.Location;
import java.util.concurrent.Future;
import org.json.JSONObject;

class LocationManager extends BaseLocationManager {

    private int lastLocationPingTime = 0;

    private int lastLocationPingTimeForGeofence = 0;

    private final BaseQueueManager mBaseQueueManager;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final CoreMetaData mCoreMetaData;

    private final DeviceInfo mDeviceInfo;

    private final Logger mLogger;

    LocationManager(CoreState coreState) {
        mContext = coreState.getContext();
        mConfig = coreState.getConfig();
        mDeviceInfo = coreState.getDeviceInfo();
        mLogger = mConfig.getLogger();

        mCoreMetaData = coreState.getCoreMetaData();
        mBaseQueueManager = coreState.getBaseEventQueueManager();
    }

    @Override
    Future<?> _setLocation(Location location) {
        if (location == null) {
            return null;
        }

        mCoreMetaData.setLocationFromUser(location);
        mLogger.verbose(mConfig.getAccountId(),
                "Location updated (" + location.getLatitude() + ", " + location.getLongitude() + ")");

        // only queue the location ping if we are in the foreground
        if (!mCoreMetaData.isLocationForGeofence() && !isAppForeground()) {
            return null;
        }

        // Queue the ping event to transmit location update to server
        // min 10 second interval between location pings
        final int now = (int) (System.currentTimeMillis() / 1000);
        Future<?> future = null;

        if (mCoreMetaData.isLocationForGeofence() && now > (lastLocationPingTimeForGeofence
                + Constants.LOCATION_PING_INTERVAL_IN_SECONDS)) {

            future = mBaseQueueManager.queueEvent(mContext, new JSONObject(), Constants.PING_EVENT);
            lastLocationPingTimeForGeofence = now;
            mLogger.verbose(mConfig.getAccountId(),
                    "Queuing location ping event for geofence location (" + location.getLatitude() + ", " + location
                            .getLongitude() + ")");

        } else if (!mCoreMetaData.isLocationForGeofence() && now > (lastLocationPingTime
                + Constants.LOCATION_PING_INTERVAL_IN_SECONDS)) {

            future = mBaseQueueManager.queueEvent(mContext, new JSONObject(), Constants.PING_EVENT);
            lastLocationPingTime = now;
            mLogger.verbose(mConfig.getAccountId(),
                    "Queuing location ping event for location (" + location.getLatitude() + ", " + location
                            .getLongitude() + ")");
        }

        return future;
    }

}
