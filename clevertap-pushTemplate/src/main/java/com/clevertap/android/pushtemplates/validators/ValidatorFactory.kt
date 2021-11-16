package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.pushtemplates.TemplateType.AUTO_CAROUSEL
import com.clevertap.android.pushtemplates.TemplateType.BASIC
import com.clevertap.android.pushtemplates.TemplateType.FIVE_ICONS
import com.clevertap.android.pushtemplates.TemplateType.INPUT_BOX
import com.clevertap.android.pushtemplates.TemplateType.MANUAL_CAROUSEL
import com.clevertap.android.pushtemplates.TemplateType.PRODUCT_DISPLAY
import com.clevertap.android.pushtemplates.TemplateType.RATING
import com.clevertap.android.pushtemplates.TemplateType.TIMER
import com.clevertap.android.pushtemplates.TemplateType.ZERO_BEZEL
import com.clevertap.android.pushtemplates.checkers.Checker
import com.clevertap.android.pushtemplates.checkers.IntSizeChecker
import com.clevertap.android.pushtemplates.checkers.JsonArraySizeChecker
import com.clevertap.android.pushtemplates.checkers.ListSizeChecker
import com.clevertap.android.pushtemplates.checkers.StringSizeChecker

const val PT_TITLE = "PT_TITLE"
const val PT_MSG = "PT_MSG"
const val PT_BG = "PT_BG"
const val PT_DEEPLINK_LIST = "PT_DEEPLINK_LIST"
const val PT_THREE_IMAGE_LIST = "PT_IMAGE_LIST"
const val PT_RATING_DEFAULT_DL = "PT_RATING_DEFAULT_DL"
const val PT_FIVE_DEEPLINK_LIST = "PT_FIVE_DEEPLINK_LIST"
const val PT_FIVE_IMAGE_LIST = "PT_FIVE_IMAGE_LIST"
const val PT_THREE_DEEPLINK_LIST = "PT_THREE_DEEPLINK_LIST"
const val PT_BIG_TEXT_LIST = "PT_BIG_TEXT_LIST"
const val PT_SMALL_TEXT_LIST = "PT_SMALL_TEXT_LIST"
const val PT_PRODUCT_DISPLAY_ACTION = "PT_PRODUCT_DISPLAY_ACTION"
const val PT_PRODUCT_DISPLAY_ACTION_CLR = "PT_PRODUCT_DISPLAY_ACTION_CLR"
const val PT_BIG_IMG = "PT_BIG_IMG"
const val PT_TIMER_THRESHOLD = "PT_TIMER_THRESHOLD"
const val PT_TIMER_END = "PT_TIMER_END"
const val PT_INPUT_FEEDBACK = "PT_INPUT_FEEDBACK"
const val PT_ACTIONS = "PT_ACTIONS"

fun Iterable<Checker<out Any>>.and(): Boolean {
    var and: Boolean = true
    for (element in this) {
        and =
            element.check() && and // checking first will allow us to execute all checks(for printing errors) instead of short circuiting
    }
    return and
}

internal class ValidatorFactory {
    companion object{

        private lateinit var keys: Map<String, Checker<out Any>>

        fun getValidator(templateType: TemplateType, templateRenderer: TemplateRenderer): Validator? {
            keys = createKeysMap(templateRenderer)

            return when (templateType) {
                BASIC -> BasicTemplateValidator(ContentValidator(keys))
                AUTO_CAROUSEL, MANUAL_CAROUSEL -> CarouselTemplateValidator(BasicTemplateValidator(ContentValidator(keys)))
                RATING -> RatingTemplateValidator(BasicTemplateValidator(ContentValidator(keys)))
                FIVE_ICONS -> FiveIconsTemplateValidator(BackgroundValidator(keys))
                PRODUCT_DISPLAY -> ProductDisplayTemplateValidator(BasicTemplateValidator(ContentValidator(keys)))
                ZERO_BEZEL -> ZeroBezelTemplateValidator(ContentValidator(keys))
                TIMER -> TimerTemplateValidator(BasicTemplateValidator(ContentValidator(keys)))
                INPUT_BOX -> InputBoxTemplateValidator(ContentValidator(keys))
                else -> null
            }

        }

        fun createKeysMap(templateRenderer: TemplateRenderer): Map<String, Checker<out Any>> {
            val hashMap = HashMap<String, Checker<out Any>>()
            //----------BASIC-------------
            hashMap[PT_TITLE] = StringSizeChecker(templateRenderer.pt_title, 0, "Title is missing or empty")
            hashMap[PT_MSG] = StringSizeChecker(templateRenderer.pt_msg, 0, "Message is missing or empty")
            hashMap[PT_BG] = StringSizeChecker(templateRenderer.pt_bg, 0, "Background colour is missing or empty")
            //----------CAROUSEL-------------
            hashMap[PT_DEEPLINK_LIST] = ListSizeChecker(templateRenderer.deepLinkList, 1, "Deeplink is missing or empty")
            hashMap[PT_THREE_IMAGE_LIST] =
                ListSizeChecker(templateRenderer.imageList, 3, "Three required images not present")
            //----------RATING-------------
            hashMap[PT_RATING_DEFAULT_DL] =
                StringSizeChecker(templateRenderer.pt_rating_default_dl, 0, "Default deeplink is missing or empty")
            //----------FIVE ICON-------------
            hashMap[PT_FIVE_DEEPLINK_LIST] =
                ListSizeChecker(templateRenderer.deepLinkList, 5, "Five required deeplinks not present")
            hashMap[PT_FIVE_IMAGE_LIST] =
                ListSizeChecker(templateRenderer.imageList, 5, "Five required images not present")
            //----------PROD DISPLAY-------------
            hashMap[PT_THREE_DEEPLINK_LIST] =
                ListSizeChecker(templateRenderer.deepLinkList, 3, "Three required deeplinks not present")
            hashMap[PT_BIG_TEXT_LIST] =
                ListSizeChecker(templateRenderer.bigTextList, 3, "Three required product titles not present")
            hashMap[PT_SMALL_TEXT_LIST] =
                ListSizeChecker(templateRenderer.smallTextList, 3, "Three required product descriptions not present")
            hashMap[PT_PRODUCT_DISPLAY_ACTION] =
                StringSizeChecker(templateRenderer.pt_product_display_action, 0, "Button label is missing or empty")
            hashMap[PT_PRODUCT_DISPLAY_ACTION_CLR] =
                StringSizeChecker(templateRenderer.pt_product_display_action_clr, 0, "Button colour is missing or empty")
            //----------ZERO BEZEL----------------
            hashMap[PT_BIG_IMG] = StringSizeChecker(templateRenderer.pt_big_img, 0, "Display Image is missing or empty")
            //----------TIMER----------------
            hashMap[PT_TIMER_THRESHOLD] =
                IntSizeChecker(templateRenderer.pt_timer_threshold, -1, "Timer Threshold or End time not defined")
            hashMap[PT_TIMER_END] =
                IntSizeChecker(templateRenderer.pt_timer_end, -1, "Timer Threshold or End time not defined")
            //----------INPUT BOX----------------
            hashMap[PT_INPUT_FEEDBACK] =
                StringSizeChecker(templateRenderer.pt_input_feedback, 0, "Feedback Text or Actions is missing or empty")
            hashMap[PT_ACTIONS] =
                JsonArraySizeChecker(templateRenderer.actions, 0, "Feedback Text or Actions is missing or empty")

            return hashMap
        }
    }
}