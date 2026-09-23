package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.clevertap.android.pushtemplates.IconsTemplateData
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateDataFactory
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The collapsed view shows only icons. The expanded view shows what pt_* text was sent, without
 * blank lines.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class IconsContentViewTextRowTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        context.applicationInfo.nonLocalizedLabel = "Test App"
        mockkStatic(LaunchPendingIntentFactory::class)
        every { LaunchPendingIntentFactory.getLaunchPendingIntent(any(), any()) } returns
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `title message and summary all set shows icons only when collapsed and title with summary when expanded`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = "PT Summary"))

        val small = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, small.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals(View.VISIBLE, iconRow(small).visibility)
        assertEquals(View.VISIBLE, big.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals("PT Title", big.findViewById<TextView>(R.id.title).text.toString())
        assertEquals("PT Summary", big.findViewById<TextView>(R.id.msg).text.toString())
        assertEquals(View.VISIBLE, iconRow(big).visibility)
    }

    @Test
    fun `expanded view shows pt_msg_summary when pt_msg is absent`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = null, ptSummary = "PT Summary"))

        val msg = inflate(IconsBigContentView(context, renderer(), data, Bundle())).findViewById<TextView>(R.id.msg)

        assertEquals(View.VISIBLE, msg.visibility)
        assertEquals("PT Summary", msg.text.toString())
    }

    @Test
    fun `summary only shows the summary in the expanded view and icons only when collapsed`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = "PT Summary"))

        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))
        val small = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))

        assertEquals(View.VISIBLE, big.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals(View.GONE, big.findViewById<TextView>(R.id.title).visibility)
        assertEquals("PT Summary", big.findViewById<TextView>(R.id.msg).text.toString())
        assertEquals(View.GONE, small.findViewById<View>(R.id.rel_lyt).visibility)
    }

    @Test
    fun `icon only payload hides the text block but keeps the app header`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))

        val small = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        for (view in listOf(small, big)) {
            assertEquals(View.GONE, view.findViewById<View>(R.id.rel_lyt).visibility)
            // Below Android 12 the header must stay without text.
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.metadata).visibility)
            assertEquals("Test App", view.findViewById<TextView>(R.id.app_name).text.toString())
        }
    }

    @Test
    fun `dashboard summary does not open the expanded text row on an icon only campaign`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null, wzrkNms = "Dashboard summary"))

        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, big.findViewById<View>(R.id.rel_lyt).visibility)
    }

    @Test
    fun `dashboard summary does not replace pt_msg in the expanded view`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null, wzrkNms = "Dashboard summary"))

        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        assertEquals("PT Message", big.findViewById<TextView>(R.id.msg).text.toString())
    }

    @Test
    fun `message only shows icons when collapsed and the message without a title line when expanded`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = "PT Message", ptSummary = null))

        val small = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, small.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals(View.GONE, big.findViewById<TextView>(R.id.title).visibility)
        assertEquals("PT Message", big.findViewById<TextView>(R.id.msg).text.toString())
    }

    @Test
    fun `title only shows icons when collapsed and the title without a message line when expanded`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = null, ptSummary = null))

        val small = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, small.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals("PT Title", big.findViewById<TextView>(R.id.title).text.toString())
        assertEquals(View.GONE, big.findViewById<TextView>(R.id.msg).visibility)
    }

    @Test
    fun `large icon slot is hidden when pt_ico is absent`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null))

        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, big.findViewById<View>(R.id.large_icon).visibility)
    }

    /** Same payloads on Android 12+, where the text row uses its layout-v31 variant. */
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `icon only payload hides the text block from Android 12`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))

        val small = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        for (view in listOf(small, big)) {
            assertEquals(View.GONE, view.findViewById<View>(R.id.rel_lyt).visibility)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `summary only shows the summary in the expanded view from Android 12`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = "PT Summary"))

        val big = inflate(IconsBigContentView(context, renderer(), data, Bundle()))
        val small = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))

        assertEquals(View.VISIBLE, big.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals(View.GONE, big.findViewById<TextView>(R.id.title).visibility)
        assertEquals("PT Summary", big.findViewById<TextView>(R.id.msg).text.toString())
        assertEquals(View.GONE, small.findViewById<View>(R.id.rel_lyt).visibility)
    }

    /** Apps targeting Android 12+ get a 48dp collapsed view; text does not change that. */
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `collapsed view keeps the compact icon strip for text where the 48dp row applies`() {
        targetAndroid12OrLater()
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null))

        val collapsed = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))
        val expanded = inflate(IconsBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, collapsed.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals(View.VISIBLE, iconRow(collapsed).visibility)
        assertEquals(dimen(R.dimen.icons_icon_row_compact), iconRow(collapsed).layoutParams.height)
        assertEquals(View.VISIBLE, iconRow(expanded).visibility)
        assertEquals(dimen(R.dimen.icons_icon_row_full), iconRow(expanded).layoutParams.height)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `both views bind the icons when text is set`() {
        targetAndroid12OrLater()
        val withImages = payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null)
            .apply { for (i in 1..5) putString("pt_img$i", "https://example.invalid/$i.png") }
        val data = dataFrom(withImages)

        val collapsed = IconsSmallContentView(context, renderer(), data, Bundle())
        val expanded = IconsBigContentView(context, renderer(), data, Bundle())

        assertEquals(5, collapsed.getUnloadedIconsCount())
        assertEquals(5, expanded.getUnloadedIconsCount())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `collapsed view keeps the icon strip for an icon only campaign where the 48dp row applies`() {
        targetAndroid12OrLater()
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))

        val collapsed = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))

        // Resized, not clipped.
        assertEquals(View.VISIBLE, iconRow(collapsed).visibility)
        assertEquals(dimen(R.dimen.icons_icon_row_compact), iconRow(collapsed).layoutParams.height)
    }

    @Test
    fun `collapsed view keeps the full icon strip for text for an app that does not target Android 12`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null))

        val collapsed = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, collapsed.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals(View.VISIBLE, iconRow(collapsed).visibility)
        assertEquals(dimen(R.dimen.icons_icon_row_full), iconRow(collapsed).layoutParams.height)
    }

    @Test
    fun `icon only collapsed view keeps the full strip for an app that does not target Android 12`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))

        val collapsed = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))

        // Regression: must not use the shorter row.
        assertEquals(dimen(R.dimen.icons_icon_row_full), iconRow(collapsed).layoutParams.height)
    }

    /** The icon-only row must fit 48dp without clipping its icons. */
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU], qualifiers = "sw400dp")
    fun `icon only collapsed view fills the 48dp row without clipping its icons`() {
        targetAndroid12OrLater()
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))
        val root = inflate(IconsSmallContentView(context, renderer(), data, Bundle()))

        val icons = listOf(R.id.cta1, R.id.cta2, R.id.cta3, R.id.cta4, R.id.cta5)
            .map { root.findViewById<View>(it) }
        icons.forEach { it.visibility = View.VISIBLE }
        root.measure(
            View.MeasureSpec.makeMeasureSpec(dp(COLLAPSED_ROW_WIDTH_DP), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(dp(COLLAPSED_ROW_DP), View.MeasureSpec.AT_MOST)
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        val iconHeight = icons.minOf { it.height }
        assertTrue(
            "collapsed view is ${px(root.measuredHeight)}dp, past the ${COLLAPSED_ROW_DP}dp row",
            root.measuredHeight <= dp(COLLAPSED_ROW_DP)
        )
        // 48dp less 8dp margins on the widest bucket.
        assertTrue("icons measured ${px(iconHeight)}dp", iconHeight >= dp(30))
    }

    /** Robolectric does not target Android 12+ by default. */
    private fun targetAndroid12OrLater() {
        context.applicationInfo.targetSdkVersion = Build.VERSION_CODES.TIRAMISU
    }

    private fun iconRow(root: View) = root.findViewById<View>(R.id.icons_icon_row)

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private fun px(value: Int) = value / context.resources.displayMetrics.density

    private fun dimen(id: Int) = context.resources.getDimensionPixelSize(id)

    private fun inflate(view: ContentView): View = view.remoteView.apply(context, FrameLayout(context))

    private fun renderer() = TemplateRenderer(context, Bundle()).apply { notificationId = 1 }

    private fun dataFrom(extras: Bundle) = TemplateDataFactory.createTemplateData(
        TemplateType.ICONS, extras, false, "alt"
    ) { arrayListOf() } as IconsTemplateData

    // No pt_img keys, so nothing is downloaded.
    private fun payload(ptTitle: String?, ptMsg: String?, ptSummary: String?, wzrkNms: String? = null) = Bundle().apply {
        putString("pt_id", "pt_icons")
        putString("nt", "base title")
        putString("nm", "base message")
        wzrkNms?.let { putString("wzrk_nms", it) }
        ptTitle?.let { putString("pt_title", it) }
        ptMsg?.let { putString("pt_msg", it) }
        ptSummary?.let { putString("pt_msg_summary", it) }
        for (i in 1..3) putString("pt_dl$i", "myapp://s$i")
    }

    private companion object {

        // Measured on an Android 15 emulator, app targeting 36, 480dpi.
        const val COLLAPSED_ROW_DP = 48
        const val COLLAPSED_ROW_WIDTH_DP = 287
    }
}
