package com.clevertap.android.pushtemplates

import android.os.Bundle
import com.clevertap.android.pushtemplates.validators.RatingTemplateValidator
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Locks the Classic pt_rating template against the pt_custom_rating work (FR-AND-01).
 *
 * The PRD's first non-goal is "any change to the Classic rendering, keys, or events", and its
 * acceptance criterion is that Classic golden payloads keep parsing and validating unchanged. These
 * are those payloads: shaped like real campaigns rather than like minimal fixtures, and asserted on
 * whole objects so a field quietly appearing or moving fails here rather than in a customer's tray.
 *
 * Nothing is mocked on purpose. A test that stubs [Utils] would still pass if the parsing underneath
 * it changed, which is exactly the regression this file exists to catch.
 */
@RunWith(RobolectricTestRunner::class)
class ClassicRatingRegressionTest {

    private val defaultAltText = "Image"

    /** A five-star Classic campaign with every optional key a real one tends to carry. */
    private fun classicPayload() = Bundle().apply {
        putString(PTConstants.PT_ID, "pt_rating")
        putString(PTConstants.PT_TITLE, "How was your delivery?")
        putString(PTConstants.PT_MSG, "Tap a star to let us know")
        putString(PTConstants.PT_MSG_SUMMARY, "We read every rating")
        putString(PTConstants.PT_SUBTITLE, "Order #4417")
        putString(PTConstants.PT_DEFAULT_DL, "ctdemo://orders/4417")
        putString(PTConstants.PT_BIG_IMG, "https://cdn.example.com/delivery.jpg")
        putString(PTConstants.PT_SCALE_TYPE, "fit_center")
        putString(PTConstants.PT_NOTIF_ICON, "https://cdn.example.com/ico.png")
        putString(PTConstants.PT_BG, "#FFFFFF")
        for (star in 1..5) {
            putString("pt_dl$star", "ctdemo://rating/$star")
        }
    }

    /**
     * Every key the new template introduced, dropped into a Classic payload. A campaign would not
     * normally carry both, but a shared composer or a duplicated template can produce exactly this,
     * and Classic has to ignore all of it.
     */
    private fun Bundle.withCustomRatingKeys() = apply {
        putString(PTConstants.PT_RATING_STYLE, "text")
        putString(PTConstants.PT_RATING_COUNT, "3")
        putString(PTConstants.PT_RATING_CTA_LABEL, "Submit")
        putString(PTConstants.PT_RATING_CTA_DL, "ctdemo://feedback")
        putString(PTConstants.PT_RATING_CTA_BG_CLR, "#0055FF")
        putString(PTConstants.PT_RATING_CTA_BORDER_CLR, "#003399")
        putString(PTConstants.PT_RATING_CTA_TXT_CLR, "#FFFFFF")
        putString(PTConstants.PT_RATING_CTA_RADIUS, "16")
        putString(PTConstants.PT_RATING_CONFIRM_MSG, "Thanks for rating us!")
        putString(PTConstants.PT_RATING_ICON_CLR, "#CCCCCC")
        putString(PTConstants.PT_RATING_ICON_SEL_CLR, "#FFB300")
        putString(PTConstants.PT_RATING_LABEL_CLR, "#333333")
        putString(PTConstants.PT_RATING_LABEL_SEL_CLR, "#000000")
        for (position in 1..3) {
            putString("${PTConstants.PT_RATING_LABEL_PREFIX}$position", "Label $position")
            putString("${PTConstants.PT_RATING_ICON_PREFIX}$position", "https://cdn.example.com/i$position.png")
            putString(
                "${PTConstants.PT_RATING_ICON_PREFIX}$position${PTConstants.PT_RATING_ICON_SELECTED_SUFFIX}",
                "https://cdn.example.com/i${position}_sel.png"
            )
        }
    }

    private fun parseClassic(extras: Bundle): RatingTemplateData =
        TemplateDataFactory.createTemplateData(
            TemplateType.RATING, extras, false, defaultAltText
        ) { arrayListOf() } as RatingTemplateData

