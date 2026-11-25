package com.clevertap.android.pushtemplates

import android.util.Log

internal class PTLog internal constructor(private val debugLevel: Int) {

    @Suppress("unused")
    private fun getDebugLevel(): Int {
        return debugLevel
    }

    companion object {

        private fun getStaticDebugLevel(): Int {
            return TemplateRenderer.debugLevel
        }

        @JvmStatic
        fun debug(message: String) {
            if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.DEBUG.intValue()) {
                Log.d(PTConstants.LOG_TAG, message)
            }
        }

        @JvmStatic
        fun info(message: String) {
            if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()) {
                Log.i(PTConstants.LOG_TAG, message)
            }
        }

        @JvmStatic
        fun verbose(message: String) {
            if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.VERBOSE.intValue()) {
                Log.v(PTConstants.LOG_TAG, message)
            }
        }

        @JvmStatic
        fun debug(message: String?, t: Throwable?) {
            if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()) {
                Log.d(PTConstants.LOG_TAG, message, t)
            }
        }

        @JvmStatic
        fun info(message: String?, t: Throwable?) {
            if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.INFO.intValue()) {
                Log.i(PTConstants.LOG_TAG, message, t)
            }
        }

        @JvmStatic
        fun verbose(message: String?, t: Throwable?) {
            if (getStaticDebugLevel() >= TemplateRenderer.LogLevel.VERBOSE.intValue()) {
                Log.v(PTConstants.LOG_TAG, message, t)
            }
        }
    }
}
