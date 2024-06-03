package com.clevertap.android.sdk.video

import com.clevertap.android.sdk.Logger

object VideoLibChecker {

    @JvmField
    val haveVideoPlayerSupport = checkForExoPlayer()

    /**
     * Method to check whether app has ExoPlayer dependencies
     *
     * @return boolean - true/false depending on app's availability of ExoPlayer dependencies
     */
    fun checkForExoPlayer(): Boolean {
        var exoPlayerPresent = false
        var className: Class<*>? = null
        try {
            className = Class.forName("com.google.android.exoplayer2.ExoPlayer")
            className = Class.forName("com.google.android.exoplayer2.source.hls.HlsMediaSource")
            className = Class.forName("com.google.android.exoplayer2.ui.StyledPlayerView")
            Logger.d("ExoPlayer is present")
            exoPlayerPresent = true
        } catch (t: Throwable) {
            Logger.d("ExoPlayer library files are missing!!!")
            Logger.d(
                "Please add ExoPlayer dependencies to render InApp or Inbox messages playing video. For more information checkout CleverTap documentation."
            )
            if (className != null) {
                Logger.d("ExoPlayer classes not found " + className.getName())
            } else {
                Logger.d("ExoPlayer classes not found")
            }
        }
        return exoPlayerPresent
    }

}