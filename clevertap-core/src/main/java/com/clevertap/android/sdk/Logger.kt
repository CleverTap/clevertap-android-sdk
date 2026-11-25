package com.clevertap.android.sdk

import android.util.Log

class Logger internal constructor(level: Int) : ILogger {

    private var debugLevel: Int = level

    override fun debug(message: String?) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message!!)
        }
    }

    override fun debug(suffix: String?, message: String?) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            if (message!!.length > 4000) {
                Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message.substring(0, 4000))
                debug(suffix, message.substring(4000))
            } else {
                Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message)
            }
        }
    }

    override fun debug(suffix: String?, message: String?, t: Throwable?) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t)
        }
    }

    override fun debug(message: String?, t: Throwable?) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message, t)
        }
    }

    override fun info(message: String?) {
        if (getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message!!)
        }
    }

    override fun info(suffix: String?, message: String?) {
        if (getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message!!)
        }
    }

    override fun info(suffix: String?, message: String?, t: Throwable?) {
        if (getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t)
        }
    }

    override fun info(message: String?, t: Throwable?) {
        if (getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message, t)
        }
    }

    override fun verbose(message: String?) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message!!)
        }
    }

    override fun verbose(suffix: String?, message: String?) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            if (message!!.length > 4000) {
                Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message.substring(0, 4000))
                verbose(suffix, message.substring(4000))
            } else {
                Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message)
            }
        }
    }

    override fun verbose(suffix: String?, message: String?, t: Throwable?) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t)
        }
    }

    override fun verbose(message: String?, t: Throwable?) {
        if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message, t)
        }
    }

    private fun getDebugLevel(): Int {
        return debugLevel
    }

    fun setDebugLevel(level: Int) {
        this.debugLevel = level
    }

    companion object {

        /**
         * Logs to Debug if the debug level is greater than 1.
         */
        @JvmStatic
        fun d(message: String?) {
            if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
                Log.d(Constants.CLEVERTAP_LOG_TAG, message!!)
            }
        }

        @JvmStatic
        fun d(suffix: String?, message: String?) {
            if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
                Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message!!)
            }
        }

        @JvmStatic
        fun d(suffix: String?, message: String?, t: Throwable?) {
            if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
                Log.d(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t)
            }
        }

        @JvmStatic
        fun d(message: String?, t: Throwable?) {
            if (getStaticDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()) {
                Log.d(Constants.CLEVERTAP_LOG_TAG, message, t)
            }
        }

        /**
         * Logs to Info if the debug level is greater than or equal to 1.
         */
        @JvmStatic
        fun i(message: String?) {
            if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
                Log.i(Constants.CLEVERTAP_LOG_TAG, message!!)
            }
        }

        @JvmStatic
        fun i(suffix: String?, message: String?) {
            if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
                Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message!!)
            }
        }

        @JvmStatic
        fun i(suffix: String?, message: String?, t: Throwable?) {
            if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
                Log.i(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t)
            }
        }

        @JvmStatic
        fun i(message: String?, t: Throwable?) {
            if (getStaticDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()) {
                Log.i(Constants.CLEVERTAP_LOG_TAG, message, t)
            }
        }

        /**
         * Logs to Verbose if the debug level is greater than 2.
         */
        @JvmStatic
        fun v(message: String?) {
            if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
                Log.v(Constants.CLEVERTAP_LOG_TAG, message!!)
            }
        }

        @JvmStatic
        fun v(suffix: String?, message: String?) {
            if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
                Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message!!)
            }
        }

        @JvmStatic
        fun v(suffix: String?, message: String?, t: Throwable?) {
            if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
                Log.v(Constants.CLEVERTAP_LOG_TAG + ":" + suffix, message, t)
            }
        }

        @JvmStatic
        fun v(message: String?, t: Throwable?) {
            if (getStaticDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()) {
                Log.v(Constants.CLEVERTAP_LOG_TAG, message, t)
            }
        }

        private fun getStaticDebugLevel(): Int {
            return CleverTapAPI.getDebugLevel()
        }
    }
}
