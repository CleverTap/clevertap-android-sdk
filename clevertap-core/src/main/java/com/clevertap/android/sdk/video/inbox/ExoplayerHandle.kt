package com.clevertap.android.sdk.video.inbox

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import com.clevertap.android.sdk.video.InboxVideoPlayerHandle
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

/**
 * Handle wrapping exoplayer library used to render video and audio for Inbox feature.
 * All the player and surface related functionality to be limited to this class to we can have multiple
 * handles for video/audio support.
 */
class ExoplayerHandle : InboxVideoPlayerHandle {

    private var videoSurfaceView: StyledPlayerView? = null
    private var player: ExoPlayer? = null

    override fun initExoplayer(
        context: Context,
        buffering: () -> Unit,
        playerReady: () -> Unit,
    ) {
        if (player != null) {
            return
        }

        val videoTrackSelectionFactory: ExoTrackSelection.Factory = AdaptiveTrackSelection.Factory()
        val trackSelector: TrackSelector = DefaultTrackSelector(context, videoTrackSelectionFactory)

        player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                volume = 0f // start off muted
                addListener(object : ExoplayerPlayerListener() {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                buffering.invoke()
                            }

                            Player.STATE_ENDED -> if (player != null) {
                                seekTo(0)
                                playWhenReady = false
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
    }

    override fun videoSurface(): View {
        return videoSurfaceView!!
    }

    override fun setPlayWhenReady(play: Boolean) {
        player?.let { ep ->
            ep.playWhenReady = play
        }
    }

    override fun pause() {
        player?.let { ep ->
            ep.stop()
            ep.release()
        }
        player = null
        videoSurfaceView = null
    }

    override fun initPlayerView(
        context: Context,
        artworkAsset: () -> Drawable?,
    ) {
        if (videoSurfaceView != null) {
            return
        }
        videoSurfaceView = StyledPlayerView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)

            resizeMode = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            useArtwork = true
            defaultArtwork = artworkAsset.invoke()

            useController = true
            controllerAutoShow = false
            player = this@ExoplayerHandle.player
        }
    }

    override fun playerVolume(): Float = player?.volume ?: 0f

    override fun handleMute() {
        player?.let { ep ->
            val playerVolume = playerVolume()

            if (playerVolume > 0) {
                ep.volume = 0f
            } else if (playerVolume == 0f) {
                ep.volume = 1f
            }
        }
    }

    override fun startPlaying(
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
            val userAgent = Util.getUserAgent(ctx, ctx.packageName)
            val mediaItem: MediaItem = MediaItem.fromUri(uriString)
            val dsf = DefaultHttpDataSource.Factory().setUserAgent(userAgent)
                .setTransferListener(defaultBandwidthMeter)
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(ctx, dsf)
            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            ep.setMediaSource(hlsMediaSource)
            ep.prepare()
            if (isMediaAudio) {
                videoSurfaceView?.showController() //show controller for audio as it is not autoplay
                ep.playWhenReady = false
                ep.volume = 1f
            } else if (isMediaVideo) {
                ep.playWhenReady = true
                ep.volume = playerVolume()
            }
        }
    }
}