package com.clevertap.android.sdk.inapp.pipsdk.internal.session

import android.app.Activity
import androidx.lifecycle.LifecycleEventObserver
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition
import com.clevertap.android.sdk.inapp.pipsdk.internal.renderer.PIPVideoPlayerWrapper
import com.clevertap.android.sdk.inapp.pipsdk.internal.view.PIPRootContainer
import java.lang.ref.WeakReference

/**
 * Holds ALL mutable runtime state for one PIP session.
 * Lives in [com.clevertap.android.sdk.inapp.pipsdk.PIPManager] singleton.
 * [activityRef] is a WeakReference to avoid leaking the Activity.
 *
 * Not a data class: mutable fields make copy()/equals()/hashCode() unsafe
 * (shared videoPlayerWrapper reference, identity-based equality needed).
 */
internal class PIPSession(
    val config: PIPConfig,
    initialPosition: PIPPosition,
    activity: Activity,
) {
    var currentPosition: PIPPosition = initialPosition
        internal set

    var videoPlayerWrapper: PIPVideoPlayerWrapper? = null
        internal set

    var playbackPositionMs: Long = 0L
        internal set

    var isMuted: Boolean = true
        internal set

    var isPlaying: Boolean = true
        internal set

    var activityRef: WeakReference<Activity> = WeakReference(activity)
        internal set

    var isExpanded: Boolean = false
        internal set

    var pipRootContainer: PIPRootContainer? = null
        internal set

    var lifecycleObserver: LifecycleEventObserver? = null
        internal set

    /** True if video was auto-paused when the Activity stopped (background).
     *  Used to auto-resume only when WE paused it, not when the user paused manually. */
    var pausedByBackground: Boolean = false
        internal set
}
