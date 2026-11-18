package com.clevertap.android.sdk.inapp.fragment

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTLocalInApp
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.InAppListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the close button of the half-interstitial, half-interstitial image, interstitial image, cover and
 * cover image fragments. The "close" key of the in-app says whether the close button is shown, even though
 * it is read into [CTInAppNotification.isHideCloseButton].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-port")
class CTInAppCloseButtonTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var config: CleverTapInstanceConfig

    @Before
    fun setUp() {
        config = CleverTapInstanceConfig.createInstance(context, "TEST", "TEST")
        mockkObject(InAppController.Companion)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `the close button is shown when the in-app has close set to true`() {
        for (kind in Kind.values()) {
            assertEquals(View.VISIBLE, show(kind, close = true).closeButton.visibility, kind.name)
        }
    }

    @Test
    fun `the close button is hidden when the in-app has close set to false`() {
        for (kind in Kind.values()) {
            assertEquals(View.GONE, show(kind, close = false).closeButton.visibility, kind.name)
        }
    }

    @Test
    fun `the close button is hidden when the in-app has no close key`() {
        for (kind in Kind.values()) {
            assertEquals(View.GONE, show(kind, close = null).closeButton.visibility, kind.name)
        }
    }

    @Test
    fun `clicking the close button dismisses the in-app and closes the activity`() {
        for (kind in Kind.values()) {
            val shown = show(kind, close = true)
            val listener = mockk<InAppListener>(relaxed = true)
            every {
                listener.inAppNotificationActionTriggered(any(), any(), any(), any(), any())
            } returns Bundle()
            shown.fragment.setListener(listener)

            shown.closeButton.performClick()
            shadowOf(Looper.getMainLooper()).idle()

            verify(exactly = 1) {
                listener.inAppNotificationActionTriggered(
                    any(), any(), eq(Constants.INAPP_CTA_DISMISS_BUTTON), any(), any()
                )
            }
            verify(exactly = 1) { listener.inAppNotificationDidDismiss(any(), any()) }
            assertTrue(shown.host.isFinishing, "${kind.name} did not close the activity")
        }
    }

    @Test
    fun `a hidden close button still closes the in-app when it is clicked`() {
        // The click listener is set whether the button is shown or not, as before.
        for (kind in Kind.values()) {
            val shown = show(kind, close = false)

            assertTrue(shown.closeButton.hasOnClickListeners(), kind.name)
        }
    }

    // ---------------------------------------------------------------------------------------

    private enum class Kind { HALF_INTERSTITIAL, HALF_INTERSTITIAL_IMAGE, INTERSTITIAL_IMAGE, COVER, COVER_IMAGE }

    private class Shown(
        val host: FragmentActivity,
        val fragment: CTInAppBaseFullFragment,
        val closeButton: CloseImageView
    )

    private fun show(kind: Kind, close: Boolean?): Shown {
        val json = when (kind) {
            Kind.HALF_INTERSTITIAL -> CTLocalInApp.builder()
                .setInAppType(CTLocalInApp.InAppType.HALF_INTERSTITIAL)
                .setTitleText("Title")
                .setMessageText("Message")
                .followDeviceOrientation(true)
                .setPositiveBtnText("Allow")
                .setNegativeBtnText("Cancel")
                .build()

            else -> JSONObject()
                .put(
                    "type",
                    when (kind) {
                        Kind.HALF_INTERSTITIAL_IMAGE -> "half-interstitial-image"
                        Kind.INTERSTITIAL_IMAGE -> "interstitial-image"
                        Kind.COVER -> "cover"
                        else -> "cover-image"
                    }
                )
                .put("_id", "close-button-test")
                .put("ti", 1)
                .put("d", true)
                .put("media", JSONObject().put("content_type", "image/png").put("url", "https://example.com/a.png"))
                .put(
                    "mediaLandscape",
                    JSONObject().put("content_type", "image/png").put("url", "https://example.com/b.png")
                )
                .also {
                    if (kind == Kind.COVER) {
                        it.put("title", JSONObject().put("text", "Title").put("color", "#000000"))
                        it.put("message", JSONObject().put("text", "Message").put("color", "#000000"))
                        it.put("buttons", JSONArray())
                    }
                }
        }
        // The local in-app builder always asks for the close button, so remove it before the test sets it.
        json.remove(Constants.KEY_HIDE_CLOSE)
        if (close != null) {
            json.put(Constants.KEY_HIDE_CLOSE, close)
        }

        val fragment: CTInAppBaseFullFragment = when (kind) {
            Kind.HALF_INTERSTITIAL -> CTInAppNativeHalfInterstitialFragment()
            Kind.HALF_INTERSTITIAL_IMAGE -> CTInAppNativeHalfInterstitialImageFragment()
            Kind.INTERSTITIAL_IMAGE -> CTInAppNativeInterstitialImageFragment()
            Kind.COVER -> CTInAppNativeCoverFragment()
            Kind.COVER_IMAGE -> CTInAppNativeCoverImageFragment()
        }
        val frameId = when (kind) {
            Kind.HALF_INTERSTITIAL -> R.id.inapp_half_interstitial_frame_layout
            Kind.HALF_INTERSTITIAL_IMAGE -> R.id.inapp_half_interstitial_image_frame_layout
            Kind.INTERSTITIAL_IMAGE -> R.id.inapp_interstitial_image_frame_layout
            Kind.COVER -> R.id.inapp_cover_frame_layout
            Kind.COVER_IMAGE -> R.id.inapp_cover_image_frame_layout
        }

        val inApp = CTInAppNotification(json, true)
        every { InAppController.getDisplayingInApp(any()) } returns inApp
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragment.setArguments(inApp, config)
        host.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        shadowOf(Looper.getMainLooper()).idle()

        val frame = fragment.view!!.findViewById<FrameLayout>(frameId)
        return Shown(host, fragment, frame.findViewById(CloseImageView.VIEW_ID))
    }
}
