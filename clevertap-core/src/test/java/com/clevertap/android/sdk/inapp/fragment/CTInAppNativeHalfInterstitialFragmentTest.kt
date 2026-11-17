package com.clevertap.android.sdk.inapp.fragment

import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTLocalInApp
import com.clevertap.android.sdk.inapp.InAppController
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CTInAppNativeHalfInterstitialFragmentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var config: CleverTapInstanceConfig
    private var savedDeviceType = 0

    // DeviceInfo keeps the device type in a static field for the whole app run.
    private val deviceTypeField = DeviceInfo::class.java.getDeclaredField("sDeviceType")
        .apply { isAccessible = true }

    @Before
    fun setUp() {
        config = CleverTapInstanceConfig.createInstance(context, "TEST", "TEST")
        savedDeviceType = deviceTypeField.getInt(null)
        mockkObject(InAppController.Companion)
    }

    @After
    fun tearDown() {
        deviceTypeField.setInt(null, savedDeviceType)
        unmockkAll()
    }

    // SDK-6328: the app started on a large screen, so the saved device type says "tablet",
    // but the screen is small now (e.g. a foldable was folded).
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `push primer uses the phone layout on a small screen even when the saved device type is tablet`() {
        deviceTypeField.setInt(null, DeviceInfo.TABLET)

        val view = showPushPrimer()

        assertEquals(PHONE_LAYOUT_MARGIN_DP, cardMarginDp(view))
    }

    @Test
    @Config(qualifiers = "w600dp-h960dp")
    fun `push primer uses the tablet layout on a tablet screen`() {
        val view = showPushPrimer()

        assertEquals(TABLET_LAYOUT_MARGIN_DP, cardMarginDp(view))
    }

    // SDK-6342: on a tall screen (phone) the card keeps its usual size, height = width x 1.3.
    @Test
    fun `portrait card height is width x 1_3 when it fits on the screen`() {
        val card = measuredCard(width = 350, height = 800)
        val params = matchParentParams()

        fragment.redrawHalfInterstitialInApp(card, params, closeButton)

        assertEquals((350 * CARD_RATIO).toInt(), params.height)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.width)
    }

    // SDK-6342: on an almost square screen (unfolded foldable) width x 1.3 is taller than the
    // screen, so the card must fit the height and become narrower.
    @Test
    fun `portrait card fits the screen and becomes narrower when width x 1_3 is too tall`() {
        val card = measuredCard(width = 780, height = 800)
        val params = matchParentParams()

        fragment.redrawHalfInterstitialInApp(card, params, closeButton)

        assertEquals(800, params.height)
        // The card keeps the same shape.
        assertEquals((800 / CARD_RATIO).toInt(), params.width)
        assertTrue(params.width < 780)
    }

    // SDK-6342: the card size must not depend on whether the close button is shown.
    @Test
    fun `portrait card size is the same when the close button is hidden`() {
        val hiddenCloseButton = CloseImageView(context).apply { visibility = View.GONE }
        val params = matchParentParams()

        fragment.redrawHalfInterstitialInApp(
            measuredCard(width = 780, height = 800), params, hiddenCloseButton
        )

        assertEquals(800, params.height)
        assertEquals((800 / CARD_RATIO).toInt(), params.width)
    }

    // SDK-6342: with no height to fit into, the card keeps its usual size instead of getting a
    // zero or negative size.
    @Test
    fun `portrait card keeps width x 1_3 when there is no available height`() {
        val params = matchParentParams()

        fragment.redrawHalfInterstitialInApp(measuredCard(width = 350, height = 0), params, closeButton)

        assertEquals((350 * CARD_RATIO).toInt(), params.height)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.width)
    }

    private val fragment = CTInAppNativeHalfInterstitialFragment()

    private val closeButton by lazy {
        CloseImageView(context).apply { measure(unspecified(), unspecified()) }
    }

    // A card measured at the given size, the way it is before the fragment resizes it.
    private fun measuredCard(width: Int, height: Int) = RelativeLayout(context).apply {
        measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
    }

    private fun matchParentParams() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun unspecified() =
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

    private fun showPushPrimer(): View {
        val json = CTLocalInApp.builder()
            .setInAppType(CTLocalInApp.InAppType.HALF_INTERSTITIAL)
            .setTitleText("Title")
            .setMessageText("Message")
            .followDeviceOrientation(true)
            .setPositiveBtnText("Allow")
            .setNegativeBtnText("Cancel")
            .build()
        val inApp = CTInAppNotification(json, true)
        every { InAppController.getDisplayingInApp(any()) } returns inApp

        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = CTInAppNativeHalfInterstitialFragment().apply { setArguments(inApp, config) }
        host.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        shadowOf(Looper.getMainLooper()).idle()

        return assertNotNull(fragment.view)
    }

    // The phone and tablet layouts use the same view ids; the card margin tells them apart.
    private fun cardMarginDp(view: View): Int {
        val card = view.findViewById<View>(R.id.half_interstitial_relative_layout)
        val params = card.layoutParams as ViewGroup.MarginLayoutParams
        return Math.round(params.leftMargin / view.resources.displayMetrics.density)
    }

    private companion object {
        const val PHONE_LAYOUT_MARGIN_DP = 30
        const val TABLET_LAYOUT_MARGIN_DP = 35
        const val CARD_RATIO = 1.3f
    }
}
