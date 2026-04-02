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
    fun setFullscreenClickListener(onClick: (isFullScreen: Boolean) -> Unit)
    fun setMuteClickListener()
    fun setActionClickListener(onClick: () -> Unit)
    fun videoSurface(): View

    /**
     * Detaches the video surface without releasing the player.
     * Called before view hierarchy removal during a configuration change (rotation).
     * Saves playback position and mute state so they survive across the config change.
     * The player instance stays alive for reattachment in the next Fragment instance.
     *
     * Default is a no-op — only handles that support rotation-survival override this.
     */
    fun detachSurface() {}

    /**
     * Pauses playback without releasing the player.
     * Called when the app goes to background so the player can resume seamlessly on foreground
     * without re-buffering or seeking. Default is a no-op.
     */
    fun softPause() {}

    companion object {
        const val PLAYER_WIDTH_PHONE_DP = 240f
        const val PLAYER_HEIGHT_PHONE_DP = 134f
        const val PLAYER_WIDTH_TABLET_DP = 408f
        const val PLAYER_HEIGHT_TABLET_DP = 299f

        const val VOLUME_MUTED = 0f
        const val VOLUME_UNMUTED = 1f
    }
}