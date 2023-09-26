package com.clevertap.android.sdk;

import android.util.Log;

public final class Logger {

    @Deprecated
    private int debugLevel;

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This constructor has been deprecated since v5.2.1 to make logging static across the SDK.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    Logger(int level) {
        this.debugLevel = level;
    }

    /**
     * Logs to Debug if the debug level is greater than 1.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.debug() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void d(String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    /**
     * Logs to Debug if the debug level is greater than 1.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.debug() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void d(String suffix, String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
        }
    }

    /**
     * Logs to Debug if the debug level is greater than 1.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.debug() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void d(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }

    /**
     * Logs to Debug if the debug level is greater than 1.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.debug() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void d(String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
    }

    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.info() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void i(String message) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    @SuppressWarnings("unused")
    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.info() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void i(String suffix, String message) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
        }
    }

    @SuppressWarnings("unused")
    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.info() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void i(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }

    @SuppressWarnings("SameParameterValue")
    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.info() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void i(String message, Throwable t) {
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
    }

    /**
     * Logs to Verbose if the debug level is greater than 2.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.verbose() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void v(String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message);
        }
    }

    /**
     * Logs to Verbose if the debug level is greater than 2.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.verbose() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void v(String suffix, String message) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message);
        }
    }

    /**
     * Logs to Verbose if the debug level is greater than 2.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.verbose() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void v(String suffix, String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t);
        }
    }

    /**
     * Logs to Verbose if the debug level is greater than 2.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.2.1 to make logging static across the SDK.
     * Use Logger.verbose() instead.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static void v(String message, Throwable t) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message, t);
        }
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

    public static void debug(String suffix, String tag, String message, Throwable t) {
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

    private static int getStaticDebugLevel() {
        return CleverTapAPI.getDebugLevel();
    }

    @Deprecated
    public void setDebugLevel(int level) {
        this.debugLevel = level;
    }
}
