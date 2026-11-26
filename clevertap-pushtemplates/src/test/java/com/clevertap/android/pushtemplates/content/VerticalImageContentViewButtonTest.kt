package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.clevertap.android.pushtemplates.BaseColorData
import com.clevertap.android.pushtemplates.BaseContent
import com.clevertap.android.pushtemplates.BaseTextData
import com.clevertap.android.pushtemplates.ButtonStyle
import com.clevertap.android.pushtemplates.GifData
import com.clevertap.android.pushtemplates.IconData
import com.clevertap.android.pushtemplates.ImageData
import com.clevertap.android.pushtemplates.MediaData
import com.clevertap.android.pushtemplates.NotificationBehavior
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.VerticalImageButtonData
import com.clevertap.android.pushtemplates.VerticalImageTemplateData
import com.clevertap.android.sdk.pushnotification.LaunchPendingIntentFactory
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The vertical image button shows its label and gets a background bitmap only when the colors its
 * style needs are valid.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class VerticalImageContentViewButtonTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        context.applicationInfo.nonLocalizedLabel = "Test App"
        mockkStatic(LaunchPendingIntentFactory::class)
        every { LaunchPendingIntentFactory.getLaunchPendingIntent(any(), any()) } returns
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        every { LaunchPendingIntentFactory.getActivityIntent(any(), any()) } returns
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `no button data leaves the button hidden`() {
        val view = inflate(button = null)

        assertEquals(View.GONE, view.findViewById<View>(R.id.vertical_img_btn_frame).visibility)
    }

    @Test
    fun `solid button with a valid color gets a background`() {
        val view = inflate(VerticalImageButtonData(name = "Buy", style = ButtonStyle.SOLID, buttonColor = "#FF0000"))

        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.vertical_img_btn_frame).visibility)
        assertEquals("Buy", view.findViewById<TextView>(R.id.vertical_img_btn).text.toString())
        assertTrue(hasBackground(view))
    }

    @Test
    fun `solid button with an invalid color gets no background`() {
        val view = inflate(VerticalImageButtonData(name = "Buy", style = ButtonStyle.SOLID, buttonColor = "nope"))

        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.vertical_img_btn_frame).visibility)
        assertFalse(hasBackground(view))
    }

    @Test
    fun `solid button without a color gets no background`() {
        assertFalse(hasBackground(inflate(VerticalImageButtonData(name = "Buy", style = ButtonStyle.SOLID))))
    }

    @Test
    fun `linear gradient button with both colors gets a background`() {
        val button = VerticalImageButtonData(
            name = "Buy",
            style = ButtonStyle.GRADIENT_LINEAR,
            gradientColor1 = "#FF0000",
            gradientColor2 = "#0000FF",
            borderColor = "#00FF00"
        )

        assertTrue(hasBackground(inflate(button)))
    }

    @Test
    fun `linear gradient button missing a color gets no background`() {
        val button = VerticalImageButtonData(
            name = "Buy",
            style = ButtonStyle.GRADIENT_LINEAR,
            gradientColor1 = "#FF0000"
        )

        assertFalse(hasBackground(inflate(button)))
    }

    @Test
    fun `radial gradient button with both colors gets a background`() {
        val button = VerticalImageButtonData(
            name = "Buy",
            style = ButtonStyle.GRADIENT_RADIAL,
            gradientColor1 = "#FF0000",
            gradientColor2 = "#0000FF"
        )

        assertTrue(hasBackground(inflate(button)))
    }

    @Test
    fun `radial gradient button with an invalid color gets no background`() {
        val button = VerticalImageButtonData(
            name = "Buy",
            style = ButtonStyle.GRADIENT_RADIAL,
            gradientColor1 = "#FF0000",
            gradientColor2 = "nope"
        )

        assertFalse(hasBackground(inflate(button)))
    }

    private fun hasBackground(view: View): Boolean =
        view.findViewById<ImageView>(R.id.vertical_img_btn_bg).drawable is BitmapDrawable

    private fun inflate(button: VerticalImageButtonData?): View {
        val renderer = TemplateRenderer(context, Bundle()).apply { notificationId = 1 }
        val contentView = VerticalImageBigContentView(context, renderer, data(button), Bundle())
        return contentView.remoteView.apply(context, FrameLayout(context))
    }

    // No image URLs, so nothing is downloaded.
    private fun data(button: VerticalImageButtonData?) = VerticalImageTemplateData(
        baseContent = BaseContent(
            textData = BaseTextData(title = "title", message = "message"),
            colorData = BaseColorData(),
            iconData = IconData(),
            deepLinkList = arrayListOf(),
            notificationBehavior = NotificationBehavior()
        ),
        mediaData = MediaData(bigImage = ImageData(altText = "alt"), gif = GifData()),
        collapsedMediaData = null,
        buttonData = button
    )
}
