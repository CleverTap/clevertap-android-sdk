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

    private val mockNotification = mockk<CTInAppNotification> {
        every { campaignId } returns "test_campaign_123"
    }
    private val mockListener = mockk<InAppListener>(relaxed = true)
    private val mockLogger = mockk<Logger>(relaxed = true)

    private val bridge = PIPInAppCallbacksBridge(mockNotification, mockListener, mockLogger)

    // ─── Callbacks that forward to InAppListener ──────────────────────────────────

    @Test
    fun `onShow calls inAppNotificationDidShow`() {
        bridge.onShow()
        verify(exactly = 1) { mockListener.inAppNotificationDidShow(mockNotification, null) }
    }

    @Test
    fun `onClose calls inAppNotificationDidDismiss`() {
        bridge.onClose()
        verify(exactly = 1) { mockListener.inAppNotificationDidDismiss(mockNotification, null) }
    }

    @Test
    fun `onAction calls inAppNotificationActionTriggered with button action`() {
        val buttonJson = JSONObject().apply {
            put("text", "Learn More")
            put("actions", JSONObject().apply {
                put("type", "url")
                put("android", "https://www.example.com")
            })
        }
        val button = CTInAppNotificationButton(buttonJson)
        every { mockNotification.buttons } returns listOf(button)

        bridge.onAction()
        verify(exactly = 1) {
            mockListener.inAppNotificationActionTriggered(
                mockNotification, button.action!!, "Learn More", isNull(), isNull()
            )
        }
    }

    @Test
    fun `onAction does nothing when no buttons`() {
        every { mockNotification.buttons } returns emptyList()

        bridge.onAction()
        verify(exactly = 0) {
            mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any())
        }
    }

    // ─── Callbacks that only log (do not forward to InAppListener) ────────────────

    @Test
    fun `onExpand does not call listener`() {
        bridge.onExpand()
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onCollapse does not call listener`() {
        bridge.onCollapse()
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onPlaybackStarted does not call listener`() {
        bridge.onPlaybackStarted()
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onPlaybackPaused does not call listener`() {
        bridge.onPlaybackPaused()
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onMediaError does not call listener`() {
        bridge.onMediaError("https://example.com/video.m3u8", "Playback error")
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }
}
