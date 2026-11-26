package com.clevertap.android.pushtemplates.styles

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.BaseTextData
import com.clevertap.android.pushtemplates.ImageData
import com.clevertap.android.pushtemplates.InputBoxTemplateData
import com.clevertap.android.pushtemplates.NotificationBehavior
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.media.TemplateMediaManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The input box notification shows its image as a big picture, and falls back to big text when
 * there is no http image or it cannot be fetched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class InputBoxStyleTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private val mediaManager = mockk<TemplateMediaManager>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `http image that loads is shown as a big picture with the message summary`() {
        every { mediaManager.getNotificationBitmap(IMAGE_URL, false, any()) } returns bitmap()

        val extras = build(imageUrl = IMAGE_URL, messageSummary = "summary")

        assertNotNull(extras.getParcelable<Bitmap>(NotificationCompat.EXTRA_PICTURE))
        assertEquals("summary", extras.getCharSequence(NotificationCompat.EXTRA_SUMMARY_TEXT).toString())
        assertNull(extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT))
    }

    @Test
    fun `big picture summary falls back to the message`() {
        every { mediaManager.getNotificationBitmap(IMAGE_URL, false, any()) } returns bitmap()

        val extras = build(imageUrl = IMAGE_URL, messageSummary = null)

        assertNotNull(extras.getParcelable<Bitmap>(NotificationCompat.EXTRA_PICTURE))
        assertEquals(MESSAGE, extras.getCharSequence(NotificationCompat.EXTRA_SUMMARY_TEXT).toString())
    }

    @Test
    fun `http image that fails to load falls back to big text`() {
        every { mediaManager.getNotificationBitmap(IMAGE_URL, false, any()) } returns null

        val extras = build(imageUrl = IMAGE_URL, messageSummary = "summary")

        assertFalse(extras.containsKey(NotificationCompat.EXTRA_PICTURE))
        assertEquals(MESSAGE, extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT).toString())
    }

    @Test
    fun `non http image is not fetched and shows big text`() {
        val extras = build(imageUrl = "file://image.png", messageSummary = "summary")

        assertFalse(extras.containsKey(NotificationCompat.EXTRA_PICTURE))
        assertEquals(MESSAGE, extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT).toString())
        verify(exactly = 0) { mediaManager.getNotificationBitmap(any(), any(), any()) }
    }

    @Test
    fun `missing image shows big text`() {
        val extras = build(imageUrl = null, messageSummary = "summary")

        assertFalse(extras.containsKey(NotificationCompat.EXTRA_PICTURE))
        assertEquals(MESSAGE, extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT).toString())
    }

    private fun bitmap(): Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

    private fun build(imageUrl: String?, messageSummary: String?): Bundle {
        val data = InputBoxTemplateData(
            textData = BaseTextData(title = "title", message = MESSAGE, messageSummary = messageSummary),
            deepLinkList = arrayListOf(),
            imageData = ImageData(url = imageUrl, altText = "alt"),
            notificationBehavior = NotificationBehavior()
        )
        val renderer = spyk(TemplateRenderer(context, Bundle())).also {
            it.smallIcon = android.R.drawable.ic_dialog_info
            every { it.templateMediaManager } returns mediaManager
        }
        val builder = InputBoxStyle(data, renderer)
            .builderFromStyle(context, Bundle(), 1, NotificationCompat.Builder(context, "test_channel"))
        return builder.build().extras
    }

    private companion object {
        const val IMAGE_URL = "https://example.com/image.png"
        const val MESSAGE = "message"
    }
}
