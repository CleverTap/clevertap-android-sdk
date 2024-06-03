package com.clevertap.android.sdk.video.inbox

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import com.clevertap.android.sdk.inbox.CTInboxActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.ExoTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util

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

    fun videoSurface(): View {
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
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            } else {
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            useArtwork = true
            defaultArtwork = artworkAsset.invoke()

            useController = true
            controllerAutoShow = false
            player = player
        }
    }

    fun playMedia() {

    }

    fun playerVolume(): Float = player?.let { ep -> ep.volume } ?: 0f

    fun handleMute() {
        player?.let { ep ->
            val playerVolume = playerVolume()

            if (playerVolume > 0) {
                ep.volume = 0f
            } else if (playerVolume == 0f) {
                ep.volume = 1f
            }
        }
    }

    fun startPlaying(
        ctx: Context,
        uriString: String,
        isMediaAudio: Boolean,
        isMediaVideo: Boolean,
    ) {
        videoSurfaceView?.let { spv ->
            spv.requestFocus()
            spv.setShowBuffering(StyledPlayerView.SHOW_BUFFERING_NEVER)
        }
        // Prepare the player with the source.
        player?.let { ep ->
            val defaultBandwidthMeter = DefaultBandwidthMeter.Builder(ctx).build()
            val userAgent = Util.getUserAgent(ctx, ctx.getPackageName())
            val mediaItem: MediaItem = MediaItem.fromUri(uriString)
            val dsf = DefaultHttpDataSource.Factory().setUserAgent(userAgent)
                .setTransferListener(defaultBandwidthMeter)
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(ctx, dsf)
            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            ep.setMediaSource(hlsMediaSource)
            ep.prepare()
            if (isMediaAudio) {
                videoSurfaceView!!.showController() //show controller for audio as it is not autoplay
                ep.playWhenReady = false
                ep.volume = 1f
            } else if (isMediaVideo) {
                ep.playWhenReady = true
                ep.volume = playerVolume()
            }
        }
    }
}