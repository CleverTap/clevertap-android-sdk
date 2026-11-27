package com.clevertap.android.sdk.inbox

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcel
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.android.sdk.customviews.InboxVideoStates
import com.clevertap.android.sdk.customviews.MediaPlayerRecyclerView
import com.clevertap.android.sdk.video.InboxVideoPlayerHandle
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Lives in the inbox package because the holder's constructor and video container are
 * package-private.
 */
@RunWith(RobolectricTestRunner::class)
class MediaPlayerRecyclerViewTest : BaseTestCase() {

    /** A video row whose player attach is reduced to what the list cares about. */
    private class VideoRowHolder(itemView: View, val container: FrameLayout) : CTInboxBaseMessageViewHolder(itemView) {

        var attachCount = 0
        var attempts = 0
        var addResult = true
        var bindCount = 0
        var isVideoRow = true
        var mediaVisibleOverride: Boolean? = null
        var messageIdValue: String? = null
        var mediaUrlValue: String? = null
        var muteClick: Function0<Float>? = null

        init {
            frameLayout = container
            itemView.tag = this
        }

        val showsVideo: Boolean
            get() = container.visibility == View.VISIBLE && container.childCount > 0

        override fun getMessageId() = messageIdValue

        override fun getMediaUrl() = mediaUrlValue

        override fun needsMediaPlayer() = isVideoRow

        override fun shouldAutoPlay() = true

        /** The middle of the row stands for the middle of its media. */
        override fun isMediaVisible(): Boolean {
            mediaVisibleOverride?.let { return it }
            val visible = android.graphics.Rect()
            if (!itemView.getLocalVisibleRect(visible)) {
                return false
            }
            val middle = itemView.height / 2
            return visible.top <= middle && middle <= visible.bottom
        }

        override fun addMediaPlayer(
            currentVolume: Float,
            muteClick: Function0<Float>,
            playMedia: Function3<String, Boolean, Boolean, Void>,
            videoSurfaceView: View
        ): Boolean {
            attempts++
            if (!addResult) {
                return false
            }
            attachCount++
            this.muteClick = muteClick
            frameLayout.removeAllViews()
            frameLayout.addView(videoSurfaceView)
            frameLayout.visibility = View.VISIBLE // what playerReady() does once the player is ready
            playMedia.invoke(mediaUrlValue.orEmpty(), false, true)
            return true
        }
    }

    /** Every row is a video row unless [videoPositions] names the ones that are. */
    private class VideoRowsAdapter(
        private val context: Context,
        private val rows: Int,
        private val videoPositions: Set<Int>?
    ) : RecyclerView.Adapter<VideoRowHolder>() {

        val holders = mutableListOf<VideoRowHolder>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoRowHolder {
            val container = FrameLayout(context).apply { visibility = View.GONE }
            val row = LinearLayout(context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ROW_HEIGHT)
                addView(container)
            }
            return VideoRowHolder(row, container).also { holders += it }
        }

        override fun onBindViewHolder(holder: VideoRowHolder, position: Int) {
            holder.bindCount++
            holder.isVideoRow = videoPositions == null || position in videoPositions
            holder.messageIdValue = "message$position"
            holder.mediaUrlValue = "url$position"
            holder.container.visibility = View.GONE // what the real holders do on bind
        }

