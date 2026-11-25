package com.clevertap.android.sdk.inapp.fragment

import android.content.Context
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTLocalInApp
import com.clevertap.android.sdk.inapp.InAppController
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests how the half-interstitial and the half-interstitial image fragments size their card.
 *
 * The screens use the xhdpi qualifier, so a dp value and its pixel value differ and a missing
 * dp to px conversion cannot go unnoticed.
 */
@RunWith(RobolectricTestRunner::class)
class CTInAppHalfInterstitialLayoutTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var config: CleverTapInstanceConfig

    @Before
    fun setUp() {
        config = CleverTapInstanceConfig.createInstance(context, "TEST", "TEST")!!
        mockkObject(InAppController.Companion)
    }

    @After
    fun tearDown() = unmockkAll()

    // ---------------------------------------------------------------------------------------
    // The functions that size the card
    // ---------------------------------------------------------------------------------------

    @Test
    @Config(qualifiers = PHONE_PORTRAIT)
    fun `landscape phone card width is height x 1_3 and the card is centered horizontally`() {
        val fragment = showText(tablet = false, local = true).fragment
        val params = matchParentParams()

        fragment.redrawLandscapeHalfInterstitialInApp(measuredCard(1000, 600), params, closeButton())

        assertEquals((600 * 1.3f).toInt(), params.width)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.height)
        assertEquals(Gravity.CENTER_HORIZONTAL, params.gravity)
        assertMargins(params, 0, 0, 0, 0)
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT)
    fun `landscape tablet in-app card width is height x 1_3 and the card is centered`() {
        val fragment = showText(tablet = false, local = true).fragment
        val params = matchParentParams()

        fragment.redrawLandscapeHalfInterstitialTabletInApp(measuredCard(1000, 600), params, closeButton())

        assertEquals((600 * 1.3f).toInt(), params.width)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.height)
        assertEquals(Gravity.CENTER, params.gravity)
        assertMargins(params, 0, 0, 0, 0)
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT)
    fun `landscape mobile in-app on a tablet gets margins and a height that leave room around the card`() {
        val fragment = showText(tablet = false, local = true).fragment
        val params = matchParentParams()

        fragment.redrawLandscapeHalfInterstitialMobileInAppOnTablet(
            measuredCard(1000, 600), params, closeButton()
        )

        // The margins and the 130dp are dp values, so they must be converted to pixels.
        val expectedHeight = 600 - px(fragment, 130)
        assertMargins(params, px(fragment, 140), px(fragment, 100), px(fragment, 140), px(fragment, 100))
        assertEquals(expectedHeight, params.height)
        assertEquals((expectedHeight * 1.3f).toInt(), params.width)
        assertEquals(Gravity.CENTER, params.gravity)
        assertTrue(px(fragment, 130) > 130, "the screen must not be mdpi, or a missing conversion is hidden")
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT)
    fun `portrait mobile in-app on a tablet gets margins, a narrower card and height x 1_3`() {
        val fragment = showText(tablet = false, local = true).fragment
        val params = matchParentParams()

        fragment.redrawHalfInterstitialMobileInAppOnTablet(measuredCard(1000, 1600), params, closeButton())

        val expectedWidth = 1000 - px(fragment, 210)
        assertMargins(params, px(fragment, 140), px(fragment, 140), px(fragment, 140), px(fragment, 140))
        assertEquals(expectedWidth, params.width)
        assertEquals((expectedWidth * 1.3f).toInt(), params.height)
    }

    // ---------------------------------------------------------------------------------------
    // Text half-interstitial fragment, landscape: which branch is chosen
    // ---------------------------------------------------------------------------------------

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun `text landscape on a phone screen uses the phone sizing`() {
        val shown = showText(tablet = false, local = true)

        assertEquals(Gravity.CENTER_HORIZONTAL, shown.params.gravity)
        assertEquals((shown.card.height * 1.3f).toInt(), shown.params.width)
        assertMargins(shown.params, px(shown, 30), px(shown, 30), px(shown, 30), px(shown, 30))
    }

    // A local in-app (the push primer) is not a tablet in-app, so on a tablet screen in landscape it must
    // get the margins of a mobile in-app and not the sizing of a tablet in-app.
    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun `text landscape on a tablet screen treats a local in-app as a mobile in-app`() {
        val shown = showText(tablet = false, local = true)

        assertMobileOnTabletLandscape(shown)
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun `text landscape on a tablet screen treats a phone in-app as a mobile in-app`() {
        val shown = showText(tablet = false, local = false)

        assertMobileOnTabletLandscape(shown)
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun `text landscape on a tablet screen uses the tablet sizing for a tablet in-app`() {
        val shown = showText(tablet = true, local = false)

        assertEquals(Gravity.CENTER, shown.params.gravity)
        assertEquals((shown.card.height * 1.3f).toInt(), shown.params.width)
        // the margins of the tablet layout are kept
        assertMargins(shown.params, px(shown, 35), px(shown, 35), px(shown, 35), px(shown, 35))
    }

    @Test
    @Config(qualifiers = FOLDABLE_LANDSCAPE)
    fun `text landscape on an unfolded foldable uses the mobile sizing for a local in-app`() {
        assertMobileOnTabletLandscape(showText(tablet = false, local = true))
    }

    // ---------------------------------------------------------------------------------------
    // Text half-interstitial fragment, portrait
    // ---------------------------------------------------------------------------------------

    @Test
    @Config(qualifiers = PHONE_PORTRAIT)
    fun `text portrait on a phone screen sizes the card from its width`() {
        val shown = showText(tablet = false, local = true)

        assertEquals((shown.card.width * 1.3f).toInt(), shown.params.height)
        assertMargins(shown.params, px(shown, 30), px(shown, 30), px(shown, 30), px(shown, 30))
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun `text portrait on a tablet screen treats a local in-app as a tablet in-app`() {
        val shown = showText(tablet = false, local = true)

        assertEquals((shown.card.width * 1.3f).toInt(), shown.params.height)
        assertMargins(shown.params, px(shown, 35), px(shown, 35), px(shown, 35), px(shown, 35))
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun `text portrait on a tablet screen treats a phone in-app as a mobile in-app`() {
        val shown = showText(tablet = false, local = false)

        assertMobileOnTabletPortrait(shown)
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun `text portrait on a tablet screen sizes a tablet in-app from its width`() {
        val shown = showText(tablet = true, local = false)

        assertEquals((shown.card.width * 1.3f).toInt(), shown.params.height)
        assertMargins(shown.params, px(shown, 35), px(shown, 35), px(shown, 35), px(shown, 35))
    }

    @Test
    @Config(qualifiers = FOLDABLE_PORTRAIT)
    fun `text portrait on an unfolded foldable fits the card in the screen height`() {
        val shown = showText(tablet = false, local = true)

        // width x 1.3 would be taller than the card area, so the card is reduced and keeps its shape
        val frame = shown.card.parent as View
        assertTrue(shown.params.height <= frame.height, "card is ${shown.params.height}, screen is ${frame.height}")
        assertEquals((shown.params.height / 1.3f).toInt(), shown.params.width)
    }

    // ---------------------------------------------------------------------------------------
    // Half-interstitial image fragment
    // ---------------------------------------------------------------------------------------

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun `image landscape on a phone screen uses the phone sizing`() {
        val shown = showImage(tablet = false)

        assertEquals(Gravity.CENTER_HORIZONTAL, shown.params.gravity)
        assertEquals((shown.card.height * 1.3f).toInt(), shown.params.width)
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun `image landscape on a tablet screen uses the tablet sizing for a tablet in-app`() {
        val shown = showImage(tablet = true)

        assertEquals(Gravity.CENTER, shown.params.gravity)
        assertEquals((shown.card.height * 1.3f).toInt(), shown.params.width)
        assertNotEquals(px(shown, 140), shown.params.leftMargin)
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun `image landscape on a tablet screen treats a phone in-app as a mobile in-app`() {
        assertMobileOnTabletLandscape(showImage(tablet = false))
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT)
    fun `image portrait on a phone screen sizes the card from its width`() {
        val shown = showImage(tablet = false)

        assertEquals((shown.card.width * 1.3f).toInt(), shown.params.height)
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun `image portrait on a tablet screen sizes a tablet in-app from its width`() {
        val shown = showImage(tablet = true)

        assertEquals((shown.card.width * 1.3f).toInt(), shown.params.height)
        assertNotEquals(px(shown, 140), shown.params.leftMargin)
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun `image portrait on a tablet screen treats a phone in-app as a mobile in-app`() {
        assertMobileOnTabletPortrait(showImage(tablet = false))
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private class Shown(
        val fragment: CTInAppBaseFullFragment,
        val card: View,
        val params: FrameLayout.LayoutParams
    )

    private fun assertMobileOnTabletLandscape(shown: Shown) {
        assertMargins(shown.params, px(shown, 140), px(shown, 100), px(shown, 140), px(shown, 100))
        assertEquals(Gravity.CENTER, shown.params.gravity)
        assertEquals((shown.params.height * 1.3f).toInt(), shown.params.width)
    }

    private fun assertMobileOnTabletPortrait(shown: Shown) {
        assertMargins(shown.params, px(shown, 140), px(shown, 140), px(shown, 140), px(shown, 140))
        assertEquals((shown.params.width * 1.3f).toInt(), shown.params.height)
    }

    private fun assertMargins(params: FrameLayout.LayoutParams, left: Int, top: Int, right: Int, bottom: Int) {
        assertEquals(
            listOf(left, top, right, bottom),
            listOf(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin)
        )
    }

    private fun px(fragment: CTInAppBaseFullFragment, dp: Int) =
        (dp * fragment.resources.displayMetrics.density).toInt()

    private fun px(shown: Shown, dp: Int) = px(shown.fragment, dp)

    private fun showText(tablet: Boolean, local: Boolean): Shown {
        val json = CTLocalInApp.builder()
            .setInAppType(CTLocalInApp.InAppType.HALF_INTERSTITIAL)
            .setTitleText("Title")
            .setMessageText("Message")
            .followDeviceOrientation(true)
            .setPositiveBtnText("Allow")
            .setNegativeBtnText("Cancel")
            .build()
            .put("tablet", tablet)
            .put(CTLocalInApp.IS_LOCAL_INAPP, local)
        return show(
            CTInAppNativeHalfInterstitialFragment(), json,
            R.id.half_interstitial_relative_layout
        )
    }

    private fun showImage(tablet: Boolean): Shown {
        val json = JSONObject()
            .put("type", "half-interstitial-image")
            .put("_id", "layout-test")
            .put("ti", 1)
            .put("d", true)
            .put("tablet", tablet)
            .put("media", JSONObject().put("content_type", "image/png").put("url", "https://example.com/a.png"))
            .put(
                "mediaLandscape",
                JSONObject().put("content_type", "image/png").put("url", "https://example.com/b.png")
            )
        return show(
            CTInAppNativeHalfInterstitialImageFragment(), json,
            R.id.half_interstitial_image_relative_layout
        )
    }

    private fun show(fragment: CTInAppBaseFullFragment, json: JSONObject, cardId: Int): Shown {
        val inApp = CTInAppNotification(json, true)
        every { InAppController.getDisplayingInApp(any()) } returns inApp

        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragment.setArguments(inApp, config)
        host.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        shadowOf(Looper.getMainLooper()).idle()

        val card = fragment.view!!.findViewById<View>(cardId)
        return Shown(fragment, card, card.layoutParams as FrameLayout.LayoutParams)
    }

    private fun closeButton() = CloseImageView(context).apply { measure(unspecified(), unspecified()) }

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

    private fun unspecified() = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

    private companion object {
        const val PHONE_PORTRAIT = "w411dp-h891dp-port-xhdpi"
        const val PHONE_LANDSCAPE = "w891dp-h411dp-land-xhdpi"
        const val TABLET_PORTRAIT = "w800dp-h1280dp-port-xhdpi"
        const val TABLET_LANDSCAPE = "w1280dp-h800dp-land-xhdpi"
        const val FOLDABLE_PORTRAIT = "w852dp-h883dp-port-xhdpi"
        const val FOLDABLE_LANDSCAPE = "w883dp-h852dp-land-xhdpi"
    }
}
