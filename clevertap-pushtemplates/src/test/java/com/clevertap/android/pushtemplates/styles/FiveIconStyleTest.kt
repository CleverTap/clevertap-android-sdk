package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.BaseColorData
import com.clevertap.android.pushtemplates.BaseContent
import com.clevertap.android.pushtemplates.BaseTextData
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.IconData
import com.clevertap.android.pushtemplates.NotificationBehavior
import com.clevertap.android.pushtemplates.TemplateRenderer
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The builder title is what the system shows when it stacks notifications into a group
 * summary, where our RemoteViews are not rendered. Five icons must feed it the pt_* text only,
 * otherwise the nt fallback (the base title, e.g. the iOS title) leaks into the summary
 * for an icon-only notification. pt_msg stands in for a campaign that sets no pt_title, which
 * would otherwise be a blank row in the summary.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class FiveIconStyleTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `builder title is unset when pt_title is absent even though base title has nt fallback`() {
        val style = styleWith(baseTitle = "IOS title", ptTitle = null)

        val notification = style.builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertNull(notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE))
    }

    @Test
    fun `builder title falls back to pt_msg when only it is set`() {
        val style = styleWith(baseTitle = "IOS title", ptTitle = null, ptMsg = "PT message")

        val notification = style.builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertEquals(
            "PT message",
            notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString()
        )
    }

    @Test
    fun `builder title is pt_title when both pt keys are set`() {
        val style = styleWith(baseTitle = "IOS title", ptTitle = "PT title", ptMsg = "PT message")

        val notification = style.builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertEquals(
            "PT title",
            notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString()
        )
    }

    @Test
    fun `builder title is pt_title when it is set`() {
        val style = styleWith(baseTitle = "PT title", ptTitle = "PT title")

        val notification = style.builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertEquals("PT title", notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString())
    }

    private fun newBuilder() = NotificationCompat.Builder(context, "test_channel")

    private fun styleWith(baseTitle: String, ptTitle: String?, ptMsg: String? = null): FiveIconStyle {
        val data = FiveIconsTemplateData(
            baseContent = BaseContent(
                textData = BaseTextData(title = baseTitle, message = "base message"),
                colorData = BaseColorData(),
                iconData = IconData(),
                deepLinkList = arrayListOf("dl1", "dl2", "dl3"),
                notificationBehavior = NotificationBehavior()
            ),
            imageList = arrayListOf(),
            iconTextData = BaseTextData(title = ptTitle, message = ptMsg)
        )
        val renderer = TemplateRenderer(context, Bundle()).apply {
            smallIcon = android.R.drawable.ic_dialog_info
        }
        // The content views load icon bitmaps over the network; they are not what this test covers.
        return spyk(FiveIconStyle(data, renderer, Bundle()), recordPrivateCalls = true).also { spy ->
            every { spy["makeSmallContentRemoteView"](any<Context>(), any<TemplateRenderer>()) } returns null as RemoteViews?
            every { spy["makeBigContentRemoteView"](any<Context>(), any<TemplateRenderer>()) } returns null as RemoteViews?
            every { spy["makePendingIntent"](any<Context>(), any<Bundle>(), any<Int>()) } returns null as PendingIntent?
        }
    }
}