        override fun getItemCount() = rows
    }

    private lateinit var handle: InboxVideoPlayerHandle
    private lateinit var surface: View
    private lateinit var list: MediaPlayerRecyclerView
    private lateinit var adapter: VideoRowsAdapter
    private lateinit var states: InboxVideoStates

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        surface = View(appCtx) // one player view for the whole list, as in the real handles
        handle = mockk(relaxed = true)
        every { handle.videoSurface() } returns surface
    }

    /** Lays the list out with [rows] video rows; three of them fit on screen. */
    private fun showList(rows: Int, videoPositions: Set<Int>? = null) {
        adapter = VideoRowsAdapter(appCtx, rows, videoPositions)
        states = InboxVideoStates()
        list = MediaPlayerRecyclerView(appCtx, handle, states).apply {
            layoutManager = LinearLayoutManager(appCtx)
            adapter = this@MediaPlayerRecyclerViewTest.adapter
            measure(
                MeasureSpec.makeMeasureSpec(LIST_WIDTH, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(LIST_HEIGHT, MeasureSpec.EXACTLY)
            )
            layout(0, 0, LIST_WIDTH, LIST_HEIGHT)
        }
    }

    /** What the list would give to a new screen: the places of all videos, including the one playing now. */
    private fun savedStates(): Bundle {
        list.rememberPlayingVideo()
        return states.save()
    }

    private fun rowAt(position: Int) = list.findViewHolderForAdapterPosition(position) as VideoRowHolder

    private fun rowsShowingVideo() = adapter.holders.count { it.showsVideo }

    /** What RecyclerView does when a scroll comes to rest. */
    private fun listStopsScrolling() {
        val listeners = RecyclerView::class.java.getDeclaredField("mScrollListeners")
            .apply { isAccessible = true }.get(list) as List<*>
        listeners.filterIsInstance<RecyclerView.OnScrollListener>()
            .forEach { it.onScrollStateChanged(list, RecyclerView.SCROLL_STATE_IDLE) }
    }

    private fun scrollPastTwoRows() = list.scrollBy(0, SCROLL_PAST_TWO_ROWS)

    private fun scrollBackPastTwoRows() = list.scrollBy(0, -SCROLL_PAST_TWO_ROWS)

    @Test
    fun `the player is created on the first play and not with the list`() {
        showList(rows = 1)
        verify(exactly = 0) { handle.initExoplayer(any(), any(), any()) }

        list.playVideo()

        verify(exactly = 1) { handle.initExoplayer(any(), any(), any()) }
    }

    @Test
    fun `detaching the playing row stops the player and clears its video container`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        assertEquals("row never became the playing row", 1, row.attachCount)
        assertEquals(View.VISIBLE, row.container.visibility)
        assertEquals(1, row.container.childCount)

        // What RecyclerView does when the row scrolls off screen.
        list.layoutManager!!.removeAllViews()

        verify(exactly = 1) { handle.stop() }
        assertEquals(0, row.container.childCount)
        assertEquals(View.GONE, row.container.visibility)
    }

    @Test
    fun `stopping a video keeps the player for the next one`() {
        showList(rows = 1)
        list.playVideo()

        list.layoutManager!!.removeAllViews()

        verify(exactly = 1) { handle.stop() }
        verify(exactly = 0) { handle.release() }
    }

    @Test
    fun `detaching a row that is not playing leaves it untouched`() {
        showList(rows = 1)
        val row = rowAt(0)

        list.layoutManager!!.removeAllViews()

        verify(exactly = 0) { handle.stop() }
        assertEquals(0, row.attachCount)
    }

    @Test
    fun `detaching another row leaves the playing row and its player alone`() {
        showList(rows = 2)
        val playing = rowAt(0)
        val other = rowAt(1)
        list.playVideo()
        assertEquals("top row never became the playing row", 1, playing.attachCount)
        assertEquals(0, other.attachCount)

        list.layoutManager!!.removeView(other.itemView)

        verify(exactly = 0) { handle.stop() }
        assertEquals(View.VISIBLE, playing.container.visibility)
        assertEquals(1, playing.container.childCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `row scrolled off screen and back plays again and only one row ever shows video`() {
        showList(rows = 6)
        val first = rowAt(0)
        list.playVideo()
        assertEquals("top row never became the playing row", 1, first.attachCount)
        assertEquals(1, rowsShowingVideo())

        // Scroll the playing row off screen; RecyclerView detaches it into its view cache.
        scrollPastTwoRows()
        assertEquals("playing row should be detached", null, first.itemView.parent)
        assertEquals(0, first.container.childCount)
        assertEquals(View.GONE, first.container.visibility)
        assertEquals(0, rowsShowingVideo())

        list.playVideo() // scroll settled: another row takes over
        assertEquals(1, first.attachCount)
        assertEquals(1, rowsShowingVideo())

        // Scroll back: the row returns from the cache without a rebind.
        scrollBackPastTwoRows()
        assertSame(first, rowAt(0))
        assertEquals("row came back through a rebind; the test no longer covers the cache path", 1, first.bindCount)
        assertEquals("a stale video container is visible next to the playing row", 1, rowsShowingVideo())

        list.playVideo() // scroll settled: the first row is the best row again
        assertEquals(2, first.attachCount)
        assertEquals(View.VISIBLE, first.container.visibility)
        assertEquals(1, first.container.childCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `the only video row plays again after it was scrolled off screen and back`() {
        showList(rows = 6, videoPositions = setOf(0))
        val video = rowAt(0)
        list.playVideo()
        assertEquals("video row never became the playing row", 1, video.attachCount)

        scrollPastTwoRows()
        assertEquals("video row should be detached", null, video.itemView.parent)
        list.playVideo() // scroll settled with no video row on screen
        assertEquals(0, rowsShowingVideo())

        scrollBackPastTwoRows()
        assertSame(video, rowAt(0))
        assertEquals("row came back through a rebind; the test no longer covers the cache path", 1, video.bindCount)

        // The list must have forgotten the row on detach; otherwise it treats the row as
        // still playing and never attaches a player to it again.
        list.playVideo()
        assertEquals(2, video.attachCount)
        assertEquals(View.VISIBLE, video.container.visibility)
        assertEquals(1, video.container.childCount)
    }

    @Test
    fun `a video that was never played starts muted from the beginning`() {
        showList(rows = 1)

        list.playVideo()

        verify(exactly = 1) { handle.setPlayerVolume(0f) }
        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }
    }

    @Test
    fun `a video resumes from where it was left with the sound it had`() {
        showList(rows = 6, videoPositions = setOf(0))
        list.playVideo()
        // The user unmuted it and it played for 12 seconds.
        every { handle.currentPositionMs() } returns 12_000L
        every { handle.playerVolume() } returns 1f

        scrollPastTwoRows()
        scrollBackPastTwoRows()
        list.playVideo()

        verify(exactly = 1) { handle.setPlayerVolume(1f) }
        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 12_000L) }
    }

    @Test
    fun `a video that was left muted resumes muted`() {
        showList(rows = 6, videoPositions = setOf(0))
        list.playVideo()
        every { handle.currentPositionMs() } returns 5_000L
        every { handle.playerVolume() } returns 0f

        scrollPastTwoRows()
        scrollBackPastTwoRows()
        list.playVideo()

        verify(exactly = 2) { handle.setPlayerVolume(0f) } // first play and the resume
        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 5_000L) }
    }

    @Test
    fun `each video keeps its own position and sound`() {
        showList(rows = 6)
        list.playVideo() // row 0
        every { handle.currentPositionMs() } returns 5_000L
        every { handle.playerVolume() } returns 1f
        scrollPastTwoRows()
        list.playVideo() // row 3 takes over; it was never played, so it starts muted from 0:00
        verify(exactly = 1) { handle.startPlaying(any(), "url3", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }

        every { handle.currentPositionMs() } returns 7_000L
        every { handle.playerVolume() } returns 0f
        scrollBackPastTwoRows()
        list.playVideo() // row 0 is back: it gets its own state, not the state of row 3

        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 5_000L) }
        verify(exactly = 1) { handle.setPlayerVolume(1f) }
    }

    @Test
    fun `a saved state is ignored when the message now has different media`() {
        showList(rows = 6, videoPositions = setOf(0))
        val video = rowAt(0)
        list.playVideo()
        every { handle.currentPositionMs() } returns 12_000L
        every { handle.playerVolume() } returns 1f
        scrollPastTwoRows()
        scrollBackPastTwoRows()
        video.mediaUrlValue = "another-url"

        list.playVideo()

        verify(exactly = 1) { handle.startPlaying(any(), "another-url", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }
        verify(exactly = 0) { handle.setPlayerVolume(1f) }
    }

    @Test
    fun `tapping the mute control switches between muted and full volume`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()

        every { handle.playerVolume() } returns 0f // muted: a tap turns the sound on
        row.muteClick!!.invoke()
        verify(exactly = 1) { handle.setPlayerVolume(1f) }

        every { handle.playerVolume() } returns 1f // audible: a tap mutes it
        row.muteClick!!.invoke()
        verify(exactly = 2) { handle.setPlayerVolume(0f) } // the muted start and this tap
    }

    @Test
    fun `a new list resumes a video from the states saved by the old one`() {
        showList(rows = 6, videoPositions = setOf(0))
        list.playVideo()
        every { handle.currentPositionMs() } returns 12_000L
        every { handle.playerVolume() } returns 1f
        val saved = savedStates() // the video is still playing
        list.releasePlayer() // the old list goes away

        showList(rows = 6, videoPositions = setOf(0)) // what a rotation builds
        states.restore(saved)
        list.playVideo()

        verify(exactly = 1) { handle.setPlayerVolume(1f) }
        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 12_000L) }
    }

    @Test
    fun `saved states keep their values through a parcel`() {
        showList(rows = 6, videoPositions = setOf(0))
        list.playVideo()
        every { handle.currentPositionMs() } returns 7_000L
        every { handle.playerVolume() } returns 1f
        val saved = savedStates()

        val parcel = Parcel.obtain()
        val restored: Bundle?
        try {
            parcel.writeBundle(saved)
            parcel.setDataPosition(0)
            restored = parcel.readBundle(javaClass.classLoader)
        } finally {
            parcel.recycle()
        }
        list.releasePlayer() // the old list goes away
        showList(rows = 6, videoPositions = setOf(0))
        states.restore(restored)
        list.playVideo()

        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 7_000L) }
        verify(exactly = 1) { handle.setPlayerVolume(1f) }
    }

    @Test
    fun `saved states hold both the playing video and the ones stopped earlier`() {
        showList(rows = 6)
        list.playVideo() // row 0
        every { handle.currentPositionMs() } returns 5_000L
        every { handle.playerVolume() } returns 1f
        scrollPastTwoRows()
        list.playVideo() // row 3 takes over
        every { handle.currentPositionMs() } returns 9_000L
        every { handle.playerVolume() } returns 0f

        val saved = savedStates()

        val stopped = saved.getBundle("message0")
        val playing = saved.getBundle("message3")
        assertNotNull("the stopped video was not saved", stopped)
        assertNotNull("the playing video was not saved", playing)
        assertEquals(5_000L, stopped!!.getLong("positionMs"))
        assertEquals(1f, stopped.getFloat("volume"), 0f)
        assertEquals(9_000L, playing!!.getLong("positionMs"))
        assertEquals(0f, playing.getFloat("volume"), 0f)
    }

    @Test
    fun `the place is saved under the message the video was started for even if the row is rebound`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        every { handle.currentPositionMs() } returns 12_000L
        every { handle.playerVolume() } returns 1f

        // The row now shows another message while it is still attached and playing.
        row.messageIdValue = "another-message"
        row.mediaUrlValue = "another-url"
        val saved = savedStates()

        val started = saved.getBundle("message0")
        assertNotNull("the place was not saved for the message that was playing", started)
        assertEquals("url0", started!!.getString("mediaUrl"))
        assertEquals(12_000L, started.getLong("positionMs"))
        assertEquals("the place was saved for the row's new message", null, saved.getBundle("another-message"))
    }

    @Test
    fun `restoring no states keeps every video starting muted from the beginning`() {
        showList(rows = 1)

        states.restore(null)
        states.restore(Bundle())
        list.playVideo()

        verify(exactly = 1) { handle.setPlayerVolume(0f) }
        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }
    }

    @Test
    fun `releasing the player stops the video and a later play creates the player again`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        assertEquals(1, row.container.childCount)

        list.releasePlayer()

        verify(exactly = 1) { handle.stop() }
        verify(exactly = 1) { handle.release() }
        assertEquals(0, row.container.childCount)
        assertEquals(View.GONE, row.container.visibility)

        list.onRestartPlayer()

        assertEquals(2, row.attachCount)
        verify(exactly = 2) { handle.initExoplayer(any(), any(), any()) }
    }

    @Test
    fun `a play queued after the player was released does not create a player`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        list.releasePlayer()

        list.playVideo()

        assertEquals(1, row.attachCount)
        verify(exactly = 1) { handle.initExoplayer(any(), any(), any()) }
        verify(exactly = 1) { handle.startPlaying(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `restarting the list plays the most visible video row`() {
        showList(rows = 1)

        list.onRestartPlayer()

        assertEquals(1, rowAt(0).attachCount)
    }

    @Test
    fun `a playing row keeps playing while enough of it is visible`() {
        showList(rows = 6, videoPositions = setOf(0))
        list.playVideo()

        list.playVideo()

        verify(exactly = 1) { handle.setPlayWhenReady(true) }
        assertEquals("the row must not be attached a second time", 1, rowAt(0).attachCount)
    }

    @Test
    fun `a playing row is paused when too little of it is visible`() {
        showList(rows = 6, videoPositions = setOf(0))
        list.playVideo()

        list.scrollBy(0, 350) // 250 of the 600 pixels of the row stay on screen
        list.playVideo()

        verify(exactly = 1) { handle.setPlayWhenReady(false) }
        verify(exactly = 0) { handle.stop() } // the row is still attached, so it is only paused
    }

    @Test
    fun `preparing for a list rebind stops the playing video and keeps the player`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()

        list.prepareForListRebind()

        verify(exactly = 1) { handle.stop() }
        verify(exactly = 0) { handle.release() }
        assertEquals(0, row.container.childCount)
    }

    @Test
    fun `preparing for a list rebind with no video playing does nothing`() {
        showList(rows = 1)

        list.prepareForListRebind()

        verify(exactly = 0) { handle.stop() }
    }

    @Test
    fun `leaving the window releases the player`() {
        showList(rows = 1)
        list.playVideo()

        shadowOf(list).callOnDetachedFromWindow()

        verify(exactly = 1) { handle.stop() }
        verify(exactly = 1) { handle.release() }
    }

    @Test
    fun `playing starts when the list stops scrolling`() {
        showList(rows = 6)
        val listeners = RecyclerView::class.java.getDeclaredField("mScrollListeners")
            .apply { isAccessible = true }.get(list) as List<*>

        listeners.filterIsInstance<RecyclerView.OnScrollListener>()
            .forEach { it.onScrollStateChanged(list, RecyclerView.SCROLL_STATE_IDLE) }

        assertEquals(1, rowAt(0).attachCount)
    }

    @Test
    fun `the list can be created the way the inbox creates it`() {
        val attrs = Robolectric.buildAttributeSet().build()
        val lists = listOf(
            MediaPlayerRecyclerView(appCtx),
            MediaPlayerRecyclerView(appCtx, attrs),
            MediaPlayerRecyclerView(appCtx, attrs, 0)
        )

        // No player is created until a video plays, so these calls must be harmless.
        lists.forEach { it.releasePlayer() }
    }

    @Test
    fun `restoring states ignores entries that are broken`() {
        showList(rows = 1)
        val broken = Bundle().apply {
            putString("message0", "not a bundle")
            putBundle("message1", Bundle()) // has no media url
        }

        states.restore(broken)
        list.playVideo()

        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }
    }

    @Test
    fun `a row without a message id is never saved or restored`() {
        showList(rows = 1)
        rowAt(0).messageIdValue = null
        list.playVideo()
        every { handle.currentPositionMs() } returns 9_000L

        val saved = savedStates()

        assertTrue(saved.isEmpty)
        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }
    }

    @Test
    fun `a row that cannot take the player is not treated as playing`() {
        showList(rows = 1)
        val row = rowAt(0)
        row.addResult = false

        list.playVideo()
        list.playVideo()

        assertEquals("the second play must try again, not take the already-playing shortcut", 2, row.attempts)
        assertEquals(0, row.attachCount)
        verify(exactly = 0) { handle.setPlayWhenReady(any()) }
    }

    @Test
    fun `buffering and ready events of the player reach the playing row`() {
        val buffering = slot<() -> Unit>()
        val ready = slot<() -> Unit>()
        justRun { handle.initExoplayer(any(), capture(buffering), capture(ready)) }
        showList(rows = 1)
        val row = rowAt(0)
        val spinner = FrameLayout(appCtx).apply { visibility = View.GONE }
        row.progressBarFrameLayout = spinner
        list.playVideo()
        row.container.visibility = View.GONE

        buffering.captured.invoke()
        assertEquals(View.VISIBLE, spinner.visibility)

        ready.captured.invoke()
        assertEquals(View.VISIBLE, row.container.visibility)
        assertEquals(View.GONE, spinner.visibility)
    }

    @Test
    fun `the player view gets the audio artwork`() {
        val artwork = slot<() -> Drawable?>()
        justRun { handle.initPlayerView(any(), capture(artwork)) }
        showList(rows = 1)

        list.playVideo()

        assertNotNull(artwork.captured.invoke())
    }

    @Test
    fun `a recycled row that is still playing is stopped even when no detach was reported`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        val recycleListeners = RecyclerView::class.java.getDeclaredField("mRecyclerListeners")
            .apply { isAccessible = true }.get(list) as List<*>
        val recycleListener = recycleListeners.single() as RecyclerView.RecyclerListener

        recycleListener.onViewRecycled(row)

        verify(exactly = 1) { handle.stop() }
        assertEquals(0, row.container.childCount)
    }

    @Test
    fun `a playing row that now shows another message is not treated as still playing`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        assertEquals(1, row.attachCount)

        // The same row is rebound to another message without being detached.
        row.messageIdValue = "another-message"
        row.mediaUrlValue = "another-url"
        list.playVideo()

        assertEquals("the new message must get its own start", 2, row.attachCount)
        verify(exactly = 1) { handle.startPlaying(any(), "another-url", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }
        verify(exactly = 0) { handle.setPlayWhenReady(any()) }
    }

    @Test
    fun `a playing row without a message id keeps playing when the list settles`() {
        showList(rows = 1)
        val row = rowAt(0)
        row.messageIdValue = null
        list.playVideo()

        list.playVideo()

        assertEquals("the second play must take the already-playing shortcut", 1, row.attachCount)
        verify(exactly = 1) { handle.setPlayWhenReady(true) }
    }

    @Test
    fun `a tab released when another tab comes to rest resumes where it was left when it restarts`() {
        showList(rows = 6, videoPositions = setOf(0))
        every { handle.currentPositionMs() } returns 9_000L
        every { handle.playerVolume() } returns 1f
        list.playVideo()

        list.releasePlayer() // what the tab does when another tab comes to rest
        list.onRestartPlayer() // what the tab does when it is resumed

        verify(exactly = 1) { handle.release() }
        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 9_000L) }
        verify(exactly = 1) { handle.setPlayerVolume(1f) }
    }

    @Test
    fun `a tab released when another tab comes to rest shows no video until it is resumed`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        list.releasePlayer()

        list.playVideo() // a late play while the tab is hidden

        assertEquals(0, row.container.childCount)
        verify(exactly = 1) { handle.initExoplayer(any(), any(), any()) }
    }

    /** A tab list of the same inbox screen: its own player handle, the [states] of the screen. */
    private fun tab(states: InboxVideoStates, tabHandle: InboxVideoPlayerHandle = handle): MediaPlayerRecyclerView {
        val tabAdapter = VideoRowsAdapter(appCtx, 3, null)
        return MediaPlayerRecyclerView(appCtx, tabHandle, states).apply {
            layoutManager = LinearLayoutManager(appCtx)
            adapter = tabAdapter
            measure(
                MeasureSpec.makeMeasureSpec(LIST_WIDTH, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(LIST_HEIGHT, MeasureSpec.EXACTLY)
            )
            layout(0, 0, LIST_WIDTH, LIST_HEIGHT)
        }
    }

    @Test
    fun `a message keeps its place when another tab of the screen shows it`() {
        val states = InboxVideoStates()
        val first = tab(states)
        val second = tab(states)
        every { handle.currentPositionMs() } returns 7_000L
        every { handle.playerVolume() } returns 1f
        first.playVideo()
        first.releasePlayer() // the first tab is hidden when the user goes to the second

        second.onRestartPlayer()

        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 7_000L) }
        verify(exactly = 1) { handle.setPlayerVolume(1f) }
    }

    @Test
    fun `lists with their own states do not share places`() {
        val first = tab(InboxVideoStates())
        val second = tab(InboxVideoStates())
        every { handle.currentPositionMs() } returns 7_000L
        first.playVideo()
        first.releasePlayer()

        second.onRestartPlayer()

        verify(exactly = 0) { handle.startPlaying(any(), any(), any(), any(), 7_000L) }
        verify(exactly = 2) { handle.startPlaying(any(), "url0", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }
    }

    @Test
    fun `the states of the screen survive a new list that is built for the same screen`() {
        val states = InboxVideoStates()
        val old = tab(states)
        every { handle.currentPositionMs() } returns 4_000L
        old.playVideo()
        old.releasePlayer() // the view is destroyed, for example when the fragment goes to the back stack

        val rebuilt = tab(states)
        rebuilt.onRestartPlayer()

        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 4_000L) }
    }

    @Test
    fun `remembering the playing video puts its place into the shared states`() {
        val states = InboxVideoStates()
        val list = tab(states)
        every { handle.currentPositionMs() } returns 6_000L
        every { handle.playerVolume() } returns 1f
        list.playVideo()

        list.rememberPlayingVideo()

        assertEquals(6_000L, states.stateOf("message0", "url0")!!.positionMs)
        assertEquals(1f, states.stateOf("message0", "url0")!!.volume, 0f)
    }

    @Test
    fun `remembering with no video playing remembers nothing`() {
        val states = InboxVideoStates()
        val list = tab(states)

        list.rememberPlayingVideo()

        assertTrue(states.save().isEmpty)
    }

    @Test
    fun `the row is found through its view holder and not through the tag of its view`() {
        showList(rows = 1)
        val row = rowAt(0)
        row.itemView.tag = null

        list.playVideo()

        assertEquals(1, row.attachCount)
    }

    @Test
    fun `a list that is attached again starts a video after it was released on detach`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        list.releasePlayer() // what onDetachedFromWindow does

        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val parent = FrameLayout(activity)
        activity.setContentView(parent)
        parent.addView(list) // the list is moved to another parent: it attaches to a window again
        parent.measure(
            MeasureSpec.makeMeasureSpec(LIST_WIDTH, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(LIST_HEIGHT, MeasureSpec.EXACTLY)
        )
        parent.layout(0, 0, LIST_WIDTH, LIST_HEIGHT)
        list.measure(
            MeasureSpec.makeMeasureSpec(LIST_WIDTH, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(LIST_HEIGHT, MeasureSpec.EXACTLY)
        )
        list.layout(0, 0, LIST_WIDTH, LIST_HEIGHT)
        list.playVideo()

        assertTrue("the list is attached to a window", list.isAttachedToWindow)
        assertEquals("rows that got the player, all holders: " + adapter.holders.map { it.attachCount }, 2, adapter.holders.sumOf { it.attachCount })
        verify(exactly = 2) { handle.initExoplayer(any(), any(), any()) }
    }

    @Test
    fun `a list that is released and was never attached again shows no video`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        list.releasePlayer()

        list.playVideo()

        assertEquals(1, row.attachCount)
    }

    @Test
    fun `a row whose play button was pressed plays instead of the most visible row`() {
        showList(rows = 6)
        val first = rowAt(0)
        val chosen = rowAt(1)

        list.playRow(chosen)

        assertEquals(1, chosen.attachCount)
        assertEquals(0, first.attachCount)
        assertEquals(1, rowsShowingVideo())
        verify(exactly = 1) { handle.initExoplayer(any(), any(), any()) }
    }

    @Test
    fun `the chosen row keeps playing when the list settles`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.playRow(chosen)

        list.playVideo()
        list.playVideo()

        assertEquals(1, chosen.attachCount)
        assertEquals(0, rowAt(0).attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `choosing the row that is playing changes nothing`() {
        showList(rows = 6)
        val first = rowAt(0)
        list.playVideo()

        list.playRow(first)

        assertEquals(1, first.attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `the video that was playing is stopped and the player is kept when another row is chosen`() {
        showList(rows = 6)
        val first = rowAt(0)
        val chosen = rowAt(1)
        list.playVideo()

        list.playRow(chosen)

        assertEquals(0, first.container.childCount)
        assertEquals(View.GONE, first.container.visibility)
        assertEquals(1, chosen.attachCount)
        assertEquals(1, rowsShowingVideo())
        verify(exactly = 1) { handle.stop() }
        verify(exactly = 0) { handle.release() }
    }

    @Test
    fun `a video that was left starts again from its place when its row is chosen`() {
        showList(rows = 6)
        every { handle.currentPositionMs() } returns 7_000L
        every { handle.playerVolume() } returns 1f
        val chosen = rowAt(1)
        list.playRow(chosen)
        list.playRow(rowAt(2)) // the first video is stopped and its place is saved

        list.playRow(chosen)

        verify { handle.startPlaying(any(), "url1", any(), any(), 7_000L) }
    }

    @Test
    fun `the choice is dropped when the list stops scrolling with the row away`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.playRow(chosen)

        scrollPastTwoRows()
        listStopsScrolling() // the most visible row takes over
        assertEquals(0, chosen.container.childCount)
        assertEquals(1, rowsShowingVideo())

        scrollBackPastTwoRows()
        listStopsScrolling() // the chosen row is back, but the choice is gone: the top row is the best row again
        assertEquals(1, rowAt(0).attachCount)
        assertEquals(1, chosen.attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `the choice is dropped when the middle of the row has left the screen`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.playRow(chosen)

        list.scrollBy(0, SCROLL_MIDDLE_OF_SECOND_ROW_GONE)
        assertSame("the chosen row should still be on screen", chosen, rowAt(1))
        listStopsScrolling()

        assertEquals(0, chosen.container.childCount)
        assertEquals(1, rowAt(2).attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `the choice stays when the middle of the row is still on screen`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.playRow(chosen)

        list.scrollBy(0, SCROLL_MIDDLE_OF_SECOND_ROW_STAYS)
        listStopsScrolling()

        assertEquals(1, chosen.attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `a play button pressed on a row that shows less than a video needs still plays it`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.scrollBy(0, SCROLL_LEAVING_LITTLE_OF_SECOND_ROW) // 350 px of the row are left, a video needs 400
        assertSame(chosen, rowAt(1))

        list.playRow(chosen)

        assertEquals(1, chosen.attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `a play button pressed on a row whose middle is off screen plays it until the list stops scrolling`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.scrollBy(0, SCROLL_MIDDLE_OF_SECOND_ROW_GONE)

        list.playRow(chosen)
        assertEquals("the user pressed it, so it plays", 1, chosen.attachCount)

        listStopsScrolling()
        assertEquals(0, chosen.container.childCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `the choice is kept when the row is not visible yet as the tab returns`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.playRow(chosen)
        list.releasePlayer() // another tab came to rest

        // The tab returns while its page is still moving into place: its media is not on screen yet.
        chosen.mediaVisibleOverride = false
        list.onRestartPlayer()
        assertEquals("the chosen row is not shown yet", 0, chosen.container.childCount)

        chosen.mediaVisibleOverride = null // the page has settled
        list.playVideo()

        assertEquals("the chosen video plays again", View.VISIBLE, chosen.container.visibility)
        assertEquals(1, chosen.container.childCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `no chosen video is saved when the user has chosen none`() {
        showList(rows = 6)
        list.playVideo()

        assertEquals(null, list.savePickedVideo())
    }

    @Test
    fun `a chosen video is saved with its message and media`() {
        showList(rows = 6)
        list.playRow(rowAt(1))

        val saved = list.savePickedVideo()!!

        assertEquals("message1", saved.getString("pickedMessageId"))
        assertEquals("url1", saved.getString("pickedMediaUrl"))
    }

    @Test
    fun `a new list plays the video that the old list had chosen`() {
        showList(rows = 6)
        list.playRow(rowAt(1))
        val saved = list.savePickedVideo()

        list.releasePlayer() // the old screen is gone and takes the player view with it
        (surface.parent as? ViewGroup)?.removeView(surface)
        showList(rows = 6) // the screen was rotated: a new list with new rows
        list.restorePickedVideo(saved)
        list.playVideo()

        assertEquals(1, rowAt(1).attachCount)
        assertEquals(0, rowAt(0).attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `restoring nothing or a broken bundle keeps the most visible row playing`() {
        showList(rows = 6)

        list.restorePickedVideo(null)
        list.restorePickedVideo(Bundle())
        list.playVideo()

        assertEquals(1, rowAt(0).attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `a restored choice for a message that is not in the list changes nothing`() {
        showList(rows = 6)
        list.restorePickedVideo(Bundle().apply { putString("pickedMessageId", "gone") })

        list.playVideo()

        assertEquals(1, rowAt(0).attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `a restored choice stays until the list stops scrolling with its row away`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.restorePickedVideo(Bundle().apply {
            putString("pickedMessageId", "message1")
            putString("pickedMediaUrl", "url1")
        })

        scrollPastTwoRows()
        listStopsScrolling()

        assertEquals(0, chosen.attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `the choice is dropped when the row now shows another message`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.playRow(chosen)

        chosen.messageIdValue = "another message"
        list.playVideo()

        assertEquals(1, rowAt(0).attachCount)
        assertEquals(0, chosen.container.childCount)
    }

    @Test
    fun `the choice is dropped when the row now shows other media`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.playRow(chosen)

        chosen.mediaUrlValue = "another url"
        list.playVideo()

        assertEquals(1, rowAt(0).attachCount)
        assertEquals(0, chosen.container.childCount)
    }

    @Test
    fun `a row that is not a video row cannot be chosen`() {
        showList(rows = 3, videoPositions = setOf(1))

        list.playRow(rowAt(0))

        assertEquals(0, rowAt(0).attachCount)
        assertEquals(0, rowsShowingVideo())
        verify(exactly = 0) { handle.initExoplayer(any(), any(), any()) }
    }

    @Test
    fun `a row without a message id cannot be chosen`() {
        showList(rows = 3)
        val row = rowAt(1)
        row.messageIdValue = null

        list.playRow(row)

        assertEquals(0, row.attachCount)
        verify(exactly = 0) { handle.initExoplayer(any(), any(), any()) }
    }

    @Test
    fun `a row that is no longer in the list cannot be chosen`() {
        showList(rows = 3)
        val row = rowAt(1)
        list.layoutManager!!.removeView(row.itemView) // detached, so it has no adapter position

        list.playRow(row)

        assertEquals(0, row.attachCount)
    }

    @Test
    fun `a released list ignores a choice and creates no player`() {
        showList(rows = 6)
        list.playVideo()
        list.releasePlayer()

        list.playRow(rowAt(1))

        assertEquals(0, rowAt(1).attachCount)
        assertEquals(0, rowsShowingVideo())
        verify(exactly = 1) { handle.initExoplayer(any(), any(), any()) }
    }

    @Test
    fun `the choice survives a release and the restart that follows`() {
        showList(rows = 6)
        val chosen = rowAt(1)
        list.playRow(chosen)

        list.releasePlayer()
        list.playVideo() // a late play while released must neither start a player nor drop the choice
        assertEquals(0, rowsShowingVideo())
        list.onRestartPlayer()

        assertEquals(2, chosen.attachCount)
        assertEquals(0, rowAt(0).attachCount)
        assertEquals(1, rowsShowingVideo())
    }

    @Test
    fun `pressing play on three rows quickly leaves one video`() {
        showList(rows = 6)
        val second = rowAt(1)
        val third = rowAt(2)

        list.playRow(second)
        list.playRow(third)
        list.playRow(second)

        assertEquals(2, second.attachCount)
        assertEquals(1, third.attachCount)
        assertEquals(1, rowsShowingVideo())
        assertEquals(View.VISIBLE, second.container.visibility)
        verify(exactly = 0) { handle.release() } // the one player is reused, never released and rebuilt
    }

    @Test
    fun `a row that fails to take the player after it was chosen is not treated as playing`() {
        showList(rows = 3)
        val row = rowAt(1)
        row.addResult = false

        list.playRow(row)

        assertEquals(1, row.attempts)
        assertEquals(0, row.attachCount)
        assertEquals(0, rowsShowingVideo())
    }

    private companion object {

        const val LIST_WIDTH = 1080
        const val LIST_HEIGHT = 1920
        const val ROW_HEIGHT = 600
        const val SCROLL_PAST_TWO_ROWS = 1300

        /** Leaves 350 px of the second row on screen, less than the 400 px a video needs, middle on screen. */
        const val SCROLL_LEAVING_LITTLE_OF_SECOND_ROW = 850

        /** The second row spans 600 to 1200; its middle, 900, is on screen at the top edge or below it. */
        const val SCROLL_MIDDLE_OF_SECOND_ROW_STAYS = 750

        /** Scrolls the middle of the second row off the top of the list. */
        const val SCROLL_MIDDLE_OF_SECOND_ROW_GONE = 1000
    }
}
