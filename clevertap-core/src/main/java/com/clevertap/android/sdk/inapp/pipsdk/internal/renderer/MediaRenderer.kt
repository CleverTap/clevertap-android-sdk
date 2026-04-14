package com.clevertap.android.sdk.inapp.pipsdk.internal.renderer

import android.view.ViewGroup
import android.widget.ImageView
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession

internal interface MediaRenderer {
    /** Called when at least one media URL loaded successfully (primary or fallback). */
    var onMediaReady: (() -> Unit)?
    /** Called when all media URLs failed — nothing to display. */
    var onAllMediaFailed: (() -> Unit)?

    fun attach(container: ViewGroup, config: PIPConfig, session: PIPSession)
    /** Rotation: detach Surface from ExoPlayer (keeps decode buffer). No-op for images. */
    fun detachSurface()
    /** Post-rotation: create new Surface and re-bind to the existing player/image. */
    fun rebindSurface(container: ViewGroup, session: PIPSession)
    /** Full cleanup — releases player and media resources. */
    fun release()
    /** Called after the container view is moved to a new parent (expand/collapse). No-op by default. */
    fun onContainerChanged() {}
    /** Changes the ScaleType of the media view. Used to switch between CENTER_CROP (compact)
     *  and FIT_CENTER (expanded) for images/GIFs. No-op for video (uses player resize mode). */
    fun setScaleType(scaleType: ImageView.ScaleType) {}
    fun togglePlayPause()
    fun toggleMute()
    val currentPositionMs: Long
    val isMuted: Boolean
    val isPlaying: Boolean
}
