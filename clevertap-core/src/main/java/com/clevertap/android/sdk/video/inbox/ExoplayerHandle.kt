package com.clevertap.android.sdk.video.inbox

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.clevertap.android.sdk.inbox.CTInboxActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.ExoTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView

class ExoplayerHandle {

    private var videoSurfaceView: StyledPlayerView? = null
    private var player: ExoPlayer? = null

    fun initExoplayer(
        context: Context,
        buffering: () -> Void,
        playerReady: () -> Void,
    ) {
        if (player != null) {
            return
        }

        val videoTrackSelectionFactory: ExoTrackSelection.Factory = AdaptiveTrackSelection.Factory()
        val trackSelector: TrackSelector = DefaultTrackSelector(context, videoTrackSelectionFactory)
        player = ExoPlayer.Builder(context).setTrackSelector(trackSelector).build()

        player!!.volume = 0f // start off muted

        player!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        buffering.invoke()
                    }

                    Player.STATE_ENDED -> if (player != null) {
                        player!!.seekTo(0)
                        player!!.playWhenReady = false
                        videoSurfaceView?.showController()
                    }

                    Player.STATE_READY -> {
                        playerReady.invoke()
                    }

                    Player.STATE_IDLE -> {}
                    else -> {}
                }
            }
        })
    }

    fun player(): View {
        return videoSurfaceView!!
    }

    fun setPlayWhenReady(play: Boolean) {
        player?.let { ep ->
            ep.playWhenReady = play
        }
    }

    fun pause() {
        player?.let { ep ->
            ep.stop()
            ep.release()
        }
        player = null
        videoSurfaceView = null
    }

    fun initPlayerView(
        context: Context,
        artworkAsset: () -> Drawable,
    ) {
        if (videoSurfaceView != null) {
            return
        }
        videoSurfaceView = StyledPlayerView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            if (CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL)
            } else {
                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
            }
            setUseArtwork(true)
            setDefaultArtwork(artworkAsset.invoke())

            setUseController(true)
            setControllerAutoShow(false)
            setPlayer(player)
        }
    }
}