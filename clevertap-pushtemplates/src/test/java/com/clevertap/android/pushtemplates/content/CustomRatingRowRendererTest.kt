package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateDataFactory
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.pushtemplates.media.TemplateMediaManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Renders the pt_custom_rating row into a real view hierarchy and asserts what a user would see.
 *
 * The RemoteViews is applied with [RemoteViews.apply], so these tests exercise the same instructions
 * the notification shade would follow rather than trusting the calls in isolation.
 */
@RunWith(RobolectricTestRunner::class)
class CustomRatingRowRendererTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    /** No artwork ever resolves, so icon positions fall back to the built-in star (R-21). */
    private val mediaManager = mockk<TemplateMediaManager>(relaxed = true).also {
        every { it.getImageBitmap(any()) } returns null
    }

    private val cellIds = intArrayOf(
        R.id.custom_rating_cell1, R.id.custom_rating_cell2, R.id.custom_rating_cell3,
        R.id.custom_rating_cell4, R.id.custom_rating_cell5
    )
    private val iconIds = intArrayOf(
        R.id.custom_rating_pos1, R.id.custom_rating_pos2, R.id.custom_rating_pos3,
        R.id.custom_rating_pos4, R.id.custom_rating_pos5
    )
    private val labelIds = intArrayOf(
        R.id.custom_rating_label1, R.id.custom_rating_label2, R.id.custom_rating_label3,
        R.id.custom_rating_label4, R.id.custom_rating_label5
    )

    private fun iconPayload(count: Int, vararg extra: Pair<String, String>) = buildMap {
        put(PTConstants.PT_RATING_STYLE, "icon")
        put(PTConstants.PT_RATING_COUNT, count.toString())
        for (position in 1..count) {
            put("${PTConstants.PT_RATING_ICON_PREFIX}$position", "https://cdn.example.com/i$position.png")
        }
        putAll(extra)
    }

    private fun textPayload(vararg positionLabels: String?, count: Int = positionLabels.size) = buildMap {
        put(PTConstants.PT_RATING_STYLE, "text")
        put(PTConstants.PT_RATING_COUNT, count.toString())
        positionLabels.forEachIndexed { index, label ->
            if (label != null) put("${PTConstants.PT_RATING_LABEL_PREFIX}${index + 1}", label)
        }
    }

    private fun data(payload: Map<String, String>): CustomRatingTemplateData {
        val extras = android.os.Bundle().apply {
            putString(PTConstants.PT_ID, "pt_custom_rating")
            putString(PTConstants.PT_TITLE, "How did we do?")
            putString(PTConstants.PT_MSG, "Tell us")
            putString(PTConstants.PT_DEFAULT_DL, "ctdemo://home")
            putString(PTConstants.PT_RATING_CTA_LABEL, "Submit")
            putString(PTConstants.PT_RATING_CTA_DL, "ctdemo://feedback")
            payload.forEach { (key, value) -> putString(key, value) }
        }
        return TemplateDataFactory.createTemplateData(
            TemplateType.CUSTOM_RATING, extras, false, "alt"
        ) { arrayListOf() } as CustomRatingTemplateData
    }

    /** Renders the row and inflates the result, returning the root of the real view tree. */
    private fun render(payload: Map<String, String>, selectedPosition: Int = 0): ViewGroup {
        val remoteViews = RemoteViews(context.packageName, R.layout.custom_rating)
        CustomRatingRowRenderer.renderRow(remoteViews, data(payload), mediaManager, selectedPosition)
        return remoteViews.apply(context, FrameLayout(context)) as ViewGroup
    }

    private fun ViewGroup.visibility(id: Int) = findViewById<View>(id).visibility

    @Test
    fun `only the configured positions are shown`() {
        val root = render(iconPayload(count = 3))

        for (index in 0..2) assertEquals(View.VISIBLE, root.visibility(cellIds[index]))
        for (index in 3..4) assertEquals(View.GONE, root.visibility(cellIds[index]))
    }

    @Test
    fun `the minimum and maximum scales both render every configured position`() {
        val two = render(iconPayload(count = 2))
        assertEquals(View.VISIBLE, two.visibility(cellIds[1]))
        assertEquals(View.GONE, two.visibility(cellIds[2]))

        val five = render(iconPayload(count = 5))
        assertEquals(View.VISIBLE, five.visibility(cellIds[4]))
    }

    @Test
    fun `icon style shows the icon and hides the text chip`() {
        val root = render(iconPayload(count = 3))

        for (index in 0..2) {
            assertEquals(View.VISIBLE, root.visibility(iconIds[index]))
            assertEquals(View.GONE, root.visibility(labelIds[index]))
        }
    }

    @Test
    fun `text style shows the labels and hides the icons`() {
        val root = render(textPayload("Bad", "Okay", "Great"))

        for (index in 0..2) {
            assertEquals(View.GONE, root.visibility(iconIds[index]))
            assertEquals(View.VISIBLE, root.visibility(labelIds[index]))
        }
        assertEquals("Bad", root.findViewById<TextView>(labelIds[0]).text.toString())
        assertEquals("Great", root.findViewById<TextView>(labelIds[2]).text.toString())
    }

    @Test
    fun `emoji labels render as text, with no artwork involved`() {
        val root = render(textPayload("😡", "😐", "😍"))

        assertEquals("😐", root.findViewById<TextView>(labelIds[1]).text.toString())
        assertEquals(View.GONE, root.visibility(iconIds[1]))
    }

    @Test
    fun `a label is ellipsised on one line rather than wrapped`() {
        val root = render(textPayload("Terrible", "Bad", "Okay", "Good", "Excellent"))
        val label = root.findViewById<TextView>(labelIds[4])

        assertEquals(1, label.maxLines)
        assertEquals(android.text.TextUtils.TruncateAt.END, label.ellipsize)
    }

    @Test
    fun `labels shrink once there are four or more positions`() {
        val three = render(textPayload("Bad", "Okay", "Great"))
            .findViewById<TextView>(labelIds[0]).textSize
        val five = render(textPayload("A", "B", "C", "D", "E"))
            .findViewById<TextView>(labelIds[0]).textSize

        assertTrue("dense rows must use smaller text", five < three)
    }

    @Test
    fun `a text row missing one label falls back to the built-in stars for the whole row`() {
        val root = render(textPayload("Bad", null, "Great", count = 3))

        for (index in 0..2) {
            assertEquals(View.VISIBLE, root.visibility(iconIds[index]))
            assertEquals(View.GONE, root.visibility(labelIds[index]))
        }
    }

    @Test
    fun `the submit button carries the campaign's label`() {
        val root = render(iconPayload(count = 3))

        assertEquals(View.VISIBLE, root.visibility(R.id.custom_rating_cta))
        assertEquals(
            "Submit",
            root.findViewById<TextView>(R.id.custom_rating_cta_label).text.toString()
        )
    }

    @Test
    fun `label colours default to the notification's own text colours when unset`() {
        val root = render(
            textPayload("No", "Yes") + mapOf(
                PTConstants.PT_TITLE_COLOR to "#101010",
                PTConstants.PT_MSG_COLOR to "#202020"
            ),
            selectedPosition = 1
        )

        val selected = root.findViewById<TextView>(labelIds[0]).currentTextColor
        val unselected = root.findViewById<TextView>(labelIds[1]).currentTextColor

        assertEquals(0xFF101010.toInt(), selected)
        assertEquals(0xFF202020.toInt(), unselected)
    }

    @Test
    fun `configured label colours win over the notification's text colours`() {
        val root = render(
            textPayload("No", "Yes") + mapOf(
                PTConstants.PT_MSG_COLOR to "#202020",
                PTConstants.PT_RATING_LABEL_CLR to "#00FF00",
                PTConstants.PT_RATING_LABEL_SEL_CLR to "#0000FF"
            ),
            selectedPosition = 2
        )

        assertEquals(0xFF00FF00.toInt(), root.findViewById<TextView>(labelIds[0]).currentTextColor)
        assertEquals(0xFF0000FF.toInt(), root.findViewById<TextView>(labelIds[1]).currentTextColor)
    }

    @Test
    fun `the row renders whichever position is selected, and only that one`() {
        // Artwork never resolves here, so the visible difference is the star drawable each cell gets.
        val none = render(iconPayload(count = 3), selectedPosition = 0)
        val second = render(iconPayload(count = 3), selectedPosition = 2)

        fun drawableState(root: ViewGroup, index: Int) =
            root.findViewById<ImageView>(iconIds[index]).drawable?.constantState

        assertEquals(drawableState(none, 1)?.javaClass, drawableState(second, 1)?.javaClass)
        assertNotEquals(drawableState(second, 0), drawableState(second, 1))
        assertEquals(drawableState(second, 0), drawableState(second, 2))
    }

    @Test
    fun `the confirmation state hides the row and the submit button`() {
        val remoteViews = RemoteViews(context.packageName, R.layout.custom_rating)
        CustomRatingRowRenderer.renderRow(remoteViews, data(iconPayload(count = 3)), mediaManager, 2)
        CustomRatingRowRenderer.hideInteractiveViews(remoteViews)

        val root = remoteViews.apply(context, FrameLayout(context)) as ViewGroup

        assertEquals(View.GONE, root.visibility(R.id.custom_rating_row))
        assertEquals(View.GONE, root.visibility(R.id.custom_rating_cta))
    }

    @Test
    fun `cellViewId maps every position and never falls outside the row`() {
        val ids = (1..PTConstants.PT_RATING_COUNT_MAX).map { CustomRatingRowRenderer.cellViewId(it) }

        assertEquals(cellIds.toList(), ids)
        // Defensive: a position outside the range clamps instead of throwing.
        assertEquals(cellIds.first(), CustomRatingRowRenderer.cellViewId(0))
        assertEquals(cellIds.last(), CustomRatingRowRenderer.cellViewId(9))
    }

    private fun renderTextRowWithChipFill(): List<android.graphics.Bitmap> {
        val remoteViews = RemoteViews(context.packageName, R.layout.custom_rating)
        return CustomRatingRowRenderer.renderRow(
            remoteViews,
            data(
                textPayload("No", "Yes") + mapOf(
                    PTConstants.PT_RATING_ICON_CLR to "#ECEFF1",
                    PTConstants.PT_RATING_ICON_SEL_CLR to "#1E88E5"
                )
            ),
            mediaManager,
            1
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `from Android 12 a chip is a tinted drawable, so no bitmap is generated for it`() {
        // Feasibility Limit 2: tinting costs nothing against the notification's image-memory budget.
        assertTrue(renderTextRowWithChipFill().isEmpty())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `below Android 12 each chip is drawn as a bitmap the caller has to recycle`() {
        val owned = renderTextRowWithChipFill()

        assertEquals(2, owned.size)
        assertTrue(owned.none { it.isRecycled })
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `below Android 12 a chip with no fill colour draws nothing`() {
        val remoteViews = RemoteViews(context.packageName, R.layout.custom_rating)
        val owned = CustomRatingRowRenderer.renderRow(
            remoteViews, data(textPayload("No", "Yes")), mediaManager, 1
        )

        assertTrue(owned.isEmpty())
    }

    @Test
    fun `an unconfigured chip colour leaves the label without a pill behind it`() {
        val root = render(textPayload("No", "Yes"))

        assertFalse(root.findViewById<TextView>(labelIds[0]).background != null)
    }
}
