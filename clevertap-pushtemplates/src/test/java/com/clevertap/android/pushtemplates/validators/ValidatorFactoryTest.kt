package com.clevertap.android.pushtemplates.validators

import android.os.Build
import com.clevertap.android.pushtemplates.AutoCarouselTemplateData
import com.clevertap.android.pushtemplates.BaseColorData
import com.clevertap.android.pushtemplates.BaseContent
import com.clevertap.android.pushtemplates.BaseTextData
import com.clevertap.android.pushtemplates.CarouselData
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.GifData
import com.clevertap.android.pushtemplates.IconData
import com.clevertap.android.pushtemplates.ImageData
import com.clevertap.android.pushtemplates.InputBoxTemplateData
import com.clevertap.android.pushtemplates.ManualCarouselTemplateData
import com.clevertap.android.pushtemplates.MediaData
import com.clevertap.android.pushtemplates.NotificationBehavior
import com.clevertap.android.pushtemplates.ProductTemplateData
import com.clevertap.android.pushtemplates.TemplateData
import com.clevertap.android.pushtemplates.ZeroBezelTemplateData
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks that [ValidatorFactory] wires the checkers each RemoteViews template's validator needs:
 * a validator that is missing a checker throws, and a wrong checker accepts or rejects the wrong data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class ValidatorFactoryTest {

    private val validText = BaseTextData(title = "title", message = "message")

    private fun baseContent(
        textData: BaseTextData = validText,
        deepLinkCount: Int = 1
    ) = BaseContent(
        textData = textData,
        colorData = BaseColorData(),
        iconData = IconData(),
        deepLinkList = ArrayList((1..deepLinkCount).map { "https://example.com/dl$it" }),
        notificationBehavior = NotificationBehavior()
    )

    private fun images(count: Int) =
        ArrayList((1..count).map { ImageData(url = "https://example.com/img$it.png", altText = "Image $it") })

    private fun texts(count: Int, prefix: String) = ArrayList((1..count).map { "$prefix $it" })

    private fun media(url: String?) = MediaData(bigImage = ImageData(url = url, altText = "alt"), gif = GifData())

    private fun carouselData(imageCount: Int = 3, deepLinkCount: Int = 1, textData: BaseTextData = validText) =
        CarouselData(
            baseContent = baseContent(textData = textData, deepLinkCount = deepLinkCount),
            imageList = images(imageCount)
        )

    private fun product(
        imageCount: Int = 3,
        deepLinkCount: Int = 3,
        bigTextCount: Int = 3,
        smallTextCount: Int = 3,
        actionText: String? = "Buy"
    ) = ProductTemplateData(
        baseContent = baseContent(deepLinkCount = deepLinkCount),
        imageList = images(imageCount),
        bigTextList = texts(bigTextCount, "big"),
        smallTextList = texts(smallTextCount, "small"),
        priceList = texts(3, "price"),
        displayActionText = actionText
    )

    private fun inputBox(feedback: String? = "Thanks", actions: JSONArray? = JSONArray().put(JSONObject())) =
        InputBoxTemplateData(
            textData = validText,
            actions = actions,
            deepLinkList = arrayListOf("https://example.com/dl"),
            imageData = ImageData(altText = "alt"),
            inputFeedback = feedback,
            notificationBehavior = NotificationBehavior()
        )

    private fun zeroBezel(bigImageUrl: String? = "https://example.com/big.png", textData: BaseTextData = validText) =
        ZeroBezelTemplateData(
            baseContent = baseContent(textData = textData),
            mediaData = media(bigImageUrl),
            collapsedMediaData = media(null)
        )

    private fun validate(data: TemplateData): Boolean {
        val validator = ValidatorFactory.getValidator(data)
        assertNotNull(validator)
        return validator.validate()
    }

    @Test
    fun `auto carousel passes with text, a deep link and three images`() {
        assertTrue(validate(AutoCarouselTemplateData(carouselData = carouselData())))
    }

    @Test
    fun `auto carousel fails with two images`() {
        assertFalse(validate(AutoCarouselTemplateData(carouselData = carouselData(imageCount = 2))))
    }

    @Test
    fun `auto carousel fails without deep links`() {
        assertFalse(validate(AutoCarouselTemplateData(carouselData = carouselData(deepLinkCount = 0))))
    }

    @Test
    fun `auto carousel fails without a title`() {
        val data = carouselData(textData = BaseTextData(title = null, message = "message"))
        assertFalse(validate(AutoCarouselTemplateData(carouselData = data)))
    }

    @Test
    fun `manual carousel passes with text, a deep link and three images`() {
        assertTrue(validate(ManualCarouselTemplateData(carouselData = carouselData())))
    }

    @Test
    fun `manual carousel fails with two images`() {
        assertFalse(validate(ManualCarouselTemplateData(carouselData = carouselData(imageCount = 2))))
    }

    @Test
    fun `manual carousel fails without deep links`() {
        assertFalse(validate(ManualCarouselTemplateData(carouselData = carouselData(deepLinkCount = 0))))
    }

    @Test
    fun `five icons passes with three icons and deep links`() {
        val data = FiveIconsTemplateData(baseContent = baseContent(deepLinkCount = 3), imageList = images(3))
        assertTrue(validate(data))
    }

    @Test
    fun `five icons fails with two deep links`() {
        val data = FiveIconsTemplateData(baseContent = baseContent(deepLinkCount = 2), imageList = images(3))
        assertFalse(validate(data))
    }

    @Test
    fun `product display passes with exactly three of everything and an action`() {
        assertTrue(validate(product()))
    }

    @Test
    fun `product display fails with four images`() {
        assertFalse(validate(product(imageCount = 4)))
    }

    @Test
    fun `product display fails with two deep links`() {
        assertFalse(validate(product(deepLinkCount = 2)))
    }

    @Test
    fun `product display fails with two product titles`() {
        assertFalse(validate(product(bigTextCount = 2)))
    }

    @Test
    fun `product display fails with two product descriptions`() {
        assertFalse(validate(product(smallTextCount = 2)))
    }

    @Test
    fun `product display fails without an action label`() {
        assertFalse(validate(product(actionText = null)))
    }

    @Test
    fun `input box passes with feedback and actions`() {
        assertTrue(validate(inputBox()))
    }

    @Test
    fun `input box fails without feedback`() {
        assertFalse(validate(inputBox(feedback = null)))
    }

    @Test
    fun `input box fails without actions`() {
        assertFalse(validate(inputBox(actions = null)))
    }

    @Test
    fun `zero bezel passes with text and a big image`() {
        assertTrue(validate(zeroBezel()))
    }

    @Test
    fun `zero bezel fails without a big image`() {
        assertFalse(validate(zeroBezel(bigImageUrl = null)))
    }

    @Test
    fun `zero bezel fails without a message`() {
        assertFalse(validate(zeroBezel(textData = BaseTextData(title = "title", message = null))))
    }
}
