package com.clevertap.android.sdk.inbox

import android.os.Looper
import android.view.View
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

    companion object {

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
