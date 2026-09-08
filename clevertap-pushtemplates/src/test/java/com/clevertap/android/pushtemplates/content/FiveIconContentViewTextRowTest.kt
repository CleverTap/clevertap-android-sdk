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
import org.junit.Assert.assertNull
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
        assertNull(small.findViewById<View>(R.id.rel_lyt))   // icon-only layout has no text block
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
}
