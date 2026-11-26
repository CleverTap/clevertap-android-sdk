package com.clevertap.android.sdk
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ILogger {
    fun debug(message: String?)

    fun debug(suffix: String?, message: String?)

    fun debug(suffix: String?, message: String?, t: Throwable?)

    fun debug(message: String?, t: Throwable?)

    fun info(message: String?)

    fun info(suffix: String?, message: String?)

    fun info(suffix: String?, message: String?, t: Throwable?)

    fun info(message: String?, t: Throwable?)

    fun verbose(message: String?)

    fun verbose(suffix: String?, message: String?)

    fun verbose(suffix: String?, message: String?, t: Throwable?)

    fun verbose(message: String?, t: Throwable?)
}
