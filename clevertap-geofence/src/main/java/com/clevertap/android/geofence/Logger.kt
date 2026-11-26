package com.clevertap.android.geofence

import android.util.Log
import androidx.annotation.IntDef

public class Logger internal constructor(@LogLevel level: Int) {

    @IntDef(OFF, INFO, DEBUG, VERBOSE)
    @Retention(AnnotationRetention.SOURCE)
    public annotation class LogLevel

    @LogLevel
    private var debugLevel = 0

    init {
        setDebugLevel(level)
    }

    /**
     * Logs to Debug if the debug level is greater than 1.
     */
    public fun debug(message: String?) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG, message!!)
        }
    }

    public fun debug(suffix: String?, message: String?) {
        if (debugLevel > INFO) {
            if (message!!.length > MAX_LOG_LENGTH) {
                Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message.substring(0, MAX_LOG_LENGTH))
                debug(suffix, message.substring(MAX_LOG_LENGTH))
            } else {
                Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message)
            }
        }
    }

    public fun debug(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    public fun debug(message: String?, t: Throwable?) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t)
        }
    }

    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     */
    public fun info(message: String?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG, message!!)
        }
    }

    public fun info(suffix: String?, message: String?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message!!)
        }
    }

    public fun info(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    public fun info(message: String?, t: Throwable?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    public fun setDebugLevel(@LogLevel level: Int) {
        this.debugLevel = level
    }

    /**
     * Logs to Verbose if the debug level is greater than 2.
     */
    public fun verbose(message: String?) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG, message!!)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    public fun verbose(suffix: String?, message: String?) {
        if (debugLevel > DEBUG) {
            if (message!!.length > MAX_LOG_LENGTH) {
                Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message.substring(0, MAX_LOG_LENGTH))
                verbose(suffix, message.substring(MAX_LOG_LENGTH))
            } else {
                Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message)
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    public fun verbose(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    public fun verbose(message: String?, t: Throwable?) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t)
        }
    }

    public companion object {
        public const val OFF: Int = -1

        public const val INFO: Int = 0

        public const val DEBUG: Int = 2

        public const val VERBOSE: Int = 3

        /** Longest message passed to one [Log] call; longer messages are split into chunks. */
        private const val MAX_LOG_LENGTH: Int = 4000
    }
}
