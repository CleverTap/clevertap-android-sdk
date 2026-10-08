package com.clevertap.android.pushtemplates.styles

import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.ProgressTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Exercises the pre-16 fallback rendering path (Robolectric runs below API 36 / has no
 * NotificationCompat.ProgressStyle on the classpath, so the reflective native path falls back).
 * Verifies both the segmented (dots/connectors) and the plain-bar branches build without error.
 */
@RunWith(RobolectricTestRunner::class)
class ProgressStyleTest {

    private val renderer = mockk<TemplateRenderer>()
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun setup() {
        every { renderer.getTitle(any(), any()) } returns "Order #1"
        every { renderer.getMessage(any()) } returns "Out for delivery"
        every { renderer.smallIcon } returns android.R.drawable.ic_dialog_info
        every { renderer.smallIconColour } returns "#FFFFFF"
        // PendingIntentFactory is left real — with the Robolectric context the optional content
        // intent is harmless; mocking an object fn with default args crashes mockk's recorder.
    }

    @After
    fun tearDown() = unmockkAll()

    private fun newBuilder() = NotificationCompat.Builder(context, "live_updates")

    @Test
    fun `segmented fallback builds a notification without crashing`() {
        val data = ProgressTemplateData(
            title = "Order #1", progress = 40, progressMax = 100, indeterminate = false,
            segments = listOf(ProgressPayloadParser.SegmentData(1, null)),
            points = listOf(ProgressPayloadParser.PointData(0, null, "Start"))
        )
        val nb = newBuilder()

        val result = ProgressStyle(data, renderer).builderFromStyle(context, Bundle(), 1, nb)

        assertEquals(nb, result)
        assertNotNull(result.build())
    }

    @Test
    fun `fallback leaves the collapsed view to the system template and sets only a custom expanded view`() {
        val data = ProgressTemplateData(
            title = "Order #1", progress = 40, progressMax = 100, indeterminate = false,
            segments = listOf(ProgressPayloadParser.SegmentData(1, null)),
            points = listOf(ProgressPayloadParser.PointData(0, null, "Start"))
        )
        val nb = newBuilder()

        val result = ProgressStyle(data, renderer).builderFromStyle(context, Bundle(), 1, nb)

        // No custom collapsed view -> the system's standard template (icon, title, time, message),
        // the same fields Android 16 shows for a collapsed native ProgressStyle.
        assertNull(result.contentView)
        assertNotNull(result.bigContentView)
        assertEquals("Order #1", result.build().extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString())
        assertEquals("Out for delivery", result.build().extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString())
    }

    private fun plainBarData() = ProgressTemplateData(
        title = "Order #1", progress = 40, progressMax = 100, indeterminate = false,
        segments = emptyList(), points = emptyList()
    )

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `fallback countdown chip shows a running countdown in the header`() {
        val whenMs = System.currentTimeMillis() + 600_000L
        val extras = Bundle().apply {
            putString("pt_chip_type", "countdown")
            putString("pt_when", whenMs.toString())
            putString("pt_countdown", "true")
        }

        val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, extras, 1, newBuilder()).build()

