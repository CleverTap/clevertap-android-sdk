package com.clevertap.android.pushtemplates

import android.os.Build
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * pt_icons needs at least three icons and three deep links. Text is optional, so a payload with
 * no pt_title/pt_msg/pt_msg_summary must still pass.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class IconsTemplateValidatorTest {

    private fun makeData(
        imageCount: Int = 5,
        deepLinkCount: Int = 5,
        title: String? = null,
        message: String? = null,
        messageSummary: String? = null
    ): IconsTemplateData {
        val images = ArrayList((1..imageCount).map { ImageData(url = "https://example.com/icon$it.png", altText = "Icon $it") })
        val deepLinks = ArrayList((1..deepLinkCount).map { "https://example.com/dl$it" })
        return IconsTemplateData(
            baseContent = BaseContent(
                textData = BaseTextData(title = "nt title", message = "nm message"),
                colorData = BaseColorData(),
                iconData = IconData(),
                deepLinkList = deepLinks,
                notificationBehavior = NotificationBehavior()
            ),
            imageList = images,
            iconTextData = BaseTextData(title = title, message = message, messageSummary = messageSummary)
        )
    }

    private fun validate(data: IconsTemplateData): Boolean {
        val validator = ValidatorFactory.getValidator(data)
        assertNotNull(validator)
        return validator!!.validate()
    }

    @Test
    fun `validator passes with five icons and five deep links`() {
        assertTrue(validate(makeData(imageCount = 5, deepLinkCount = 5)))
    }

    @Test
    fun `validator passes with four icons and four deep links`() {
        assertTrue(validate(makeData(imageCount = 4, deepLinkCount = 4)))
    }

    @Test
    fun `validator passes with three icons and three deep links`() {
        assertTrue(validate(makeData(imageCount = 3, deepLinkCount = 3)))
    }

    @Test
    fun `validator fails with two icons`() {
        assertFalse(validate(makeData(imageCount = 2, deepLinkCount = 5)))
    }

    @Test
    fun `validator fails with no icons`() {
        assertFalse(validate(makeData(imageCount = 0, deepLinkCount = 5)))
    }

    @Test
    fun `validator fails with two deep links`() {
        assertFalse(validate(makeData(imageCount = 5, deepLinkCount = 2)))
    }

    @Test
    fun `validator fails with no deep links`() {
        assertFalse(validate(makeData(imageCount = 5, deepLinkCount = 0)))
    }

    @Test
    fun `validator passes an icon only payload with no pt text`() {
        assertTrue(validate(makeData(title = null, message = null, messageSummary = null)))
    }

    @Test
    fun `validator passes with pt text set`() {
        assertTrue(validate(makeData(title = "Title", message = "Message", messageSummary = "Summary")))
    }
}
