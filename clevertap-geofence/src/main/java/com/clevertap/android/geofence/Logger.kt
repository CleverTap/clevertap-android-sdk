package com.clevertap.android.geofence

import android.util.Log
import androidx.annotation.IntDef

/**
 * The Geofence SDK's logcat logger, returned by [CTGeofenceAPI.getLogger]. Messages are logged under
 * the tag `CTGeofence`, or `CTGeofence:<suffix>` for the overloads that take a suffix. Info messages
 * are logged at [INFO] and above, debug messages above [INFO] and verbose messages above [DEBUG]; the
 * level comes from [CTGeofenceSettings.getLogLevel]. Overloads without a [Throwable] throw a
 * [NullPointerException] for a `null` message when the level is enabled.
 */
public class Logger internal constructor(@LogLevel level: Int) {

    /** The log levels accepted by [setDebugLevel]: [OFF], [INFO], [DEBUG] or [VERBOSE]. */
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

    /**
     * Logs a debug message under the tag `CTGeofence:<suffix>`.
     *
     * @param suffix appended to the log tag
     * @param message the message
     */
    public fun debug(suffix: String?, message: String?) {
        if (debugLevel > INFO) {
            if (message!!.length > 4000) {
                Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message.substring(0, 4000))
                debug(suffix, message.substring(4000))
            } else {
                Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message)
            }
        }
    }

    /**
     * Logs a debug message and a throwable under the tag `CTGeofence:<suffix>`.
     *
     * @param suffix appended to the log tag
     * @param message the message
     * @param t the throwable whose stack trace is logged
     */
    public fun debug(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    /**
     * Logs a debug message and a throwable under the tag `CTGeofence`.
     *
     * @param message the message
     * @param t the throwable whose stack trace is logged
     */
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

    /**
     * Logs a info message under the tag `CTGeofence:<suffix>`.
     *
     * @param suffix appended to the log tag
     * @param message the message
     */
    public fun info(suffix: String?, message: String?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message!!)
        }
    }

    /**
     * Logs a info message and a throwable under the tag `CTGeofence:<suffix>`.
     *
     * @param suffix appended to the log tag
     * @param message the message
     * @param t the throwable whose stack trace is logged
     */
    public fun info(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    /**
     * Logs a info message and a throwable under the tag `CTGeofence`.
     *
     * @param message the message
     * @param t the throwable whose stack trace is logged
     */
    public fun info(message: String?, t: Throwable?) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t)
        }
    }

    /**
     * Sets the level of this logger.
     *
     * @param level one of [OFF], [INFO], [DEBUG] or [VERBOSE]
     */
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

    /**
     * Logs a verbose message under the tag `CTGeofence:<suffix>`.
     *
     * @param suffix appended to the log tag
     * @param message the message
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun verbose(suffix: String?, message: String?) {
        if (debugLevel > DEBUG) {
            if (message!!.length > 4000) {
                Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message.substring(0, 4000))
                verbose(suffix, message.substring(4000))
            } else {
                Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message)
            }
        }
    }

    /**
     * Logs a verbose message and a throwable under the tag `CTGeofence:<suffix>`.
     *
     * @param suffix appended to the log tag
     * @param message the message
     * @param t the throwable whose stack trace is logged
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun verbose(suffix: String?, message: String?, t: Throwable?) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t)
        }
    }

    /**
     * Logs a verbose message and a throwable under the tag `CTGeofence`.
     *
     * @param message the message
     * @param t the throwable whose stack trace is logged
     */
    public fun verbose(message: String?, t: Throwable?) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t)
        }
    }

    public companion object {
        /** Log level that disables logging. */
        public const val OFF: Int = -1

        /** Log level for info messages. */
        public const val INFO: Int = 0

        /** Log level for debug and info messages (the default). */
        public const val DEBUG: Int = 2

        /** Log level for verbose, debug and info messages. */
        public const val VERBOSE: Int = 3
    }
}
