package com.clevertap.android.pushtemplates.content

import android.graphics.Bitmap
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateDataFactory
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.pushtemplates.media.TemplateMediaManager
import android.widget.RemoteViews
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The icon cache is what stops a position tap costing a round of downloads.
 *
 * Without it a re-render asks [TemplateMediaManager] for every icon again, and that manager is built
 * fresh for each broadcast with an empty cache — so the row could not be redrawn until the network
 * answered, and a failure part-way through turned a row of campaign artwork into built-in stars
 * while the user was tapping it.
 */
@RunWith(RobolectricTestRunner::class)
class ScaledIconCacheTest {

    private val context get() = RuntimeEnvironment.getApplication()

    private fun bitmap(size: Int = 240) =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

    @Before
    fun emptyTheCache() {
        ScaledIconCache.clear()
    }

    @Test
    fun `a bitmap comes back at the size it was stored for`() {
        val stored = bitmap()
        ScaledIconCache.put("https://cdn.example.com/a.png", 126, stored)

        assertSame(stored, ScaledIconCache.get("https://cdn.example.com/a.png", 126))
    }

    @Test
    fun `the same url at a different size is a different entry`() {
        ScaledIconCache.put("https://cdn.example.com/a.png", 126, bitmap())

        assertNull(ScaledIconCache.get("https://cdn.example.com/a.png", 64))
    }

    @Test
    fun `a url that was never stored misses`() {
        assertNull(ScaledIconCache.get("https://cdn.example.com/never.png", 126))
    }

    @Test
    fun `a recycled entry is dropped rather than handed back`() {
        val stored = bitmap()
        ScaledIconCache.put("https://cdn.example.com/a.png", 126, stored)
        stored.recycle()

        assertNull(ScaledIconCache.get("https://cdn.example.com/a.png", 126))
    }

    @Test
    fun `the cache stays bounded`() {
        repeat(40) { ScaledIconCache.put("https://cdn.example.com/$it.png", 126, bitmap(8)) }

        // Bounded because it is a convenience, not a store: it must never be the reason a process
        // runs out of memory.
        assert(ScaledIconCache.size() <= 24) { "cache grew to ${ScaledIconCache.size()}" }
    }

    // ── The behaviour that matters: a re-render must not hit the network ──────────────

    private fun extras(count: Int = 3) = android.os.Bundle().apply {
        putString(PTConstants.PT_ID, "pt_custom_rating")
        putString(PTConstants.PT_TITLE, "How did we do?")
        putString(PTConstants.PT_MSG, "Tap a face")
        putString(PTConstants.PT_DEFAULT_DL, "ctdemo://home")
        putString(PTConstants.PT_RATING_STYLE, "icon")
        putString(PTConstants.PT_RATING_COUNT, count.toString())
        for (position in 1..count) {
            putString(
                "${PTConstants.PT_RATING_ICON_PREFIX}$position",
                "https://cdn.example.com/icon$position.png"
            )
        }
        putString(PTConstants.PT_RATING_CTA_LABEL, "Submit")
        putString(PTConstants.PT_RATING_CTA_DL, "ctdemo://feedback")
    }

    private fun data() = TemplateDataFactory.createTemplateData(
        TemplateType.CUSTOM_RATING, extras(), false, "alt"
    ) { arrayListOf() } as CustomRatingTemplateData

    @Test
    fun `re-rendering a selection does not ask the network again`() {
        val manager = mockk<TemplateMediaManager>(relaxed = true)
        every { manager.getImageBitmap(any()) } returns bitmap()
        val payload = data()

        // First render: three icons have to be fetched.
        CustomRatingRowRenderer.renderRow(
            RemoteViews(context.packageName, R.layout.custom_rating), payload, manager, 0, 126
        )
        verify(exactly = 3) { manager.getImageBitmap(any()) }

        // Every later render draws the same three icons and must be free.
        repeat(5) { round ->
            CustomRatingRowRenderer.renderRow(
                RemoteViews(context.packageName, R.layout.custom_rating),
                payload, manager, round + 1, 126
            )
        }
        verify(exactly = 3) { manager.getImageBitmap(any()) }
    }

    @Test
    fun `a cached icon is reused rather than re-scaled into a new bitmap`() {
        val manager = mockk<TemplateMediaManager>(relaxed = true)
        every { manager.getImageBitmap(any()) } returns bitmap()
        val payload = data()

        val first = CustomRatingRowRenderer.renderRow(
            RemoteViews(context.packageName, R.layout.custom_rating), payload, manager, 0, 126
        )
        val second = CustomRatingRowRenderer.renderRow(
            RemoteViews(context.packageName, R.layout.custom_rating), payload, manager, 1, 126
        )

        // Only the greyscale copies belong to a render; the scaled icons belong to the cache and
        // are never handed back to be recycled. The selected position is not greyscaled, so the
        // second render owns exactly one bitmap fewer than the first.
        assertEquals(3, first.size)
        assertEquals(2, second.size)
    }
}
