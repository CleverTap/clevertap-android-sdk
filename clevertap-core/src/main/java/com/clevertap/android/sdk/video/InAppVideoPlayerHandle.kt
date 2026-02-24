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
    fun setFullscreenClickListener(onClick: () -> Unit)
    fun setMuteClickListener()
    fun videoSurface(): View

    companion object {
        const val PLAYER_WIDTH_PHONE_DP = 240f
        const val PLAYER_HEIGHT_PHONE_DP = 134f
        const val PLAYER_WIDTH_TABLET_DP = 408f
        const val PLAYER_HEIGHT_TABLET_DP = 299f

        const val VOLUME_MUTED = 0f
        const val VOLUME_UNMUTED = 1f
    }
}