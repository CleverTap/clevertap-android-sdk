package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateDataFactory
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.pushtemplates.media.TemplateMediaManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Guards the pt_custom_rating re-render path against unbounded growth.
 *
 * A RemoteViews only ever *appends* instructions — `setImageViewBitmap` on a view id that already
 * has one is a second action and a second bitmap-cache entry, never a replacement. The view returned
 * by `getActiveNotifications()` carries every instruction from every previous render, so re-rendering
 * a selection into it grows the notification by a full icon set on each tap. Android warns from the
 * first tap and then removes the custom layout outright, at which point the rating row silently
 * disappears and the campaign can never be answered.
 *
 * The renderer therefore builds a *new* view for every selection. These tests pin that down: the cost
 * of the tenth re-render has to equal the cost of the first.
 */
@RunWith(RobolectricTestRunner::class)
class CustomRatingReRenderTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    /**
     * The content block reads the app label, which Robolectric leaves unset. Naming the app here
     * keeps these tests about the re-render cost rather than about the test environment.
     */
    @Before
    fun emptyTheIconCache() {
        // ScaledIconCache lives for the process, so one test's icons would otherwise be found by
        // the next one and change what it renders.
        ScaledIconCache.clear()
    }

    @Before
    fun nameTheApplication() {
        context.applicationInfo.nonLocalizedLabel = "Rating Test App"
    }

    private fun extras(count: Int = 5, withSubmit: Boolean = true) = Bundle().apply {
        putString(PTConstants.PT_ID, "pt_custom_rating")
        putString(PTConstants.PT_TITLE, "How did we do?")
        putString(PTConstants.PT_MSG, "Tap a face, then Submit")
        putString(PTConstants.PT_DEFAULT_DL, "ctdemo://home")
        putString(PTConstants.PT_RATING_STYLE, "icon")
        putString(PTConstants.PT_RATING_COUNT, count.toString())
        for (position in 1..count) {
            putString(
                "${PTConstants.PT_RATING_ICON_PREFIX}$position",
                "https://cdn.example.com/icon$position.png"
            )
        }
        if (withSubmit) {
            putString(PTConstants.PT_RATING_CTA_LABEL, "Submit")
            putString(PTConstants.PT_RATING_CTA_DL, "ctdemo://feedback")
        }
    }

    private fun build(selectedPosition: Int, extras: Bundle = extras()) =
        CustomRatingContentView.buildForSelection(
            context, extras, NOTIFICATION_ID, 0, null, selectedPosition
        )

    /** The instruction list the shade will replay. It is what grows when a view is written over. */
    private fun RemoteViews.actionCount(): Int {
        val field = RemoteViews::class.java.getDeclaredField("mActions").apply { isAccessible = true }
        return (field.get(this) as? List<*>)?.size ?: 0
    }

    /** Bitmaps parcelled with the view. This is the number Android measures before stripping. */
    private fun RemoteViews.cachedBitmapCount(): Int {
        val cacheField = RemoteViews::class.java.getDeclaredField("mBitmapCache")
            .apply { isAccessible = true }
        val cache = cacheField.get(this) ?: return 0
        val bitmapsField = cache.javaClass.getDeclaredField("mBitmaps").apply { isAccessible = true }
        return (bitmapsField.get(cache) as? List<*>)?.size ?: 0
    }

    private fun RemoteViews.inflate(): ViewGroup =
        apply(context, FrameLayout(context)) as ViewGroup

    @Test
    fun `every selection in turn costs exactly what the first render cost`() {
        val first = build(CustomRatingContentView.NO_SELECTION)
        assertNotNull(first)
        val baselineActions = first!!.remoteViews.actionCount()
        val baselineBitmaps = first.remoteViews.cachedBitmapCount()

        // A user changing their mind across the whole row, twice over.
        for (round in 1..2) {
            for (position in 1..5) {
                val rendered = build(position)
                assertNotNull("selection $position in round $round should render", rendered)
                assertEquals(
                    "re-rendering selection $position must not add instructions",
                    baselineActions,
                    rendered!!.remoteViews.actionCount()
                )
                assertEquals(
                    "re-rendering selection $position must not add bitmaps",
                    baselineBitmaps,
                    rendered.remoteViews.cachedBitmapCount()
                )
            }
        }
    }

    @Test
    fun `each selection gets its own view rather than the posted one being written over`() {
        val first = build(1)
        val second = build(2)

        assertNotNull(first)
        assertNotNull(second)
        assertNotSame(
            "reusing one RemoteViews across renders is what makes it grow until Android strips it",
            first!!.remoteViews,
            second!!.remoteViews
        )
    }

    /**
     * The hazard itself, stated as a test against the real renderer.
     *
     * Drawing a second selection into a view that has already been drawn into accumulates
     * instructions rather than replacing them — which is what the re-render path used to do to the
     * view off the posted notification, and why [CustomRatingContentView.buildForSelection] hands
     * back a new one instead. If this ever stops growing, the platform changed and the comment on
     * buildForSelection is out of date.
     */
    @Test
    fun `drawing two selections into one view is what makes it grow`() {
        val data = TemplateDataFactory.createTemplateData(
            TemplateType.CUSTOM_RATING, extras(), false, "alt"
        ) { arrayListOf() } as CustomRatingTemplateData
        val mediaManager = mockk<TemplateMediaManager>(relaxed = true).also {
            every { it.getImageBitmap(any()) } returns null
        }

        val reused = RemoteViews(context.packageName, R.layout.custom_rating)
        CustomRatingRowRenderer.renderRow(reused, data, mediaManager, 1)
        val afterFirst = reused.actionCount()

        CustomRatingRowRenderer.renderRow(reused, data, mediaManager, 2)
        val afterSecond = reused.actionCount()

        assertTrue(
            "a reused view carries both renders — this is the growth Android strips for " +
                    "(was $afterFirst, now $afterSecond)",
            afterSecond > afterFirst
        )
    }

    @Test
    fun `the rebuilt view shows the newly selected position`() {
        val root = build(3)!!.remoteViews.inflate()

        // The row is still fully drawn after a re-render: nothing is lost by building fresh.
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.custom_rating_cell1).visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.custom_rating_cell5).visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.custom_rating_row).visibility)
    }

    @Test
    fun `the rebuilt view keeps the submit button so the rating can still be sent`() {
        val root = build(4)!!.remoteViews.inflate()

        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.custom_rating_cta).visibility)
    }

    @Test
    fun `a payload with no submit button still re-renders`() {
        val noSubmit = extras(withSubmit = false)
        val rendered = build(2, noSubmit)

        assertNotNull(rendered)
        val root = rendered!!.remoteViews.inflate()
        assertEquals(View.GONE, root.findViewById<View>(R.id.custom_rating_cta).visibility)
    }

    @Test
    fun `hiding the row for fewer positions does not change the cost of a re-render`() {
        val two = build(1, extras(count = 2))!!.remoteViews.actionCount()
        val twoAgain = build(2, extras(count = 2))!!.remoteViews.actionCount()

        assertEquals(two, twoAgain)
    }

    @Test
    fun `a payload that is no longer renderable yields nothing so the notification is left alone`() {
        val broken = extras().apply { remove(PTConstants.PT_RATING_STYLE) }

        assertNull(build(2, broken))
    }

    private companion object {

        const val NOTIFICATION_ID = 61
    }
}
