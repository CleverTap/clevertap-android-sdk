package com.clevertap.android.sdk;

import android.util.Log;

public final class Logger {

    @Deprecated
    private int debugLevel;

    /**
     * Logs to Debug if the debug level is greater than 1.
     */
    @Deprecated
    public static void d(String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    @Deprecated
    public static void d(String suffix, String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
        }
    }
    @Deprecated
    public static void d(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }
    @Deprecated
    public static void d(String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
    }

    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     */
    @Deprecated
    public static void i(String message) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    @SuppressWarnings("unused")
    @Deprecated
    public static void i(String suffix, String message) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
        }
    }

    @SuppressWarnings("unused")
    @Deprecated
    public static void i(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }

    @SuppressWarnings("SameParameterValue")
    @Deprecated
    public static void i(String message, Throwable t) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
    }

    /**
     * Logs to Verbose if the debug level is greater than 2.
     */
    @Deprecated
    public static void v(String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    @Deprecated
    public static void v(String suffix, String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
        }
    }

    @Deprecated
    public static void v(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }

    @Deprecated
    public static void v(String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
    }

    @Deprecated
    Logger(int level) {
        this.debugLevel = level;
    }

    public static void debug(String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    public static void debug(String suffix, String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            if (message.length() > 4000) {
                Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message.substring(0, 4000));
                debug(suffix, message.substring(4000));
            } else {
                Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
            }
        }
    }

    public static void debug(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }

    @SuppressWarnings("unused")
    public static void debug(String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
    }

    public static void debug(String suffix, String tag,  String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message, t);
        }
    }

    public static void debug(String suffix, String tag, String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            if (message.length() > 4000) {
                Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message.substring(0, 4000));
                debug(suffix, tag, message.substring(4000));
            } else {
                Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void info(String message) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    public static void info(String suffix, String message) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
        }
    }

    @SuppressWarnings("unused")
    public static void info(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }

    @SuppressWarnings("unused")
    public static void info(String message, Throwable t) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
    }

    public static void info(String suffix, String tag, String message, Throwable t) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message, t);
        }
    }

    public static void info(String suffix, String tag, String message) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            if (message.length() > 4000) {
                Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message.substring(0, 4000));
                info(suffix, tag, message.substring(4000));
            } else {
                Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message);
            }
        }
    }

    public static void verbose(String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    public static void verbose(String suffix, String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            if (message.length() > 4000) {
                Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message.substring(0, 4000));
                verbose(suffix, message.substring(4000));
            } else {
                Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
            }
        }
    }

    public static void verbose(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }

    public static void verbose(String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
    }

    public static void verbose(String suffix, String tag, String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message, t);
        }
    }

    public static void verbose(String suffix, String tag, String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            if (message.length() > 4000) {
                Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message.substring(0, 4000));
                verbose(suffix, tag, message.substring(4000));
            } else {
                Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + tag + ":" + suffix, message);
            }
        }
    }

    @Deprecated
    public void setDebugLevel(int level){
        this.debugLevel = level;
    }

    private static int getStaticDebugLevel() {
        return CleverTapAPI.getDebugLevel();
    }
}
