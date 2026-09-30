package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import android.graphics.Color
import com.clevertap.android.pushtemplates.ImageBorderData
import android.os.Build
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    @Test
    fun `resolveCornerRadiusPx should scale with the reference it is given`() {
        // Given

        // Then
        assertEquals(18f, NotificationBitmapUtils.resolveCornerRadiusPx(180, 10f))
        assertEquals(30f, NotificationBitmapUtils.resolveCornerRadiusPx(300, 10f))
        assertEquals(80f, NotificationBitmapUtils.resolveCornerRadiusPx(800, 10f))
    }

    @Test
    fun `resolveCornerRadiusPx should treat the value as a percentage and not as pixels`() {
        // Given

        // Then
        assertEquals(72f, NotificationBitmapUtils.resolveCornerRadiusPx(180, 40f))
    }

    @Test
    fun `resolveCornerRadiusPx should cap at half the reference`() {
        // Given

        // Then
        assertEquals(50f, NotificationBitmapUtils.resolveCornerRadiusPx(100, 50f))
        assertEquals(50f, NotificationBitmapUtils.resolveCornerRadiusPx(100, 80f))
        assertEquals(50f, NotificationBitmapUtils.resolveCornerRadiusPx(100, 1000f))
    }

    @Test
    fun `resolveCornerRadiusPx should clamp a negative percentage to zero`() {
        assertEquals(0f, NotificationBitmapUtils.resolveCornerRadiusPx(100, -10f))
    }

    @Test
    fun `resolveBorderWidthPx should scale with the reference it is given`() {
        assertEquals(9f, NotificationBitmapUtils.resolveBorderWidthPx(180, 50f))
        assertEquals(40f, NotificationBitmapUtils.resolveBorderWidthPx(800, 50f))
    }

    @Test
    fun `resolveBorderWidthPx should cap at a tenth of the reference`() {
        assertEquals(10f, NotificationBitmapUtils.resolveBorderWidthPx(100, 100f))
        assertEquals(10f, NotificationBitmapUtils.resolveBorderWidthPx(100, 400f))
    }

    @Test
    fun `resolveBorderWidthPx should clamp a negative value to zero`() {
        assertEquals(0f, NotificationBitmapUtils.resolveBorderWidthPx(100, -5f))
    }

    @Test
    fun `resolveBorderWidthPx should stay zero when no border is requested`() {
        assertEquals(0f, NotificationBitmapUtils.resolveBorderWidthPx(1000, 0f))
    }

    @Test
    fun `resolveBorderWidthPx should never drop below one pixel once a border is requested`() {
        // 600 * 1 / 1000 = 0.6px would render as a faint smear, so it is lifted to a whole pixel.
        assertEquals(1f, NotificationBitmapUtils.resolveBorderWidthPx(600, 1f))
        assertEquals(1f, NotificationBitmapUtils.resolveBorderWidthPx(600, 0.5f))
        // Anything already at or above a pixel is left alone.
        assertEquals(1.2f, NotificationBitmapUtils.resolveBorderWidthPx(600, 2f), 0.0001f)
    }

    @Test
    fun `a fully rounded radius still leaves room for a capped border stroke`() {
        // Given
        val reference = 200
        val radius = NotificationBitmapUtils.resolveCornerRadiusPx(reference, 50f)
        val stroke = NotificationBitmapUtils.resolveBorderWidthPx(reference, 100f)

        // Then
        assertEquals(100f, radius)
        assertEquals(20f, stroke)
        assertTrue("stroke radius must stay positive", radius - stroke / 2f > 0f)
        assertTrue("stroke must fit inside the reference", stroke < reference / 2f)
    }

    private fun sourceBitmap(w: Int = width, h: Int = height): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmapsToRecycle.add(it) }

    private fun style(
        radius: Float = 0f,
        borderWidth: Float = 0f,
        borderColor: Int? = null
    ) = ImageBorderData(
        cornerRadiusPercent = radius,
        borderWidthValue = borderWidth,
        borderColor = borderColor
    )

    @Test
    fun `should return the source untouched when no styling key is set`() {
        // Given
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(source, style())

        // Then
        assertSame(source, result)
    }

    @Test
    fun `should return the source untouched when the payload is inactive`() {
        // Given
        val source = sourceBitmap()

        // Then
        assertSame(source, NotificationBitmapUtils.applyRoundedBorderToBitmap(source, style(borderColor = borderColor)))
        assertSame(source, NotificationBitmapUtils.applyRoundedBorderToBitmap(source, style(borderWidth = 5f)))
        assertSame(source, NotificationBitmapUtils.applyRoundedBorderToBitmap(source, style(radius = 0f)))
    }

    @Test
    fun `should composite a new bitmap when only a corner radius is set`() {
        // Given
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(source, style(radius = 20f))
        bitmapsToRecycle.add(result)

        // Then
        assertNotSame(source, result)
        assertEquals(source.width, result.width)
        assertEquals(source.height, result.height)
    }

    @Test
    fun `should composite a new bitmap when a border width and colour are both set`() {
        // Given
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source, style(borderWidth = 5f, borderColor = borderColor)
        )
        bitmapsToRecycle.add(result)

        // Then
        assertNotSame(source, result)
        assertEquals(source.width, result.width)
        assertEquals(source.height, result.height)
    }

    @Test
    fun `should keep the border out when the colour cannot be parsed`() {
        // Given
        val source = sourceBitmap()

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source, style(radius = 20f, borderWidth = 5f, borderColor = null)
        )
        bitmapsToRecycle.add(result)

        // Then
        assertNotSame(source, result)
        assertEquals(source.width, result.width)
    }

    @Test
    fun `a border wider than twice the radius must not square off the corners`() {
        // Given
        val minDimension = 400
        val radiusPx = NotificationBitmapUtils.resolveCornerRadiusPx(minDimension, 2f)
        val strokePx = NotificationBitmapUtils.resolveBorderWidthPx(minDimension, 100f)

        // Then
        assertEquals(8f, radiusPx)
        assertEquals(40f, strokePx)
        assertTrue("this case only matters while half the stroke exceeds the radius",
            strokePx / 2f > radiusPx)

        val source = sourceBitmap(minDimension, minDimension)
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source, style(radius = 2f, borderWidth = 100f, borderColor = borderColor)
        )
        bitmapsToRecycle.add(result)
        assertNotSame(source, result)
        assertEquals(minDimension, result.width)
        assertEquals(minDimension, result.height)
    }

    @Test
    fun `the border ring stays inside the bitmap for every radius and width pair`() {
        // Given
        val source = sourceBitmap(400, 400)
        for (radius in listOf(0f, 1f, 2f, 5f, 10f, 25f, 50f)) {
            for (width in listOf(0f, 10f, 20f, 50f, 100f)) {
                // When
                val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
                    source, style(radius = radius, borderWidth = width, borderColor = borderColor)
                )
                if (result !== source) bitmapsToRecycle.add(result)

                // Then
                val strokePx = NotificationBitmapUtils.resolveBorderWidthPx(400, width)
                assertTrue("inset rect inverts at radius=$radius width=$width",
                    400 - 2 * strokePx > 0f)
                assertEquals("width at radius=$radius width=$width", 400, result.width)
                assertEquals("height at radius=$radius width=$width", 400, result.height)
            }
        }
    }

    @Test
    fun `should preserve the dimensions of any source size`() {
        // Given
        listOf(1 to 1, 320 to 50, 400 to 200, 1200 to 600, 120 to 180).forEach { (w, h) ->
            val source = sourceBitmap(w, h)

            // When
            val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
                source, style(radius = 50f, borderWidth = 10f, borderColor = borderColor)
            )
            bitmapsToRecycle.add(result)

            // Then
            assertEquals("width for ${w}x$h", w, result.width)
            assertEquals("height for ${w}x$h", h, result.height)
        }
    }

    @Test
    fun `should not fail for out of range or malformed size values`() {
        // Given
        val source = sourceBitmap()

        listOf(
            style(radius = 1000f),
            style(radius = -40f, borderWidth = 400f, borderColor = borderColor),
            style(borderWidth = -5f, borderColor = borderColor),
            style(radius = Float.MAX_VALUE, borderWidth = Float.MAX_VALUE, borderColor = borderColor)
        ).forEach { border ->
            // When
            val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(source, border)
            bitmapsToRecycle.add(result)

            // Then
            assertEquals(source.width, result.width)
            assertEquals(source.height, result.height)
        }
    }

    @Test
    fun `should handle a single pixel bitmap`() {
        // Given
        val source = sourceBitmap(1, 1)

        // When
        val result = NotificationBitmapUtils.applyRoundedBorderToBitmap(
            source, style(radius = 50f, borderWidth = 10f, borderColor = borderColor)
        )
        bitmapsToRecycle.add(result)

        // Then
        assertEquals(1, result.width)
        assertEquals(1, result.height)
    }
}
