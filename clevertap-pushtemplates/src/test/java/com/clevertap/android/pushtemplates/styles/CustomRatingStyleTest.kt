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
import com.clevertap.android.pushtemplates.CustomRatingTemplateData
import com.clevertap.android.pushtemplates.GifData
import com.clevertap.android.pushtemplates.IconData
import com.clevertap.android.pushtemplates.ImageBorderData
import com.clevertap.android.pushtemplates.ImageData
import com.clevertap.android.pushtemplates.MediaData
import com.clevertap.android.pushtemplates.NotificationBehavior
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.RatingCtaData
import com.clevertap.android.pushtemplates.RatingStyleType
import com.clevertap.android.pushtemplates.TemplateRenderer
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the intent contract of CustomRatingStyle: the body tap content intent is always present,
 * and no dismiss intent is set so the OS handles the swipe-away on its own (FR-AND-04).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class CustomRatingStyleTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `dismiss intent is null so swiping the notification does not fire an event`() {
        val notification = styleWith(defaultDeepLink = "myapp://home")
            .builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertNull(notification.deleteIntent)
    }

    @Test
    fun `body tap produces a content intent so there is always something to open`() {
        val notification = styleWith(defaultDeepLink = "myapp://home")
            .builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertNotNull(notification.contentIntent)
    }

    @Test
    fun `body tap intent is still set when there is no default deep link`() {
        // A null destination is valid: the receiver will open the launcher activity instead.
        val notification = styleWith(defaultDeepLink = null)
            .builderFromStyle(context, Bundle(), 1, newBuilder()).build()

        assertNotNull(notification.contentIntent)
    }

    private fun newBuilder() = NotificationCompat.Builder(context, "test_channel")

    private fun styleWith(defaultDeepLink: String?): CustomRatingStyle {
        val data = CustomRatingTemplateData(
            baseContent = BaseContent(
                textData = BaseTextData(title = "Rate us", message = "How did we do?"),
                colorData = BaseColorData(),
                iconData = IconData(),
                deepLinkList = arrayListOf(),
                notificationBehavior = NotificationBehavior()
            ),
            mediaData = MediaData(
                bigImage = ImageData(url = null, altText = ""),
                gif = GifData(url = null),
                scaleType = PTScaleType.CENTER_CROP,
                imageBorderData = ImageBorderData()
            ),
            defaultDeepLink = defaultDeepLink,
            ratingStyle = RatingStyleType.ICON,
            ratingCount = 3,
            ctaData = RatingCtaData()
        )
        val renderer = TemplateRenderer(context, Bundle()).apply {
            smallIcon = android.R.drawable.ic_dialog_info
        }
        return spyk(CustomRatingStyle(data, renderer, Bundle()), recordPrivateCalls = true).also { spy ->
            every { spy["makeSmallContentRemoteView"](any<Context>(), any<TemplateRenderer>()) } returns null as RemoteViews?
            every { spy["makeBigContentRemoteView"](any<Context>(), any<TemplateRenderer>()) } returns null as RemoteViews?
        }
    }
}
