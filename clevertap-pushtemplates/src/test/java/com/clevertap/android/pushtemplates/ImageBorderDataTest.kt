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
 * Covers the decisions that route image styling: whether there is anything to draw and whether
 * the views or the bitmap draw it.
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
                baked?.isActive == true
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
}
