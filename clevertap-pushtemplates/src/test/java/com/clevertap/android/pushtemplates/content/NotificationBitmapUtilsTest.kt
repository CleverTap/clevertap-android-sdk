package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
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

    // resolveCornerRadiusPx / resolveBorderWidthPx tests
    //
    // These cover the percentage-to-pixel conversion directly. Canvas drawing is a no-op under
    // Robolectric's legacy graphics mode, so asserting on the output bitmap cannot distinguish a
    // percentage from a raw pixel count - only these assertions can.

    @Test
    fun `resolveCornerRadiusPx should scale with the shortest side`() {
        // Given - the same payload value of 10 against differently sized images

        // Then - 10% of the shortest side in each case, so the result stays proportional
        assertEquals(18f, NotificationBitmapUtils.resolveCornerRadiusPx(180, 10f))
        assertEquals(30f, NotificationBitmapUtils.resolveCornerRadiusPx(300, 10f))
        assertEquals(80f, NotificationBitmapUtils.resolveCornerRadiusPx(800, 10f))
    }

    @Test
    fun `resolveCornerRadiusPx should treat the value as a percentage and not as pixels`() {
        // Given - a payload value that is deliberately larger than the shortest side

        // Then - as pixels this would be 40px; as a percentage of 180 it is 72px
        assertEquals(72f, NotificationBitmapUtils.resolveCornerRadiusPx(180, 40f))
    }

    @Test
    fun `resolveCornerRadiusPx should cap at half the shortest side`() {
        // Given - 50% is a full pill; anything beyond has no additional visible effect

        // Then
        assertEquals(90f, NotificationBitmapUtils.resolveCornerRadiusPx(180, 50f))
        assertEquals(90f, NotificationBitmapUtils.resolveCornerRadiusPx(180, 80f))
        assertEquals(90f, NotificationBitmapUtils.resolveCornerRadiusPx(180, 5000f))
    }

    @Test
    fun `resolveCornerRadiusPx should clamp a negative percentage to zero`() {
        // Then
        assertEquals(0f, NotificationBitmapUtils.resolveCornerRadiusPx(180, -10f))
        assertEquals(0f, NotificationBitmapUtils.resolveCornerRadiusPx(180, 0f))
    }

    @Test
    fun `resolveBorderWidthPx should default to ten percent of the shortest side`() {
        // Given - the payload omits pt_img_border_width

        // Then
        assertEquals(18f, NotificationBitmapUtils.resolveBorderWidthPx(180, null))
        assertEquals(80f, NotificationBitmapUtils.resolveBorderWidthPx(800, null))
    }

    @Test
    fun `resolveBorderWidthPx should scale an explicit percentage with the shortest side`() {
        // Then
        assertEquals(9f, NotificationBitmapUtils.resolveBorderWidthPx(180, 5f))
        assertEquals(40f, NotificationBitmapUtils.resolveBorderWidthPx(800, 5f))
    }

    @Test
    fun `resolveBorderWidthPx should cap at twenty five percent of the shortest side`() {
        // Given - a border thicker than a quarter of the image would swallow the image

        // Then
        assertEquals(45f, NotificationBitmapUtils.resolveBorderWidthPx(180, 25f))
        assertEquals(45f, NotificationBitmapUtils.resolveBorderWidthPx(180, 90f))
        assertEquals(45f, NotificationBitmapUtils.resolveBorderWidthPx(180, 5000f))
    }

    @Test
    fun `resolveBorderWidthPx should clamp a negative percentage to zero`() {
        // Given - an unclamped negative value would both drop the border and push the draw rect
        // outside the bitmap bounds

        // Then
        assertEquals(0f, NotificationBitmapUtils.resolveBorderWidthPx(180, -5f))
    }

    // applyRoundedBorderToBitmap tests

    private fun sourceBitmap(w: Int = width, h: Int = height): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmapsToRecycle.add(it) }

    @Test
    fun `applyRoundedBorderToBitmap should return the source when radius is zero and no border color`() {
        // Given
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = source,
            cornerRadiusPercent = 0f,
            borderColor = null,
            borderWidthPercent = null
        )

        // Then - nothing to draw, so the cached source is reused instead of allocating a copy
        assertSame(source, result)
    }

    @Test
    fun `applyRoundedBorderToBitmap should return a new bitmap of the same size when radius is set`() {
        // Given
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = source,
            cornerRadiusPercent = 10f,
            borderColor = null,
            borderWidthPercent = null
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotSame(source, result)
        assertEquals(source.width, result.width)
        assertEquals(source.height, result.height)
    }

    @Test
    fun `applyRoundedBorderToBitmap should return a new bitmap when only a border color is set`() {
        // Given - a border with no corner radius is still a border
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = source,
            cornerRadiusPercent = 0f,
            borderColor = borderColor,
            borderWidthPercent = null
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotSame(source, result)
        assertEquals(source.width, result.width)
        assertEquals(source.height, result.height)
    }

    @Test
    fun `applyRoundedBorderToBitmap should preserve the dimensions of any source size`() {
        // Given - a small and a large image. The percentage maths is asserted by the
        // resolveCornerRadiusPx tests above; this only pins the output dimensions.
        val small = sourceBitmap(240, 180)
        val large = sourceBitmap(1200, 800)

        // When
        val smallResult = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = small,
            cornerRadiusPercent = 10f,
            borderColor = borderColor,
            borderWidthPercent = 10f
        ).also { bitmapsToRecycle.add(it) }
        val largeResult = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = large,
            cornerRadiusPercent = 10f,
            borderColor = borderColor,
            borderWidthPercent = 10f
        ).also { bitmapsToRecycle.add(it) }

        // Then - each output keeps its own dimensions; the radius is derived from them, so one
        // payload value renders proportionally on both instead of being a fixed pixel count
        assertEquals(240, smallResult.width)
        assertEquals(180, smallResult.height)
        assertEquals(1200, largeResult.width)
        assertEquals(800, largeResult.height)
    }

    @Test
    fun `applyRoundedBorderToBitmap should not fail for an oversized corner radius`() {
        // Given - well beyond the 50 percent cap
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = source,
            cornerRadiusPercent = 500f,
            borderColor = borderColor,
            borderWidthPercent = null
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(result)
        assertEquals(source.width, result.width)
        assertEquals(source.height, result.height)
    }

    @Test
    fun `applyRoundedBorderToBitmap should not fail for an oversized border width`() {
        // Given - well beyond the 25 percent cap
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = source,
            cornerRadiusPercent = 10f,
            borderColor = borderColor,
            borderWidthPercent = 900f
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(result)
        assertEquals(source.width, result.width)
        assertEquals(source.height, result.height)
    }

    @Test
    fun `applyRoundedBorderToBitmap should not fail for a negative border width`() {
        // Given - the clamp itself is asserted by resolveBorderWidthPx; this only checks the draw
        // path survives the value
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = source,
            cornerRadiusPercent = 10f,
            borderColor = borderColor,
            borderWidthPercent = -5f
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotSame(source, result)
        assertEquals(source.width, result.width)
        assertEquals(source.height, result.height)
    }

    @Test
    fun `applyRoundedBorderToBitmap should ignore a negative corner radius`() {
        // Given - a negative radius counts as no radius, so with no border color there is nothing
        // to draw and the source is returned untouched
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = source,
            cornerRadiusPercent = -10f,
            borderColor = null,
            borderWidthPercent = null
        )

        // Then
        assertSame(source, result)
    }

    @Test
    fun `applyRoundedBorderToBitmap should handle a single pixel bitmap`() {
        // Given - the smallest bitmap the platform will hand us
        val source = sourceBitmap(1, 1)

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = source,
            cornerRadiusPercent = 50f,
            borderColor = borderColor,
            borderWidthPercent = 25f
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(result)
        assertEquals(1, result.width)
        assertEquals(1, result.height)
    }
}
