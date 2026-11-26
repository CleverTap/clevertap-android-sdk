package com.clevertap.android.sdk

import android.util.Log

class Logger internal constructor(private var debugLevel: Int) : ILogger {

    override fun debug(message: String?) {
        if (isDebugEnabled) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
        }
    }

    override fun debug(suffix: String?, message: String?) {
        if (isDebugEnabled) {
            logChunked(tagWith(suffix), message.requireMessage(), Log::d)
        }
    }

    override fun debug(suffix: String?, message: String?, t: Throwable?) {
        if (isDebugEnabled) {
            Log.d(tagWith(suffix), message, t)
        }
    }

    override fun debug(message: String?, t: Throwable?) {
        if (isDebugEnabled) {
            Log.d(Constants.CLEVERTAP_LOG_TAG, message, t)
        }
    }

    override fun info(message: String?) {
        if (isInstanceInfoEnabled) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
        }
    }

    override fun info(suffix: String?, message: String?) {
        if (isInstanceInfoEnabled) {
            Log.i(tagWith(suffix), message.requireMessage())
        }
    }

    override fun info(suffix: String?, message: String?, t: Throwable?) {
        if (isInstanceInfoEnabled) {
            Log.i(tagWith(suffix), message, t)
        }
    }

    override fun info(message: String?, t: Throwable?) {
        if (isInstanceInfoEnabled) {
            Log.i(Constants.CLEVERTAP_LOG_TAG, message, t)
        }
    }

    override fun verbose(message: String?) {
        if (isVerboseEnabled) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
        }
    }

    override fun verbose(suffix: String?, message: String?) {
        if (isVerboseEnabled) {
            logChunked(tagWith(suffix), message.requireMessage(), Log::v)
        }
    }

    override fun verbose(suffix: String?, message: String?, t: Throwable?) {
        if (isVerboseEnabled) {
            Log.v(tagWith(suffix), message, t)
        }
    }

    override fun verbose(message: String?, t: Throwable?) {
        if (isVerboseEnabled) {
            Log.v(Constants.CLEVERTAP_LOG_TAG, message, t)
        }
    }

    fun setDebugLevel(level: Int) {
        debugLevel = level
    }

    private val isInstanceInfoEnabled: Boolean
        get() = debugLevel >= CleverTapAPI.LogLevel.INFO.intValue()

    companion object {

        /** Logcat truncates longer entries, so instance debug/verbose logs are split into chunks. */
        private const val MAX_LOG_CHUNK_LENGTH = 4000

        /**
         * Logs to Debug if the debug level is greater than 1.
         */
        @JvmStatic
        fun d(message: String?) {
            if (isDebugEnabled) {
                Log.d(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
            }
        }

        @JvmStatic
        fun d(suffix: String?, message: String?) {
            if (isDebugEnabled) {
                Log.d(tagWith(suffix), message.requireMessage())
            }
        }

        @JvmStatic
        fun d(suffix: String?, message: String?, t: Throwable?) {
            if (isDebugEnabled) {
                Log.d(tagWith(suffix), message, t)
            }
        }

        @JvmStatic
        fun d(message: String?, t: Throwable?) {
            if (isDebugEnabled) {
                Log.d(Constants.CLEVERTAP_LOG_TAG, message, t)
            }
        }

        /**
         * Logs to Info if the debug level is greater than or equal to 1.
         */
        @JvmStatic
        fun i(message: String?) {
            if (isInfoEnabled) {
                Log.i(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
            }
        }

        @JvmStatic
        fun i(suffix: String?, message: String?) {
            if (isInfoEnabled) {
                Log.i(tagWith(suffix), message.requireMessage())
            }
        }

        @JvmStatic
        fun i(suffix: String?, message: String?, t: Throwable?) {
            if (isInfoEnabled) {
                Log.i(tagWith(suffix), message, t)
            }
        }

        @JvmStatic
        fun i(message: String?, t: Throwable?) {
            if (isInfoEnabled) {
                Log.i(Constants.CLEVERTAP_LOG_TAG, message, t)
            }
        }

        /**
         * Logs to Verbose if the debug level is greater than 2.
         */
        @JvmStatic
        fun v(message: String?) {
            if (isVerboseEnabled) {
                Log.v(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
            }
        }

        @JvmStatic
        fun v(suffix: String?, message: String?) {
            if (isVerboseEnabled) {
                Log.v(tagWith(suffix), message.requireMessage())
            }
        }

        @JvmStatic
        fun v(suffix: String?, message: String?, t: Throwable?) {
            if (isVerboseEnabled) {
                Log.v(tagWith(suffix), message, t)
            }
        }

        @JvmStatic
        fun v(message: String?, t: Throwable?) {
            if (isVerboseEnabled) {
                Log.v(Constants.CLEVERTAP_LOG_TAG, message, t)
            }
        }

        private val isInfoEnabled: Boolean
            get() = CleverTapAPI.getDebugLevel() >= CleverTapAPI.LogLevel.INFO.intValue()

        private val isDebugEnabled: Boolean
            get() = CleverTapAPI.getDebugLevel() > CleverTapAPI.LogLevel.INFO.intValue()

        private val isVerboseEnabled: Boolean
            get() = CleverTapAPI.getDebugLevel() > CleverTapAPI.LogLevel.DEBUG.intValue()

        private fun tagWith(suffix: String?) = "${Constants.CLEVERTAP_LOG_TAG}:$suffix"

        /**
         * `Log.x(tag, msg)` declares `msg` non-null and throws an NPE for a null message on a device;
         * the Java Logger passed null straight through, so keep failing with an NPE.
         */
        private fun String?.requireMessage(): String = this ?: throw NullPointerException("println needs a message")

        private tailrec fun logChunked(tag: String, message: String, log: (String, String) -> Int) {
            if (message.length > MAX_LOG_CHUNK_LENGTH) {
                log(tag, message.substring(0, MAX_LOG_CHUNK_LENGTH))
                logChunked(tag, message.substring(MAX_LOG_CHUNK_LENGTH), log)
            } else {
                log(tag, message)
            }
        }
    }
}
