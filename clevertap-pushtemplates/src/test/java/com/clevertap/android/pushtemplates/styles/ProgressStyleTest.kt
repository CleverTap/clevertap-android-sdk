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
