package com.clevertap.android.pushtemplates.styles

import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.ProgressTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

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
