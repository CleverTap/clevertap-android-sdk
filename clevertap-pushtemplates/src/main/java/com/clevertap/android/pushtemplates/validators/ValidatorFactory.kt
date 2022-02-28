package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.pushtemplates.TemplateType.*
import com.clevertap.android.pushtemplates.checkers.*

const val PT_TITLE = "PT_TITLE"
const val PT_MSG = "PT_MSG"
const val PT_BG = "PT_BG"
const val PT_DEEPLINK_LIST = "PT_DEEPLINK_LIST"
const val PT_THREE_IMAGE_LIST = "PT_IMAGE_LIST"
const val PT_PRODUCT_THREE_IMAGE_LIST = "PT_PRODUCT_THREE_IMAGE_LIST"
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
    companion object {

        private lateinit var keys: Map<String, Checker<out Any>>

        fun getValidator(
            templateType: TemplateType,
            templateRenderer: TemplateRenderer
        ): Validator? {
            keys = createKeysMap(templateRenderer)


            if (templateType == BASIC){
                return BasicTemplateValidator(ContentValidator(keys))
            }else if (templateType == AUTO_CAROUSEL || templateType == MANUAL_CAROUSEL){
                return CarouselTemplateValidator(
                    BasicTemplateValidator(
                        ContentValidator(
                            keys
                        )
                    )
                )
            }else if (templateType == RATING){
                return RatingTemplateValidator(BasicTemplateValidator(ContentValidator(keys)))
            }else if (templateType == FIVE_ICONS){
                return FiveIconsTemplateValidator(BackgroundValidator(keys))
            }else if (templateType == PRODUCT_DISPLAY){
                return ProductDisplayTemplateValidator(
                    BasicTemplateValidator(
                        ContentValidator(keys)
                    )
                )
            }else if (templateType == ZERO_BEZEL){
                return ZeroBezelTemplateValidator(ContentValidator(keys))
            }else if (templateType == TIMER){
                return when {
                    templateRenderer.pt_timer_threshold != -1 -> {
                        TimerTemplateValidator(BasicTemplateValidator(ContentValidator(keys)))
                    }
                    templateRenderer.pt_timer_end < System.currentTimeMillis() -> {
                        TimerEndTemplateValidator(BasicTemplateValidator(ContentValidator(keys)))
                    }
                    else -> {
                        PTLog.debug("Not rendering notification Timer threshold or Timer end value is required")
                        null
                    }
                }
            }else if (templateType == INPUT_BOX){
                return InputBoxTemplateValidator(ContentValidator(keys))
            }else{
                return null
            }
        }

        private fun createKeysMap(templateRenderer: TemplateRenderer): Map<String, Checker<out Any>> {
            val hashMap = HashMap<String, Checker<out Any>>()
            //----------BASIC-------------
            hashMap[PT_TITLE] =
                StringSizeChecker(templateRenderer.pt_title, 0, "Title is missing or empty")
            hashMap[PT_MSG] =
                StringSizeChecker(templateRenderer.pt_msg, 0, "Message is missing or empty")
            hashMap[PT_BG] = StringSizeChecker(
                templateRenderer.pt_bg,
                0,
                "Background colour is missing or empty"
            )
            //----------CAROUSEL-------------
            hashMap[PT_DEEPLINK_LIST] =
                ListSizeChecker(templateRenderer.deepLinkList, 1, "Deeplink is missing or empty")
            hashMap[PT_THREE_IMAGE_LIST] =
                ListSizeChecker(templateRenderer.imageList, 3, "Three required images not present")
            //----------RATING-------------
            hashMap[PT_RATING_DEFAULT_DL] =
                StringSizeChecker(
                    templateRenderer.pt_rating_default_dl,
                    0,
                    "Default deeplink is missing or empty"
                )
            //----------FIVE ICON-------------
            hashMap[PT_FIVE_DEEPLINK_LIST] =
                ListSizeChecker(
                    templateRenderer.deepLinkList,
                    3,
                    "Three required deeplinks not present"
                )
            hashMap[PT_FIVE_IMAGE_LIST] =
                ListSizeChecker(templateRenderer.imageList, 3, "Three required images not present")
            //----------PROD DISPLAY-------------
            hashMap[PT_PRODUCT_THREE_IMAGE_LIST] =
                ListEqualSizeChecker(
                    templateRenderer.imageList,
                    3,
                    "Only three images are required"
                )
            hashMap[PT_THREE_DEEPLINK_LIST] =
                ListEqualSizeChecker(
                    templateRenderer.deepLinkList,
                    3,
                    "Three required deeplinks not present"
                )
            hashMap[PT_BIG_TEXT_LIST] =
                ListEqualSizeChecker(
                    templateRenderer.bigTextList,
                    3,
                    "Three required product titles not present"
                )
            hashMap[PT_SMALL_TEXT_LIST] =
                ListEqualSizeChecker(
                    templateRenderer.smallTextList,
                    3,
                    "Three required product descriptions not present"
                )
            hashMap[PT_PRODUCT_DISPLAY_ACTION] =
                StringSizeChecker(
                    templateRenderer.pt_product_display_action,
                    0,
                    "Button label is missing or empty"
                )
            hashMap[PT_PRODUCT_DISPLAY_ACTION_CLR] =
                StringSizeChecker(
                    templateRenderer.pt_product_display_action_clr,
                    0,
                    "Button colour is missing or empty"
                )
            //----------ZERO BEZEL----------------
            hashMap[PT_BIG_IMG] =
                StringSizeChecker(
                    templateRenderer.pt_big_img,
                    0,
                    "Display Image is missing or empty"
                )
            //----------TIMER----------------
            hashMap[PT_TIMER_THRESHOLD] =
                IntSizeChecker(
                    templateRenderer.pt_timer_threshold,
                    -1,
                    "Timer threshold not defined"
                )
            hashMap[PT_TIMER_END] =
                IntSizeChecker(
                    templateRenderer.pt_timer_end,
                    -1,
                    "Timer end time not defined"
                )
            //----------INPUT BOX----------------
            hashMap[PT_INPUT_FEEDBACK] =
                StringSizeChecker(
                    templateRenderer.pt_input_feedback,
                    0,
                    "Feedback Text or Actions is missing or empty"
                )
            hashMap[PT_ACTIONS] =
                JsonArraySizeChecker(
                    templateRenderer.actions,
                    0,
                    "Feedback Text or Actions is missing or empty"
                )

            return hashMap
        }
    }
}