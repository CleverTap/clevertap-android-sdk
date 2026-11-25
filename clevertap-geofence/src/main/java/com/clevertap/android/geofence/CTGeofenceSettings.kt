package com.clevertap.android.geofence

import com.clevertap.android.geofence.Logger.Companion.DEBUG
import com.clevertap.android.geofence.Logger.LogLevel

/**
 * Provides various configurable settings to give more control to developer. Instance of this class
 * can be created using [CTGeofenceSettings.Builder] class and can be set on
 * [CTGeofenceAPI.init]
 *
 * For complete details of these
 */
open class CTGeofenceSettings private constructor(builder: Builder) {

    /**
     * Builder class for creating an instance of [CTGeofenceSettings]
     */
    class Builder {

        internal var backgroundLocationUpdates = true

        internal var fastestInterval = GoogleLocationAdapter.INTERVAL_FASTEST_IN_MILLIS

        internal var geofenceMonitoringCount = DEFAULT_GEO_MONITOR_COUNT

        internal var id: String? = null

        internal var interval = GoogleLocationAdapter.INTERVAL_IN_MILLIS

        internal var locationAccuracy = ACCURACY_HIGH

        internal var locationFetchMode = FETCH_LAST_LOCATION_PERIODIC

        @LogLevel
        internal var logLevel = DEBUG

        internal var smallestDisplacement = GoogleLocationAdapter.SMALLEST_DISPLACEMENT_IN_METERS

        internal var geofenceNotificationResponsiveness =
            GoogleGeofenceAdapter.GEOFENCE_NOTIFICATION_RESPONSIVENESS_IN_MILLISECONDS

        fun build(): CTGeofenceSettings {
            // applying minimum interval restriction
            if (interval < GoogleLocationAdapter.INTERVAL_IN_MILLIS) {
                interval = GoogleLocationAdapter.INTERVAL_IN_MILLIS
            }

            // applying minimum fastest interval restriction
            if (fastestInterval < GoogleLocationAdapter.INTERVAL_FASTEST_IN_MILLIS) {
                fastestInterval = GoogleLocationAdapter.INTERVAL_FASTEST_IN_MILLIS
            }

            // applying minimum displacement restriction
            if (smallestDisplacement < GoogleLocationAdapter.SMALLEST_DISPLACEMENT_IN_METERS) {
                smallestDisplacement = GoogleLocationAdapter.SMALLEST_DISPLACEMENT_IN_METERS
            }

            return CTGeofenceSettings(this)
        }

        /**
         * Set Background Location Updates.
         *  * When true, this will allow SDK to register background location updates through any of the above
         * mentioned fetch modes.
         *  * When false, this will inform SDK to fetch location only in foreground when the app is
         * launched or through [CTGeofenceAPI.triggerLocation] and not to register background location updates
         * through
         * any of the above mentioned fetch modes.
         *
         * @param backgroundLocationUpdates true or false. Default value is true.
         * @return [CTGeofenceSettings.Builder]
         */
        fun enableBackgroundLocationUpdates(backgroundLocationUpdates: Boolean): Builder {
            this.backgroundLocationUpdates = backgroundLocationUpdates
            return this
        }

        /**
         * Applicable only for [CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC]
         *
         * Explicitly sets the fastest interval for location updates, in milliseconds. Default value is 30 minutes.
         * Values less than 30 minutes will be ignored by SDK.
         *
         * This controls the fastest rate at which your application will receive location updates,
         * which might be faster than [setInterval] in some situations
         * (for example, if other applications are triggering location updates).
         *
         * This allows your application to passively acquire locations at a rate faster than it
         * actively acquires locations, saving power.
         *
         * Unlike [setInterval], this parameter is exact. Your application will
         * never receive updates faster than this value.
         *
         * @param fastestInterval in milliseconds. Default value is 30 minutes
         * @return [CTGeofenceSettings.Builder]
         */
        fun setFastestInterval(fastestInterval: Long): Builder {
            this.fastestInterval = fastestInterval
            return this
        }

        /**
         * Set number of geofences to monitor using CleverTap Geofence SDK.
         *
         * @param geofenceMonitoringCount must be in the range of 1-100. Default value is 50
         * @return [CTGeofenceSettings.Builder]
         */
        fun setGeofenceMonitoringCount(geofenceMonitoringCount: Int): Builder {
            this.geofenceMonitoringCount = geofenceMonitoringCount
            return this
        }

        /**
         * Set CleverTap Account Id.
         *
         * @return [CTGeofenceSettings.Builder]
         */
        fun setId(id: String?): Builder {
            this.id = id
            return this
        }

