package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class NotificationBitmapUtilsTest {

    private val width = 100
    private val height = 50
    private val cornerRadius = 8f
    private val bgColor = Color.RED
    private val color1 = Color.RED
    private val color2 = Color.BLUE
    private val borderColor = Color.BLACK

    private val bitmapsToRecycle = mutableListOf<Bitmap>()

    @After
    fun tearDown() {
        bitmapsToRecycle.forEach { it.recycle() }
        bitmapsToRecycle.clear()
    }

    // createSolidBitmap tests

    @Test
    fun `createSolidBitmap should return non-null bitmap without border`() {
        // Given
        // borderColor = null, borderWidth = null

        // When
        val bitmap = NotificationBitmapUtils.createSolidBitmap(
            bgColor = bgColor,
            borderColor = null,
            width = width,
            height = height,
            cornerRadius = cornerRadius
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun `createSolidBitmap should return non-null bitmap with border`() {
        // Given
        // borderColor provided

        // When
        val bitmap = NotificationBitmapUtils.createSolidBitmap(
            bgColor = bgColor,
            borderColor = borderColor,
            width = width,
            height = height,
            cornerRadius = cornerRadius
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun `createSolidBitmap should return non-null bitmap with border and null borderWidth`() {
        // Given
        // borderColor provided, borderWidth = null → BORDER_STROKE_RATIO used internally

        // When
        val bitmap = NotificationBitmapUtils.createSolidBitmap(
            bgColor = bgColor,
            borderColor = borderColor,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            borderWidth = null
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun `createSolidBitmap should return non-null bitmap with explicit borderWidth`() {
        // Given
        // borderColor and explicit borderWidth provided

        // When
        val bitmap = NotificationBitmapUtils.createSolidBitmap(
            bgColor = bgColor,
            borderColor = borderColor,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            borderWidth = 4f
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    // createLinearGradientBitmap tests

    @Test
    fun `createLinearGradientBitmap should return non-null bitmap without border`() {
        // Given
        // direction = 90.0, no border

        // When
        val bitmap = NotificationBitmapUtils.createLinearGradientBitmap(
            color1 = color1,
            color2 = color2,
            direction = 90.0,
            width = width,
            height = height,
            cornerRadius = cornerRadius
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun `createLinearGradientBitmap should return non-null bitmap with border`() {
        // Given
        // direction = 90.0, borderColor provided

        // When
        val bitmap = NotificationBitmapUtils.createLinearGradientBitmap(
            color1 = color1,
            color2 = color2,
            direction = 90.0,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            borderColor = borderColor
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun `createLinearGradientBitmap should return non-null bitmap with custom direction angle`() {
        // Given
        // direction = 45.0 (diagonal gradient)

        // When
        val bitmap = NotificationBitmapUtils.createLinearGradientBitmap(
            color1 = color1,
            color2 = color2,
            direction = 45.0,
            width = width,
            height = height,
            cornerRadius = cornerRadius
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun `createLinearGradientBitmap should return non-null bitmap with border and explicit borderWidth`() {
        // Given
        // borderColor and explicit borderWidth provided

        // When
        val bitmap = NotificationBitmapUtils.createLinearGradientBitmap(
            color1 = color1,
            color2 = color2,
            direction = 90.0,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            borderColor = borderColor,
            borderWidth = 4f
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    // createRadialBitmap tests

    @Test
    fun `createRadialBitmap should return non-null bitmap without border`() {
        // Given
        // no border

        // When
        val bitmap = NotificationBitmapUtils.createRadialBitmap(
            color1 = color1,
            color2 = color2,
            width = width,
            height = height,
            cornerRadius = cornerRadius
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun `createRadialBitmap should return non-null bitmap with border`() {
        // Given
        // borderColor provided

        // When
        val bitmap = NotificationBitmapUtils.createRadialBitmap(
            color1 = color1,
            color2 = color2,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            borderColor = borderColor
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun `createRadialBitmap should return non-null bitmap with border and explicit borderWidth`() {
        // Given
        // borderColor and explicit borderWidth provided

        // When
        val bitmap = NotificationBitmapUtils.createRadialBitmap(
            color1 = color1,
            color2 = color2,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            borderColor = borderColor,
            borderWidth = 4f
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }
}
