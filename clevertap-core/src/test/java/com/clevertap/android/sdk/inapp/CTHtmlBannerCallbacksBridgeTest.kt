package com.clevertap.android.sdk.inapp

import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.utils.configMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTHtmlBannerCallbacksBridgeTest {

    private val listener = mockk<InAppListener>(relaxed = true)
    private val overlay = mockk<CTInAppHtmlBannerOverlay>(relaxed = true)
    private val notification = mockk<CTInAppNotification>(relaxed = true)

    private lateinit var bridge: CTHtmlBannerCallbacksBridge

    @Before
    fun setUp() {
        bridge = CTHtmlBannerCallbacksBridge(notification, configMock(), listener)
        bridge.overlay = overlay
    }

    @Test
    fun `onBannerShown reports did-show`() {
        bridge.onBannerShown()
        verify(exactly = 1) { listener.inAppNotificationDidShow(notification, null) }
    }

    @Test
    fun `openActionUrl triggers open-url action then dismisses the overlay`() {
        val url = Uri.parse("https://clevertap.com").buildUpon()
            .appendQueryParameter("param1", "value")
            .build().toString()

        bridge.openActionUrl(url)

        verifyOrder {
            listener.inAppNotificationActionTriggered(
                notification,
                match { it.type == InAppActionType.OPEN_URL && it.actionUrl == url },
                any(),
                match { it.getString("param1") == "value" },
                any()
            )
            overlay.dismiss()
        }
    }

    @Test
    fun `onBannerSwipeDismissed triggers a close action with swipe c2a then dismisses`() {
        bridge.onBannerSwipeDismissed()

        verifyOrder {
            listener.inAppNotificationActionTriggered(
                notification,
                match { it.type == InAppActionType.CLOSE },
                Constants.INAPP_CTA_SWIPE_DISMISS,
                any(),
                any()
            )
            overlay.dismiss()
        }
    }

    @Test
    fun `didDismiss dismisses the overlay`() {
        bridge.didDismiss(null)
        verify(exactly = 1) { overlay.dismiss() }
    }

    @Test
    fun `dismiss data resolved by the action is reported only once the overlay is removed`() {
        val resolved = Bundle().apply { putString("resolved", "yes") }
        every {
            listener.inAppNotificationActionTriggered(any(), any(), any(), any(), any())
        } returns resolved

        bridge.openActionUrl("https://clevertap.com")
        // No dismiss reported yet — only the action has fired.
        verify(exactly = 0) { listener.inAppNotificationDidDismiss(any(), any()) }

        // The overlay signals it has been removed from the window.
        bridge.onBannerRemoved()
        verify(exactly = 1) { listener.inAppNotificationDidDismiss(notification, resolved) }
    }
}
