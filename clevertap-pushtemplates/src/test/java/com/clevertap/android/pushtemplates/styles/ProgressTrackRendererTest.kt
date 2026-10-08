package com.clevertap.android.pushtemplates.styles

import android.graphics.Color
import androidx.core.graphics.ColorUtils
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
    // Black background: every color used below already has 3:1 contrast on it, so none is adjusted.
    private val spec = ProgressTrackRenderer.Spec(
        widthPx = 214, density = 1f, scaledDensity = 1f,
        defaultColor = Color.GRAY, titleColor = Color.LTGRAY, backgroundColor = Color.BLACK
    )

    private fun layout(
        segments: List<SegmentData>,
        points: List<PointData> = emptyList(),
        progress: Int = 0,
        styledByProgress: Boolean = false,
        hasTracker: Boolean = false
    ) = ProgressTrackRenderer.layout(
        spec, segments, points, ProgressTrackRenderer.total(segments, 100),
        progress, styledByProgress, hasTracker, ProgressTrackRenderer.titlePaint(spec)
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
        assertEquals(listOf(Color.RED, Color.GRAY), l.dots.map { it.color }) // same theme accent as segments
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
        val bitmap = ProgressTrackRenderer.render(
            spec, segments, listOf(PointData(10, null, "Cooking")), 100, 0, false, null
        )

        val l = layout(segments, listOf(PointData(10, null, "Cooking")))
        assertEquals(l.width, bitmap.width)
        assertEquals(l.height, bitmap.height)
    }

    @Test
    fun `not styled by progress keeps every segment and point at full color`() {
        val l = layout(
            listOf(SegmentData(50, Color.GREEN), SegmentData(50, Color.CYAN)),
            listOf(PointData(75, Color.RED)), progress = 25
        )

        assertEquals(listOf(Color.GREEN, Color.CYAN), l.segments.map { it.color })
        assertTrue(l.segments.none { it.faded })
        assertEquals(Color.RED, l.dots.single().color)
        assertEquals(57f, l.progressX, 0.01f) // still tracked, for the tracker
    }

    @Test
    fun `styled by progress splits the segment at the progress and fades what is ahead`() {
        val l = layout(
            listOf(SegmentData(50, Color.GREEN), SegmentData(50, Color.CYAN)),
            listOf(PointData(25, Color.RED), PointData(75, Color.RED)),
            progress = 40, styledByProgress = true
        )

        // progress 40 -> x 87; no tracker, so a 4px gap before the split.
        assertEquals(
            listOf(
                ProgressTrackRenderer.SegmentRect(7f, 83f, Color.GREEN),
                ProgressTrackRenderer.SegmentRect(87f, 107f, ProgressTrackRenderer.faded(Color.GREEN), faded = true),
                ProgressTrackRenderer.SegmentRect(107f, 207f, ProgressTrackRenderer.faded(Color.CYAN), faded = true)
            ),
            l.segments
        )
        assertEquals(listOf(Color.RED, ProgressTrackRenderer.faded(Color.RED)), l.dots.map { it.color })
    }

    @Test
    fun `progress on a segment boundary fades the next segment whole`() {
        val l = layout(
            listOf(SegmentData(50, Color.GREEN), SegmentData(50, Color.CYAN)),
            progress = 50, styledByProgress = true
        )

        assertEquals(listOf(false, true), l.segments.map { it.faded })
        assertEquals(Color.GREEN, l.segments[0].color)
    }

    @Test
    fun `complete progress fades nothing`() {
        val l = layout(listOf(SegmentData(100, Color.GREEN)), listOf(PointData(50, Color.RED)),
            progress = 100, styledByProgress = true)

        assertTrue(l.segments.none { it.faded })
        assertEquals(Color.RED, l.dots.single().color)
    }

    @Test
    fun `tracker sits centered on the progress and removes the progress gap`() {
        val l = layout(listOf(SegmentData(100, Color.GREEN)), progress = 50,
            styledByProgress = true, hasTracker = true)

        val tracker = l.tracker!!
        assertEquals(107f, tracker.centerX(), 0.01f)
        assertEquals(20f, tracker.width(), 0.01f)
        assertEquals(l.trackY, tracker.centerY(), 0.01f)
        assertEquals(107f, l.segments[0].right, 0.01f) // no gap: the tracker covers the split
        assertEquals(20, l.height) // tracker (20) is taller than a dot (14)
    }

    @Test
    fun `tracker stays on the canvas at both ends`() {
        val atStart = layout(listOf(SegmentData(100, null)), progress = 0, hasTracker = true).tracker!!
        val atEnd = layout(listOf(SegmentData(100, null)), progress = 100, hasTracker = true).tracker!!

        assertEquals(0f, atStart.left, 0.01f)
        assertEquals(214f, atEnd.right, 0.01f)
    }

    @Test
    fun `no tracker means no tracker bounds`() {
        assertEquals(null, layout(listOf(SegmentData(100, null)), progress = 50).tracker)
    }

    @Test
    fun `faded color halves the alpha and keeps the rgb`() {
        assertEquals(Color.argb(128, 0, 255, 0), ProgressTrackRenderer.faded(Color.GREEN))
    }

    @Test
    fun `a color with enough contrast is kept as is`() {
        assertEquals(Color.WHITE, ProgressTrackRenderer.ensureContrast(Color.WHITE, Color.BLACK))
        assertEquals(Color.BLACK, ProgressTrackRenderer.ensureContrast(Color.BLACK, Color.WHITE))
    }

    @Test
    fun `a low-contrast color is darkened on a light card and lightened on a dark card`() {
        val onLight = ProgressTrackRenderer.ensureContrast(Color.WHITE, Color.WHITE)
        val onDark = ProgressTrackRenderer.ensureContrast(Color.rgb(0, 0, 80), Color.rgb(48, 48, 48))

        assertTrue(ColorUtils.calculateContrast(onLight, Color.WHITE) >= 3.0)
        assertTrue(ColorUtils.calculateLuminance(onLight) < ColorUtils.calculateLuminance(Color.WHITE))
        assertTrue(ColorUtils.calculateContrast(onDark, Color.rgb(48, 48, 48)) >= 3.0)
        assertTrue(ColorUtils.calculateLuminance(onDark) > ColorUtils.calculateLuminance(Color.rgb(0, 0, 80)))
        assertTrue(Color.blue(onDark) > Color.red(onDark)) // still blue, just lighter
    }

    @Test
    fun `payload colors are contrast-adjusted against the card`() {
        val light = spec.copy(backgroundColor = Color.WHITE)
        val l = ProgressTrackRenderer.layout(
            light, listOf(SegmentData(100, Color.WHITE)), listOf(PointData(50, Color.WHITE)), 100,
            0, false, false, ProgressTrackRenderer.titlePaint(light)
        )

        assertTrue(ColorUtils.calculateContrast(l.segments.single().color, Color.WHITE) >= 3.0)
        assertTrue(ColorUtils.calculateContrast(l.dots.single().color, Color.WHITE) >= 3.0)
    }

    private fun assertNoOverlap(slots: List<ProgressTrackRenderer.TitleSlot>, width: Float, gap: Float) {
        slots.forEach { assertTrue("$it off-canvas", it.left >= -0.01f && it.right <= width + 0.01f) }
        slots.zipWithNext().forEach { (a, b) -> assertTrue("$a and $b too close", a.right + gap <= b.left + 0.01f) }
    }

    @Test
    fun `well spaced titles stay centered under their dots`() {
        val slots = ProgressTrackRenderer.titleSlots(listOf(50f, 150f, 250f), listOf(30f, 30f, 30f), 300f, 6f)

        assertEquals(listOf(35f, 135f, 235f), slots.map { it.left })
        assertEquals(listOf(65f, 165f, 265f), slots.map { it.right })
    }

    @Test
    fun `close titles slide apart and keep reaching their dots`() {
        // Unequal 10/80/10 demo: Placed @0, Cooking @10%, On way @90%, Delivered @100%.
        val centers = listOf(7f, 36f, 268f, 293f)
        val slots = ProgressTrackRenderer.titleSlots(centers, listOf(30f, 33f, 32f, 44f), 300f, 6f)

        assertNoOverlap(slots, 300f, 6f)
        assertEquals(30f, slots[0].right - slots[0].left, 0.01f) // "Placed" untouched
        assertEquals(33f, slots[1].right - slots[1].left, 0.01f) // "Cooking" only shifted right
        slots.zip(centers).forEach { (s, cx) -> assertTrue("$s misses $cx", s.left <= cx + 0.01f && cx <= s.right + 0.01f) }
    }

    @Test
    fun `dots closer than the gap split at the midpoint`() {
        val slots = ProgressTrackRenderer.titleSlots(listOf(100f, 102f), listOf(40f, 40f), 300f, 6f)

        assertNoOverlap(slots, 300f, 6f)
    }

    @Test
    fun `many crowded titles never overlap`() {
        val centers = (0..9).map { 10f + it * 12f }
        val slots = ProgressTrackRenderer.titleSlots(centers, List(10) { 40f }, 140f, 6f)

        assertNoOverlap(slots, 140f, 6f)
    }
}
