package com.clevertap.android.pushtemplates

import android.os.Build
import android.os.Bundle
import com.clevertap.android.pushtemplates.PTConstants.*
import com.clevertap.android.pushtemplates.content.CustomRatingRowRenderer
import com.clevertap.android.sdk.Constants
import io.mockk.*
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers parsing of the pt_custom_rating payload contract: the required style and count keys, the
 * per-position assets and overrides, and the submit button configuration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class CustomRatingTemplateDataTest {

    private lateinit var mockBundle: Bundle
    private val defaultAltText = "Default Alt Text"
    private val notificationIdsProvider = { arrayListOf(1, 2, 3) }

    private companion object {
        const val SAMPLE_TITLE = "Rate us"
        const val SAMPLE_MESSAGE = "How did we do?"
        const val SAMPLE_COLOR = "#FF0000"
        const val DEFAULT_DL = "myapp://home"
        const val CTA_DL = "myapp://feedback"
    }

    @Before
    fun setUp() {
        mockBundle = mockk(relaxed = true)
        mockkStatic(Utils::class)

        every { Utils.fromJson(any()) } returns mockBundle
        every { Utils.createColorMap(any(), any()) } returns mapOf(
            PT_TITLE_COLOR to SAMPLE_COLOR,
            PT_BG to SAMPLE_COLOR
        )
        every { Utils.getActionKeys(any()) } returns JSONArray()
        every { Utils.getImageDataListFromExtras(any(), any()) } returns arrayListOf()
        every { Utils.getDeepLinkListFromExtras(any()) } returns arrayListOf()

        every { mockBundle.getString(PT_JSON) } returns null
        every { mockBundle.getString(PT_TITLE) } returns SAMPLE_TITLE
        every { mockBundle.getString(Constants.NOTIF_TITLE) } returns SAMPLE_TITLE
        every { mockBundle.getString(PT_MSG) } returns SAMPLE_MESSAGE
        every { mockBundle.getString(Constants.NOTIF_MSG) } returns SAMPLE_MESSAGE
        every { mockBundle.getString(PT_DEFAULT_DL) } returns DEFAULT_DL
        every { mockBundle.getString(PT_RATING_CTA_LABEL) } returns "Submit"
        every { mockBundle.getString(PT_RATING_CTA_DL) } returns CTA_DL
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun parse(): CustomRatingTemplateData? = TemplateDataFactory.createTemplateData(
        templateType = TemplateType.CUSTOM_RATING,
        extras = mockBundle,
        isDarkMode = false,
        defaultAltText = defaultAltText,
        notificationIdsProvider = notificationIdsProvider
    ) as? CustomRatingTemplateData

    private fun givenIconStyle(count: Int) {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "icon"
        every { mockBundle.getString(PT_RATING_COUNT) } returns count.toString()
        for (position in 1..count) {
            every { mockBundle.getString("$PT_RATING_ICON_PREFIX$position") } returns
                    "https://cdn.example.com/icon$position.png"
            every {
                mockBundle.getString("$PT_RATING_ICON_PREFIX$position$PT_RATING_ICON_SELECTED_SUFFIX")
            } returns "https://cdn.example.com/icon${position}_sel.png"
        }
    }

    @Test
    fun `parses icon style with three positions`() {
        givenIconStyle(count = 3)

        val data = parse()

        assertNotNull(data)
        assertEquals(TemplateType.CUSTOM_RATING, data!!.templateType)
        assertEquals(RatingStyleType.ICON, data.ratingStyle)
        assertEquals(3, data.ratingCount)
        assertEquals(3, data.positions.size)
        assertEquals("https://cdn.example.com/icon1.png", data.positions[0].iconUrl)
        assertEquals("https://cdn.example.com/icon2_sel.png", data.positions[1].selectedIconUrl)
        assertEquals(DEFAULT_DL, data.defaultDeepLink)
    }

    @Test
    fun `text style is recognised and its labels are parsed`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "text"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "2"
        every { mockBundle.getString("${PT_RATING_LABEL_PREFIX}1") } returns "👍"
        every { mockBundle.getString("${PT_RATING_LABEL_PREFIX}2") } returns "👎"

        val data = parse()

        assertEquals(RatingStyleType.TEXT, data!!.ratingStyle)
        assertEquals("👍", data.positions[0].label)
        assertEquals("👎", data.positions[1].label)
    }

    @Test
    fun `unknown style yields a null style so the renderer can fall back`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "emoji"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "3"

        assertNull(parse()!!.ratingStyle)
    }

    @Test
    fun `count above the supported maximum is clamped`() {
        givenIconStyle(count = 5)
        every { mockBundle.getString(PT_RATING_COUNT) } returns "9"

        assertEquals(PT_RATING_COUNT_MAX, parse()!!.ratingCount)
    }

    @Test
    fun `count below the supported minimum is rejected rather than padded out`() {
        // A single position is not a rating, and inventing a second one renders something the
        // campaign never configured. The template degrades to Basic instead (R-22).
        every { mockBundle.getString(PT_RATING_STYLE) } returns "icon"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "1"

        val data = parse()!!
        assertEquals(0, data.ratingCount)
        assertTrue(data.positions.isEmpty())
    }

    @Test
    fun `missing count leaves zero positions so validation reports the missing key`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "icon"
        every { mockBundle.getString(PT_RATING_COUNT) } returns null

        val data = parse()!!
        assertEquals(0, data.ratingCount)
        assertTrue(data.positions.isEmpty())
    }

    @Test
    fun `non numeric count is treated as missing rather than defaulting to five`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "icon"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "three"

        assertEquals(0, parse()!!.ratingCount)
    }

    @Test
    fun `per position deep links map onto positions in order`() {
        givenIconStyle(count = 3)
        every { Utils.getDeepLinkListFromExtras(any()) } returns
                arrayListOf("myapp://one", "myapp://two", "myapp://three")

        val positions = parse()!!.positions
        assertEquals("myapp://one", positions[0].deepLink)
        assertEquals("myapp://two", positions[1].deepLink)
        assertEquals("myapp://three", positions[2].deepLink)
    }

    @Test
    fun `positions without an override fall through to the submit destination`() {
        givenIconStyle(count = 3)
        every { Utils.getDeepLinkListFromExtras(any()) } returns arrayListOf("myapp://one")

        val positions = parse()!!.positions
        assertEquals("myapp://one", positions[0].deepLink)
        assertNull(positions[1].deepLink)
        assertNull(positions[2].deepLink)
    }

    @Test
    fun `cta radius defaults when the key is absent`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString(PT_RATING_CTA_RADIUS) } returns null

        assertEquals(PT_RATING_CTA_RADIUS_DEFAULT, parse()!!.ctaData.cornerRadiusDp)
    }

    @Test
    fun `cta radius is clamped to the supported range`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString(PT_RATING_CTA_RADIUS) } returns "99"

        assertEquals(PT_RATING_RADIUS_MAX, parse()!!.ctaData.cornerRadiusDp)
    }

    @Test
    fun `negative cta radius is clamped to zero`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString(PT_RATING_CTA_RADIUS) } returns "-4"

        assertEquals(0, parse()!!.ctaData.cornerRadiusDp)
    }

    @Test
    fun `blank cta label and destination are treated as absent`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString(PT_RATING_CTA_LABEL) } returns "   "
        every { mockBundle.getString(PT_RATING_CTA_DL) } returns ""

        val cta = parse()!!.ctaData
        assertNull(cta.label)
        assertNull(cta.deepLink)
    }

    @Test
    fun `confirmation message is parsed when present`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString(PT_RATING_CONFIRM_MSG) } returns "Thanks for rating us!"

        assertEquals("Thanks for rating us!", parse()!!.confirmationMessage)
    }

    @Test
    fun `renderablePositionCount counts only positions with a usable icon`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString("${PT_RATING_ICON_PREFIX}3") } returns null

        assertEquals(2, parse()!!.renderablePositionCount)
    }

    @Test
    fun `renderablePositionCount is zero when the style is unknown`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString(PT_RATING_STYLE) } returns null

        assertEquals(0, parse()!!.renderablePositionCount)
    }

    @Test
    fun `text style positions are not renderable without labels`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "text"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "3"

        assertEquals(0, parse()!!.renderablePositionCount)
    }

    @Test
    fun `classic rating parsing is unaffected by the new keys`() {
        // The Classic template must ignore every pt_custom_rating key (PRD non-goal: pt_rating frozen).
        givenIconStyle(count = 5)

        val classic = TemplateDataFactory.createTemplateData(
            templateType = TemplateType.RATING,
            extras = mockBundle,
            isDarkMode = false,
            defaultAltText = defaultAltText,
            notificationIdsProvider = notificationIdsProvider
        )

        assertTrue(classic is RatingTemplateData)
        assertEquals(TemplateType.RATING, (classic as RatingTemplateData).templateType)
        assertEquals(DEFAULT_DL, classic.defaultDeepLink)
    }

    @Test
    fun `label colours are parsed from the colour map`() {
        every { Utils.createColorMap(any(), any()) } returns mapOf(
            PT_RATING_LABEL_CLR to "#111111",
            PT_RATING_LABEL_SEL_CLR to "#222222",
            PT_RATING_ICON_CLR to "#333333",
            PT_RATING_ICON_SEL_CLR to "#444444"
        )
        givenIconStyle(count = 3)

        val data = parse()!!

        assertEquals("#111111", data.labelColor)
        assertEquals("#222222", data.selectedLabelColor)
        assertEquals("#333333", data.iconColor)
        assertEquals("#444444", data.selectedIconColor)
    }

    @Test
    fun `a text row missing one label is reported as incomplete so it can fall back to stars`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "text"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "3"
        every { mockBundle.getString("${PT_RATING_LABEL_PREFIX}1") } returns "Bad"
        every { mockBundle.getString("${PT_RATING_LABEL_PREFIX}2") } returns "Okay"

        val data = parse()!!

        assertEquals(2, data.renderablePositionCount)
        assertTrue(data.hasIncompleteTextRow)
    }

    @Test
    fun `a complete text row is not flagged as incomplete`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "text"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "2"
        every { mockBundle.getString("${PT_RATING_LABEL_PREFIX}1") } returns "No"
        every { mockBundle.getString("${PT_RATING_LABEL_PREFIX}2") } returns "Yes"

        assertFalse(parse()!!.hasIncompleteTextRow)
    }

    @Test
    fun `an icon row with a missing asset is not treated as an incomplete text row`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString("${PT_RATING_ICON_PREFIX}2") } returns null

        assertFalse(parse()!!.hasIncompleteTextRow)
    }

    @Test
    fun `a fully configured text row is renderable rather than degraded to the basic template`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "text"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "3"
        for (position in 1..3) {
            every { mockBundle.getString("$PT_RATING_LABEL_PREFIX$position") } returns "L$position"
        }

        assertTrue(CustomRatingRowRenderer.isRenderable(parse()!!))
    }

    @Test
    fun `a text row with fewer than two labels degrades to the basic template`() {
        every { mockBundle.getString(PT_RATING_STYLE) } returns "text"
        every { mockBundle.getString(PT_RATING_COUNT) } returns "3"
        every { mockBundle.getString("${PT_RATING_LABEL_PREFIX}1") } returns "Only one"

        assertFalse(CustomRatingRowRenderer.isRenderable(parse()!!))
    }

    @Test
    fun `a rating without a submit destination degrades to the basic template`() {
        givenIconStyle(count = 3)
        every { mockBundle.getString(PT_RATING_CTA_DL) } returns null

        assertFalse(CustomRatingRowRenderer.isRenderable(parse()!!))
    }

    @Test
    fun `template id maps to the custom rating type`() {
        assertEquals(TemplateType.CUSTOM_RATING, TemplateType.fromString("pt_custom_rating"))
        assertEquals(TemplateType.RATING, TemplateType.fromString("pt_rating"))
        assertEquals("pt_custom_rating", TemplateType.CUSTOM_RATING.toString())
    }
}
