package com.clevertap.android.pushtemplates

import android.os.Build
import com.clevertap.android.pushtemplates.validators.ValidatorFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class VerticalImageTemplateValidatorTest {

    private val defaultAltText = "Alt Text"

    private fun makeData(
        title: String? = "Title",
        message: String? = "Message",
        bigImageUrl: String? = "https://example.com/image.jpg"
    ): VerticalImageTemplateData {
        return VerticalImageTemplateData(
            baseContent = BaseContent(
                textData = BaseTextData(title = title, message = message),
                colorData = BaseColorData(),
                iconData = IconData(),
                deepLinkList = arrayListOf(),
                notificationBehavior = NotificationBehavior()
            ),
            mediaData = MediaData(
                bigImage = ImageData(url = bigImageUrl, altText = defaultAltText),
                gif = GifData(url = null, numberOfFrames = 10),
                scaleType = PTScaleType.CENTER_CROP
            ),
            collapsedMediaData = null
        )
    }

    @Test
    fun `validator passes when title, message and bigImage are present`() {
        val validator = ValidatorFactory.getValidator(makeData())
        assertNotNull(validator)
        assertTrue(validator!!.validate())
    }

    @Test
    fun `validator fails when title is null`() {
        val validator = ValidatorFactory.getValidator(makeData(title = null))
        assertNotNull(validator)
        assertFalse(validator!!.validate())
    }

    @Test
    fun `validator fails when title is empty`() {
        val validator = ValidatorFactory.getValidator(makeData(title = ""))
        assertNotNull(validator)
        assertFalse(validator!!.validate())
    }

    @Test
    fun `validator fails when message is null`() {
        val validator = ValidatorFactory.getValidator(makeData(message = null))
        assertNotNull(validator)
        assertFalse(validator!!.validate())
    }

    @Test
    fun `validator fails when message is empty`() {
        val validator = ValidatorFactory.getValidator(makeData(message = ""))
        assertNotNull(validator)
        assertFalse(validator!!.validate())
    }

    @Test
    fun `validator fails when bigImage url is null`() {
        val validator = ValidatorFactory.getValidator(makeData(bigImageUrl = null))
        assertNotNull(validator)
        assertFalse(validator!!.validate())
    }

    @Test
    fun `validator fails when bigImage url is empty`() {
        val validator = ValidatorFactory.getValidator(makeData(bigImageUrl = ""))
        assertNotNull(validator)
        assertFalse(validator!!.validate())
    }

    @Test
    fun `ValidatorFactory returns non-null validator for VerticalImageTemplateData`() {
        val validator = ValidatorFactory.getValidator(makeData())
        assertNotNull(validator)
    }
}
