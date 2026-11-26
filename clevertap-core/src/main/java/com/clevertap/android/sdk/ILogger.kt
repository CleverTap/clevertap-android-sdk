package com.clevertap.android.sdk
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ILogger {
    public fun debug(message: String?)

    public fun debug(suffix: String?, message: String?)

    public fun debug(suffix: String?, message: String?, t: Throwable?)

    public fun debug(message: String?, t: Throwable?)

    public fun info(message: String?)

    public fun info(suffix: String?, message: String?)

    public fun info(suffix: String?, message: String?, t: Throwable?)

    public fun info(message: String?, t: Throwable?)

    public fun verbose(message: String?)

    public fun verbose(suffix: String?, message: String?)

    public fun verbose(suffix: String?, message: String?, t: Throwable?)

    public fun verbose(message: String?, t: Throwable?)
}
