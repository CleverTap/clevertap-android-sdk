package com.clevertap.android.sdk.customviews

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.AbsListView
import androidx.annotation.RestrictTo
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inbox.CTInboxBaseMessageViewHolder
import com.clevertap.android.sdk.video.InboxVideoPlayerHandle
import com.clevertap.android.sdk.video.VideoLibChecker
import com.clevertap.android.sdk.video.VideoLibraryIntegrated
import com.clevertap.android.sdk.video.inbox.ExoplayerHandle
import com.clevertap.android.sdk.video.inbox.Media3Handle

@UnstableApi
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MediaPlayerRecyclerView : RecyclerView {

    private val handle : InboxVideoPlayerHandle = when (VideoLibChecker.mediaLibType) {
        VideoLibraryIntegrated.MEDIA3 -> {
            Media3Handle()
        }
        else -> {
            ExoplayerHandle()
        }
    }
    private val rect = Rect()
    private val onScrollListener: OnScrollListener = object : OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                playVideo()
            }
        }
    }
    private val onChildAttachStateChangeListener: OnChildAttachStateChangeListener =
        object : OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {}
            override fun onChildViewDetachedFromWindow(view: View) {
                playingHolder?.let { ph ->
                    if (ph.itemView == view) {
                        stop()
                    }
                }
            }
        }

    private var playingHolder: CTInboxBaseMessageViewHolder? = null

    /**
     * {@inheritDoc}
     */
    constructor(context: Context) : super(context) {
        initialize()
    }

    /**
     * {@inheritDoc}
     */
    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs) {
        initialize()
    }

    /**
     * {@inheritDoc}
     */
    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    fun onPausePlayer() {
        handle.setPlayWhenReady(false)
    }

    fun onRestartPlayer() {
        initialize()
        playVideo()
    }

    fun playVideo() {
        val targetHolder = findBestVisibleMediaHolder()

        // Case 1 : No viewholder has video item in it
        if (targetHolder == null) {
            removeVideoView()
            return
        }

        // Case 2 : Found viewholder is same with surface and player attached
        playingHolder?.let { ph ->
            if (ph.itemView == targetHolder.itemView) {
                val measured = ph.itemView.getGlobalVisibleRect(rect)
                val visibleHeight = if (measured) {
                    rect.height()
                } else {
                    0
                }
                val play = visibleHeight >= 400
                if (play && ph.shouldAutoPlay()) {
                    handle.setPlayWhenReady(true)
                } else {
                    handle.setPlayWhenReady(false)
                }
                return
            } else {
                // no-op
            }
        }

        // Case 3: Video has to be played in different view holder so we remove and reattch to correct one
        removeVideoView()
        initialize()
        val currentVolume = handle.playerVolume()
        val addedVideo = targetHolder.addMediaPlayer(
            currentVolume,
            {
                handle.handleMute()
                handle.playerVolume()
            },
            { uri: String, isMediaAudio: Boolean, isMediaVideo: Boolean ->
                handle.startPlaying(
                    ctx = context,
                    uriString = uri,
                    isMediaAudio = isMediaAudio,
                    isMediaVideo = isMediaVideo
                )
                null
            },
            handle.videoSurface()
        )
        if (addedVideo) {
            playingHolder = targetHolder
        }
    }

    fun stop() {
        /*if (player != null) {
            player.stop();
        }*/
        handle.pause()
        playingHolder = null
    }

    private fun findBestVisibleMediaHolder(): CTInboxBaseMessageViewHolder? {
        var bestHolder: CTInboxBaseMessageViewHolder? = null
        val startPosition = (layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition() ?: 0
        val endPosition = (layoutManager as LinearLayoutManager?)?.findLastVisibleItemPosition() ?: 0
        var bestHeight = 0
        for (i in startPosition..endPosition) {
            val pos = i - startPosition
            val child = getChildAt(pos) ?: continue

            val holder = child.tag as? CTInboxBaseMessageViewHolder
            if (holder != null) {
                if (!holder.needsMediaPlayer()) {
                    continue
                }
                val measured = holder.itemView.getGlobalVisibleRect(rect)
                val height = if (measured) {
                    rect.height()
                } else {
                    0
                }
                if (height > bestHeight) {
                    bestHeight = height
                    bestHolder = holder
                }
            }
        }
        return bestHolder
    }

    private fun initialize() {
        handle.initExoplayer(
            context = context.applicationContext,
            buffering = ::bufferingStarted,
            playerReady = ::playerReady
        )
        handle.initPlayerView(
            context = context.applicationContext,
            artworkAsset = ::artworkAsset
        )
        recyclerViewListeners()
    }

    private fun bufferingStarted() {
        playingHolder?.playerBuffering()
    }

    private fun playerReady() {
        playingHolder?.playerReady()
    }

    private fun artworkAsset(): Drawable {
        return ResourcesCompat.getDrawable(resources, R.drawable.ct_audio, null)!!
    }

    private fun recyclerViewListeners() {
        removeOnScrollListener(onScrollListener)
        removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener)
        addOnScrollListener(onScrollListener)
        addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)
    }

    private fun removeVideoView() {
        handle.pause()
        playingHolder?.playerRemoved() // removes all the views from video container
    }
}
