package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PIPInAppCallbacksBridgeTest {

    private val mockListener = mockk<InAppListener>(relaxed = true)
    private val mockShowFailureHandler = mockk<PIPShowFailureHandler>(relaxed = true)
    private val mockLogger = mockk<Logger>(relaxed = true)

    // ─── Callbacks that forward to InAppListener ──────────────────────────────────

    @Test
    fun `onShow calls inAppNotificationDidShow`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onShow()
        verify(exactly = 1) { mockListener.inAppNotificationDidShow(notification, null) }
    }

    @Test
    fun `onClose calls inAppNotificationDidDismiss`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onClose()
        verify(exactly = 1) { mockListener.inAppNotificationDidDismiss(notification, null) }
    }

    @Test
    fun `onAction calls inAppNotificationActionTriggered with onClick from pipConfigJson`() {
        val onClickJson = JSONObject().apply {
            put("type", "url")
            put("android", "https://www.example.com")
        }
        val pipJson = JSONObject().put("onClick", onClickJson)
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
            every { pipConfigJson } returns pipJson
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onAction()
        verify(exactly = 1) {
            mockListener.inAppNotificationActionTriggered(
                notification, any(), "pip_cta", null, null
            )
        }
    }

    @Test
    fun `onAction does nothing when pipConfigJson has no onClick`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
            every { pipConfigJson } returns JSONObject() // no onClick key
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onAction()
        verify(exactly = 0) {
            mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `onAction does nothing when pipConfigJson is null`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
            every { pipConfigJson } returns null
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onAction()
        verify(exactly = 0) {
            mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any())
        }
    }

    // ─── Callbacks that only log (do not forward to InAppListener) ────────────────

    @Test
    fun `onExpand does not call listener`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onExpand()
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onCollapse does not call listener`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onCollapse()
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onPlaybackStarted does not call listener`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onPlaybackStarted()
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onPlaybackPaused does not call listener`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onPlaybackPaused()
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onMediaError does not call listener`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, mockLogger)
        bridge.onMediaError("https://example.com/video.m3u8", "Playback error")
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }
}