        /**
         * Applicable for both fetch modes.
         *
         * Set the desired interval for active location updates, in milliseconds. Default value is 30 minutes.
         * Values less than 30 minutes will be ignored by SDK.
         *
         *  * For [CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC]:
         * This interval is inexact. You may not receive updates at all (if no location sources are available),
         * or you may receive them slower than requested. You may also receive them faster than requested
         * (if other applications are requesting location at a faster interval).
         *
         * The fastest rate that you will receive updates can be controlled with [setFastestInterval].
         * By default this fastest rate is 30 minutes.
         *
         *  * For [CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC]:
         * This interval is defined using [androidx.work.WorkManager].
         *
         * @param interval in milliseconds. Default value is 30 minutes
         * @return [CTGeofenceSettings.Builder]
         */
        fun setInterval(interval: Long): Builder {
            this.interval = interval
            return this
        }

        /**
         * Set location accuracy. Applicable only for [FETCH_CURRENT_LOCATION_PERIODIC]
         *
         * @param locationAccuracy can be one of [ACCURACY_HIGH], [ACCURACY_LOW] or
         * [ACCURACY_MEDIUM]. Default value is [ACCURACY_HIGH]
         * @return [CTGeofenceSettings.Builder]
         */
        fun setLocationAccuracy(locationAccuracy: Byte): Builder {
            this.locationAccuracy = locationAccuracy
            return this
        }

        /**
         * Set location fetch mode
         *
         * @param locationFetchMode can be one of [FETCH_CURRENT_LOCATION_PERIODIC] or
         * [FETCH_LAST_LOCATION_PERIODIC]. Default value is [FETCH_LAST_LOCATION_PERIODIC]
         * @return [CTGeofenceSettings.Builder]
         */
        fun setLocationFetchMode(locationFetchMode: Byte): Builder {
            this.locationFetchMode = locationFetchMode
            return this
        }

        /**
         * Set log level
         *
         * @param logLevel can be one of [Logger.DEBUG], [Logger.INFO], [Logger.VERBOSE] or
         * [Logger.OFF]. Default value is [Logger.DEBUG]
         * @return [CTGeofenceSettings.Builder]
         */
        fun setLogLevel(@LogLevel logLevel: Int): Builder {
            this.logLevel = logLevel
            return this
        }

        /**
         * Applicable only for [CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC]
         *
         * Set the minimum displacement between location updates in meters. Default value is 200 meters.
         * Values less than 200 meters will be ignored by SDK.
         *
         * @param smallestDisplacement in meters. Default value is 200 meters.
         * @return [CTGeofenceSettings.Builder]
         */
        fun setSmallestDisplacement(smallestDisplacement: Float): Builder {
            this.smallestDisplacement = smallestDisplacement
            return this
        }

        /**
         * Sets the best-effort notification responsiveness of the geofence. Defaults to 0.
         * Setting a big responsiveness value, for example 5 minutes, can save power significantly.
         * For example, if you set a responsiveness value of five minutes your app only checks for an entrance or
         * exit
         * alert once every five minutes.Setting lower values doesn't necessarily mean that users are notified within
         * that time period (for example, if you set a value of 5 seconds it may take a bit longer than that to
         * receive the alert).
         *
         * @param geofenceNotificationResponsiveness in (milliseconds) defines the best-effort description of how
         * soon should the callback be called when the transition associated
         * with the Geofence is triggered.
         * For instance, if set to 300000 milliseconds the callback will be
         * called 5 minutes within entering or
         * exiting the geofence.
         * @return [CTGeofenceSettings.Builder]
         */
        fun setGeofenceNotificationResponsiveness(
            geofenceNotificationResponsiveness: Int
        ): Builder {
            this.geofenceNotificationResponsiveness = geofenceNotificationResponsiveness
            return this
        }
    }

    private val backgroundLocationUpdatesValue: Boolean = builder.backgroundLocationUpdates

    private val fastestIntervalValue: Long = builder.fastestInterval

    private val geofenceMonitoringCountValue: Int = builder.geofenceMonitoringCount

    private val idValue: String? = builder.id

    private val intervalValue: Long = builder.interval

    private val locationAccuracyValue: Byte = builder.locationAccuracy

    private val locationFetchModeValue: Byte = builder.locationFetchMode // WorkManager or BroadcastReceiver

    private val geofenceNotificationResponsivenessValue: Int = builder.geofenceNotificationResponsiveness

    @LogLevel
    private val logLevelValue: Int = builder.logLevel

    private val smallestDisplacementValue: Float = builder.smallestDisplacement

    open fun getFastestInterval(): Long {
        return fastestIntervalValue
    }

    /** Kotlin property syntax, equivalent to [getFastestInterval]. */
    val fastestInterval: Long
        @JvmSynthetic @JvmName("ktGetFastestInterval") inline get() = getFastestInterval()

    open fun getGeofenceMonitoringCount(): Int {
        return geofenceMonitoringCountValue
    }

    /** Kotlin property syntax, equivalent to [getGeofenceMonitoringCount]. */
    val geofenceMonitoringCount: Int
        @JvmSynthetic @JvmName("ktGetGeofenceMonitoringCount") inline get() = getGeofenceMonitoringCount()

