package com.clevertap.android.sdk.video

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface InboxVideoPlayerHandle {
    fun initExoplayer(
        context: Context,
        buffering: () -> Unit,
        playerReady: () -> Unit,
    )

    fun videoSurface(): View
    fun setPlayWhenReady(play: Boolean)

    /**
     * Stops the current video and detaches the player from its view. The player and the view stay
     * alive so the next video can reuse them; call [release] when they are no longer needed.
     */
    fun stop()

    /** Releases the player and its view. */
    fun release()
    fun initPlayerView(
        context: Context,
        artworkAsset: () -> Drawable?,
    )

    fun playerVolume(): Float
    fun setPlayerVolume(volume: Float)
    fun currentPositionMs(): Long
    fun startPlaying(
        ctx: Context,
        uriString: String,
        isMediaAudio: Boolean,
        isMediaVideo: Boolean,
        startPositionMs: Long,
    )
}
