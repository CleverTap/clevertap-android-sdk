package com.clevertap.android.pushtemplates.styles

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric (for real org.json + android.graphics.Color) tests for the pt_progress parser. */
@RunWith(RobolectricTestRunner::class)
class ProgressPayloadParserTest {

    @Test
    fun `parseSegments reads length and color, defaults length to 1 and color to null`() {
        val segs = ProgressPayloadParser.parseSegments(
            """[{"length":41,"color":"#4CAF50"},{},{"length":2}]"""
        )
        assertEquals(3, segs.size)
        assertEquals(41, segs[0].length)
        assertEquals(Color.parseColor("#4CAF50"), segs[0].color)
        assertEquals(1, segs[1].length)   // default when missing
        assertNull(segs[1].color)         // missing color -> null
        assertEquals(2, segs[2].length)
        assertNull(segs[2].color)
    }

    @Test
    fun `parsePoints reads position and color, defaults position to 0`() {
        val pts = ProgressPayloadParser.parsePoints(
            """[{"position":60,"color":"#9E9E9E"},{}]"""
        )
        assertEquals(2, pts.size)
        assertEquals(60, pts[0].position)
        assertEquals(Color.parseColor("#9E9E9E"), pts[0].color)
        assertEquals(0, pts[1].position)  // default when missing
        assertNull(pts[1].color)
    }

    @Test
    fun `null empty and malformed yield empty lists`() {
        assertTrue(ProgressPayloadParser.parseSegments(null).isEmpty())
        assertTrue(ProgressPayloadParser.parseSegments("").isEmpty())
        assertTrue(ProgressPayloadParser.parseSegments("not json").isEmpty())
        assertTrue(ProgressPayloadParser.parsePoints(null).isEmpty())
        assertTrue(ProgressPayloadParser.parsePoints("[garbage").isEmpty())
    }

    @Test
    fun `colorOrNull parses valid hex and returns null for blank or invalid`() {
        assertEquals(Color.parseColor("#FF9500"), ProgressPayloadParser.colorOrNull("#FF9500"))
        assertNull(ProgressPayloadParser.colorOrNull(null))
        assertNull(ProgressPayloadParser.colorOrNull(""))
        assertNull(ProgressPayloadParser.colorOrNull("nope"))
    }
}
