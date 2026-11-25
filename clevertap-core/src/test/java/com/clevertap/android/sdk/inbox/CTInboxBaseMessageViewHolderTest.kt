package com.clevertap.android.sdk.inbox

import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
        val spinner = holder.progressBarFrameLayout!!

        holder.playerBuffering()
        assertEquals(View.VISIBLE, spinner.visibility)

        holder.playerReady()
        assertEquals(View.GONE, spinner.visibility)
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
