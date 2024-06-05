package com.clevertap.android.sdk.video

import com.clevertap.android.sdk.Logger

// todo check logs which get printed if methods fail detecing media stream libs
internal object VideoLibChecker {

    private val hasExoplayer = checkForExoPlayer()
    private val hasMedia3 = checkForMedia3()

    @JvmField
    val haveVideoPlayerSupport = hasExoplayer || hasMedia3

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
            Logger.d("Please add ExoPlayer dependencies to render InApp or Inbox messages playing video. For more information checkout CleverTap documentation.")
            if (className != null) {
                Logger.d("ExoPlayer classes not found " + className.getName())
            } else {
                Logger.d("ExoPlayer classes not found")
            }
        }
        return exoPlayerPresent
    }

    private fun checkForMedia3(): Boolean {
        var media3ExoplayerPresent = false
        var className: Class<*>? = null
        try {
            className = Class.forName("androidx.media3.exoplayer.ExoPlayer")
            className = Class.forName("androidx.media3.exoplayer.hls.HlsMediaSource")
            className = Class.forName("androidx.media3.ui.PlayerView")
            Logger.d("ExoPlayer from Media3 is present")
            media3ExoplayerPresent = true
        } catch (t: Throwable) {
            Logger.d("Media3 ExoPlayer library files are missing!!!")
            Logger.d("Please add ExoPlayer dependencies to render InApp or Inbox messages playing video. For more information checkout CleverTap documentation.")
            if (className != null) {
                Logger.d("ExoPlayer classes not found " + className.getName())
            } else {
                Logger.d("ExoPlayer classes not found")
            }
        }
        return media3ExoplayerPresent
    }
}

enum class VideoLibraryIntegrated {
    EXOPLAYER, MEDIA3, NONE
}