package com.clevertap.android.pushtemplates;

import android.util.Log;

final class PTLog {

    private final int debugLevel;

    PTLog(int level){
        this.debugLevel = level;
    }

    private int getDebugLevel() {
        return debugLevel;
    }

    private static int getStaticDebugLevel(){
        return TemplateRenderer.getDebugLevel();
    }


    static void debug(String message){
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.DEBUG.intValue()){
            Log.d(Constants.LOG_TAG,message);
        }
    }

    static void info(String message){
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()){
            Log.i(Constants.LOG_TAG,message);
        }
    }

    static void verbose(String message){
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.VERBOSE.intValue()){
            Log.v(Constants.LOG_TAG,message);
        }
    }

    static void debug(String message, Throwable t){
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()){
            Log.d(Constants.LOG_TAG,message,t);
        }
    }

    static void info(String message, Throwable t){
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()){
            Log.i(Constants.LOG_TAG,message,t);
        }
    }

    static void verbose(String message, Throwable t){
        if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.VERBOSE.intValue()){
            Log.v(Constants.LOG_TAG,message,t);
        }
    }


}
