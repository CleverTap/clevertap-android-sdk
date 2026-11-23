package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.Clock
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveUpdateBroadcastManagerTest {

    private val store = mockk<LiveUpdateTopicStore>(relaxed = true)
    private val subscriber = mockk<LiveUpdateTopicSubscriber>(relaxed = true)
    private val clock = mockk<Clock> { every { currentTimeSeconds() } returns NOW }
    private val manager = LiveUpdateBroadcastManager(
        store = store,
        subscriber = subscriber,
        clock = clock,
        accountId = "acc",
        logger = mockk<ILogger>(relaxed = true),
        softCap = 2
    )

    private fun push(
        event: String,
        type: String? = "1",
        topic: String? = TOPIC,
        dismissal: String? = null
    ) = mockk<Bundle>(relaxed = true) {
        every { getString(Constants.WZRK_LIVE_UPDATE) } returns "true"
        every { getString(Constants.WZRK_LIVE_UPDATE_EVENT) } returns event
        every { getString(Constants.WZRK_LIVE_UPDATE_ACTIVITY_TYPE) } returns type
        every { getString(Constants.WZRK_LIVE_UPDATE_TOPIC) } returns topic
        every { getString(Constants.WZRK_LIVE_UPDATE_ID) } returns ACTIVITY
        every { getString(Constants.WZRK_LIVE_UPDATE_DISMISSAL_DATE) } returns dismissal
        every { getString(Constants.WZRK_LIVE_UPDATE_RUN_ID) } returns "$NOW"
    }

    private fun entry(topic: String, subscribedAt: Long = NOW, expiresAt: Long = NOW + 100) =
        LiveUpdateTopicEntry(topic, ACTIVITY, "acc", subscribedAt, expiresAt)

    @Test
    fun `broadcast START subscribes and records the run with dismissal_date`() {
        every { store.all() } returns emptyList()
        val saved = slot<LiveUpdateTopicEntry>()

        manager.onReceived(push(event = "start", dismissal = "${NOW + 60}"))

        verify { store.put(capture(saved)) }
        verify { subscriber.subscribe(TOPIC) }
        assertEquals(NOW + 60, saved.captured.expiresAtSeconds)
    }

    @Test
    fun `START without dismissal_date expires at runId plus 12h`() {
        every { store.all() } returns emptyList()
        val saved = slot<LiveUpdateTopicEntry>()

        manager.onReceived(push(event = "start"))

        verify { store.put(capture(saved)) }
        assertEquals(NOW + Constants.LIVE_UPDATE_DEFAULT_LIFETIME_SECONDS, saved.captured.expiresAtSeconds)
    }

    @Test
    fun `1-to-1 START does not subscribe`() {
        manager.onReceived(push(event = "start", type = "0"))

        verify(exactly = 0) { subscriber.subscribe(any()) }
    }

    @Test
    fun `End with wzrk_topic unsubscribes the run`() {
        every { store.remove(TOPIC) } returns entry(TOPIC)

        manager.onReceived(push(event = "end"))

        verify { subscriber.unsubscribe(TOPIC) }
    }

    @Test
    fun `End for an already closed run does not unsubscribe again`() {
        every { store.remove(TOPIC) } returns null

        manager.onReceived(push(event = "end"))

        verify(exactly = 0) { subscriber.unsubscribe(any()) }
    }

    @Test
    fun `topic update neither subscribes nor unsubscribes`() {
        every { store.get(TOPIC) } returns entry(TOPIC)

        assertTrue(manager.onReceived(push(event = "update")))

        verify(exactly = 0) { subscriber.subscribe(any()) }
        verify(exactly = 0) { subscriber.unsubscribe(any()) }
    }

    @Test
    fun `START prunes expired runs and evicts the oldest at the cap`() {
        val expired = entry("old", expiresAt = NOW - 1)
        val oldest = entry("a", subscribedAt = 1)
        val newer = entry("b", subscribedAt = 2)
        every { store.all() } returnsMany listOf(listOf(expired, oldest, newer), listOf(oldest, newer))
        every { store.remove(any()) } answers { entry(firstArg()) }

        manager.onReceived(push(event = "start"))

        verify { subscriber.unsubscribe("old") }
        verify { subscriber.unsubscribe("a") }
        verify(exactly = 0) { subscriber.unsubscribe("b") }
        verify { subscriber.subscribe(TOPIC) }
    }

    @Test
    fun `expire unsubscribes an open run once and ignores an already closed one`() {
        every { store.remove(TOPIC) } returnsMany listOf(entry(TOPIC), null)

        manager.expire(TOPIC)
        manager.expire(TOPIC)

        verify(exactly = 1) { subscriber.unsubscribe(TOPIC) }
    }

    @Test
    fun `topic update for a run past dismissal_date is dropped and unsubscribes`() {
        every { store.get(TOPIC) } returns entry(TOPIC, expiresAt = NOW)
        every { store.remove(TOPIC) } returns entry(TOPIC, expiresAt = NOW)

        assertFalse(manager.onReceived(push(event = "update")))
        verify { subscriber.unsubscribe(TOPIC) }
    }

    private companion object {
        const val NOW = 1_793_943_903L
        const val TOPIC = "ct_la_topic"
        const val ACTIVITY = "anonandroid45"
    }
}
