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

    private val store = mockk<LiveUpdateTopicStore>(relaxed = true).also {
        every { it.get(any()) } returns null
    }
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
        action: String? = null,
        liveUpdate: Boolean = true,
        laEvent: String? = "update",
        topic: String? = TOPIC,
        dismissal: String? = null,
        unsubOnDismiss: String? = null
    ) = mockk<Bundle>(relaxed = true) {
        every { getString(Constants.WZRK_LIVE_UPDATE) } returns if (liveUpdate) "true" else null
        every { getString(Constants.WZRK_LIVE_UPDATE_EVENT) } returns laEvent
        every { getString(Constants.WZRK_TOPIC_ACTION) } returns action
        every { getString(Constants.WZRK_LIVE_UPDATE_TOPIC) } returns topic
        every { getString(Constants.WZRK_LIVE_UPDATE_ID) } returns ACTIVITY
        every { getString(Constants.WZRK_LIVE_UPDATE_DISMISSAL_DATE) } returns dismissal
        every { getString(Constants.WZRK_LIVE_UPDATE_RUN_ID) } returns "$NOW"
        every { getString(Constants.WZRK_TOPIC_UNSUB_ON_DISMISS) } returns unsubOnDismiss
    }

    private fun entry(
        topic: String,
        subscribedAt: Long = NOW,
        expiresAt: Long = NOW + 100,
        unsubOnDismiss: Boolean = false
    ) = LiveUpdateTopicEntry(topic, ACTIVITY, "acc", subscribedAt, expiresAt, unsubOnDismiss)

    @Test
    fun `subscribe subscribes and records the run with dismissal_date and dismiss flag`() {
        every { store.all() } returns emptyList()
        val saved = slot<LiveUpdateTopicEntry>()

        manager.onReceived(push(action = "subscribe", laEvent = "start", dismissal = "${NOW + 60}", unsubOnDismiss = "true"))

        verify { store.put(capture(saved)) }
        verify { subscriber.subscribe(TOPIC) }
        assertEquals(NOW + 60, saved.captured.expiresAtSeconds)
        assertTrue(saved.captured.unsubscribeOnDismiss)
    }

    @Test
    fun `subscribe without dismissal_date expires at runId plus 12h`() {
        every { store.all() } returns emptyList()
        val saved = slot<LiveUpdateTopicEntry>()

        manager.onReceived(push(action = "subscribe"))

        verify { store.put(capture(saved)) }
        assertEquals(NOW + Constants.LIVE_UPDATE_DEFAULT_LIFETIME_SECONDS, saved.captured.expiresAtSeconds)
    }

    @Test
    fun `live activity start without wzrk_topic_action does not subscribe`() {
        manager.onReceived(push(laEvent = "start"))

        verify(exactly = 0) { subscriber.subscribe(any()) }
    }

    @Test
    fun `live activity end without unsubscribe keeps the subscription`() {
        manager.onReceived(push(laEvent = "end"))

        verify(exactly = 0) { subscriber.unsubscribe(any()) }
    }

    @Test
    fun `unsubscribe reaches FCM even with no local record`() {
        every { store.remove(TOPIC) } returns null

        manager.onReceived(push(action = "unsubscribe"))

        verify(exactly = 1) { subscriber.unsubscribe(TOPIC) }
    }

    @Test
    fun `topic push for an unknown run is adopted without subscribing`() {
        val saved = slot<LiveUpdateTopicEntry>()

        manager.onReceived(push(dismissal = "${NOW + 60}"))

        verify { store.put(capture(saved)) }
        assertEquals(NOW + 60, saved.captured.expiresAtSeconds)
        verify(exactly = 0) { subscriber.subscribe(any()) }
    }

    @Test
    fun `pure control message is not rendered, piggybacked one is`() {
        every { store.all() } returns emptyList()

        assertFalse(manager.onReceived(push(action = "subscribe", liveUpdate = false)))
        assertTrue(manager.onReceived(push(action = "subscribe", laEvent = "start")))
        assertTrue(manager.onReceived(push()))
    }

    @Test
    fun `swipe unsubscribes only when the run opted in`() {
        every { store.get(TOPIC) } returns entry(TOPIC, unsubOnDismiss = false)
        manager.onDismissed(push())
        verify(exactly = 0) { subscriber.unsubscribe(any()) }

        every { store.get(TOPIC) } returns entry(TOPIC, unsubOnDismiss = true)
        every { store.remove(TOPIC) } returns entry(TOPIC, unsubOnDismiss = true)
        manager.onDismissed(push())
        verify(exactly = 1) { subscriber.unsubscribe(TOPIC) }
    }

    @Test
    fun `subscribe prunes expired runs and evicts the oldest at the cap`() {
        val expired = entry("old", expiresAt = NOW - 1)
        val oldest = entry("a", subscribedAt = 1)
        val newer = entry("b", subscribedAt = 2)
        every { store.all() } returnsMany listOf(listOf(expired, oldest, newer), listOf(oldest, newer))
        every { store.remove(any()) } answers { entry(firstArg()) }

        manager.onReceived(push(action = "subscribe"))

        verify { subscriber.unsubscribe("old") }
        verify { subscriber.unsubscribe("a") }
        verify(exactly = 0) { subscriber.unsubscribe("b") }
        verify { subscriber.subscribe(TOPIC) }
    }

    private companion object {
        const val NOW = 1_793_943_903L
        const val TOPIC = "ct_la_topic"
        const val ACTIVITY = "anonandroid45"
    }
}
