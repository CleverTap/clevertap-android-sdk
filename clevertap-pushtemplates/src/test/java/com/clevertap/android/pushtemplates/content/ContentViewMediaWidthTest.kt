package com.clevertap.android.pushtemplates.content

import android.util.DisplayMetrics
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The dp width the expanded media is laid out at, which every pt_media_radius and
 * pt_media_border_width conversion is measured against.
 *
 * The notification tray pads its card by 16dp a side and the template pads its content by another
 * 16dp a side, so 64dp of the screen is gone before the media gets any of it. Counting only half of
 * that rendered every radius about 11% shallower than the campaign asked for.
 */
@RunWith(RobolectricTestRunner::class)
class ContentViewMediaWidthTest {

    private fun metrics(screenWidthDp: Int, density: Float = 2f) = DisplayMetrics().apply {
        this.density = density
        widthPixels = (screenWidthDp * density).toInt()
    }

    @Test
    fun `a common phone leaves 296dp of content width`() {
        assertEquals(296f, ContentView.mediaWidthDp(metrics(360)), 0.5f)
    }

    @Test
    fun `a small phone leaves 256dp`() {
        assertEquals(256f, ContentView.mediaWidthDp(metrics(320)), 0.5f)
    }

    @Test
    fun `a large phone leaves 348dp`() {
        assertEquals(348f, ContentView.mediaWidthDp(metrics(412)), 0.5f)
    }

    @Test
    fun `the width scales with density rather than with raw pixels`() {
        assertEquals(
            ContentView.mediaWidthDp(metrics(360, density = 2f)),
            ContentView.mediaWidthDp(metrics(360, density = 3.5f)),
            0.5f
        )
    }

    @Test
    fun `unusable display metrics fall back instead of dividing by zero`() {
        assertEquals(
            ContentView.FALLBACK_MEDIA_WIDTH_DP,
            ContentView.mediaWidthDp(metrics(360, density = 0f)),
            0.001f
        )
    }

    @Test
    fun `an implausibly narrow screen is floored rather than going negative`() {
        assertEquals(
            ContentView.MIN_MEDIA_WIDTH_DP,
            ContentView.mediaWidthDp(metrics(40)),
            0.001f
        )
    }
}
