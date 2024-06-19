package com.clevertap.android.sdk.video.inapps

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle
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
class ExoplayerHandle : InAppVideoPlayerHandle {

    private var player: ExoPlayer? = null
    private var playerView: StyledPlayerView? = null

    private var playerViewLayoutParamsNormal: ViewGroup.LayoutParams? = null
    private var playerViewLayoutParamsFullScreen =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

    private var mediaPosition = 0L

    override fun initExoplayer(
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

    override fun initPlayerView(
        context: Context,
        isTablet: Boolean
    ) {
        if (playerView != null) {
            return
        }

        val playerWidth = playerWidth(context = context, isTablet = isTablet)
        val playerHeight = playerHeight(context = context, isTablet = isTablet)

        playerView = StyledPlayerView(context).apply {
            playerViewLayoutParamsNormal = FrameLayout.LayoutParams(playerWidth, playerHeight)
            setLayoutParams(playerViewLayoutParamsNormal)
            setShowBuffering(StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            useArtwork = true
            controllerAutoShow = false
            defaultArtwork = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ct_audio,
                null
            )
        }
    }

    override fun play() {
        playerView?.let { pv ->
            pv.requestFocus()
            pv.visibility = View.VISIBLE
            pv.player = player
        }
        player?.let {
            it.playWhenReady = true
        }
    }

    override fun pause() {
        player?.let { ep ->
            ep.stop()
            ep.release()
            player = null
        }
    }

    override fun savePosition() {
        if (player != null) {
            mediaPosition = player!!.currentPosition
        }
    }

    override fun switchToFullScreen(isFullScreen: Boolean) {
        if (isFullScreen) {
            playerViewLayoutParamsNormal = playerView!!.layoutParams
            playerView!!.layoutParams = playerViewLayoutParamsFullScreen
        } else {
            playerView!!.layoutParams = playerViewLayoutParamsNormal
        }
    }

    override fun videoSurface(): View {
        return playerView!!
    }

    fun playerWidth(
        context: Context,
        isTablet: Boolean
    ) : Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            if (isTablet) {
                408f
            } else {
                240f
            },
            context.resources.displayMetrics
        ).toInt()
    }

    fun playerHeight(
        context: Context,
        isTablet: Boolean
    ) : Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            if (isTablet) {
                299f
            } else {
                134f
            },
            context.resources.displayMetrics
        ).toInt()
    }
}