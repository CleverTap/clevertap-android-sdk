package com.clevertap.android.pushtemplates;

import android.util.Log;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;

/**
 * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
 * This class has been deprecated since v1.x to make logging static across the SDK
 * and will be removed in the future versions of the SDK.
 * Use Logger class of the core-sdk instead"
 * </p>
 */
@Deprecated
public final class PTLog {

    private final int debugLevel;

    PTLog(int level) {
        this.debugLevel = level;
    }

    private int getDebugLevel() {
        return debugLevel;
    }

    private static int getStaticDebugLevel() {
        return TemplateRenderer.getDebugLevel();
    }


    public static void debug(String message) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.DEBUG.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + PTConstants.LOG_TAG, message);
        } else if (getStaticDebugLevel() == -2) {
            Logger.debug(PTConstants.LOG_TAG, message);
        }
    }

    static void info(String message) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + PTConstants.LOG_TAG, message);
        } else if (getStaticDebugLevel() == -2) {
            Logger.info(PTConstants.LOG_TAG, message);
        }
    }

    public static void verbose(String message) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.VERBOSE.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + PTConstants.LOG_TAG, message);
        } else if (getStaticDebugLevel() == -2) {
            Logger.verbose(PTConstants.LOG_TAG, message);
        }
    }

    static void debug(String message, Throwable t) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.DEBUG.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + PTConstants.LOG_TAG, message, t);
        } else if (getStaticDebugLevel() == -2) {
            Logger.debug(PTConstants.LOG_TAG, message, t);
        }
    }

    static void info(String message, Throwable t) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + PTConstants.LOG_TAG, message, t);
        } else if (getStaticDebugLevel() == -2) {
            Logger.info(PTConstants.LOG_TAG, message, t);
        }
    }

    public static void verbose(String message, Throwable t) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.VERBOSE.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + PTConstants.LOG_TAG, message, t);
        } else if (getStaticDebugLevel() == -2) {
            Logger.verbose(PTConstants.LOG_TAG, message, t);
        }
    }


}
