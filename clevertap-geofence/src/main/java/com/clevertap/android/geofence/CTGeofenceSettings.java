package com.clevertap.android.geofence;

import static com.clevertap.android.geofence.Logger.DEBUG;

import com.clevertap.android.geofence.Logger.LogLevel;
import com.clevertap.android.sdk.CleverTapAPI;

/**
 * Provides various configurable settings to give more control to developer. Instance of this class
 * can be created using {@link CTGeofenceSettings.Builder} class and can be set on
 * {@link CTGeofenceAPI#init(CTGeofenceSettings, CleverTapAPI)}
 * <p>
 * For complete details of these
 */
public class CTGeofenceSettings {


    /**
     * Builder class for creating an instance of {@link CTGeofenceSettings}
     */
    public static final class Builder {

        private boolean backgroundLocationUpdates = true;

        private long fastestInterval = GoogleLocationAdapter.INTERVAL_FASTEST_IN_MILLIS;

        private int geofenceMonitoringCount = DEFAULT_GEO_MONITOR_COUNT;

        private String id;

        private long interval = GoogleLocationAdapter.INTERVAL_IN_MILLIS;

        private byte locationAccuracy = ACCURACY_HIGH;

        private byte locationFetchMode = FETCH_LAST_LOCATION_PERIODIC;

        private @LogLevel
        int logLevel = DEBUG;

        private float smallestDisplacement = GoogleLocationAdapter.SMALLEST_DISPLACEMENT_IN_METERS;

        public Builder() {

        }

        public CTGeofenceSettings build() {

            // applying minimum interval restriction
            if (interval < GoogleLocationAdapter.INTERVAL_IN_MILLIS) {
                interval = GoogleLocationAdapter.INTERVAL_IN_MILLIS;
            }

            // applying minimum fastest interval restriction
            if (fastestInterval < GoogleLocationAdapter.INTERVAL_FASTEST_IN_MILLIS) {
                fastestInterval = GoogleLocationAdapter.INTERVAL_FASTEST_IN_MILLIS;
            }

            // applying minimum displacement restriction
            if (smallestDisplacement < GoogleLocationAdapter.SMALLEST_DISPLACEMENT_IN_METERS) {
                smallestDisplacement = GoogleLocationAdapter.SMALLEST_DISPLACEMENT_IN_METERS;
            }

            return new CTGeofenceSettings(this);
        }

        /**
         * Set Background Location Updates.<br>
         * <li>When true, this will allow SDK to register background location updates through any of the above
         * mentioned fetch modes.</li>
         * <li>When false, this will inform SDK to fetch location only in foreground when the app is
         * launched or through {@link CTGeofenceAPI#triggerLocation()} and not to register background location updates
         * through
         * any of the above mentioned fetch modes.</li>
         *
         * @param backgroundLocationUpdates true or false. Default value is true.
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder enableBackgroundLocationUpdates(boolean backgroundLocationUpdates) {
            this.backgroundLocationUpdates = backgroundLocationUpdates;
            return this;
        }

        /**
         * Applicable only for {@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC}<br>
         * <p>
         * Explicitly sets the fastest interval for location updates, in milliseconds. Default value is 30 minutes.
         * Values less than 30 minutes will be ignored by SDK.<br>
         * <p>
         * This controls the fastest rate at which your application will receive location updates,
         * which might be faster than {@link #setInterval(long)} in some situations
         * (for example, if other applications are triggering location updates).<br>
         * <p>
         * This allows your application to passively acquire locations at a rate faster than it
         * actively acquires locations, saving power.<br>
         * <p>
         * Unlike {@link #setInterval(long)}, this parameter is exact. Your application will
         * never receive updates faster than this value.
         *
         * @param fastestInterval in milliseconds. Default value is 30 minutes
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder setFastestInterval(long fastestInterval) {
            this.fastestInterval = fastestInterval;
            return this;
        }

        /**
         * Set number of geofences to monitor using CleverTap Geofence SDK.
         *
         * @param geofenceMonitoringCount must be in the range of 1-100. Default value is 50
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder setGeofenceMonitoringCount(int geofenceMonitoringCount) {
            this.geofenceMonitoringCount = geofenceMonitoringCount;
            return this;
        }

        /**
         * Set CleverTap Account Id.
         *
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder setId(String id) {
            this.id = id;
            return this;
        }

        /**
         * Applicable for both fetch modes.<br>
         * <p>
         * Set the desired interval for active location updates, in milliseconds. Default value is 30 minutes.
         * Values less than 30 minutes will be ignored by SDK.<br>
         *
         * <li>For {@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC}: </li>
         * This interval is inexact. You may not receive updates at all (if no location sources are available),
         * or you may receive them slower than requested. You may also receive them faster than requested
         * (if other applications are requesting location at a faster interval).<br>
         * <p>
         * The fastest rate that you will receive updates can be controlled with {@link #setFastestInterval(long)}.
         * By default this fastest rate is 30 minutes.
         *
         * <li>For {@link CTGeofenceSettings#FETCH_LAST_LOCATION_PERIODIC}: </li>
         * This interval is defined using {@link androidx.work.WorkManager}.
         *
         * @param interval in milliseconds. Default value is 30 minutes
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder setInterval(long interval) {
            this.interval = interval;
            return this;
        }

        /**
         * Set location accuracy. Applicable only for {@link #FETCH_CURRENT_LOCATION_PERIODIC}<br>
         *
         * @param locationAccuracy can be one of {@link #ACCURACY_HIGH}, {@link #ACCURACY_LOW} or
         *                         {@link #ACCURACY_MEDIUM}. Default value is {@link #ACCURACY_HIGH}
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder setLocationAccuracy(byte locationAccuracy) {
            this.locationAccuracy = locationAccuracy;
            return this;
        }

        /**
         * Set location fetch mode
         *
         * @param locationFetchMode can be one of {@link #FETCH_CURRENT_LOCATION_PERIODIC} or
         *                          {@link #FETCH_LAST_LOCATION_PERIODIC}. Default value is {@link
         *                          #FETCH_LAST_LOCATION_PERIODIC}
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder setLocationFetchMode(byte locationFetchMode) {
            this.locationFetchMode = locationFetchMode;
            return this;
        }

        /**
         * Set log level
         *
         * @param logLevel can be one of {@link Logger#DEBUG}, {@link Logger#INFO}, {@link Logger#VERBOSE} or
         *                 {@link Logger#OFF}. Default value is {@link Logger#DEBUG}
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder setLogLevel(@LogLevel int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Applicable only for {@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC}<br>
         * <p>
         * Set the minimum displacement between location updates in meters. Default value is 200 meters.
         * Values less than 200 meters will be ignored by SDK.
         *
         * @param smallestDisplacement in meters. Default value is 200 meters.
         * @return {@link CTGeofenceSettings.Builder}
         */
        public CTGeofenceSettings.Builder setSmallestDisplacement(float smallestDisplacement) {
            this.smallestDisplacement = smallestDisplacement;
            return this;
        }
    }

