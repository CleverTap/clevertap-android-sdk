package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
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
 * pt_title and pt_msg are independently optional on five icons. The text row must show what was
 * sent without leaving blank lines, and the expanded view must keep pt_msg_summary visible even
 * when pt_msg itself is absent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class FiveIconContentViewTextRowTest {

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
    fun `title message and summary all set shows message when collapsed and summary when expanded`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = "PT Summary"))

        val small = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))

        for (view in listOf(small, big)) {
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.rel_lyt).visibility)
            assertEquals("PT Title", view.findViewById<TextView>(R.id.title).text.toString())
        }
        assertEquals("PT Message", small.findViewById<TextView>(R.id.msg).text.toString())
        assertEquals("PT Summary", big.findViewById<TextView>(R.id.msg).text.toString())
    }

    @Test
    fun `expanded view shows pt_msg_summary when pt_msg is absent`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = null, ptSummary = "PT Summary"))

        val msg = inflate(FiveIconBigContentView(context, renderer(), data, Bundle())).findViewById<TextView>(R.id.msg)

        assertEquals(View.VISIBLE, msg.visibility)
        assertEquals("PT Summary", msg.text.toString())
    }

    @Test
    fun `summary only shows the summary in the expanded view and icons only when collapsed`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = "PT Summary"))

        val big = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))
        val small = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))

        assertEquals(View.VISIBLE, big.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals(View.GONE, big.findViewById<TextView>(R.id.title).visibility)
        assertEquals("PT Summary", big.findViewById<TextView>(R.id.msg).text.toString())
        assertEquals(View.GONE, small.findViewById<View>(R.id.rel_lyt).visibility)
    }

    @Test
    fun `icon only payload hides the text block but keeps the app header`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))

        val small = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))

        for (view in listOf(small, big)) {
            assertEquals(View.GONE, view.findViewById<View>(R.id.rel_lyt).visibility)
            // Below Android 12 the layout carries its own header; it must survive without text.
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.metadata).visibility)
            assertEquals("Test App", view.findViewById<TextView>(R.id.app_name).text.toString())
        }
    }

    @Test
    fun `dashboard summary does not open the expanded text row on an icon only campaign`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null, wzrkNms = "Dashboard summary"))

        val big = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, big.findViewById<View>(R.id.rel_lyt).visibility)
    }

    @Test
    fun `dashboard summary does not replace pt_msg in the expanded view`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null, wzrkNms = "Dashboard summary"))

        val big = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))

        assertEquals("PT Message", big.findViewById<TextView>(R.id.msg).text.toString())
    }

    @Test
    fun `collapsed view hides the message line when pt_msg is absent`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = null, ptSummary = "PT Summary"))

        val view = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))

        assertEquals(View.VISIBLE, view.findViewById<TextView>(R.id.title).visibility)
        assertEquals(View.GONE, view.findViewById<TextView>(R.id.msg).visibility)
    }

    @Test
    fun `both views hide the title line when pt_title is absent`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = "PT Message", ptSummary = null))

        val small = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, small.findViewById<TextView>(R.id.title).visibility)
        assertEquals("PT Message", small.findViewById<TextView>(R.id.msg).text.toString())
        assertEquals(View.GONE, big.findViewById<TextView>(R.id.title).visibility)
        assertEquals("PT Message", big.findViewById<TextView>(R.id.msg).text.toString())
    }

    @Test
    fun `large icon slot is hidden when pt_ico is absent`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null))

        val small = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, small.findViewById<View>(R.id.large_icon).visibility)
    }

    /**
     * The same two payloads on Android 12 and up, where the text row resolves to its layout-v31
     * variant, which has no header of its own for the system to leave room for.
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `icon only payload hides the text block from Android 12`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))

        val small = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))
        val big = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))

        for (view in listOf(small, big)) {
            assertEquals(View.GONE, view.findViewById<View>(R.id.rel_lyt).visibility)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `summary only shows the summary in the expanded view from Android 12`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = "PT Summary"))

        val big = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))
        val small = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))

        assertEquals(View.VISIBLE, big.findViewById<View>(R.id.rel_lyt).visibility)
        assertEquals(View.GONE, big.findViewById<TextView>(R.id.title).visibility)
        assertEquals("PT Summary", big.findViewById<TextView>(R.id.msg).text.toString())
        assertEquals(View.GONE, small.findViewById<View>(R.id.rel_lyt).visibility)
    }

    /**
     * Android 12 gives a collapsed custom view 48dp, measured on a device, and only to apps that
     * target it. A title and message fill that on their own, so the strip is dropped there and
     * the icons are the expanded view's to show; an app that still has the full height keeps both
     * together, which is why this follows what the app targets and not the device.
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `collapsed view drops the icon strip for text where the 48dp row applies`() {
        targetAndroid12OrLater()
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null))

        val collapsed = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))
        val expanded = inflate(FiveIconBigContentView(context, renderer(), data, Bundle()))

        assertEquals(View.GONE, iconRow(collapsed).visibility)
        assertEquals(View.VISIBLE, collapsed.findViewById<View>(R.id.title).visibility)
        assertEquals(View.VISIBLE, collapsed.findViewById<View>(R.id.msg).visibility)
        assertEquals(View.VISIBLE, iconRow(expanded).visibility)
        assertEquals(dimen(R.dimen.five_cta_icon_row_full), iconRow(expanded).layoutParams.height)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `collapsed view binds no icons into a hidden strip`() {
        targetAndroid12OrLater()
        val withImages = payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null)
            .apply { for (i in 1..5) putString("pt_img$i", "https://example.invalid/$i.png") }
        val data = dataFrom(withImages)

        val collapsed = FiveIconSmallContentView(context, renderer(), data, Bundle())
        val expanded = FiveIconBigContentView(context, renderer(), data, Bundle())

        // Nothing was loaded into the hidden strip, so its bitmaps are not parcelled a second
        // time; the expanded view still loads all five, which is what the renderer's fallback on
        // too many failed images counts.
        assertEquals(0, collapsed.getUnloadedFiveIconsCount())
        assertEquals(5, expanded.getUnloadedFiveIconsCount())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `collapsed view keeps the icon strip for an icon only campaign where the 48dp row applies`() {
        targetAndroid12OrLater()
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))

        val collapsed = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))

        // Resized to the row rather than clipped by it, as the full strip was.
        assertEquals(View.VISIBLE, iconRow(collapsed).visibility)
        assertEquals(dimen(R.dimen.five_cta_icon_row_compact), iconRow(collapsed).layoutParams.height)
    }

    @Test
    fun `collapsed view keeps text and icons together for an app that does not target Android 12`() {
        val data = dataFrom(payload(ptTitle = "PT Title", ptMsg = "PT Message", ptSummary = null))

        val collapsed = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))

        assertEquals(View.VISIBLE, iconRow(collapsed).visibility)
        assertEquals(dimen(R.dimen.five_cta_icon_row_with_text), iconRow(collapsed).layoutParams.height)
    }

    @Test
    fun `icon only collapsed view keeps the full strip for an app that does not target Android 12`() {
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))

        val collapsed = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))

        // Regression: this case briefly shared the shorter strip, halving these icons.
        assertEquals(dimen(R.dimen.five_cta_icon_row_full), iconRow(collapsed).layoutParams.height)
    }

    /**
     * The strip an icon only campaign keeps has to fit the 48dp row with its icons whole, which
     * the full strip did not. This lays the real thing out under that constraint.
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU], qualifiers = "sw400dp")
    fun `icon only collapsed view fills the 48dp row without clipping its icons`() {
        targetAndroid12OrLater()
        val data = dataFrom(payload(ptTitle = null, ptMsg = null, ptSummary = null))
        val root = inflate(FiveIconSmallContentView(context, renderer(), data, Bundle()))

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
        // On the widest bucket the icons get 48dp less their 8dp margins; narrower screens use a
        // smaller margin and so get more. The row itself has no padding from Android 12 up.
        assertTrue("icons measured ${px(iconHeight)}dp", iconHeight >= dp(30))
    }

    /** The 48dp row applies to apps targeting Android 12+, which Robolectric does not by default. */
    private fun targetAndroid12OrLater() {
        context.applicationInfo.targetSdkVersion = Build.VERSION_CODES.TIRAMISU
    }

    private fun iconRow(root: View) = root.findViewById<View>(R.id.five_cta_icon_row)

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private fun px(value: Int) = value / context.resources.displayMetrics.density

    private fun dimen(id: Int) = context.resources.getDimensionPixelSize(id)

    private fun inflate(view: ContentView): View = view.remoteView.apply(context, FrameLayout(context))

    private fun renderer() = TemplateRenderer(context, Bundle()).apply { notificationId = 1 }

    private fun dataFrom(extras: Bundle) = TemplateDataFactory.createTemplateData(
        TemplateType.FIVE_ICONS, extras, false, "alt"
    ) { arrayListOf() } as FiveIconsTemplateData

    // No pt_img keys: the icons stay hidden and nothing is downloaded; only the text row is under test.
    private fun payload(ptTitle: String?, ptMsg: String?, ptSummary: String?, wzrkNms: String? = null) = Bundle().apply {
        putString("pt_id", "pt_five_icons")
        putString("nt", "base title")
        putString("nm", "base message")
        wzrkNms?.let { putString("wzrk_nms", it) }
        ptTitle?.let { putString("pt_title", it) }
        ptMsg?.let { putString("pt_msg", it) }
        ptSummary?.let { putString("pt_msg_summary", it) }
        for (i in 1..3) putString("pt_dl$i", "myapp://s$i")
    }

    private companion object {

        // Measured on an Android 15 emulator with an app targeting 36: the row the system lays a
        // collapsed custom view out in, and the width it left the view at 480dpi.
        const val COLLAPSED_ROW_DP = 48
        const val COLLAPSED_ROW_WIDTH_DP = 287
    }
}
