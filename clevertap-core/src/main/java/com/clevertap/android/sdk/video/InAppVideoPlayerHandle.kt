package com.clevertap.android.sdk.video

import android.content.Context
import android.view.View

interface InAppVideoPlayerHandle {
    fun initExoplayer(
        context: Context,
        url: String
    )

    fun initPlayerView(
        context: Context,
        isTablet: Boolean
    )

    fun play()
    fun pause()
    fun savePosition()
    fun switchToFullScreen(isFullScreen: Boolean)
    fun videoSurface(): View
}