    /**
     * Provides the most accurate location possible, which is computed using as many inputs as necessary
     * (it enables GPS, Wi-Fi, and cell, and uses a variety of Sensors), and may cause significant battery drain.
     */
    public static final byte ACCURACY_HIGH = 1;

    /**
     * Provides accurate location while optimising for power. Very rarely uses GPS.
     * Typically uses a combination of Wi-Fi and cell information to compute device location.
     */
    @SuppressWarnings("unused")
    public static final byte ACCURACY_MEDIUM = 2;

    /**
     * Largely relies on cell towers and avoids GPS and Wi-Fi inputs, providing coarse (city-level)
     * accuracy with minimal battery drain.
     */
    @SuppressWarnings("unused")
    public static final byte ACCURACY_LOW = 3;

    /**
     * This value will use Periodic Receiver which will fetch current device location from OS.
     * <br>Accuracy and battery optimisation can vary from high to low based on interval, displacement
     * and accuracy values provided in CTGeofenceSettings.
     */
    public static final byte FETCH_CURRENT_LOCATION_PERIODIC = 1; // BroadcastReceiver // current

    /**
     * This value will use Periodic work manager which will fetch last known location from OS
     * periodically(use setInterval() to set the interval).
     * <br>Location fetched using this may be less accurate and might be null in case Location is
     * turned off in the device settings, the device never recorded its location or Google Play services
     * on the device have restarted.
     * <br>This will give better battery optimisation with less location accuracy.
     */
    public static final byte FETCH_LAST_LOCATION_PERIODIC = 2; // Work Manager // call getLastLocation()

    public static final int DEFAULT_GEO_MONITOR_COUNT = 50;

    private final boolean backgroundLocationUpdates;

    private final long fastestInterval;

    private final int geofenceMonitoringCount;

    private final String id;

    private final long interval;

    private final byte locationAccuracy;

    private final byte locationFetchMode; // WorkManager or BroadcastReceiver

    private final @LogLevel
    int logLevel;

    private final float smallestDisplacement;

    private CTGeofenceSettings(Builder builder) {
        backgroundLocationUpdates = builder.backgroundLocationUpdates;
        locationAccuracy = builder.locationAccuracy;
        locationFetchMode = builder.locationFetchMode;
        logLevel = builder.logLevel;
        geofenceMonitoringCount = builder.geofenceMonitoringCount;
        id = builder.id;
        interval = builder.interval;
        fastestInterval = builder.fastestInterval;
        smallestDisplacement = builder.smallestDisplacement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CTGeofenceSettings that = (CTGeofenceSettings) o;
        return backgroundLocationUpdates == that.backgroundLocationUpdates &&
                locationAccuracy == that.locationAccuracy &&
                locationFetchMode == that.locationFetchMode &&
                logLevel == that.logLevel && geofenceMonitoringCount == that.geofenceMonitoringCount
                && id.equals(that.id) && interval == that.interval && fastestInterval == that.fastestInterval
                && smallestDisplacement == that.smallestDisplacement;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public long getFastestInterval() {
        return fastestInterval;
    }

    public int getGeofenceMonitoringCount() {
        return geofenceMonitoringCount;
    }

    public String getId() {
        return id;
    }

    public long getInterval() {
        return interval;
    }

    public int getLocationAccuracy() {
        return locationAccuracy;
    }

    public int getLocationFetchMode() {
        return locationFetchMode;
    }

    public @LogLevel
    int getLogLevel() {
        return logLevel;
    }

    public float getSmallestDisplacement() {
        return smallestDisplacement;
    }

    public boolean isBackgroundLocationUpdatesEnabled() {
        return backgroundLocationUpdates;
    }
}
