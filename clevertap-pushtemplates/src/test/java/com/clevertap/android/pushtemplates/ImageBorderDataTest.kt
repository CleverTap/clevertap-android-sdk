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

@RunWith(RobolectricTestRunner::class)
class ImageBorderDataTest {

    private val radiusOnly = ImageBorderData(cornerRadiusPercent = 10f)
    private val borderOnly = ImageBorderData(borderWidthValue = 5f, borderColor = Color.RED)
    private val both = ImageBorderData(
        cornerRadiusPercent = 10f, borderWidthValue = 5f, borderColor = Color.RED
    )
    private val inactive = ImageBorderData()

    @Test
    fun `a border is drawn only when it has both a width and a colour`() {
        // Given
        val widths = listOf(0f to "no width", 5f to "a width")
        val colours = listOf(null to "no colour", Color.RED to "a colour")

        widths.forEach { (width, widthLabel) ->
            colours.forEach { (colour, colourLabel) ->
                // When
                val data = ImageBorderData(borderWidthValue = width, borderColor = colour)

                // Then
                val expected = width > 0f && colour != null
                assertEquals("$widthLabel + $colourLabel", expected, data.hasBorder)
            }
        }
    }

    @Test
    fun `a negative border width does not count as a border`() {
        // Given
        val data = ImageBorderData(borderWidthValue = -5f, borderColor = Color.RED)

        // Then
        assertFalse(data.hasBorder)
        assertFalse(data.isActive)
    }

    @Test
    fun `styling is active when there is either a radius or a full border`() {
        // Given / Then
        assertTrue("a radius alone", radiusOnly.isActive)
        assertTrue("a border alone", borderOnly.isActive)
        assertTrue("both together", both.isActive)
        assertFalse("neither", inactive.isActive)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `on API 31 only CENTER_CROP is styled by the views`() {
        // Given / Then
        assertTrue(PTScaleType.CENTER_CROP.usesNativeImageStyling())
        assertFalse(PTScaleType.FIT_CENTER.usesNativeImageStyling())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `below API 31 no scale type is styled by the views`() {
        // Given / Then
        assertFalse(useNativeImageStyling)
        PTScaleType.values().forEach {
            assertFalse("$it must not use the view path", it.usesNativeImageStyling())
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `on API 31 only FIT_CENTER bakes its styling into the bitmap`() {
        // Given
        listOf(radiusOnly, borderOnly, both).forEach { data ->
            // When / Then
            assertNull("$data with CENTER_CROP", data.bakedInto(PTScaleType.CENTER_CROP))
            assertSame("$data with FIT_CENTER", data, data.bakedInto(PTScaleType.FIT_CENTER))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `below API 31 image styling is ignored for every scale type`() {
        // Given / Then
        listOf(radiusOnly, borderOnly, both).forEach { data ->
            PTScaleType.values().forEach { scaleType ->
                assertNull("$data with $scaleType", data.bakedInto(scaleType))
            }
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `an inactive payload bakes nothing whichever scale type is used`() {
        PTScaleType.values().forEach { scaleType ->
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
