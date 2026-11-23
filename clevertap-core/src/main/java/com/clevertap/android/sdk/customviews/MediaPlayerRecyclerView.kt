package com.clevertap.android.sdk.customviews

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.AbsListView
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inbox.CTInboxBaseMessageViewHolder
import com.clevertap.android.sdk.video.InboxVideoPlayerHandle
import com.clevertap.android.sdk.video.VideoLibChecker
import com.clevertap.android.sdk.video.VideoLibraryIntegrated
import com.clevertap.android.sdk.video.inbox.ExoplayerHandle
import com.clevertap.android.sdk.video.inbox.Media3Handle

/** Inbox list that plays the most visible video row with one shared player. */
@UnstableApi
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MediaPlayerRecyclerView : RecyclerView {

    /** The message and media the player was started for. The row may show another one later. */
    private class PlayingVideo(val messageId: String?, val mediaUrl: String?)

    private val handle: InboxVideoPlayerHandle
    private val rect = Rect()
    private val states: InboxVideoStates
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
                if (playingHolder?.itemView == view) {
                    // The row can return from the view cache without a rebind, so clear its container
                    stopVideo()
                }
            }
        }

    private var playingHolder: CTInboxBaseMessageViewHolder? = null
    private var playingVideo: PlayingVideo? = null

    /** True from [releasePlayer] until [onRestartPlayer] or until the list is attached to a window again. */
    private var released = false

    /**
     * {@inheritDoc}
     */
    constructor(context: Context) : super(context) {
        states = InboxVideoStates()
        handle = createHandle()
        registerListeners()
    }

    /**
     * {@inheritDoc}
     */
    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs) {
        states = InboxVideoStates()
        handle = createHandle()
        registerListeners()
    }

    /**
     * {@inheritDoc}
     */
    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        states = InboxVideoStates()
        handle = createHandle()
        registerListeners()
    }

    /** Lets tests supply the player handle; neither video library is on the unit test classpath. */
    @VisibleForTesting
    internal constructor(
        context: Context,
        handle: InboxVideoPlayerHandle,
        states: InboxVideoStates = InboxVideoStates()
    ) : super(context) {
        this.handle = handle
        this.states = states
        registerListeners()
    }

    /** The lists of one inbox screen share [states], so a video keeps its place across its tabs. */
    internal constructor(
        context: Context,
        states: InboxVideoStates
    ) : super(context) {
        this.states = states
        handle = createHandle()
        registerListeners()
    }

    fun onRestartPlayer() {
        released = false
        playVideo()
    }

    fun playVideo() {
        val targetHolder = findBestVisibleMediaHolder()

        // Case 1 : No viewholder has video item in it
        if (targetHolder == null) {
            stopVideo()
            return
        }

        val messageId = targetHolder.messageId
        val mediaUrl = targetHolder.mediaUrl

        // Case 2 : Found viewholder is same with surface and player attached
        playingHolder?.let { ph ->
            // The row can be rebound to another message, so also compare the message and media
            val samePlayingVideo = playingVideo?.messageId == messageId && playingVideo?.mediaUrl == mediaUrl
            if (ph.itemView == targetHolder.itemView && samePlayingVideo) {
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
            }
        }

        // Case 3: Video has to be played in a different view holder, so move the player there
        stopVideo()
        if (released) {
            // A late playVideo() after onStop must not start a player
            return
        }
        initPlayer()
        val savedState = states.stateOf(messageId, mediaUrl)
        val volume = savedState?.volume ?: MUTED_VOLUME
        handle.setPlayerVolume(volume)
        // Set before addMediaPlayer(), because the player callbacks read playingHolder
        playingHolder = targetHolder
        playingVideo = PlayingVideo(messageId, mediaUrl)
        val addedVideo = targetHolder.addMediaPlayer(
            volume,
            ::toggleMute,
            { uri: String, isMediaAudio: Boolean, isMediaVideo: Boolean ->
                handle.startPlaying(
                    ctx = context,
                    uriString = uri,
                    isMediaAudio = isMediaAudio,
                    isMediaVideo = isMediaVideo,
                    startPositionMs = savedState?.positionMs ?: InboxVideoPlayerHandle.DEFAULT_START_POSITION
                )
                null
            },
            handle.videoSurface()
        )
        if (!addedVideo) {
            playingHolder = null
            playingVideo = null
        }
    }

    /** Call before changing the adapter data, so the player view is not left in a rebound row. */
    fun prepareForListRebind() {
        Logger.v(
            if (playingHolder != null) {
                "prepareForListRebind: detaching video surface from playing holder"
            } else {
                "prepareForListRebind: no active video — nothing to detach"
            }
        )
        stopVideo()
    }

    /** Stops the video and releases the player; the next [playVideo] after a restart creates a new one. */
    fun releasePlayer() {
        released = true
        stopVideo()
        handle.release()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // The list can be moved to another parent without a lifecycle change, which released the player
        released = false
    }

    override fun onDetachedFromWindow() {
        // Backs up the fragment: a page removed from the pager never reaches onDestroy().
        releasePlayer()
        super.onDetachedFromWindow()
    }

    private fun createHandle(): InboxVideoPlayerHandle = when (VideoLibChecker.mediaLibType) {
        VideoLibraryIntegrated.MEDIA3 -> {
            Media3Handle()
        }
        else -> {
            ExoplayerHandle()
        }
    }

    private fun findBestVisibleMediaHolder(): CTInboxBaseMessageViewHolder? {
        var bestHolder: CTInboxBaseMessageViewHolder? = null
        var bestHeight = 0
        for (i in 0 until childCount) {
            val holder = getChildViewHolder(getChildAt(i)) as? CTInboxBaseMessageViewHolder ?: continue
            // A row that is animating out of the list has no adapter position any more
            if (holder.bindingAdapterPosition == NO_POSITION || !holder.needsMediaPlayer()) {
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
        return bestHolder
    }

    /** Creates the player and its view if they do not exist; a no-op while they are alive. */
    private fun initPlayer() {
        handle.initExoplayer(
            context = context,
            buffering = ::bufferingStarted,
            playerReady = ::playerReady
        )
        handle.initPlayerView(
            context = context,
            artworkAsset = ::artworkAsset
        )
    }

    /** Switches between muted and full volume and returns the new volume. */
    private fun toggleMute(): Float {
        handle.setPlayerVolume(if (handle.playerVolume() > MUTED_VOLUME) MUTED_VOLUME else FULL_VOLUME)
        return handle.playerVolume()
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

    private fun registerListeners() {
        addOnScrollListener(onScrollListener)
        addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)
        // Backs up the detach callback, which RecyclerView does not guarantee for every recycle.
        addRecyclerListener { holder ->
            if (holder === playingHolder) {
                stopVideo()
            }
        }
    }

    /** Saves the place, clears the row's container and stops the player, which stays alive. */
    private fun stopVideo() {
        val holder = playingHolder ?: return
        rememberPlayingVideo()
        holder.playerRemoved() // removes all the views from video container
        handle.stop()
        playingHolder = null
        playingVideo = null
    }

    /** Saves the place of the video that is playing now, under the message the player was started for. */
    fun rememberPlayingVideo() {
        val video = playingVideo ?: return
        val messageId = video.messageId ?: return
        val mediaUrl = video.mediaUrl ?: return
        states.remember(messageId, InboxVideoStates.VideoState(mediaUrl, handle.currentPositionMs(), handle.playerVolume()))
    }

    private companion object {

        /** A video that was never played starts muted. */
        const val MUTED_VOLUME = 0f
        const val FULL_VOLUME = 1f
    }
}
