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
 */
internal data class PIPSession(
    val config: PIPConfig,
    var currentPosition: PIPPosition,
    var videoPlayerWrapper: PIPVideoPlayerWrapper? = null,
    var playbackPositionMs: Long = 0L,
    var isMuted: Boolean = true,
    var isPlaying: Boolean = true,
    var activityRef: WeakReference<Activity>,
    var isExpanded: Boolean = false,
    var pipRootContainer: PIPRootContainer? = null,
    var lifecycleObserver: LifecycleEventObserver? = null,
)
