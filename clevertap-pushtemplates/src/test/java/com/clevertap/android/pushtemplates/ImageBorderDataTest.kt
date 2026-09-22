package com.clevertap.android.pushtemplates

import android.graphics.Color
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the decisions that route image styling: whether there is anything to draw, whether the
 * views or the bitmap draw it, and what Zero Bezel adds on top.
 *
 * These are the rules a payload passes through before a single pixel is touched, so they are
 * checked exhaustively rather than by example - each matrix below walks every combination its
 * inputs allow.
 */
@RunWith(RobolectricTestRunner::class)
class ImageBorderDataTest {

    private val radiusOnly = ImageBorderData(cornerRadiusPercent = 10f)
    private val borderOnly = ImageBorderData(borderWidthValue = 5f, borderColor = Color.RED)
    private val both = ImageBorderData(
        cornerRadiusPercent = 10f, borderWidthValue = 5f, borderColor = Color.RED
    )
    private val inactive = ImageBorderData()

    // ---------------------------------------------------------------- hasBorder

    @Test
    fun `a border is drawn only when it has both a width and a colour`() {
        // Given - every combination of the two keys that switch a border on
        val widths = listOf(0f to "no width", 5f to "a width")
        val colours = listOf(null to "no colour", Color.RED to "a colour")

        widths.forEach { (width, widthLabel) ->
            colours.forEach { (colour, colourLabel) ->
                // When
                val data = ImageBorderData(borderWidthValue = width, borderColor = colour)

                // Then - only the pair draws; either key alone is inert
                val expected = width > 0f && colour != null
                assertEquals("$widthLabel + $colourLabel", expected, data.hasBorder)
            }
        }
    }

    @Test
    fun `a negative border width does not count as a border`() {
        // Given - a value send-time validation should have rejected, arriving anyway
        val data = ImageBorderData(borderWidthValue = -5f, borderColor = Color.RED)

        // Then
        assertFalse(data.hasBorder)
        assertFalse(data.isActive)
    }

    // ---------------------------------------------------------------- isActive

    @Test
    fun `styling is active when there is either a radius or a full border`() {
        // Given / Then - the radius and the border switch styling on independently
        assertTrue("a radius alone", radiusOnly.isActive)
        assertTrue("a border alone", borderOnly.isActive)
        assertTrue("both together", both.isActive)
        assertFalse("neither", inactive.isActive)
    }

    @Test
    fun `a scrim on its own does not make styling active`() {
        // Given - withScrim describes something extra to paint, not a reason to start painting.
        // Zero Bezel adds it only to a payload that is already styled; on its own there is no
        // radius to round the scrim to, so the scrim view is left to do its normal job.
        val scrimOnly = ImageBorderData(withScrim = true)

        // Then
        assertFalse(scrimOnly.isActive)
        assertFalse(scrimOnly.hasBorder)
    }

