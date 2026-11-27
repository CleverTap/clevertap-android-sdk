package com.clevertap.android.sdk

import android.util.Log

/**
 * The SDK's logcat logger. Messages are logged under the tag `CleverTap`, or `CleverTap:<suffix>` for
 * the overloads that take a suffix.
 *
 * The static methods ([d], [i], [v]) follow the global level set with `CleverTapAPI.setDebugLevel`;
 * an instance (see [CleverTapInstanceConfig.getLogger]) follows its own level, set with [setDebugLevel],
 * for info messages only; its debug and verbose messages follow the global level too.
 * Info messages are logged at [CleverTapAPI.LogLevel.INFO] and above, debug messages above INFO and
 * verbose messages above [CleverTapAPI.LogLevel.DEBUG]. Overloads without a [Throwable] throw a
 * [NullPointerException] for a `null` message when the level is enabled.
 */
public class Logger internal constructor(private var debugLevel: Int) : ILogger {

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

    /**
     * Sets the level this logger instance uses for info messages. Its debug and verbose messages follow
     * the global level set with `CleverTapAPI.setDebugLevel`.
     *
     * @param level the [CleverTapAPI.LogLevel.intValue] of a [CleverTapAPI.LogLevel]
     */
    public fun setDebugLevel(level: Int) {
        debugLevel = level
    }

    private val isInstanceInfoEnabled: Boolean
        get() = debugLevel >= CleverTapAPI.LogLevel.INFO.intValue()

    public companion object {

        /** Logcat truncates longer entries, so instance debug/verbose logs are split into chunks. */
        private const val MAX_LOG_CHUNK_LENGTH = 4000

        /**
         * Logs to Debug if the debug level is greater than 1.
         */
        @JvmStatic
        public fun d(message: String?) {
            if (isDebugEnabled) {
                Log.d(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
            }
        }

        /**
         * Logs a debug message under the tag `CleverTap:<suffix>`.
         *
         * @param suffix appended to the log tag
         * @param message the message
         */
        @JvmStatic
        public fun d(suffix: String?, message: String?) {
            if (isDebugEnabled) {
                Log.d(tagWith(suffix), message.requireMessage())
            }
        }

        /**
         * Logs a debug message and a throwable under the tag `CleverTap:<suffix>`.
         *
         * @param suffix appended to the log tag
         * @param message the message
         * @param t the throwable whose stack trace is logged
         */
        @JvmStatic
        public fun d(suffix: String?, message: String?, t: Throwable?) {
            if (isDebugEnabled) {
                Log.d(tagWith(suffix), message, t)
            }
        }

        /**
         * Logs a debug message and a throwable under the tag `CleverTap`.
         *
         * @param message the message
         * @param t the throwable whose stack trace is logged
         */
        @JvmStatic
        public fun d(message: String?, t: Throwable?) {
            if (isDebugEnabled) {
                Log.d(Constants.CLEVERTAP_LOG_TAG, message, t)
            }
        }

        /**
         * Logs to Info if the debug level is greater than or equal to 1.
         */
        @JvmStatic
        public fun i(message: String?) {
            if (isInfoEnabled) {
                Log.i(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
            }
        }

        /**
         * Logs a info message under the tag `CleverTap:<suffix>`.
         *
         * @param suffix appended to the log tag
         * @param message the message
         */
        @JvmStatic
        public fun i(suffix: String?, message: String?) {
            if (isInfoEnabled) {
                Log.i(tagWith(suffix), message.requireMessage())
            }
        }

        /**
         * Logs a info message and a throwable under the tag `CleverTap:<suffix>`.
         *
         * @param suffix appended to the log tag
         * @param message the message
         * @param t the throwable whose stack trace is logged
         */
        @JvmStatic
        public fun i(suffix: String?, message: String?, t: Throwable?) {
            if (isInfoEnabled) {
                Log.i(tagWith(suffix), message, t)
            }
        }

        /**
         * Logs a info message and a throwable under the tag `CleverTap`.
         *
         * @param message the message
         * @param t the throwable whose stack trace is logged
         */
        @JvmStatic
        public fun i(message: String?, t: Throwable?) {
            if (isInfoEnabled) {
                Log.i(Constants.CLEVERTAP_LOG_TAG, message, t)
            }
        }

        /**
         * Logs to Verbose if the debug level is greater than 2.
         */
        @JvmStatic
        public fun v(message: String?) {
            if (isVerboseEnabled) {
                Log.v(Constants.CLEVERTAP_LOG_TAG, message.requireMessage())
            }
        }

        /**
         * Logs a verbose message under the tag `CleverTap:<suffix>`.
         *
         * @param suffix appended to the log tag
         * @param message the message
         */
        @JvmStatic
        public fun v(suffix: String?, message: String?) {
            if (isVerboseEnabled) {
                Log.v(tagWith(suffix), message.requireMessage())
            }
        }

        /**
         * Logs a verbose message and a throwable under the tag `CleverTap:<suffix>`.
         *
         * @param suffix appended to the log tag
         * @param message the message
         * @param t the throwable whose stack trace is logged
         */
        @JvmStatic
        public fun v(suffix: String?, message: String?, t: Throwable?) {
            if (isVerboseEnabled) {
                Log.v(tagWith(suffix), message, t)
            }
        }

        /**
         * Logs a verbose message and a throwable under the tag `CleverTap`.
         *
         * @param message the message
         * @param t the throwable whose stack trace is logged
         */
        @JvmStatic
        public fun v(message: String?, t: Throwable?) {
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
