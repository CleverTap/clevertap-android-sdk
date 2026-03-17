package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

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
    fun `onRedirect calls inAppNotificationActionTriggered with url`() {
        val url = "https://www.example.com"
        bridge.onRedirect(url)
        verify(exactly = 1) {
            mockListener.inAppNotificationActionTriggered(
                mockNotification, any(), eq(url), isNull(), isNull()
            )
        }
    }

    @Test
    fun `onRedirect creates open url action with correct type`() {
        val url = "https://www.example.com"
        bridge.onRedirect(url)
        verify {
            mockListener.inAppNotificationActionTriggered(
                any(), match { it.type == InAppActionType.OPEN_URL }, any(), any(), any()
            )
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
