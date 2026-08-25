package com.clevertap.android.pushtemplates

import android.os.Bundle
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The keys the pt_custom_rating payload contract marks as required, checked the way the renderer
 * checks them before it builds anything.
 *
 * Per-position artwork is deliberately not validated here: a single failed asset is substituted at
 * render time (R-21), and whether enough positions survive is the renderer's call (R-22).
 */
@RunWith(RobolectricTestRunner::class)
class CustomRatingTemplateValidatorTest {

    private fun validate(vararg overrides: Pair<String, String?>): Boolean {
        val extras = Bundle().apply {
            putString(PTConstants.PT_ID, "pt_custom_rating")
            putString(PTConstants.PT_TITLE, "How did we do?")
            putString(PTConstants.PT_MSG, "Tell us")
            putString(PTConstants.PT_DEFAULT_DL, "ctdemo://home")
            putString(PTConstants.PT_RATING_STYLE, "icon")
            putString(PTConstants.PT_RATING_COUNT, "3")
            putString(PTConstants.PT_RATING_CTA_LABEL, "Submit")
            putString(PTConstants.PT_RATING_CTA_DL, "ctdemo://feedback")
            for (position in 1..3) {
                putString("${PTConstants.PT_RATING_ICON_PREFIX}$position", "https://cdn.example.com/i$position.png")
            }
            overrides.forEach { (key, value) -> if (value == null) remove(key) else putString(key, value) }
        }
        val data = TemplateDataFactory.createTemplateData(
            TemplateType.CUSTOM_RATING, extras, false, "alt"
        ) { arrayListOf() }!!
        return ValidatorFactory.getValidator(data)!!.validate()
    }

    @Test
    fun `a complete payload validates`() {
        assertTrue(validate())
    }

    @Test
    fun `the rating style is required`() {
        assertFalse(validate(PTConstants.PT_RATING_STYLE to null))
    }

    @Test
    fun `an unrecognised rating style is rejected rather than guessed at`() {
        assertFalse(validate(PTConstants.PT_RATING_STYLE to "emoji"))
    }

    @Test
    fun `the rating count is required and must reach the minimum scale`() {
        assertFalse(validate(PTConstants.PT_RATING_COUNT to null))
        assertFalse(validate(PTConstants.PT_RATING_COUNT to "1"))
        assertTrue(validate(PTConstants.PT_RATING_COUNT to "2"))
    }

    @Test
    fun `the submit button needs both a label and a destination`() {
        assertFalse(validate(PTConstants.PT_RATING_CTA_LABEL to null))
        assertFalse(validate(PTConstants.PT_RATING_CTA_DL to null))
        assertFalse(validate(PTConstants.PT_RATING_CTA_LABEL to " "))
    }

    @Test
    fun `the body-tap destination is required`() {
        assertFalse(validate(PTConstants.PT_DEFAULT_DL to null))
    }

    @Test
    fun `title and message are required, as on every other template`() {
        assertFalse(validate(PTConstants.PT_TITLE to null))
        assertFalse(validate(PTConstants.PT_MSG to null))
    }

    @Test
    fun `the text style validates on the same required keys`() {
        assertTrue(
            validate(
                PTConstants.PT_RATING_STYLE to "text",
                "${PTConstants.PT_RATING_LABEL_PREFIX}1" to "No",
                "${PTConstants.PT_RATING_LABEL_PREFIX}2" to "Maybe",
                "${PTConstants.PT_RATING_LABEL_PREFIX}3" to "Yes"
            )
        )
    }
}
