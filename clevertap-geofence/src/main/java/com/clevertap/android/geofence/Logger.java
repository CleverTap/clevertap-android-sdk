package com.clevertap.android.geofence;

import android.util.Log;
import androidx.annotation.IntDef;
import com.clevertap.android.sdk.Constants;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
 * Note: This class has been deprecated since v1.3.0 to make logging static across the SDK.
 * It will be removed in the future versions of this SDK.
 * Use Logger class of the core-sdk instead"
 * </p>
 */
@Deprecated
public final class Logger {

    @IntDef({UNSET, OFF, INFO, DEBUG, VERBOSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogLevel {

    }

    public static final int UNSET = -2;

    public static final int OFF = -1;

    public static final int INFO = 0;

    public static final int DEBUG = 2;

    public static final int VERBOSE = 3;

    public static final String COMBINED_LOG_TAG = Constants.CLEVERTAP_LOG_TAG + ":" + CTGeofenceAPI.GEOFENCE_LOG_TAG;

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
            Log.d(COMBINED_LOG_TAG, message);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, message);
        }
    }

    public void debug(String suffix, String message) {
        if (debugLevel > INFO) {
            if (message.length() > 4000) {
                Log.d(Constants.CLEVERTAP_LOG_TAG  + ":" + suffix, message.substring(0, 4000));
                debug(suffix, message.substring(4000));
            } else {
                Log.d(Constants.CLEVERTAP_LOG_TAG  + ":" + suffix, message);
            }
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.debug(suffix, message);
        }
    }

    public void debug(String suffix, String message, Throwable t) {
        if (debugLevel > INFO) {
            Log.d(Constants.CLEVERTAP_LOG_TAG  + ":" + suffix, message, t);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.debug(suffix, message, t);
        }
    }

    public void debug(String message, Throwable t) {
        if (debugLevel > INFO) {
            Log.d(COMBINED_LOG_TAG, message, t);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t);
        }
    }

    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     */

    public void info(String message) {
        if (debugLevel >= INFO) {
            Log.i(COMBINED_LOG_TAG, message);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.info(CTGeofenceAPI.GEOFENCE_LOG_TAG, message);
        }
    }

    public void info(String suffix, String message) {
        if (debugLevel >= INFO) {
            Log.i(Constants.CLEVERTAP_LOG_TAG  + ":" + suffix, message);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.info(suffix, message);
        }
    }

    public void info(String suffix, String message, Throwable t) {
        if (debugLevel >= INFO) {
            Log.i(Constants.CLEVERTAP_LOG_TAG  + ":" + suffix, message, t);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.info(suffix, message, t);
        }
    }

    public void info(String message, Throwable t) {
        if (debugLevel >= INFO) {
            Log.i(COMBINED_LOG_TAG, message, t);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.info(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t);
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
            Log.v(COMBINED_LOG_TAG, message);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG, message);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void verbose(String suffix, String message) {
        if (debugLevel > DEBUG) {
            if (message.length() > 4000) {
                Log.v(Constants.CLEVERTAP_LOG_TAG  + ":" + suffix, message.substring(0, 4000));
                verbose(suffix, message.substring(4000));
            } else {
                Log.v(Constants.CLEVERTAP_LOG_TAG  + ":" + suffix, message);
            }
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.verbose(suffix, message);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void verbose(String suffix, String message, Throwable t) {
        if (debugLevel > DEBUG) {
            Log.v(Constants.CLEVERTAP_LOG_TAG  + ":" + suffix, message, t);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.verbose(suffix, message, t);
        }
    }

    public void verbose(String message, Throwable t) {
        if (debugLevel > DEBUG) {
            Log.v(COMBINED_LOG_TAG, message, t);
        } else if(debugLevel == UNSET) {
            com.clevertap.android.sdk.Logger.verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG, message, t);
        }
    }

}
