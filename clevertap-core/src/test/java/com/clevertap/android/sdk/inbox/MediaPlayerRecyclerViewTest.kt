package com.clevertap.android.sdk.inbox

import android.content.Context
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
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Lives in the inbox package because the holder's constructor and video container are
 * package-private.
 */
@RunWith(RobolectricTestRunner::class)
class MediaPlayerRecyclerViewTest : BaseTestCase() {

    /** A video row whose player attach is reduced to what the list cares about. */
    private class VideoRowHolder(itemView: View, val container: FrameLayout) : CTInboxBaseMessageViewHolder(itemView) {

        var attachCount = 0
        var bindCount = 0
        var isVideoRow = true

        init {
            frameLayout = container
            itemView.tag = this
        }

        val showsVideo: Boolean
            get() = container.visibility == View.VISIBLE && container.childCount > 0

        override fun needsMediaPlayer() = isVideoRow

        override fun shouldAutoPlay() = true

        override fun addMediaPlayer(
            currentVolume: Float,
            muteClick: Function0<Float>,
            playMedia: Function3<String, Boolean, Boolean, Void>,
            videoSurfaceView: View
        ): Boolean {
            attachCount++
            frameLayout.removeAllViews()
            frameLayout.addView(videoSurfaceView)
            frameLayout.visibility = View.VISIBLE // what playerReady() does once the player is ready
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
            holder.container.visibility = View.GONE // what the real holders do on bind
        }

        override fun getItemCount() = rows
    }

    private lateinit var handle: InboxVideoPlayerHandle
    private lateinit var list: MediaPlayerRecyclerView
    private lateinit var adapter: VideoRowsAdapter

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        handle = mockk(relaxed = true)
        every { handle.videoSurface() } answers { View(appCtx) }
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

    @Test
    fun `detaching the playing row releases the player and clears its video container`() {
        showList(rows = 1)
        val row = rowAt(0)
        list.playVideo()
        assertEquals("row never became the playing row", 1, row.attachCount)
        assertEquals(View.VISIBLE, row.container.visibility)
        assertEquals(1, row.container.childCount)
        verify(exactly = 1) { handle.pause() } // playVideo() releases any earlier player first

        // What RecyclerView does when the row scrolls off screen.
        list.layoutManager!!.removeAllViews()

        verify(exactly = 2) { handle.pause() }
        assertEquals(0, row.container.childCount)
        assertEquals(View.GONE, row.container.visibility)
    }

    @Test
    fun `detaching a row that is not playing leaves it untouched`() {
        showList(rows = 1)
        val row = rowAt(0)

        list.layoutManager!!.removeAllViews()

        verify(exactly = 0) { handle.pause() }
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
        verify(exactly = 1) { handle.pause() }

        list.layoutManager!!.removeView(other.itemView)

        verify(exactly = 1) { handle.pause() }
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
        list.scrollBy(0, SCROLL_PAST_TWO_ROWS)
        assertEquals("playing row should be detached", null, first.itemView.parent)
        assertEquals(0, first.container.childCount)
        assertEquals(View.GONE, first.container.visibility)
        assertEquals(0, rowsShowingVideo())

        list.playVideo() // scroll settled: another row takes over
        assertEquals(1, first.attachCount)
        assertEquals(1, rowsShowingVideo())

        // Scroll back: the row returns from the cache without a rebind.
        list.scrollBy(0, -SCROLL_PAST_TWO_ROWS)
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

        list.scrollBy(0, SCROLL_PAST_TWO_ROWS)
        assertEquals("video row should be detached", null, video.itemView.parent)
        list.playVideo() // scroll settled with no video row on screen
        assertEquals(0, rowsShowingVideo())

        list.scrollBy(0, -SCROLL_PAST_TWO_ROWS)
        assertSame(video, rowAt(0))
        assertEquals("row came back through a rebind; the test no longer covers the cache path", 1, video.bindCount)

        // The list must have forgotten the row on detach; otherwise it treats the row as
        // still playing and never attaches a player to it again.
        list.playVideo()
        assertEquals(2, video.attachCount)
        assertEquals(View.VISIBLE, video.container.visibility)
        assertEquals(1, video.container.childCount)
    }

    private companion object {

        const val LIST_WIDTH = 1080
        const val LIST_HEIGHT = 1920
        const val ROW_HEIGHT = 600
        const val SCROLL_PAST_TWO_ROWS = 1300
    }
}