    open fun getId(): String? {
        return idValue
    }

    /** Kotlin property syntax, equivalent to [getId]. */
    val id: String?
        @JvmSynthetic @JvmName("ktGetId") inline get() = getId()

    open fun getInterval(): Long {
        return intervalValue
    }

    /** Kotlin property syntax, equivalent to [getInterval]. */
    val interval: Long
        @JvmSynthetic @JvmName("ktGetInterval") inline get() = getInterval()

    open fun getLocationAccuracy(): Int {
        return locationAccuracyValue.toInt()
    }

    /** Kotlin property syntax, equivalent to [getLocationAccuracy]. */
    val locationAccuracy: Int
        @JvmSynthetic @JvmName("ktGetLocationAccuracy") inline get() = getLocationAccuracy()

    open fun getLocationFetchMode(): Int {
        return locationFetchModeValue.toInt()
    }

    /** Kotlin property syntax, equivalent to [getLocationFetchMode]. */
    val locationFetchMode: Int
        @JvmSynthetic @JvmName("ktGetLocationFetchMode") inline get() = getLocationFetchMode()

    @LogLevel
    open fun getLogLevel(): Int {
        return logLevelValue
    }

    /** Kotlin property syntax, equivalent to [getLogLevel]. */
    val logLevel: Int
        @JvmSynthetic @JvmName("ktGetLogLevel") inline get() = getLogLevel()

    open fun getSmallestDisplacement(): Float {
        return smallestDisplacementValue
    }

    /** Kotlin property syntax, equivalent to [getSmallestDisplacement]. */
    val smallestDisplacement: Float
        @JvmSynthetic @JvmName("ktGetSmallestDisplacement") inline get() = getSmallestDisplacement()

    open fun isBackgroundLocationUpdatesEnabled(): Boolean {
        return backgroundLocationUpdatesValue
    }

    /** Kotlin property syntax, equivalent to [isBackgroundLocationUpdatesEnabled]. */
    val isBackgroundLocationUpdatesEnabled: Boolean
        @JvmSynthetic @JvmName("ktIsBackgroundLocationUpdatesEnabled") inline get() = isBackgroundLocationUpdatesEnabled()

    open fun getGeofenceNotificationResponsiveness(): Int {
        return geofenceNotificationResponsivenessValue
    }

    /** Kotlin property syntax, equivalent to [getGeofenceNotificationResponsiveness]. */
    val geofenceNotificationResponsiveness: Int
        @JvmSynthetic @JvmName("ktGetGeofenceNotificationResponsiveness") inline get() = getGeofenceNotificationResponsiveness()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as CTGeofenceSettings
        return backgroundLocationUpdatesValue == that.backgroundLocationUpdatesValue &&
                locationAccuracyValue == that.locationAccuracyValue &&
                locationFetchModeValue == that.locationFetchModeValue &&
                logLevelValue == that.logLevelValue && geofenceMonitoringCountValue == that.geofenceMonitoringCountValue
                && idValue!! == that.idValue && intervalValue == that.intervalValue
                && fastestIntervalValue == that.fastestIntervalValue
                && smallestDisplacementValue == that.smallestDisplacementValue
                && geofenceNotificationResponsivenessValue == that.geofenceNotificationResponsivenessValue
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    companion object {
        /**
         * Provides the most accurate location possible, which is computed using as many inputs as necessary
         * (it enables GPS, Wi-Fi, and cell, and uses a variety of Sensors), and may cause significant battery drain.
         */
        const val ACCURACY_HIGH: Byte = 1

        /**
         * Provides accurate location while optimising for power. Very rarely uses GPS.
         * Typically uses a combination of Wi-Fi and cell information to compute device location.
         */
        @Suppress("unused")
        const val ACCURACY_MEDIUM: Byte = 2

        /**
         * Largely relies on cell towers and avoids GPS and Wi-Fi inputs, providing coarse (city-level)
         * accuracy with minimal battery drain.
         */
        @Suppress("unused")
        const val ACCURACY_LOW: Byte = 3

        /**
         * This value will use Periodic Receiver which will fetch current device location from OS.
         * Accuracy and battery optimisation can vary from high to low based on interval, displacement
         * and accuracy values provided in CTGeofenceSettings.
         */
        const val FETCH_CURRENT_LOCATION_PERIODIC: Byte = 1 // BroadcastReceiver // current

        /**
         * This value will use Periodic work manager which will fetch last known location from OS
         * periodically(use setInterval() to set the interval).
         * Location fetched using this may be less accurate and might be null in case Location is
         * turned off in the device settings, the device never recorded its location or Google Play services
         * on the device have restarted.
         * This will give better battery optimisation with less location accuracy.
         */
        const val FETCH_LAST_LOCATION_PERIODIC: Byte = 2 // Work Manager // call getLastLocation()

        const val DEFAULT_GEO_MONITOR_COUNT: Int = 50
    }
}
