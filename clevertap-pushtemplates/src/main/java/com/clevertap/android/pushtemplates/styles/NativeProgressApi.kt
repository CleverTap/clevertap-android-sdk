package com.clevertap.android.pushtemplates.styles

import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.clevertap.android.pushtemplates.PTLog
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * The androidx.core 1.17.0 APIs behind the native Android 16 ProgressStyle, looked up by reflection
 * ONCE per process and reused for every render.
 *
 * The SDK does not depend on androidx.core 1.17.0 at build time (the host app brings its own core),
 * so these APIs may be missing at runtime. Whether they exist cannot change while the app runs, so
 * the lookup result — found or not — is cached:
 * - all found -> [methods] holds them, and each render only calls `invoke`;
 * - any missing (core < 1.17.0, or stripped by R8) -> [methods] is null and the caller uses the
 *   RemoteViews fallback without looking them up again.
 *
 * All-or-nothing on purpose: one missing API makes the whole set unavailable, so a notification is
 * never built half native. Only the reflective handles are cached; each notification still creates
 * its own ProgressStyle / Segment / Point instances.
 */
internal object NativeProgressApi {

    class Methods(
        val styleCtor: Constructor<*>,
        val setStyledByProgress: Method,
        val setProgress: Method,
        val setProgressIndeterminate: Method,
        val setProgressSegments: Method,
        val setProgressPoints: Method,
        val setProgressTrackerIcon: Method,
        val setProgressStartIcon: Method,
        val setProgressEndIcon: Method,
        val segmentCtor: Constructor<*>,
        val segmentSetColor: Method,
        val pointCtor: Constructor<*>,
        val pointSetColor: Method,
        val setRequestPromotedOngoing: Method,
        val setShortCriticalText: Method
    )

    val methods: Methods? by cachedLookup(::lookup)

    /**
     * Runs [lookup] at most once and keeps the result, found or not. A failure is caught INSIDE the
     * initializer: lazy() retries an initializer that throws, so without this an app on an older
     * core would repeat the failed lookup on every render. Split out so tests drive the same code.
     */
    @VisibleForTesting
    internal fun cachedLookup(lookup: () -> Methods): Lazy<Methods?> = lazy {
        runCatching(lookup)
            .onFailure { PTLog.verbose("pt_progress: native ProgressStyle unavailable (androidx.core < 1.17.0?), using fallback", it) }
            .getOrNull()
    }

    // The literal class/method names track androidx.core 1.17.0 (unchanged through 1.19.1).
    private fun lookup(): Methods {
        val style = Class.forName("androidx.core.app.NotificationCompat\$ProgressStyle")
        val segment = Class.forName("androidx.core.app.NotificationCompat\$ProgressStyle\$Segment")
        val point = Class.forName("androidx.core.app.NotificationCompat\$ProgressStyle\$Point")
        val builder = NotificationCompat.Builder::class.java
        val int = Int::class.javaPrimitiveType
        val bool = Boolean::class.javaPrimitiveType
        val icon = IconCompat::class.java
        return Methods(
            styleCtor = style.getConstructor(),
            setStyledByProgress = style.getMethod("setStyledByProgress", bool),
            setProgress = style.getMethod("setProgress", int),
            setProgressIndeterminate = style.getMethod("setProgressIndeterminate", bool),
            setProgressSegments = style.getMethod("setProgressSegments", List::class.java),
            setProgressPoints = style.getMethod("setProgressPoints", List::class.java),
            setProgressTrackerIcon = style.getMethod("setProgressTrackerIcon", icon),
            setProgressStartIcon = style.getMethod("setProgressStartIcon", icon),
            setProgressEndIcon = style.getMethod("setProgressEndIcon", icon),
            segmentCtor = segment.getConstructor(int),
            segmentSetColor = segment.getMethod("setColor", int),
            pointCtor = point.getConstructor(int),
            pointSetColor = point.getMethod("setColor", int),
            setRequestPromotedOngoing = builder.getMethod("setRequestPromotedOngoing", bool),
            setShortCriticalText = builder.getMethod("setShortCriticalText", String::class.java)
        )
    }
}
