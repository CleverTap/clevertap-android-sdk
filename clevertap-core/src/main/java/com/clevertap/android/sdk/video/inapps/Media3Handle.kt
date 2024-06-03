package com.clevertap.android.sdk.video.inapps

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
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
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import com.clevertap.android.sdk.R

@UnstableApi
class Media3Handle {
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    private var playerViewLayoutParams: ViewGroup.LayoutParams? = null

    private var mediaPosition = 0L

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

        val playerWidth = playerWidth(context = context, isTablet = isTablet)
        val playerHeight = playerHeight(context = context, isTablet = isTablet)

        playerView = PlayerView(context).apply {
            playerViewLayoutParams = FrameLayout.LayoutParams(playerWidth, playerHeight)
            setLayoutParams(playerViewLayoutParams)
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            useArtwork = true
            controllerAutoShow = false
            defaultArtwork = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ct_audio,
                null
            )
        }
    }

    fun play() {
        playerView?.let { pv ->
            pv.requestFocus()
            pv.visibility = View.VISIBLE
            pv.player = player
        }
        player?.let {
            it.playWhenReady = true
        }
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

    private fun playerWidth(
        context: Context,
        isTablet: Boolean
    ): Int {
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

    private fun playerHeight(
        context: Context,
        isTablet: Boolean
    ): Int {
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