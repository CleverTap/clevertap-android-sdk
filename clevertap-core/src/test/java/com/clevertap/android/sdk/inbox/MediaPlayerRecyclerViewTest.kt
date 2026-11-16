package com.clevertap.android.sdk.inbox

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
    private class VideoRowHolder(itemView: View, container: FrameLayout) : CTInboxBaseMessageViewHolder(itemView) {

        var attachCount = 0

        init {
            frameLayout = container
            itemView.tag = this
        }

        override fun needsMediaPlayer() = true

        override fun addMediaPlayer(
            currentVolume: Float,
            muteClick: Function0<Float>,
            playMedia: Function3<String, Boolean, Boolean, Void>,
            videoSurfaceView: View
        ): Boolean {
            attachCount++
            frameLayout.addView(videoSurfaceView)
            frameLayout.visibility = View.VISIBLE // what playerReady() does once the player is ready
            return true
        }
    }

    private class SingleRowAdapter(private val holder: RecyclerView.ViewHolder) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = holder
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
        override fun getItemCount() = 1
    }

    private lateinit var handle: InboxVideoPlayerHandle
    private lateinit var list: MediaPlayerRecyclerView
    private lateinit var holder: VideoRowHolder
    private lateinit var container: FrameLayout

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        handle = mockk(relaxed = true)
        every { handle.videoSurface() } answers { View(appCtx) }

        container = FrameLayout(appCtx).apply { visibility = View.GONE }
        val row = LinearLayout(appCtx).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ROW_HEIGHT)
            addView(container)
        }
        holder = VideoRowHolder(row, container)

        list = MediaPlayerRecyclerView(appCtx, handle).apply {
            layoutManager = LinearLayoutManager(appCtx)
            adapter = SingleRowAdapter(holder)
            measure(
                MeasureSpec.makeMeasureSpec(LIST_WIDTH, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(LIST_HEIGHT, MeasureSpec.EXACTLY)
            )
            layout(0, 0, LIST_WIDTH, LIST_HEIGHT)
        }
    }

    @Test
    fun `detaching the playing row releases the player and clears its video container`() {
        list.playVideo()
        assertEquals("row never became the playing row", 1, holder.attachCount)
        assertEquals(View.VISIBLE, container.visibility)
        assertEquals(1, container.childCount)
        verify(exactly = 1) { handle.pause() } // playVideo() releases any earlier player first

        // What RecyclerView does when the row scrolls off screen.
        list.layoutManager!!.removeAllViews()

        verify(exactly = 2) { handle.pause() }
        assertEquals(0, container.childCount)
        assertEquals(View.GONE, container.visibility)
    }

    @Test
    fun `detaching a row that is not playing leaves it untouched`() {
        list.layoutManager!!.removeAllViews()

        verify(exactly = 0) { handle.pause() }
        assertEquals(0, holder.attachCount)
    }

    private companion object {

        const val LIST_WIDTH = 1080
        const val LIST_HEIGHT = 1920
        const val ROW_HEIGHT = 600
    }
}
