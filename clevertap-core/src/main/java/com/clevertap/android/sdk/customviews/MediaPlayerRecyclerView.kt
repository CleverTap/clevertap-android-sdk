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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inbox.CTInboxBaseMessageViewHolder
import com.clevertap.android.sdk.video.InboxVideoPlayerHandle
import com.clevertap.android.sdk.video.VideoLibChecker
import com.clevertap.android.sdk.video.VideoLibraryIntegrated
import com.clevertap.android.sdk.video.inbox.ExoplayerHandle
import com.clevertap.android.sdk.video.inbox.Media3Handle

/**
 * Inbox list that plays the video of the most visible video row.
 *
 * One player serves the whole list and moves between rows. It is created on the first play and
 * released by [releasePlayer]. Where each video was left (position and volume) is remembered by
 * message id while the list lives, so a video that is stopped by scrolling resumes from there.
 */
@UnstableApi
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MediaPlayerRecyclerView : RecyclerView {

    /** Where a video was left, for the media it was playing. */
    private class VideoState(val mediaUrl: String, val positionMs: Long, val volume: Float)

    /** The message and media the player was started for. The row may show another one later. */
    private class PlayingVideo(val messageId: String?, val mediaUrl: String?)

    private val handle: InboxVideoPlayerHandle
    private val rect = Rect()
    private val videoStates = mutableMapOf<String, VideoState>()
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
                    // The row can come back from RecyclerView's view cache without a rebind,
                    // so its video container must be cleared here.
                    stopVideo()
                }
            }
        }

    private var playingHolder: CTInboxBaseMessageViewHolder? = null
    private var playingVideo: PlayingVideo? = null

    /** True from [releasePlayer] until [onRestartPlayer]: a late [playVideo] must not rebuild the player. */
    private var released = false

    /**
     * {@inheritDoc}
     */
    constructor(context: Context) : super(context) {
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
        handle = createHandle()
        registerListeners()
    }

    /** Lets tests supply the player handle; neither video library is on the unit test classpath. */
    @VisibleForTesting
    internal constructor(
        context: Context,
        handle: InboxVideoPlayerHandle
    ) : super(context) {
        this.handle = handle
        registerListeners()
    }

    fun onPausePlayer() {
        handle.setPlayWhenReady(false)
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
            // A row can be rebound to another message while it is attached, so the row alone
            // does not tell that the same video is playing.
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
            // A queued or delayed playVideo() after onStop must not start a new player
            return
        }
        initPlayer()
        val savedState = savedStateOf(messageId, mediaUrl)
        val volume = savedState?.volume ?: MUTED_VOLUME
        handle.setPlayerVolume(volume)
        // Set before addMediaPlayer(): it starts playback, and the buffering/ready callbacks
        // read playingHolder to find the views they must show.
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
                    startPositionMs = savedState?.positionMs ?: 0L
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

    /**
     * Stops the playing video and detaches the shared player view from its row. The player is
     * kept for the next video.
     *
     * Must be called before mutating the adapter data and calling notifyDataSetChanged(),
     * otherwise the player view stays parented inside a holder that gets rebound to a
     * different message.
     */
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

    /**
     * Where every video was left, including the one playing now, to give to a new list through
     * [restoreVideoStates]. The fragment uses it to keep the videos' places across a rotation.
     */
    fun saveVideoStates(): Bundle {
        rememberVideoState()
        val saved = Bundle()
        for ((messageId, state) in videoStates) {
            saved.putBundle(
                messageId,
                Bundle().apply {
                    putString(KEY_MEDIA_URL, state.mediaUrl)
                    putLong(KEY_POSITION_MS, state.positionMs)
                    putFloat(KEY_VOLUME, state.volume)
                }
            )
        }
        return saved
    }

    /** Takes back what [saveVideoStates] returned. A null or empty bundle changes nothing. */
    fun restoreVideoStates(saved: Bundle?) {
        if (saved == null) {
            return
        }
        for (messageId in saved.keySet()) {
            val state = saved.getBundle(messageId) ?: continue
            val mediaUrl = state.getString(KEY_MEDIA_URL) ?: continue
            videoStates[messageId] = VideoState(
                mediaUrl = mediaUrl,
                positionMs = state.getLong(KEY_POSITION_MS),
                volume = state.getFloat(KEY_VOLUME)
            )
        }
    }

    /**
     * Stops the playing video and releases the player. The next [playVideo] creates a new
     * player, and videos resume from where they were left.
     */
    fun releasePlayer() {
        released = true
        stopVideo()
        handle.release()
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
        setRecyclerListener { holder ->
            if (holder === playingHolder) {
                stopVideo()
            }
        }
    }

    /**
     * Remembers where the playing video was, empties its row's container and stops the player.
     * The shared player and its view stay alive, so the shortcut in [playVideo] must not treat
     * the row as playing any more: the holder is forgotten.
     */
    private fun stopVideo() {
        val holder = playingHolder ?: return
        rememberVideoState()
        holder.playerRemoved() // removes all the views from video container
        handle.stop()
        playingHolder = null
        playingVideo = null
    }

    /**
     * Saves the place of the video that is playing, under the message the player was started
     * for. The row's current message is not used: the row can be rebound while it is attached.
     */
    private fun rememberVideoState() {
        val video = playingVideo ?: return
        val messageId = video.messageId ?: return
        val mediaUrl = video.mediaUrl ?: return
        videoStates[messageId] = VideoState(mediaUrl, handle.currentPositionMs(), handle.playerVolume())
    }

    /** The saved state of the message, only if its media is still the same. */
    private fun savedStateOf(messageId: String?, mediaUrl: String?): VideoState? {
        if (messageId == null) {
            return null
        }
        return videoStates[messageId]?.takeIf { it.mediaUrl == mediaUrl }
    }

    private companion object {

        /** A video that was never played starts muted. */
        const val MUTED_VOLUME = 0f
        const val FULL_VOLUME = 1f

        const val KEY_MEDIA_URL = "mediaUrl"
        const val KEY_POSITION_MS = "positionMs"
        const val KEY_VOLUME = "volume"
    }
}
