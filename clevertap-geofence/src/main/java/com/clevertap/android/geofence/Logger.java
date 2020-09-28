package com.clevertap.android.geofence;

import android.util.Log;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Logger {

    @IntDef({OFF, INFO, DEBUG, VERBOSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogLevel {

    }

    public static final int OFF = -1;

    public static final int INFO = 0;

    public static final int DEBUG = 2;

    public static final int VERBOSE = 3;

    private @LogLevel
    int debugLevel;

    Logger(@LogLevel int level) {
        setDebugLevel(level);
    }

    /**
     * Logs to Debug if the debug level is greater than 1.
     */

    public void debug(String message) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG, message);
        }
    }

    public void debug(String suffix, String message) {
        if (debugLevel > INFO) {
            if (message.length() > 4000) {
                Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message.substring(0, 4000));
                debug(suffix, message.substring(4000));
            } else {
                Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message);
            }
        }
    }

    public void debug(String suffix, String message, Throwable t) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t);
        }
    }

    public void debug(String message, Throwable t) {
        if (debugLevel > INFO) {
            Log.d(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t);
        }
    }

    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     */

    public void info(String message) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG, message);
        }
    }

    public void info(String suffix, String message) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message);
        }
    }

    public void info(String suffix, String message, Throwable t) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t);
        }
    }

    public void info(String message, Throwable t) {
        if (debugLevel >= INFO) {
            Log.i(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void setDebugLevel(@LogLevel int level) {
        this.debugLevel = level;
    }

    /**
     * Logs to Verbose if the debug level is greater than 2.
     */

    public void verbose(String message) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG, message);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void verbose(String suffix, String message) {
        if (debugLevel > DEBUG) {
            if (message.length() > 4000) {
                Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message.substring(0, 4000));
                verbose(suffix, message.substring(4000));
            } else {
                Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void verbose(String suffix, String message, Throwable t) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG + ":" + suffix, message, t);
        }
    }

    public void verbose(String message, Throwable t) {
        if (debugLevel > DEBUG) {
            Log.v(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t);
        }
    }

}
