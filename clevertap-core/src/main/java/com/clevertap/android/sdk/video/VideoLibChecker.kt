package com.clevertap.android.sdk.video

import com.clevertap.android.sdk.Logger

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
     * @return boolean - true/false depending on app's availability of either ExoPlayer or Media3 dependencies
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
        val requiredExoplayerClassNames = listOf(
            "com.google.android.exoplayer2.ExoPlayer",
            "com.google.android.exoplayer2.source.hls.HlsMediaSource",
            "com.google.android.exoplayer2.ui.StyledPlayerView"
        )

        for (className in requiredExoplayerClassNames) {
            try {
                Class.forName(className)
            } catch (t: Throwable) {
                Logger.d("$className is missing!!!")
                Logger.d("One or more ExoPlayer library files are missing!!!")
                return false
            }
        }

        Logger.d("ExoPlayer is present")
        return true
    }

    /**
     * Method to check whether app has Media3 dependencies
     *
     * @return boolean - true/false depending on app's availability of Media3 dependencies
     */
    private fun checkForMedia3(): Boolean {
        val requiredMedia3ClassNames = listOf(
            "androidx.media3.exoplayer.ExoPlayer",
            "androidx.media3.exoplayer.hls.HlsMediaSource",
            "androidx.media3.ui.PlayerView"
        )

        for (className in requiredMedia3ClassNames) {
            try {
                Class.forName(className)
            } catch (t: Throwable) {
                Logger.d("$className is missing!!!")
                Logger.d("One or more Media3 library files are missing!!!")
                return false
            }
        }

        Logger.d("Media3 is present")
        return true
    }
}

enum class VideoLibraryIntegrated {
    EXOPLAYER, MEDIA3, NONE
}