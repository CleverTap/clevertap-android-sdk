package com.clevertap.android.pushtemplates.styles

import android.graphics.Color
import com.clevertap.android.pushtemplates.styles.ProgressPayloadParser.PointData
import com.clevertap.android.pushtemplates.styles.ProgressPayloadParser.SegmentData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Geometry of the pre-16 milestone track. Robolectric 4.9 does not rasterize, so this checks the
 * computed layout (where each segment and dot goes) rather than pixels.
 */
@RunWith(RobolectricTestRunner::class)
class ProgressTrackRendererTest {

    // 1px per dp keeps the numbers easy to read: dot radius 7, usable track = width - 14.
    private val spec = ProgressTrackRenderer.Spec(
        widthPx = 214, density = 1f, scaledDensity = 1f,
        trackColor = Color.GRAY, pointColor = Color.WHITE, titleColor = Color.LTGRAY
    )

    private fun layout(segments: List<SegmentData>, points: List<PointData> = emptyList()) =
        ProgressTrackRenderer.layout(
            spec, segments, points, ProgressTrackRenderer.total(segments, 100),
            ProgressTrackRenderer.titlePaint(spec)
        )

    @Test
    fun `segment widths follow their lengths`() {
        val l = layout(listOf(SegmentData(10, null), SegmentData(80, null), SegmentData(10, null)))

        val widths = l.segments.map { it.right - it.left }
        assertEquals(20f, widths[0], 0.01f)   // 10% of 200
        assertEquals(160f, widths[1], 0.01f)  // 80% of 200
        assertEquals(20f, widths[2], 0.01f)
        assertEquals(7f, l.segments.first().left, 0.01f)   // starts after the dot inset
        assertEquals(207f, l.segments.last().right, 0.01f) // ends before the dot inset
    }

    @Test
    fun `points sit at their position on the segment-length scale`() {
        val segments = listOf(SegmentData(10, null), SegmentData(80, null), SegmentData(10, null))
        val points = listOf(PointData(0, null), PointData(10, null), PointData(90, null), PointData(100, null))

        val cx = layout(segments, points).dots.map { it.cx }

        assertEquals(listOf(7f, 27f, 187f, 207f), cx)
    }

    @Test
    fun `total is the segment-length sum like native, else the fallback total`() {
        assertEquals(3, ProgressTrackRenderer.total(listOf(SegmentData(1, null), SegmentData(2, null)), 100))
        assertEquals(100, ProgressTrackRenderer.total(emptyList(), 100))
        assertEquals(1, ProgressTrackRenderer.total(emptyList(), -5)) // malformed max never divides by zero
    }

    @Test
    fun `out-of-range point positions are clamped onto the track`() {
        val cx = layout(listOf(SegmentData(100, null)), listOf(PointData(-20, null), PointData(500, null))).dots.map { it.cx }

        assertEquals(listOf(7f, 207f), cx)
    }

    @Test
    fun `uses payload colors and falls back to the defaults`() {
        val l = layout(
            listOf(SegmentData(50, Color.GREEN), SegmentData(50, null)),
            listOf(PointData(0, Color.RED), PointData(100, null))
        )

        assertEquals(listOf(Color.GREEN, Color.GRAY), l.segments.map { it.color })
        assertEquals(listOf(Color.RED, Color.WHITE), l.dots.map { it.color })
    }

    @Test
    fun `bitmap is taller only when a point has a title`() {
        val segments = listOf(SegmentData(100, null))
        val plain = layout(segments, listOf(PointData(0, null)))
        val titled = layout(segments, listOf(PointData(0, null, "Placed")))

        assertEquals(14, plain.height) // just the dot
        assertTrue(titled.height > plain.height)
    }

    @Test
    fun `render returns a bitmap of the computed size`() {
        val segments = listOf(SegmentData(10, null), SegmentData(90, null))
        val bitmap = ProgressTrackRenderer.render(spec, segments, listOf(PointData(10, null, "Cooking")), 100)

        val l = layout(segments, listOf(PointData(10, null, "Cooking")))
        assertEquals(l.width, bitmap.width)
        assertEquals(l.height, bitmap.height)
    }
}
