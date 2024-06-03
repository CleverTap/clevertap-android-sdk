package com.clevertap.android.sdk.video.inapps

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.clevertap.android.sdk.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.ExoTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util

/**
 * Handle wrapping exoplayer library used to render video and audio for Inapps feature.
 * All the player and surface related functionality to be limited to this class to we can have multiple
 * handles for video/audio support.
 */
class ExoplayerHandle {

    companion object {
        var mediaPosition: Long = 0
    }

    private var player: ExoPlayer? = null
    private var playerView: StyledPlayerView? = null

    private var playerViewLayoutParams: ViewGroup.LayoutParams? = null

    fun initExoplayer(
        context: Context,
        url: String
    ) {
        if (player != null) {
            return
        }
        val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        val videoTrackSelectionFactory: ExoTrackSelection.Factory = AdaptiveTrackSelection.Factory()
        val trackSelector: TrackSelector = DefaultTrackSelector(
            context,
            videoTrackSelectionFactory
        )

        val userAgent = Util.getUserAgent(context, context.packageName)
        val listener = bandwidthMeter.transferListener
        val dsf = DefaultHttpDataSource.Factory().setUserAgent(userAgent).setTransferListener(listener)
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context, dsf)
        val mediaItem = MediaItem.fromUri(url)
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

        player = ExoPlayer.Builder(context).setTrackSelector(trackSelector).build().apply {
            setMediaSource(hlsMediaSource)
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
            seekTo(mediaPosition)
        }
    }

    fun initPlayerView(
        context: Context,
        isTablet: Boolean
    ) {
        if (playerView != null) {
            return
        }
        playerView = StyledPlayerView(context)

        val playerWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            if (isTablet) {
                408f
            } else {
                240f
            },
            context.resources.displayMetrics
        ).toInt()

        val playerHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            if (isTablet) {
                299f
            } else {
                134f
            },
            context.resources.displayMetrics
        ).toInt()

        playerViewLayoutParams = FrameLayout.LayoutParams(playerWidth, playerHeight)
        playerView!!.setLayoutParams(playerViewLayoutParams)
        playerView!!.setShowBuffering(StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        playerView!!.useArtwork = true
        playerView!!.controllerAutoShow = false
        playerView!!.defaultArtwork = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.ct_audio,
            null
        )
    }

    fun play() {
        playerView!!.requestFocus()
        playerView!!.visibility = View.VISIBLE
        playerView!!.player = player
        player!!.playWhenReady = true
    }

    fun pause() {
        player?.let { ep ->
            ep.stop()
            ep.release()
            player = null
        }
    }

    fun savePosition() {
        if (player != null) {
            mediaPosition = player!!.currentPosition
        }
    }

    fun switchToFullScreen(isFullScreen: Boolean) {
        if (isFullScreen) {
            playerViewLayoutParams = playerView!!.layoutParams
        } else {
            playerView!!.setLayoutParams(playerViewLayoutParams)
        }
    }

    fun videoSurface(): View {
        return playerView!!
    }
}