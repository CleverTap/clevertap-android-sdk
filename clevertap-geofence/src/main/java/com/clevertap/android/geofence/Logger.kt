package com.clevertap.android.geofence

import android.util.Log
import androidx.annotation.IntDef

class Logger internal constructor(@LogLevel level: Int) {

    @IntDef(OFF, INFO, DEBUG, VERBOSE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LogLevel

    @LogLevel
    private var debugLevel = 0

    init {
        setDebugLevel(level)
    }

    /**
     * Logs to Debug if the debug level is greater than 1.
     */
    fun debug(message: String?) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG, message!!)
        }
    }

    fun debug(suffix: String?, message: String?) {
        if (debugLevel > INFO) {
            if (message!!.length > 4000) {
                Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message.substring(0, 4000))
                debug(suffix, message.substring(4000))
            } else {
                Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message)
            }
        }
    }

    fun debug(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    fun debug(message: String?, t: Throwable?) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t)
        }
    }

    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     */
    fun info(message: String?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG, message!!)
        }
    }

    fun info(suffix: String?, message: String?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message!!)
        }
    }

    fun info(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    fun info(message: String?, t: Throwable?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setDebugLevel(@LogLevel level: Int) {
        this.debugLevel = level
    }

    /**
     * Logs to Verbose if the debug level is greater than 2.
     */
    fun verbose(message: String?) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG, message!!)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun verbose(suffix: String?, message: String?) {
        if (debugLevel > DEBUG) {
            if (message!!.length > 4000) {
                Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message.substring(0, 4000))
                verbose(suffix, message.substring(4000))
            } else {
                Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message)
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun verbose(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    fun verbose(message: String?, t: Throwable?) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t)
        }
    }

    companion object {
        const val OFF: Int = -1

        const val INFO: Int = 0

        const val DEBUG: Int = 2

        const val VERBOSE: Int = 3
    }
}
