package com.clevertap.android.pushtemplates

import android.util.Log
import androidx.annotation.RestrictTo

/** Push templates' Logcat logger, filtered by [TemplateRenderer.debugLevel]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PTLog {

    private fun isEnabled(level: TemplateRenderer.LogLevel): Boolean =
        TemplateRenderer.debugLevel >= level.intValue()

    fun debug(message: String) {
        if (isEnabled(TemplateRenderer.LogLevel.DEBUG)) {
            Log.d(PTConstants.LOG_TAG, message)
        }
    }

    fun info(message: String) {
        if (isEnabled(TemplateRenderer.LogLevel.INFO)) {
            Log.i(PTConstants.LOG_TAG, message)
        }
    }

    fun verbose(message: String) {
        if (isEnabled(TemplateRenderer.LogLevel.VERBOSE)) {
            Log.v(PTConstants.LOG_TAG, message)
        }
    }

    // Gated on INFO, not DEBUG, as it always was.
    fun debug(message: String?, t: Throwable?) {
        if (isEnabled(TemplateRenderer.LogLevel.INFO)) {
            Log.d(PTConstants.LOG_TAG, message, t)
        }
    }

    fun info(message: String?, t: Throwable?) {
        if (isEnabled(TemplateRenderer.LogLevel.INFO)) {
            Log.i(PTConstants.LOG_TAG, message, t)
        }
    }

    fun verbose(message: String?, t: Throwable?) {
        if (isEnabled(TemplateRenderer.LogLevel.VERBOSE)) {
            Log.v(PTConstants.LOG_TAG, message, t)
        }
    }
}
