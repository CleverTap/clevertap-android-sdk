package com.clevertap.android.pushtemplates;

import android.util.Log;

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
            Log.d(PTConstants.LOG_TAG, message);
        }
    }

    static void info(String message) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()) {
            Log.i(PTConstants.LOG_TAG, message);
        }
    }

    public static void verbose(String message) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.VERBOSE.intValue()) {
            Log.v(PTConstants.LOG_TAG, message);
        }
    }

    static void debug(String message, Throwable t) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()) {
            Log.d(PTConstants.LOG_TAG, message, t);
        }
    }

    static void info(String message, Throwable t) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()) {
            Log.i(PTConstants.LOG_TAG, message, t);
        }
    }

    public static void verbose(String message, Throwable t) {
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.VERBOSE.intValue()) {
            Log.v(PTConstants.LOG_TAG, message, t);
        }
    }


}
