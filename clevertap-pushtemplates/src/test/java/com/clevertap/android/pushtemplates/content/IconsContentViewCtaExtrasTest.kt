package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.clevertap.android.pushtemplates.BaseColorData
import com.clevertap.android.pushtemplates.BaseContent
import com.clevertap.android.pushtemplates.BaseTextData
import com.clevertap.android.pushtemplates.IconsTemplateData
import com.clevertap.android.pushtemplates.IconData
import com.clevertap.android.pushtemplates.NotificationBehavior
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Constants
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
 * The SDK does not dismiss on an icon tap, so every icon bundle must carry the actionId,
 * autoCancel and notificationId extras the app's dismiss handler reads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class IconsContentViewCtaExtrasTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val notificationId = 4521
    private val capturedBundles = mutableListOf<Bundle>()

    @Before
    fun setUp() {
        // Robolectric's test app has no label for the header.
        context.applicationInfo.nonLocalizedLabel = "Test App"
        mockkStatic(LaunchPendingIntentFactory::class)
        every { LaunchPendingIntentFactory.getLaunchPendingIntent(any(), any()) } answers {
            capturedBundles.add(firstArg())
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `small view attaches actionId autoCancel and notificationId to all five icon bundles`() {
        IconsSmallContentView(context, rendererWith(notificationId), dataWith(fiveDeepLinks), Bundle())

        assertCtaExtras(fiveDeepLinks)
    }

    @Test
    fun `big view attaches actionId autoCancel and notificationId to all five icon bundles`() {
        IconsBigContentView(context, rendererWith(notificationId), dataWith(fiveDeepLinks), Bundle())

        assertCtaExtras(fiveDeepLinks)
    }

    @Test
    fun `four deep links wire exactly four icons in both views`() {
        IconsSmallContentView(context, rendererWith(notificationId), dataWith(fourDeepLinks), Bundle())
        assertCtaExtras(fourDeepLinks)

        capturedBundles.clear()
        IconsBigContentView(context, rendererWith(notificationId), dataWith(fourDeepLinks), Bundle())
        assertCtaExtras(fourDeepLinks)
    }

    @Test
    fun `small view only builds bundles for the icons that have a deep link`() {
        IconsSmallContentView(context, rendererWith(notificationId), dataWith(threeDeepLinks), Bundle())

        assertCtaExtras(threeDeepLinks)
    }

    @Test
    fun `big view only builds bundles for the icons that have a deep link`() {
        IconsBigContentView(context, rendererWith(notificationId), dataWith(threeDeepLinks), Bundle())

        assertCtaExtras(threeDeepLinks)
    }

    private fun assertCtaExtras(deepLinks: List<String>) {
        assertEquals(deepLinks.size, capturedBundles.size)
        capturedBundles.forEachIndexed { index, bundle ->
            val icon = index + 1
            assertEquals("cta$icon", bundle.getString(PTConstants.PT_ACTION_ID))
            assertTrue(bundle.getBoolean(PTConstants.PT_AUTO_CANCEL, false))
            assertEquals(notificationId, bundle.getInt(PTConstants.PT_NOTIF_ID, -1))
            assertTrue(bundle.getBoolean("cta$icon", false))
            assertEquals(deepLinks[index], bundle.getString(Constants.DEEP_LINK_KEY))
        }
    }

    private fun rendererWith(notificationId: Int) = TemplateRenderer(context, Bundle()).apply {
        this.notificationId = notificationId
    }

    // No images, so nothing is downloaded.
    private fun dataWith(deepLinks: List<String>) = IconsTemplateData(
        baseContent = BaseContent(
            textData = BaseTextData(title = "title", message = "message"),
            colorData = BaseColorData(),
            iconData = IconData(),
            deepLinkList = ArrayList(deepLinks),
            notificationBehavior = NotificationBehavior()
        ),
        imageList = arrayListOf(),
        iconTextData = BaseTextData(title = "title", message = "message")
    )

    private companion object {
        val fiveDeepLinks = (1..5).map { "myapp://screen$it" }
        val fourDeepLinks = fiveDeepLinks.take(4)
        val threeDeepLinks = fiveDeepLinks.take(3)
    }
}
