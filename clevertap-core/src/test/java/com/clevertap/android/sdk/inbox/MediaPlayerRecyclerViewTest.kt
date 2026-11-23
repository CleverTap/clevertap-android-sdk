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
        list = MediaPlayerRecyclerView(appCtx, handle).apply {
            layoutManager = LinearLayoutManager(appCtx)
            adapter = this@MediaPlayerRecyclerViewTest.adapter
            measure(
                MeasureSpec.makeMeasureSpec(LIST_WIDTH, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(LIST_HEIGHT, MeasureSpec.EXACTLY)
            )
            layout(0, 0, LIST_WIDTH, LIST_HEIGHT)
        }
    }

    private fun rowAt(position: Int) = list.findViewHolderForAdapterPosition(position) as VideoRowHolder

    private fun rowsShowingVideo() = adapter.holders.count { it.showsVideo }

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
        val saved = list.saveVideoStates() // the video is still playing
        list.releasePlayer() // the old list goes away

        showList(rows = 6, videoPositions = setOf(0)) // what a rotation builds
        list.restoreVideoStates(saved)
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
        val saved = list.saveVideoStates()

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
        list.restoreVideoStates(restored)
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

        val saved = list.saveVideoStates()

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
        val saved = list.saveVideoStates()

        val started = saved.getBundle("message0")
        assertNotNull("the place was not saved for the message that was playing", started)
        assertEquals("url0", started!!.getString("mediaUrl"))
        assertEquals(12_000L, started.getLong("positionMs"))
        assertEquals("the place was saved for the row's new message", null, saved.getBundle("another-message"))
    }

    @Test
    fun `restoring no states keeps every video starting muted from the beginning`() {
        showList(rows = 1)

        list.restoreVideoStates(null)
        list.restoreVideoStates(Bundle())
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

        list.restoreVideoStates(broken)
        list.playVideo()

        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, InboxVideoPlayerHandle.DEFAULT_START_POSITION) }
    }

    @Test
    fun `a row without a message id is never saved or restored`() {
        showList(rows = 1)
        rowAt(0).messageIdValue = null
        list.playVideo()
        every { handle.currentPositionMs() } returns 9_000L

        val saved = list.saveVideoStates()

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
    fun `a tab released on pause resumes where it was left when it restarts`() {
        showList(rows = 6, videoPositions = setOf(0))
        every { handle.currentPositionMs() } returns 9_000L
        every { handle.playerVolume() } returns 1f
        list.playVideo()

        list.releasePlayer() // what the tab does when it is paused
        list.onRestartPlayer() // what the tab does when it is resumed

        verify(exactly = 1) { handle.release() }
        verify(exactly = 1) { handle.startPlaying(any(), "url0", false, true, 9_000L) }
        verify(exactly = 1) { handle.setPlayerVolume(1f) }
    }

    @Test
    fun `a tab released on pause shows no video until it is resumed`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        list.releasePlayer()

        list.playVideo() // a late play while the tab is paused

        assertEquals(0, row.container.childCount)
        verify(exactly = 1) { handle.initExoplayer(any(), any(), any()) }
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

    private companion object {

        const val LIST_WIDTH = 1080
        const val LIST_HEIGHT = 1920
        const val ROW_HEIGHT = 600
        const val SCROLL_PAST_TWO_ROWS = 1300
    }
}
