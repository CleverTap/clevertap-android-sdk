package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
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
    private val logger = TestLogger()

    // ─── Callbacks that forward to InAppListener ──────────────────────────────────

    @Test
    fun `onShow calls inAppNotificationDidShow`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onShow()
        verify(exactly = 1) { mockListener.inAppNotificationDidShow(notification, null) }
    }

    @Test
    fun `onClose calls inAppNotificationDidDismiss`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onClose()
        verify(exactly = 1) { mockListener.inAppNotificationDidDismiss(notification, null) }
    }

    @Test
    fun `onAction calls inAppNotificationActionTriggered with onClick from pipConfigJson`() {
        val onClickJson = JSONObject().apply {
            put("type", "url")
            put("android", "https://www.example.com")
            put("c2a", "pip_cta")
        }
        val pipJson = JSONObject().put("onClick", onClickJson)
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
            every { pipConfigJson } returns pipJson
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onAction()
        verify(exactly = 1) {
            mockListener.inAppNotificationActionTriggered(
                notification,
                any(),
                "pip_cta",
                match { it.getString(Constants.KEY_WZRK_ELEMENT_ID) == Constants.INAPP_ELEMENT_ID_PIP_CTA },
                null
            )
        }
    }

    @Test
    fun `onAction does nothing when pipConfigJson has no onClick`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
            every { pipConfigJson } returns JSONObject() // no onClick key
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
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
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onAction()
        verify(exactly = 0) {
            mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `onCloseButtonClick raises a close clicked event with the shared close descriptors`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onCloseButtonClick()
        verify(exactly = 1) {
            mockListener.inAppNotificationActionTriggered(
                notification,
                match { it.type == InAppActionType.CLOSE },
                Constants.INAPP_CTA_DISMISS_BUTTON,
                match { it.getString(Constants.KEY_WZRK_ELEMENT_ID) == Constants.INAPP_ELEMENT_ID_CLOSE },
                null
            )
        }
    }

    @Test
    fun `onCloseButtonClick does not report a dismiss (that is onClose's job)`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onCloseButtonClick()
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
    }

    @Test
    fun `onShowFailed forwards the notification and media type to the failure handler`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onShowFailed(PIPMediaType.VIDEO)
        verify(exactly = 1) { mockShowFailureHandler.onPIPShowFailed(notification, PIPMediaType.VIDEO) }
    }

    @Test
    fun `onShowFailed does not report a show or a dismiss (PIP was never visible)`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onShowFailed(PIPMediaType.IMAGE)
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }

    // ─── Callbacks that only log (do not forward to InAppListener) ────────────────

    @Test
    fun `onExpand does not call listener`() {
        val notification = mockk<CTInAppNotification> {
            every { campaignId } returns "test_campaign_123"
        }
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
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
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
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
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
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
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
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
        val bridge = PIPInAppCallbacksBridge(notification, mockListener, mockShowFailureHandler, logger)
        bridge.onMediaError("https://example.com/video.m3u8", "Playback error")
        verify(exactly = 0) { mockListener.inAppNotificationDidShow(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationDidDismiss(any(), any()) }
        verify(exactly = 0) { mockListener.inAppNotificationActionTriggered(any(), any(), any(), any(), any()) }
    }
}
