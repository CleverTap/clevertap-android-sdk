package com.clevertap.android.pushtemplates

import android.graphics.Color
import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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
                assertEquals(expected, data.hasBorder, "$widthLabel + $colourLabel")
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
        assertTrue(radiusOnly.isActive, "a radius alone")
        assertTrue(borderOnly.isActive, "a border alone")
        assertTrue(both.isActive, "both together")
        assertFalse(inactive.isActive, "neither")
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
            assertFalse(it.usesNativeImageStyling(), "$it must not use the view path")
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `on API 31 only FIT_CENTER bakes its styling into the bitmap`() {
        // Given
        listOf(radiusOnly, borderOnly, both).forEach { data ->
            // When / Then
            assertNull(data.bakedInto(PTScaleType.CENTER_CROP), "$data with CENTER_CROP")
            assertSame(data, data.bakedInto(PTScaleType.FIT_CENTER), "$data with FIT_CENTER")
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `below API 31 image styling is ignored for every scale type`() {
        // Given / Then
        listOf(radiusOnly, borderOnly, both).forEach { data ->
            PTScaleType.values().forEach { scaleType ->
                assertNull(data.bakedInto(scaleType), "$data with $scaleType")
            }
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `an inactive payload bakes nothing whichever scale type is used`() {
        PTScaleType.values().forEach { scaleType ->
            val baked = inactive.bakedInto(scaleType)
            assertFalse(baked?.isActive == true, "inactive data must never produce something to draw ($scaleType)")
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `a null payload bakes nothing whichever scale type is used`() {
        val none: ImageBorderData? = null
        PTScaleType.values().forEach { scaleType ->
            assertNull(none.bakedInto(scaleType), "$scaleType")
        }
    }
}
