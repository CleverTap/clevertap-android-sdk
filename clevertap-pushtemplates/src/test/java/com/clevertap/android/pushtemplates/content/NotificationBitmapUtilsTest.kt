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
    // These cover the unit conversions directly. Canvas drawing is a no-op under Robolectric's legacy
    // graphics mode, so asserting on the output bitmap cannot distinguish one radius from another -
    // only these assertions can.

    @Test
    fun `resolveCornerRadiusPx should convert dp into the bitmap's pixel space`() {
        // Given - a 16dp radius on media laid out at 360dp wide

        // Then - a 360px bitmap is 1px per dp, a 720px bitmap is 2px per dp, and so on, so the same
        // dp value produces the same visible curve on each
        assertEquals(16f, NotificationBitmapUtils.resolveCornerRadiusPx(360, 360, 16, 360f))
        assertEquals(32f, NotificationBitmapUtils.resolveCornerRadiusPx(720, 720, 16, 360f))
        assertEquals(48f, NotificationBitmapUtils.resolveCornerRadiusPx(1080, 1080, 16, 360f))
    }

    @Test
    fun `resolveCornerRadiusPx should treat the value as dp and not as raw pixels`() {
        // Given - a 1200x800 campaign image shown at 360dp

        // Then - as raw pixels this would be 24px; converted from dp it is 80px
        assertEquals(80f, NotificationBitmapUtils.resolveCornerRadiusPx(1200, 800, 24, 360f))
    }

    @Test
    fun `resolveCornerRadiusPx should scale with the on-screen media width`() {
        // Given - the same bitmap on a narrow and a wide device

        // Then - fewer bitmap pixels per dp on the wider layout, so a smaller pixel radius
        assertEquals(40f, NotificationBitmapUtils.resolveCornerRadiusPx(720, 720, 16, 288f))
        assertEquals(24f, NotificationBitmapUtils.resolveCornerRadiusPx(720, 720, 16, 480f))
    }

    @Test
    fun `resolveCornerRadiusPx should cap at half the shortest side`() {
        // Given - a large radius on a small image, where a round rect degenerates into a pill

        // Then - capped at half of 180, not the 96px the dp conversion alone would give
        assertEquals(90f, NotificationBitmapUtils.resolveCornerRadiusPx(360, 180, 32, 120f))
    }

    @Test
    fun `resolveCornerRadiusPx should clamp above the supported maximum`() {
        // Given - a payload value beyond the documented 0-32 range

        // Then - clamped to 32dp, which at 1px per dp is 32px
        assertEquals(32f, NotificationBitmapUtils.resolveCornerRadiusPx(360, 360, 99, 360f))
    }

    @Test
    fun `resolveCornerRadiusPx should clamp a negative radius to zero`() {
        // Then
        assertEquals(0f, NotificationBitmapUtils.resolveCornerRadiusPx(360, 360, -10, 360f))
        assertEquals(0f, NotificationBitmapUtils.resolveCornerRadiusPx(360, 360, 0, 360f))
    }

    @Test
    fun `resolveBorderWidthPx should default to one dp when the payload omits the key`() {
        // Given - a 720px bitmap shown at 360dp is 2px per dp

        // Then
        assertEquals(2f, NotificationBitmapUtils.resolveBorderWidthPx(720, 720, null, 360f))
    }

    @Test
    fun `resolveBorderWidthPx should convert dp into the bitmap's pixel space`() {
        // Given - the same 4dp border on differently sized bitmaps at the same on-screen width

        // Then - proportionally more pixels on the larger bitmap, so the same visible thickness
        assertEquals(4f, NotificationBitmapUtils.resolveBorderWidthPx(360, 360, 4, 360f))
        assertEquals(8f, NotificationBitmapUtils.resolveBorderWidthPx(720, 720, 4, 360f))
    }

    @Test
    fun `resolveBorderWidthPx should not vary with aspect ratio`() {
        // Given - a 3:2 and a 1:1 image, both laid out at 360dp wide, both asking for 4dp
        val wide = NotificationBitmapUtils.resolveBorderWidthPx(1200, 800, 4, 360f)
        val square = NotificationBitmapUtils.resolveBorderWidthPx(1200, 1200, 4, 360f)

        // Then - identical pixel strokes, because the conversion keys off the width in both cases.
        // A percentage of the shortest side would have produced different on-screen thicknesses.
        assertEquals(wide, square)
    }

    @Test
    fun `resolveBorderWidthPx should clamp above the supported maximum`() {
        // Given - a payload value beyond the documented 0-16 range

        // Then - clamped to 16dp, which at 1px per dp is 16px
        assertEquals(16f, NotificationBitmapUtils.resolveBorderWidthPx(360, 360, 99, 360f))
    }

    @Test
    fun `resolveBorderWidthPx should clamp a negative width to zero`() {
        // Then
        assertEquals(0f, NotificationBitmapUtils.resolveBorderWidthPx(360, 360, -4, 360f))
        assertEquals(0f, NotificationBitmapUtils.resolveBorderWidthPx(360, 360, 0, 360f))
    }

    @Test
    fun `resolveBorderWidthPx should cap at a quarter of the shortest side`() {
        // Given - 16dp on a small image, where the border would otherwise swallow the picture

        // Then - capped at 25% of 80, not the 160px the dp conversion alone would give
        assertEquals(20f, NotificationBitmapUtils.resolveBorderWidthPx(160, 80, 16, 16f))
    }

    @Test
    fun `resolveBorderWidthPx should return zero for an unusable media width`() {
        // Then
        assertEquals(0f, NotificationBitmapUtils.resolveBorderWidthPx(360, 360, 4, 0f))
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
            cornerRadiusDp = 0,
            borderColor = null,
            borderWidthDp = null,
            mediaWidthDp = 360f
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
            cornerRadiusDp = 16,
            borderColor = null,
            borderWidthDp = null,
            mediaWidthDp = 360f
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
            cornerRadiusDp = 0,
            borderColor = borderColor,
            borderWidthDp = null,
            mediaWidthDp = 360f
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
            cornerRadiusDp = 16,
            borderColor = borderColor,
            borderWidthDp = 1,
            mediaWidthDp = 360f
        ).also { bitmapsToRecycle.add(it) }
        val largeResult = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source = large,
            cornerRadiusDp = 16,
            borderColor = borderColor,
            borderWidthDp = 1,
            mediaWidthDp = 360f
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
            cornerRadiusDp = 500,
            borderColor = borderColor,
            borderWidthDp = null,
            mediaWidthDp = 360f
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
            cornerRadiusDp = 16,
            borderColor = borderColor,
            borderWidthDp = 90,
            mediaWidthDp = 360f
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
            cornerRadiusDp = 16,
            borderColor = borderColor,
            borderWidthDp = -5,
            mediaWidthDp = 360f
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
            cornerRadiusDp = -10,
            borderColor = null,
            borderWidthDp = null,
            mediaWidthDp = 360f
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
            cornerRadiusDp = 32,
            borderColor = borderColor,
            borderWidthDp = 16,
            mediaWidthDp = 360f
        ).also { bitmapsToRecycle.add(it) }

        // Then
        assertNotNull(result)
        assertEquals(1, result.width)
        assertEquals(1, result.height)
    }
}
