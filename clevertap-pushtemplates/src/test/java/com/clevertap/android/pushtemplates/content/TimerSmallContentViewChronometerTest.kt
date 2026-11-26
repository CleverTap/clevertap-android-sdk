package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.clevertap.android.pushtemplates.TimerTemplateData
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
 * The timer chronometer gets a visible background bitmap only when the colors its style needs are valid.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TimerSmallContentViewChronometerTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        context.applicationInfo.nonLocalizedLabel = "Test App"
    }

    @Test
    fun `solid style with a valid background color shows the background`() {
        assertBackgroundShown(inflate(style = ButtonStyle.SOLID, bgColor = "#FF0000", borderColor = "#00FF00"))
    }

    @Test
    fun `solid style with an invalid background color shows no background`() {
        assertNoBackground(inflate(style = ButtonStyle.SOLID, bgColor = "nope"))
    }

    @Test
    fun `solid style without a background color shows no background`() {
        assertNoBackground(inflate(style = ButtonStyle.SOLID))
    }

    @Test
    fun `linear gradient with both colors shows the background`() {
        assertBackgroundShown(
            inflate(style = ButtonStyle.GRADIENT_LINEAR, gradientColor1 = "#FF0000", gradientColor2 = "#0000FF")
        )
    }

    @Test
    fun `linear gradient with an invalid first color shows no background`() {
        assertNoBackground(
            inflate(style = ButtonStyle.GRADIENT_LINEAR, gradientColor1 = "nope", gradientColor2 = "#0000FF")
        )
    }

    @Test
    fun `radial gradient with both colors shows the background`() {
        assertBackgroundShown(
            inflate(style = ButtonStyle.GRADIENT_RADIAL, gradientColor1 = "#FF0000", gradientColor2 = "#0000FF")
        )
    }

    @Test
    fun `radial gradient missing the second color shows no background`() {
        assertNoBackground(inflate(style = ButtonStyle.GRADIENT_RADIAL, gradientColor1 = "#FF0000"))
    }

    private fun assertBackgroundShown(view: View) {
        val background = view.findViewById<ImageView>(R.id.chronometer_bg)
        assertEquals(View.VISIBLE, background.visibility)
        assertTrue(background.drawable is BitmapDrawable)
    }

    private fun assertNoBackground(view: View) {
        assertFalse(view.findViewById<ImageView>(R.id.chronometer_bg).drawable is BitmapDrawable)
    }

    private fun inflate(
        style: ButtonStyle,
        bgColor: String? = null,
        borderColor: String? = null,
        gradientColor1: String? = null,
        gradientColor2: String? = null
    ): View {
        val renderer = TemplateRenderer(context, Bundle()).apply { notificationId = 1 }
        val media = MediaData(bigImage = ImageData(altText = "alt"), gif = GifData())
        val data = TimerTemplateData(
            baseContent = BaseContent(
                textData = BaseTextData(title = "title", message = "message"),
                colorData = BaseColorData(),
                iconData = IconData(),
                deepLinkList = arrayListOf(),
                notificationBehavior = NotificationBehavior()
            ),
            mediaData = media,
            terminalTextData = BaseTextData(),
            terminalMediaData = media,
            chronometerBgColor = bgColor,
            chronometerBorderColor = borderColor,
            chronometerStyle = style,
            chronometerGradientColor1 = gradientColor1,
            chronometerGradientColor2 = gradientColor2
        )
        val contentView = TimerSmallContentView(context, 60_000L, renderer, data)
        return contentView.remoteView.apply(context, FrameLayout(context))
    }
}
