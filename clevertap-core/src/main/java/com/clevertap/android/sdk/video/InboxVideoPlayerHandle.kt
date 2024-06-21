package com.clevertap.android.sdk.video

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View

interface InboxVideoPlayerHandle {
    fun initExoplayer(
        context: Context,
        buffering: () -> Unit,
        playerReady: () -> Unit,
    )

    fun videoSurface(): View
    fun setPlayWhenReady(play: Boolean)
    fun pause()
    fun initPlayerView(
        context: Context,
        artworkAsset: () -> Drawable?,
    )

    fun playerVolume(): Float
    fun handleMute()
    fun startPlaying(
        ctx: Context,
        uriString: String,
        isMediaAudio: Boolean,
        isMediaVideo: Boolean,
    )
}