    @Test
    fun `a classic payload still parses into the classic template data`() {
        val data = parseClassic(classicPayload())

        assertEquals(TemplateType.RATING, data.templateType)
        assertEquals("How was your delivery?", data.baseContent.textData.title)
        assertEquals("Tap a star to let us know", data.baseContent.textData.message)
        assertEquals("We read every rating", data.baseContent.textData.messageSummary)
        assertEquals("Order #4417", data.baseContent.textData.subtitle)
        assertEquals("ctdemo://orders/4417", data.defaultDeepLink)
        assertEquals("https://cdn.example.com/delivery.jpg", data.mediaData.bigImage.url)
        assertEquals(PTScaleType.FIT_CENTER, data.mediaData.scaleType)
        assertEquals("https://cdn.example.com/ico.png", data.baseContent.iconData.largeIcon)
    }

    /**
     * The whole point of the isolation requirement: the new keys must be inert on Classic. Compared
     * as whole objects, so this catches a new field being populated as readily as an existing one
     * being overwritten.
     */
    @Test
    fun `custom rating keys do not change how a classic payload parses`() {
        val clean = parseClassic(classicPayload())
        val polluted = parseClassic(classicPayload().withCustomRatingKeys())

        assertEquals(clean, polluted)
    }

    /**
     * The media border keys shipped alongside the new template and apply to every template, so
     * Classic picking them up is correct — but only those keys, and only into the border block.
     */
    @Test
    fun `classic still honours the shared media border keys without disturbing anything else`() {
        val clean = parseClassic(classicPayload())
        val bordered = parseClassic(classicPayload().apply {
            putString(PTConstants.PT_MEDIA_RADIUS, "12")
            putString(PTConstants.PT_MEDIA_BORDER_CLR, "#FF0000")
            putString(PTConstants.PT_MEDIA_BORDER_WIDTH, "2")
        })

        assertEquals(12, bordered.mediaData.imageBorderData.cornerRadiusDp)
        assertEquals(2, bordered.mediaData.imageBorderData.borderWidthDp)
        assertTrue(bordered.mediaData.imageBorderData.isActive)
        assertFalse(clean.mediaData.imageBorderData.isActive)
        // Everything outside the border block is untouched.
        assertEquals(
            clean.copy(mediaData = bordered.mediaData),
            bordered
        )
    }

    @Test
    fun `the two rating templates resolve to distinct types`() {
        assertEquals(TemplateType.RATING, TemplateType.fromString("pt_rating"))
        assertEquals(TemplateType.CUSTOM_RATING, TemplateType.fromString("pt_custom_rating"))
        assertNotEquals(TemplateType.RATING, TemplateType.fromString("pt_custom_rating"))
        assertEquals("pt_rating", TemplateType.RATING.toString())
        assertEquals("pt_custom_rating", TemplateType.CUSTOM_RATING.toString())
    }

    /**
     * Classic's required-key set is pt_dl{n} plus pt_default_dl on top of the content keys, and the
     * new template must not have widened it — a Classic campaign that validated before this branch
     * has to keep validating.
     */
    @Test
    fun `classic keeps its own validator and its own required keys`() {
        val validator = ValidatorFactory.getValidator(parseClassic(classicPayload()))

        assertTrue(validator is RatingTemplateValidator)
        assertTrue(validator!!.validate())
        assertEquals(2, validator.loadKeys().size)
    }

    @Test
    fun `classic still fails validation when its own required keys are missing`() {
        val noDefaultDeepLink = classicPayload().apply { remove(PTConstants.PT_DEFAULT_DL) }
        assertFalse(ValidatorFactory.getValidator(parseClassic(noDefaultDeepLink))!!.validate())

        val noStarDeepLinks = classicPayload().apply {
            for (star in 1..5) remove("pt_dl$star")
        }
        assertFalse(ValidatorFactory.getValidator(parseClassic(noStarDeepLinks))!!.validate())
    }

    /**
     * Classic validates on its own required keys only. Handing it a payload that also carries the
     * new template's keys must not make it stricter — nor pass a Classic payload that should fail.
     */
    @Test
    fun `custom rating keys do not change classic validation either`() {
        val polluted = classicPayload().withCustomRatingKeys()
        assertTrue(ValidatorFactory.getValidator(parseClassic(polluted))!!.validate())

        val pollutedButBroken = classicPayload().withCustomRatingKeys()
            .apply { remove(PTConstants.PT_DEFAULT_DL) }
        assertFalse(ValidatorFactory.getValidator(parseClassic(pollutedButBroken))!!.validate())
    }
}
