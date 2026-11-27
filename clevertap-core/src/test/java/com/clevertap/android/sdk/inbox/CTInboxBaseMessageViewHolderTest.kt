package com.clevertap.android.sdk.inbox

import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import com.clevertap.android.sdk.R
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class CTInboxBaseMessageViewHolderTest : BaseTestCase() {

    private class TestViewHolder(itemView: View) : CTInboxBaseMessageViewHolder(itemView) {

        fun invokeMarkItemAsRead(message: CTInboxMessage, position: Int) {
            markItemAsRead(message, position)
        }
    }

    private lateinit var holder: TestViewHolder
    private lateinit var readDotView: ImageView
    private lateinit var fragmentSpy: CTInboxListViewFragment

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        readDotView = ImageView(appCtx).apply {
            id = R.id.read_circle
            visibility = View.VISIBLE
        }
        val itemView = LinearLayout(appCtx).apply { addView(readDotView) }
        holder = TestViewHolder(itemView)

        val activity = mockk<FragmentActivity>(relaxed = true)
        every { activity.runOnUiThread(any()) } answers { firstArg<Runnable>().run() }
        fragmentSpy = spyk(CTInboxListViewFragment())
        every { fragmentSpy.activity } returns activity
        justRun { fragmentSpy.didShow(any(), any()) }
        fragmentSpy.haveVideoPlayerSupport = true
    }

    private fun unreadMessage(id: String): CTInboxMessage {
        val json = JSONObject(MESSAGE_JSON).put("id", id).put("isRead", false)
        return CTInboxMessage(json)
    }

    @Test
    fun `playerRemoved empties and hides the video container left visible by playerReady`() {
        val container = FrameLayout(appCtx).apply { addView(View(appCtx)) }
        holder.frameLayout = container
        holder.playerReady()
        assertEquals(View.VISIBLE, container.visibility)

        holder.playerRemoved()

        assertEquals(0, container.childCount)
        assertEquals(View.GONE, container.visibility)
    }

    @Test
    fun `delayed markItemAsRead fires didShow with the exact bound message when holder is not rebound`() {
        val messageA = unreadMessage("msg_A")
        holder.configureWithMessage(messageA, fragmentSpy, 0)

        holder.invokeMarkItemAsRead(messageA, 0)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2000))

        verify(exactly = 1) { fragmentSpy.didShow(null, messageA) }
        assertEquals(View.GONE, readDotView.visibility)
        assertTrue(messageA.isRead)
    }

    @Test
    fun `delayed markItemAsRead does nothing when holder was rebound to another message`() {
        val messageA = unreadMessage("msg_A")
        val messageB = unreadMessage("msg_B")
        holder.configureWithMessage(messageA, fragmentSpy, 0)
        holder.invokeMarkItemAsRead(messageA, 0)

        // Simulate a list refresh recycling this holder to a different message
        // before the 2s timer fires.
        holder.configureWithMessage(messageB, fragmentSpy, 0)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2000))

        verify(exactly = 0) { fragmentSpy.didShow(any(), any()) }
        assertEquals(View.VISIBLE, readDotView.visibility)
        assertFalse(messageA.isRead)
        assertFalse(messageB.isRead)
    }

    @Test
    fun `stale timer does not crash when the message list shrank in the meantime`() {
        val messageA = unreadMessage("msg_A")
        val messageB = unreadMessage("msg_B")
        holder.configureWithMessage(messageA, fragmentSpy, 5)
        holder.invokeMarkItemAsRead(messageA, 5)

        holder.configureWithMessage(messageB, fragmentSpy, 0)
        fragmentSpy.inboxMessages.clear()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2000))

        verify(exactly = 0) { fragmentSpy.didShow(any(), any()) }
    }

    private fun videoMessage(id: String, contentType: String = "video/mp4"): CTInboxMessage {
        val media = "\"media\":{\"url\":\"$CLIP_URL\",\"content_type\":\"$contentType\"}"
        val json = JSONObject(MESSAGE_JSON.replace("\"media\":{}", media)).put("id", id).put("isRead", true)
        return CTInboxMessage(json)
    }

    /** Binds [message] and gives the holder the views the real layouts give it. */
    private fun bind(message: CTInboxMessage): FrameLayout {
        holder.configureWithMessage(message, fragmentSpy, 0)
        holder.context = appCtx // the spy fragment has no context to hand over
        val container = FrameLayout(appCtx).apply { visibility = View.GONE }
        holder.frameLayout = container
        holder.progressBarFrameLayout = FrameLayout(appCtx)
        return container
    }

    private class Started(val url: String, val audio: Boolean, val video: Boolean)

    @Test
    fun `message id and media url are null before the first bind and follow the bound message`() {
        assertNull(holder.messageId)
        assertNull(holder.mediaUrl)

        holder.configureWithMessage(videoMessage("msg_V"), fragmentSpy, 0)

        assertEquals("msg_V", holder.messageId)
        assertEquals(CLIP_URL, holder.mediaUrl)
    }

    @Test
    fun `addMediaPlayer does nothing for a message that is not audio or video`() {
        val container = bind(unreadMessage("msg_text"))
        val started = mutableListOf<Started>()

        val added = holder.addMediaPlayer(0f, { 0f }, { url, audio, video -> started += Started(url, audio, video); null }, View(appCtx))

        assertFalse(added)
        assertEquals(0, container.childCount)
        assertTrue(started.isEmpty())
    }

    @Test
    fun `addMediaPlayer puts the player view and mute control in the hidden container and starts the video`() {
        val container = bind(videoMessage("msg_V"))
        val surface = View(appCtx)
        val started = mutableListOf<Started>()

        val added = holder.addMediaPlayer(0f, { 0f }, { url, audio, video -> started += Started(url, audio, video); null }, surface)

        assertTrue(added)
        assertEquals(2, container.childCount) // the player view and the mute control
        assertSame(surface, container.getChildAt(0))
        assertEquals("the container is shown only once the player is ready", View.GONE, container.visibility)
        assertEquals(1, started.size)
        assertEquals(CLIP_URL, started[0].url)
        assertFalse(started[0].audio)
        assertTrue(started[0].video)
        assertEquals(View.GONE, container.getChildAt(1).visibility)

        holder.playerReady()

        assertEquals(View.VISIBLE, container.visibility)
        assertEquals(View.VISIBLE, container.getChildAt(1).visibility)
    }

    @Test
    fun `addMediaPlayer gives an audio message no mute control`() {
        val container = bind(videoMessage("msg_A", contentType = "audio/mpeg"))
        val started = mutableListOf<Started>()

        val added = holder.addMediaPlayer(0f, { 0f }, { url, audio, video -> started += Started(url, audio, video); null }, View(appCtx))

        assertTrue(added)
        assertEquals(1, container.childCount)
        assertTrue(started[0].audio)
        assertFalse(started[0].video)
    }

    @Test
    fun `addMediaPlayer takes the shared player view out of the row it was still in`() {
        val container = bind(videoMessage("msg_V"))
        val surface = View(appCtx)
        val previousRow = FrameLayout(appCtx).apply { addView(surface) }

        holder.addMediaPlayer(0f, { 0f }, { _, _, _ -> null }, surface)

        assertEquals(0, previousRow.childCount)
        assertSame(container, surface.parent)
    }

    @Test
    fun `the mute control calls back and shows the label for the volume it gets`() {
        val container = bind(videoMessage("msg_V"))
        var volumeAfterTap = 1f
        var taps = 0
        holder.addMediaPlayer(0f, { taps++; volumeAfterTap }, { _, _, _ -> null }, View(appCtx))
        val muteControl = container.getChildAt(1) as ImageView
        val unmuteLabel = appCtx.getString(R.string.ct_unmute_button_content_description)
        val muteLabel = appCtx.getString(R.string.ct_mute_button_content_description)
        assertEquals("starts muted, so the control offers to unmute", unmuteLabel, muteControl.contentDescription)

        muteControl.performClick()

        assertEquals(1, taps)
        assertEquals("now audible, so the control offers to mute", muteLabel, muteControl.contentDescription)

        volumeAfterTap = 0f
        muteControl.performClick()

        assertEquals(2, taps)
        assertEquals(unmuteLabel, muteControl.contentDescription)
    }

    @Test
    fun `the spinner shows while buffering and hides when the player is ready`() {
        bind(videoMessage("msg_V"))
        val spinner = holder.progressBarFrameLayout

        holder.playerBuffering()
        assertEquals(View.VISIBLE, spinner.visibility)

        holder.playerReady()
        assertEquals(View.GONE, spinner.visibility)
    }

    /** The views the real row layouts give a holder: a media area that holds the player container. */
    private class MediaViews(val mediaLayout: RelativeLayout, val container: FrameLayout)

    private fun bindWithMediaArea(message: CTInboxMessage): MediaViews {
        val container = FrameLayout(appCtx).apply { visibility = View.GONE }
        val mediaLayout = RelativeLayout(appCtx).apply {
            addView(View(appCtx)) // the poster or placeholder
            addView(container)
        }
        holder.mediaLayout = mediaLayout
        holder.frameLayout = container
        holder.progressBarFrameLayout = FrameLayout(appCtx)
        holder.configureWithMessage(message, fragmentSpy, 0)
        holder.context = appCtx
        return MediaViews(mediaLayout, container)
    }

    private fun playButton(views: MediaViews): View? {
        val label = appCtx.getString(R.string.ct_inbox_play_video_content_description)
        return (0 until views.mediaLayout.childCount).map { views.mediaLayout.getChildAt(it) }
            .firstOrNull { it.contentDescription == label }
    }

    private fun addPlayer() =
        holder.addMediaPlayer(0f, { 0f }, { _, _, _ -> null }, View(appCtx))

    @Test
    fun `a video row shows a play button below its player container`() {
        val views = bindWithMediaArea(videoMessage("msg_V"))

        val button = playButton(views)

        assertTrue("no play button on a video row", button != null)
        assertEquals(View.VISIBLE, button!!.visibility)
        assertTrue(
            "the button must sit below the player container, so a ready video covers it",
            views.mediaLayout.indexOfChild(button) < views.mediaLayout.indexOfChild(views.container)
        )
        assertTrue(button.isClickable)
    }

    @Test
    fun `an audio row and a row without media have no play button`() {
        val audio = bindWithMediaArea(videoMessage("msg_A", contentType = "audio/mpeg"))
        assertNull(playButton(audio))

        val text = bindWithMediaArea(unreadMessage("msg_text"))
        assertNull(playButton(text))
    }

    @Test
    fun `a row that has the player hides its play button and shows it again when the player leaves`() {
        val views = bindWithMediaArea(videoMessage("msg_V"))
        val button = playButton(views)!!

        addPlayer()
        assertEquals(View.GONE, button.visibility)

        holder.playerRemoved()
        assertEquals(View.VISIBLE, button.visibility)
    }

    @Test
    fun `a row that is bound again while it has the player keeps its play button hidden`() {
        val views = bindWithMediaArea(videoMessage("msg_V"))
        val button = playButton(views)!!
        addPlayer()

        holder.configureWithMessage(videoMessage("msg_V"), fragmentSpy, 0)

        assertEquals(View.GONE, button.visibility)
    }

    @Test
    fun `a row bound to another message gets a play button even if the player was never reported removed`() {
        val views = bindWithMediaArea(videoMessage("msg_V"))
        val button = playButton(views)!!
        addPlayer()

        holder.configureWithMessage(videoMessage("msg_other"), fragmentSpy, 0)

        assertEquals(View.VISIBLE, button.visibility)
    }

    @Test
    fun `a row bound to a message that is not a video hides the play button`() {
        val views = bindWithMediaArea(videoMessage("msg_V"))
        val button = playButton(views)!!

        holder.configureWithMessage(unreadMessage("msg_text"), fragmentSpy, 0)
        assertEquals(View.GONE, button.visibility)

        holder.configureWithMessage(videoMessage("msg_audio", contentType = "audio/mpeg"), fragmentSpy, 0)
        assertEquals(View.GONE, button.visibility)

        holder.configureWithMessage(videoMessage("msg_V2"), fragmentSpy, 0)
        assertEquals(View.VISIBLE, button.visibility)
    }

    @Test
    fun `binding a row many times creates one play button`() {
        val views = bindWithMediaArea(videoMessage("msg_V"))
        val childrenWithButton = views.mediaLayout.childCount

        repeat(5) { holder.configureWithMessage(videoMessage("msg_V$it"), fragmentSpy, 0) }

        assertEquals(childrenWithButton, views.mediaLayout.childCount)
    }

    @Test
    fun `pressing the play button asks the list to play the row`() {
        justRun { fragmentSpy.onPlayButtonClicked(any()) }
        val views = bindWithMediaArea(videoMessage("msg_V"))

        playButton(views)!!.performClick()

        verify(exactly = 1) { fragmentSpy.onPlayButtonClicked(holder) }
    }

    @Test
    fun `the media of a row is visible while its middle is on screen`() {
        val media = mockk<RelativeLayout>(relaxed = true)
        every { media.height } returns 600
        holder.mediaLayout = media

        every { media.getLocalVisibleRect(any()) } answers { firstArg<android.graphics.Rect>().set(0, 0, 1080, 600); true }
        assertTrue("the whole media is on screen", holder.isMediaVisible())

        every { media.getLocalVisibleRect(any()) } answers { firstArg<android.graphics.Rect>().set(0, 300, 1080, 600); true }
        assertTrue("the middle is just on screen", holder.isMediaVisible())

        every { media.getLocalVisibleRect(any()) } answers { firstArg<android.graphics.Rect>().set(0, 301, 1080, 600); true }
        assertFalse("the middle is just off screen at the top", holder.isMediaVisible())

        every { media.getLocalVisibleRect(any()) } answers { firstArg<android.graphics.Rect>().set(0, 0, 1080, 299); true }
        assertFalse("the middle is just off screen at the bottom", holder.isMediaVisible())

        every { media.getLocalVisibleRect(any()) } returns false
        assertFalse("none of it is on screen", holder.isMediaVisible())
    }

    @Test
    fun `the media of a row that is not laid out or has no media area is not visible`() {
        assertFalse(holder.isMediaVisible())

        val media = mockk<RelativeLayout>(relaxed = true)
        every { media.height } returns 0
        every { media.getLocalVisibleRect(any()) } returns true
        holder.mediaLayout = media
        assertFalse(holder.isMediaVisible())
    }

    @Test
    fun `a hide of the spinner queued when the player left does not win over the next player`() {
        val views = bindWithMediaArea(videoMessage("msg_V"))
        val spinner = holder.progressBarFrameLayout
        addPlayer()
        holder.playerRemoved() // queues the hide of the spinner

        addPlayer() // the same row gets the player again before the queue runs
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(View.VISIBLE, spinner.visibility)
        assertTrue(views.container.childCount > 0)
    }

    @Test
    fun `the default video placeholder keeps the 16 to 9 shape of the media area`() {
        val drawable = androidx.core.content.ContextCompat.getDrawable(appCtx, R.drawable.ct_video_1)

        assertTrue("the placeholder drawable is missing", drawable != null)
        assertEquals(16 * drawable!!.intrinsicHeight, 9 * drawable.intrinsicWidth)
    }

    @Test
    fun `a video row has no play button when the app has no video library`() {
        fragmentSpy.haveVideoPlayerSupport = false

        val views = bindWithMediaArea(videoMessage("msg_V"))

        assertNull("a play button here could not do anything", playButton(views))
    }

    @Test
    fun `a play button that is shown is hidden again when the app has no video library`() {
        val views = bindWithMediaArea(videoMessage("msg_V"))
        val button = playButton(views)!!

        fragmentSpy.haveVideoPlayerSupport = false
        holder.configureWithMessage(videoMessage("msg_V2"), fragmentSpy, 0)

        assertEquals(View.GONE, button.visibility)
    }

    @Test
    fun `a row without a media area has no play button and binds without a problem`() {
        holder.mediaLayout = null

        holder.configureWithMessage(videoMessage("msg_V"), fragmentSpy, 0)
        holder.playerRemoved()
    }

    companion object {

        private const val CLIP_URL = "https://example.com/clip.mp4"


        private val MESSAGE_JSON = """{"id":"1674122920_1674457409","msg":{"bg":"#ECEDF2","orientation":"l",
            |"content":[{"key":5385896764,
            |"message":{"replacements":"SampleMessage","text":"SampleMessage","color":"#434761"},
            |"title":{"replacements":"SampleTitle","text":"SampleTitle","color":"#434761"},
            |"action":{"hasUrl":false,"hasLinks":false,"url":{},"links":[]},"media":{},"icon":{}}],
            |"type":"simple","tags":[],"enableTags":false},"isRead":false,"date":1674457409,
            |"wzrk_ttl":1675062209,"tags":[""],"wzrk_id":"1674122920_20230123",
            |"wzrkParams":{"wzrk_ttl":1675062209,"wzrk_id":"1674122920_20230123","wzrk_pivot":"wzrk_default"}}""".trimMargin()
    }
}