    // ---------------------------------------------------------------- usesNativeImageStyling

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `on API 31 only CENTER_CROP is styled by the views`() {
        // Given / Then - CENTER_CROP fills its view, so the view's edge is the picture's edge.
        // FIT_CENTER is scaled to fit and centred, so it is not.
        assertTrue(PTScaleType.CENTER_CROP.usesNativeImageStyling())
        assertFalse(PTScaleType.FIT_CENTER.usesNativeImageStyling())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `below API 31 no scale type is styled by the views`() {
        // Given / Then - the outline and margin setters this relies on arrived in API 31
        assertFalse(useNativeImageStyling)
        PTScaleType.values().forEach {
            assertFalse("$it must not use the view path", it.usesNativeImageStyling())
        }
    }

    // ---------------------------------------------------------------- bakedInto

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `on API 31 only FIT_CENTER bakes its styling into the bitmap`() {
        // Given - every payload shape, against both scale types
        listOf(radiusOnly, borderOnly, both).forEach { data ->
            // When / Then - CENTER_CROP hands the styling to the views, so nothing is baked
            assertNull("$data with CENTER_CROP", data.bakedInto(PTScaleType.CENTER_CROP))
            // and FIT_CENTER bakes it unchanged
            assertSame("$data with FIT_CENTER", data, data.bakedInto(PTScaleType.FIT_CENTER))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `below API 31 nothing reaches the views, so the bitmap carries everything`() {
        // Given / Then - the payload is dropped at parse time on these versions, but the routing
        // still has to send whatever it is given to the bitmap rather than to the views
        listOf(radiusOnly, borderOnly, both).forEach { data ->
            PTScaleType.values().forEach { scaleType ->
                assertSame("$data with $scaleType", data, data.bakedInto(scaleType))
            }
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `an inactive payload bakes nothing whichever scale type is used`() {
        PTScaleType.values().forEach { scaleType ->
            // CENTER_CROP has nothing to bake because the views would draw it
            // FIT_CENTER passes the inactive data straight through, which draws nothing either
            val baked = inactive.bakedInto(scaleType)
            assertFalse(
                "inactive data must never produce something to draw ($scaleType)",
                baked?.isActive == true || baked?.withScrim == true
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `a null payload bakes nothing whichever scale type is used`() {
        val none: ImageBorderData? = null
        PTScaleType.values().forEach { scaleType ->
            assertNull("$scaleType", none.bakedInto(scaleType))
        }
    }

    // ---------------------------------------------------------------- scrim routing

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `a CENTER_CROP scrim is baked alone, leaving radius and border to the views`() {
        // Given - Zero Bezel's payload: styled, and asking for the scrim too
        val zeroBezel = both.forZeroBezel(PTScaleType.CENTER_CROP)!!

        // When
        val baked = zeroBezel.bakedInto(PTScaleType.CENTER_CROP)!!

        // Then - only the scrim goes into the bitmap. A baked corner or ring would be cropped away
        // by the fill, which is the whole reason the views draw those.
        assertTrue("the scrim is baked", baked.withScrim)
        assertEquals("no radius is baked", 0f, baked.cornerRadiusPercent)
        assertEquals("no border width is baked", 0f, baked.borderWidthValue)
        assertNull("no border colour is baked", baked.borderColor)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `FIT_CENTER keeps the scrim view instead of baking it into the picture`() {
        // Given - a picture that does not fill its area. Its text still sits at the area's bottom
        // edge, outside the picture, so baking the scrim into the picture would darken where there
        // is no text and leave the text with no darkening at all.

        // When
        val forFit = both.forZeroBezel(PTScaleType.FIT_CENTER)!!

        // Then - no scrim is folded in, which is what leaves the scrim view in place
        assertFalse(forFit.withScrim)
        assertSame("the payload is otherwise untouched", both, forFit)

        // and nothing scrim-shaped reaches the bitmap either
        assertFalse(forFit.bakedInto(PTScaleType.FIT_CENTER)!!.withScrim)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `forZeroBezel adds the scrim to any styled payload and leaves its sizes alone`() {
        listOf(radiusOnly, borderOnly, both).forEach { data ->
            // When
            val withScrim = data.forZeroBezel(PTScaleType.CENTER_CROP)!!

            // Then
            assertTrue("$data must gain the scrim", withScrim.withScrim)
            assertEquals("$data radius", data.cornerRadiusPercent, withScrim.cornerRadiusPercent)
            assertEquals("$data width", data.borderWidthValue, withScrim.borderWidthValue)
            assertEquals("$data colour", data.borderColor, withScrim.borderColor)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `forZeroBezel leaves an unstyled payload alone`() {
        // Given / Then - with no radius there is nothing to round the scrim to, so the template's
        // own scrim view is left in place and nothing is baked
        PTScaleType.values().forEach { scaleType ->
            assertFalse("$scaleType", inactive.forZeroBezel(scaleType)!!.withScrim)
            assertNull("$scaleType", null.forZeroBezel(scaleType))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `below API 31 no scrim is folded in, since nothing is styled there at all`() {
        // Given / Then - the styling keys are dropped at parse time on these versions, so the
        // scrim view keeps doing its normal job for both scale types
        PTScaleType.values().forEach { scaleType ->
            assertFalse("$scaleType", both.forZeroBezel(scaleType)!!.withScrim)
        }
    }
}
