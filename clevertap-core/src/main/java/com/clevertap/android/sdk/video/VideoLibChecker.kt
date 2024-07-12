package com.clevertap.android.sdk.video

import com.clevertap.android.sdk.Logger

// todo check logs which get printed if methods fail detecing media stream libs
internal object VideoLibChecker {

    private val hasExoplayer = checkForExoPlayer()
    private val hasMedia3 = checkForMedia3()

    @JvmField
    val haveVideoPlayerSupport = checkForVideoPlayerSupport()

    @JvmField
    val mediaLibType = when {
        hasMedia3 -> {
            VideoLibraryIntegrated.MEDIA3
        }
        hasExoplayer -> {
            VideoLibraryIntegrated.EXOPLAYER
        }
        else -> {
            VideoLibraryIntegrated.NONE
        }
    }

    /**
     * Method to check whether app has either ExoPlayer or Media3 dependencies
     *
     * @return boolean - true/false depending on app's availability of ExoPlayer dependencies
     */
    private fun checkForVideoPlayerSupport(): Boolean {
        if (!hasMedia3 && !hasExoplayer)
            Logger.d("Please add ExoPlayer/Media3 dependencies to render InApp or Inbox messages playing video. For more information checkout CleverTap documentation.")
        return hasExoplayer || hasMedia3
    }

    /**
     * Method to check whether app has ExoPlayer dependencies
     *
     * @return boolean - true/false depending on app's availability of ExoPlayer dependencies
     */
    private fun checkForExoPlayer(): Boolean {
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
        }
        return exoPlayerPresent
    }

    /**
     * Method to check whether app has Media3 dependencies
     *
     * @return boolean - true/false depending on app's availability of ExoPlayer dependencies
     */
    private fun checkForMedia3(): Boolean {
        var media3ExoplayerPresent = false
        var className: Class<*>? = null
        try {
            className = Class.forName("androidx.media3.exoplayer.ExoPlayer")
            className = Class.forName("androidx.media3.exoplayer.hls.HlsMediaSource")
            className = Class.forName("androidx.media3.ui.PlayerView")
            Logger.d("Media3 is present")
            media3ExoplayerPresent = true
        } catch (t: Throwable) {
            Logger.d("Media3 library files are missing!!!")
        }
        return media3ExoplayerPresent
    }
}

enum class VideoLibraryIntegrated {
    EXOPLAYER, MEDIA3, NONE
}