        assertEquals(whenMs, n.`when`)
        assertTrue(n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
        assertTrue(n.extras.getBoolean(NotificationCompat.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test
    fun `fallback countdown on Android 6 still shows a running timer without crashing`() {
        // setChronometerCountDown only exists from API 24; on API 23 the timer still runs.
        val whenMs = System.currentTimeMillis() + 600_000L
        val extras = Bundle().apply {
            putString("pt_chip_type", "countdown")
            putString("pt_when", whenMs.toString())
            putString("pt_countdown", "true")
        }

        val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, extras, 1, newBuilder()).build()

        assertEquals(whenMs, n.`when`)
        assertTrue(n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `fallback timer chip counts up when pt_countdown is not set`() {
        val whenMs = System.currentTimeMillis() - 60_000L
        val extras = Bundle().apply {
            putString("pt_chip_type", "timer")
            putString("pt_when", whenMs.toString())
        }

        val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, extras, 1, newBuilder()).build()

        assertEquals(whenMs, n.`when`)
        assertTrue(n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
        assertFalse(n.extras.getBoolean(NotificationCompat.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test
    fun `fallback does not start a timer for a text chip or a bad pt_when`() {
        val textChip = Bundle().apply {
            putString("pt_chip_type", "text")
            putString("pt_chip_text", "12 min")
            putString("pt_when", (System.currentTimeMillis() + 600_000L).toString())
        }
        val badWhen = Bundle().apply {
            putString("pt_chip_type", "countdown")
            putString("pt_when", "not-a-number")
        }

        for (extras in listOf(textChip, badWhen)) {
            val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, extras, 1, newBuilder()).build()
            assertFalse(n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `no running timer or countdown after the live update ends`() {
        val now = System.currentTimeMillis()
        val endedTimer = Bundle().apply {
            putString("pt_chip_type", "timer")
            putString("pt_when", (now - 300_000L).toString())
            putString("wzrk_la_event", "end")
        }
        val endedCountdown = Bundle().apply {
            putString("pt_chip_type", "countdown")
            putString("pt_when", (now + 240_000L).toString())
            putString("pt_countdown", "true")
            putString("wzrk_la_event", "end")
        }

        for (extras in listOf(endedTimer, endedCountdown)) {
            val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, extras, 1, newBuilder()).build()
            assertFalse(n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `a countdown that is past or has under 10 seconds left is not started`() {
        val now = System.currentTimeMillis()
        for (whenMs in listOf(now - 60_000L, now + 5_000L)) {
            val extras = Bundle().apply {
                putString("pt_chip_type", "countdown")
                putString("pt_when", whenMs.toString())
                putString("pt_countdown", "true")
            }
            val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, extras, 1, newBuilder()).build()
            assertFalse(n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `a countdown with a minute left still runs`() {
        val whenMs = System.currentTimeMillis() + 60_000L
        val extras = Bundle().apply {
            putString("pt_chip_type", "countdown")
            putString("pt_when", whenMs.toString())
            putString("pt_countdown", "true")
            putString("wzrk_la_event", "update")
        }

        val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, extras, 1, newBuilder()).build()

        assertEquals(whenMs, n.`when`)
        assertTrue(n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
        assertTrue(n.extras.getBoolean(NotificationCompat.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    private fun chipVisibility(extras: Bundle): Int {
        val nb = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, extras, 1, newBuilder())
        val view = nb.bigContentView!!.apply(context, android.widget.FrameLayout(context))
        return view.findViewById<android.view.View>(com.clevertap.android.pushtemplates.R.id.pt_chip).visibility
    }

    @Test
    fun `fallback shows chip text only for chip type text`() {
        val text = Bundle().apply {
            putString("pt_chip_type", "text")
            putString("pt_chip_text", "12 min")
        }
        assertEquals(android.view.View.VISIBLE, chipVisibility(text))

        for (type in listOf("none", "timer", "countdown", null)) {
            val extras = Bundle().apply {
                type?.let { putString("pt_chip_type", it) }
                putString("pt_chip_text", "12 min")
            }
            assertEquals("type=$type", android.view.View.GONE, chipVisibility(extras))
        }
    }

    @Test
    fun `fallback shows start and end icons beside both the plain bar and the milestone bar`() {
        val media = mockk<com.clevertap.android.pushtemplates.media.TemplateMediaManager>()
        every { media.getNotificationBitmap(any(), any(), any()) } returns
            android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
        every { renderer.templateMediaManager } returns media
        val extras = Bundle().apply {
            putString("pt_progress_start_icon", "https://example.com/start.png")
            putString("pt_progress_end_icon", "https://example.com/end.png")
        }
        val milestone = ProgressTemplateData(
            title = "Order #1", progress = 1, progressMax = null, indeterminate = false,
            segments = listOf(ProgressPayloadParser.SegmentData(1, null), ProgressPayloadParser.SegmentData(1, null)),
            points = emptyList()
        )

        for (data in listOf(plainBarData(), milestone)) {
            val nb = ProgressStyle(data, renderer).builderFromStyle(context, extras, 1, newBuilder())
            val view = nb.bigContentView!!.apply(context, android.widget.FrameLayout(context))
            assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(com.clevertap.android.pushtemplates.R.id.pt_start_icon).visibility)
            assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(com.clevertap.android.pushtemplates.R.id.pt_end_icon).visibility)
        }
    }

    @Test
    fun `large icon is set on the builder for the system to draw, without cropping`() {
        val media = mockk<com.clevertap.android.pushtemplates.media.TemplateMediaManager>()
        // A wide 8x4 icon: it must reach the system as is (the progress icons are square-cropped).
        every { media.getNotificationBitmap(any(), any(), any()) } returns
            android.graphics.Bitmap.createBitmap(8, 4, android.graphics.Bitmap.Config.ARGB_8888)
        every { renderer.templateMediaManager } returns media
        val withIcon = plainBarData().copy(largeIcon = "https://example.com/logo.png")

        val icon = ProgressStyle(withIcon, renderer).builderFromStyle(context, Bundle(), 1, newBuilder()).build().getLargeIcon()

        assertNotNull(icon)
        val drawable = icon!!.loadDrawable(context)!!
        assertEquals(2 * drawable.intrinsicHeight, drawable.intrinsicWidth)
    }

    @Test
    fun `no large icon when pt_ico is not sent`() {
        val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertNull(n.getLargeIcon())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `pt_dismiss sets the native timeout so Android removes the card`() {
        val data = plainBarData().copy(dismissAfter = 3_600_000L)

        val n = ProgressStyle(data, renderer).builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertEquals(3_600_000L, n.timeoutAfter)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `no timeout when pt_dismiss is not sent`() {
        val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertEquals(0L, n.timeoutAfter)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `pt_dismiss below Android 8 is ignored without crashing`() {
        val data = plainBarData().copy(dismissAfter = 3_600_000L)

        assertNotNull(ProgressStyle(data, renderer).builderFromStyle(context, Bundle(), 1, newBuilder()).build())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun `subtitle is set as the sub text below Android 12 too, since the header is the system's`() {
        val n = ProgressStyle(plainBarData().copy(subtitle = "Burger Palace"), renderer)
            .builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertEquals("Burger Palace", n.extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT).toString())
    }

    @Test
    fun `subtitle is set as the sub text on the default test SDK`() {
        val n = ProgressStyle(plainBarData().copy(subtitle = "Burger Palace"), renderer)
            .builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertEquals("Burger Palace", n.extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT).toString())
    }

    @Test
    fun `no sub text when pt_subtitle is not sent`() {
        val n = ProgressStyle(plainBarData(), renderer).builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertNull(n.extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT))
    }

    @Test
    fun `fallback hides chip text after the live update ends`() {
        val extras = Bundle().apply {
            putString("pt_chip_type", "text")
            putString("pt_chip_text", "0 min")
            putString("wzrk_la_event", "end")
        }
        assertEquals(android.view.View.GONE, chipVisibility(extras))
    }

    @Test
    fun `plain indeterminate bar fallback builds a notification without crashing`() {
        val data = ProgressTemplateData(
            title = "Order #1", progress = null, progressMax = null, indeterminate = true,
            segments = emptyList(), points = emptyList()
        )
        val nb = newBuilder()

        val result = ProgressStyle(data, renderer).builderFromStyle(context, Bundle(), 1, nb)

        assertEquals(nb, result)
        assertNotNull(result.build())
    }
}
