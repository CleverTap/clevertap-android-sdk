package com.clevertap.android.sdk;

import android.util.Log;

final class Logger {

    private int debugLevel;

    Logger(int level){
        this.debugLevel = level;
    }

    private int getDebugLevel() {
        return debugLevel;
    }

    private static int getStaticDebugLevel(){
        return CleverTapAPI.getDebugLevel();
    }

    /**
     * Logs to Debug if the debug level is greater than 1.
     */
    static void d(String message){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()){
            Log.d(Constants.CLEVERTAP_LOG_TAG,message);
        }
    }

    static void d(String suffix, String message){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()){
            Log.d(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message);
        }
    }
    static void d(String suffix, String message, Throwable t){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()){
            Log.d(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message,t);
        }
    }
    static void d(String message, Throwable t){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()){
            Log.d(Constants.CLEVERTAP_LOG_TAG,message,t);
        }
    }
    void debug(String message){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()){
            Log.d(Constants.CLEVERTAP_LOG_TAG,message);
        }
    }

    void debug(String suffix, String message){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()){
            Log.d(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message);
        }
    }
    void debug(String suffix, String message, Throwable t){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()){
            Log.d(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message,t);
        }
    }
    @SuppressWarnings("unused")
    void debug(String message, Throwable t){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()){
            Log.d(Constants.CLEVERTAP_LOG_TAG,message,t);
        }
    }
    /**
     * Logs to Verbose if the debug level is greater than 2.
     */
    static void v(String message){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()){
            Log.v(Constants.CLEVERTAP_LOG_TAG,message);
        }
    }
    static void v(String suffix, String message){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()){
            Log.v(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message);
        }
    }
    static void v(String suffix, String message, Throwable t){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()){
            Log.v(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message,t);
        }
    }
    static void v(String message, Throwable t){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()){
            Log.v(Constants.CLEVERTAP_LOG_TAG,message,t);
        }
    }
    void verbose(String message){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()){
            Log.v(Constants.CLEVERTAP_LOG_TAG,message);
        }
    }
    void verbose(String suffix, String message){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()){
            Log.v(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message);
        }
    }
    void verbose(String suffix, String message, Throwable t){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()){
            Log.v(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message,t);
        }
    }
    void verbose(String message, Throwable t){
        if(getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()){
            Log.v(Constants.CLEVERTAP_LOG_TAG,message,t);
        }
    }

    /**
     * Logs to Info if the debug level is greater than or equal to 1.
     */
    static void i(String message){
        if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()){
            Log.i(Constants.CLEVERTAP_LOG_TAG,message);
        }
    }
    @SuppressWarnings("unused")
    static void i(String suffix, String message){
        if(getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()){
            Log.i(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message);
        }
    }
    @SuppressWarnings("unused")
    static void i(String suffix, String message, Throwable t){
        if(getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()){
            Log.i(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message,t);
        }
    }
    @SuppressWarnings("SameParameterValue")
    static void i(String message, Throwable t){
        if(getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()){
            Log.i(Constants.CLEVERTAP_LOG_TAG,message,t);
        }
    }

    @SuppressWarnings("unused")
    void info(String message){
        if (getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()){
            Log.i(Constants.CLEVERTAP_LOG_TAG,message);
        }
    }

    void info(String suffix, String message){
        if(getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()){
            Log.i(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message);
        }
    }
    @SuppressWarnings("unused")
    void info(String suffix, String message, Throwable t){
        if(getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()){
            Log.i(Constants.CLEVERTAP_LOG_TAG+":"+suffix, message,t);
        }
    }
    @SuppressWarnings("unused")
    void info(String message, Throwable t){
        if(getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()){
            Log.i(Constants.CLEVERTAP_LOG_TAG,message,t);
        }
    }


}
