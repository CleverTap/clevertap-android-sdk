package com.clevertap.android.sdk.video.inbox

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.clevertap.android.sdk.video.InboxVideoPlayerHandle

@UnstableApi
class Media3Handle: InboxVideoPlayerHandle {

    private var videoSurfaceView: PlayerView? = null
    private var player: ExoPlayer? = null

    override fun initExoplayer(
        context: Context,
        buffering: () -> Void,
        playerReady: () -> Void,
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
                addListener(object : Player.Listener {
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
        artworkAsset: () -> Drawable,
    ) {
        if (videoSurfaceView != null) {
            return
        }
        videoSurfaceView = PlayerView(context).apply {
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
            player = this@Media3Handle.player
        }
    }

    override fun playerVolume(): Float = player?.let { ep -> ep.volume } ?: 0f

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
            spv.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
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