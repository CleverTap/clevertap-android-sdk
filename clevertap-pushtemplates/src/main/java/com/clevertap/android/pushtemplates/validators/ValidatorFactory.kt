package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.*
import com.clevertap.android.pushtemplates.checkers.*
import org.json.JSONArray

const val PT_TITLE = "PT_TITLE"
const val PT_MSG = "PT_MSG"
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
const val PT_BIG_IMG = "PT_BIG_IMG"
const val PT_TIMER_THRESHOLD = "PT_TIMER_THRESHOLD"
const val PT_TIMER_END = "PT_TIMER_END"
const val PT_INPUT_FEEDBACK = "PT_INPUT_FEEDBACK"
const val PT_ACTIONS = "PT_ACTIONS"

fun Iterable<Checker<out Any>>.and(): Boolean {
    var and = true
    for (element in this) {
        and =
            element.check() && and // checking first will allow us to execute all checks(for printing errors) instead of short circuiting
    }
    return and
}

fun Iterable<Checker<out Any>>.or(): Boolean {
    var or = false
    for (element in this) {
        or =
            element.check() || or
        if (or) break
    }
    return or
}

/**
 * Builder class for creating validation checkers based on template data.
 * Uses a fluent interface to build only the necessary checkers for each template type.
 */
internal class ValidationCheckersBuilder {
    private val checkers = mutableMapOf<String, Checker<out Any>>()

    /**
     * Adds basic text validation (title and message) from BaseTextData
     */
    fun addBasicTextValidation(textData: BaseTextData): ValidationCheckersBuilder {
        addTitleValidation(textData.title)
        addMessageValidation(textData.message)
        return this
    }

    /**
     * Adds title validation for templates that have standalone title fields
     */
    fun addTitleValidation(title: String?): ValidationCheckersBuilder {
        checkers[PT_TITLE] = StringSizeChecker(title, 0, "Title is missing or empty")
        return this
    }

    /**
     * Adds title validation for templates that have standalone title fields
     */
    fun addMessageValidation(message: String?): ValidationCheckersBuilder {
        checkers[PT_MSG] = StringSizeChecker(message, 0, "Message is missing or empty")
        return this
    }

    /**
     * Adds deeplink list validation with configurable minimum size
     */
    fun addDeepLinkValidation(
        deepLinkList: ArrayList<String>?,
        minSize: Int = 1,
        key: String = PT_DEEPLINK_LIST
    ): ValidationCheckersBuilder {
        checkers[key] = ListSizeChecker(
            deepLinkList,
            minSize,
            if (minSize == 1) "Deeplink is missing or empty" else "$minSize required deeplinks not present"
        )
        return this
    }

    /**
     * Adds image list validation with support for exact or minimum size requirements
     */
    fun addImageListValidation(
        imageList: ArrayList<ImageData>?,
        requiredSize: Int,
        exact: Boolean = false,
        key: String
    ): ValidationCheckersBuilder {
        val errorMessage = if (exact) {
            "Only $requiredSize images are required"
        } else {
            "$requiredSize required images not present"
        }

        val checker = if (exact) {
            ListEqualSizeChecker(imageList, requiredSize, errorMessage)
        } else {
            ListSizeChecker(imageList, requiredSize, errorMessage)
        }
        checkers[key] = checker
        return this
    }

    /**
     * Adds string list validation with support for exact or minimum size requirements
     */
    fun addStringListValidation(
        stringList: ArrayList<String>?,
        requiredSize: Int,
        key: String,
        errorMessage: String,
        exact: Boolean = false
    ): ValidationCheckersBuilder {
        val checker = if (exact) {
            ListEqualSizeChecker(stringList, requiredSize, errorMessage)
        } else {
            ListSizeChecker(stringList, requiredSize, errorMessage)
        }
        checkers[key] = checker
        return this
    }

    /**
     * Adds string field validation
     */
    fun addStringValidation(value: String?, key: String, errorMessage: String): ValidationCheckersBuilder {
        checkers[key] = StringSizeChecker(value, 0, errorMessage)
        return this
    }

    /**
     * Adds integer field validation with minimum value check
     */
    fun addIntValidation(value: Int, minValue: Int, key: String, errorMessage: String): ValidationCheckersBuilder {
        checkers[key] = IntSizeChecker(value, minValue, errorMessage)
        return this
    }

    /**
     * Adds JSON array validation
     */
    fun addJsonArrayValidation(jsonArray: JSONArray?, key: String, errorMessage: String): ValidationCheckersBuilder {
        checkers[key] = JsonArraySizeChecker(jsonArray, 0, errorMessage)
        return this
    }


    /**
     * Builds and returns the final map of validation checkers
     */
    fun build(): Map<String, Checker<out Any>> = checkers.toMap()
}

/**
 * Factory for creating validators based on TemplateData objects.
 * Uses the builder pattern to create only the necessary validation checkers for each template type.
 */
internal class ValidatorFactory {
    companion object {

        /**
         * Creates a validator for the given template data.
         * Only instantiates the validation checkers required for the specific template type.
         */
        fun getValidator(templateData: TemplateData): Validator? {
            val keys = buildCheckersForTemplateData(templateData)
            return createValidatorFromKeys(templateData.templateType, keys)
        }

        /**
         * Builds validation checkers specific to the template data type using the builder pattern
         */
        private fun buildCheckersForTemplateData(templateData: TemplateData): Map<String, Checker<out Any>> {
            val builder = ValidationCheckersBuilder()

            return when (templateData) {
                is BasicTemplateData -> {
                    builder
                        .addBasicTextValidation(templateData.baseContent.textData)
                        .build()
                }

                is AutoCarouselTemplateData -> {
                    builder
                        .addBasicTextValidation(templateData.carouselData.baseContent.textData)
                        .addDeepLinkValidation(templateData.carouselData.baseContent.deepLinkList)
                        .addImageListValidation(templateData.carouselData.imageList, 3, key = PT_THREE_IMAGE_LIST)
                        .build()
                }

                is ManualCarouselTemplateData -> {
                    builder
                        .addBasicTextValidation(templateData.carouselData.baseContent.textData)
                        .addDeepLinkValidation(templateData.carouselData.baseContent.deepLinkList)
                        .addImageListValidation(templateData.carouselData.imageList, 3, key = PT_THREE_IMAGE_LIST)
                        .build()
                }

                is RatingTemplateData -> {
                    builder
                        .addBasicTextValidation(templateData.baseContent.textData)
                        .addDeepLinkValidation(templateData.baseContent.deepLinkList)
                        .addStringValidation(
                            templateData.defaultDeepLink,
                            PT_RATING_DEFAULT_DL,
                            "Default deeplink is missing or empty"
                        )
                        .build()
                }

                is FiveIconsTemplateData -> {
                    builder
                        .addDeepLinkValidation(templateData.deepLinkList, 3, PT_FIVE_DEEPLINK_LIST)
                        .addImageListValidation(templateData.imageList, 3, key = PT_FIVE_IMAGE_LIST)
                        .build()
                }

                is ProductTemplateData -> {
                    val productBuilder = builder
                        .addBasicTextValidation(templateData.baseContent.textData)
                        .addImageListValidation(
                            templateData.imageList,
                            3,
                            exact = true,
                            key = PT_PRODUCT_THREE_IMAGE_LIST
                        )
                        .addStringListValidation(
                            templateData.baseContent.deepLinkList,
                            3,
                            PT_THREE_DEEPLINK_LIST,
                            "Three required deeplinks not present",
                            exact = true
                        )
                        .addStringValidation(
                            templateData.displayActionText,
                            PT_PRODUCT_DISPLAY_ACTION,
                            "Button label is missing or empty"
                        )

                        productBuilder.addStringListValidation(
                            templateData.bigTextList,
                            3,
                            PT_BIG_TEXT_LIST,
                            "Three required product titles not present",
                            exact = true
                        )

                        productBuilder.addStringListValidation(
                            templateData.smallTextList,
                            3,
                            PT_SMALL_TEXT_LIST,
                            "Three required product descriptions not present",
                            exact = true
                        )

                    productBuilder.build()
                }

                is ZeroBezelTemplateData -> {
                    builder
                        .addBasicTextValidation(templateData.baseContent.textData)
                        .addStringValidation(
                            templateData.mediaData.bigImage.url,
                            PT_BIG_IMG,
                            "Display Image is missing or empty"
                        )
                        .build()
                }

                is TimerTemplateData -> {
                    builder
                        .addBasicTextValidation(templateData.baseContent.textData)
                        .addIntValidation(
                            templateData.timerThreshold,
                            -1,
                            PT_TIMER_THRESHOLD,
                            "Timer threshold not defined"
                        )
                        .addIntValidation(
                            templateData.timerEnd,
                            -1,
                            PT_TIMER_END,
                            "Not rendering notification Timer End value lesser than threshold (10 seconds) from current time"
                        )
                        .build()
                }

                is InputBoxTemplateData -> {
                    builder
                        .addBasicTextValidation(templateData.textData)
                        .addStringValidation(
                            templateData.inputFeedback,
                            PT_INPUT_FEEDBACK,
                            "Feedback Text or Actions is missing or empty"
                        )
                        .addJsonArrayValidation(
                            templateData.actions.actions,
                            PT_ACTIONS,
                            "Feedback Text or Actions is missing or empty"
                        )
                        .build()
                }

                is CancelTemplateData -> {
                    // No validation needed for cancel template
                    emptyMap()
                }
            }
        }

        /**
         * Creates the appropriate validator instance based on template type and validation checkers
         */
        private fun createValidatorFromKeys(templateType: TemplateType, keys: Map<String, Checker<out Any>>): Validator? {
            return when (templateType) {
                TemplateType.BASIC -> ContentValidator(keys)
                TemplateType.AUTO_CAROUSEL, TemplateType.MANUAL_CAROUSEL -> CarouselTemplateValidator(
                    ContentValidator(keys)
                )
                TemplateType.RATING -> RatingTemplateValidator(ContentValidator(keys))
                TemplateType.FIVE_ICONS -> FiveIconsTemplateValidator(keys)
                TemplateType.PRODUCT_DISPLAY -> ProductDisplayTemplateValidator(
                    ContentValidator(keys)
                )
                TemplateType.ZERO_BEZEL -> ZeroBezelTemplateValidator(ContentValidator(keys))
                TemplateType.TIMER -> TimerTemplateValidator(ContentValidator(keys))
                TemplateType.INPUT_BOX -> InputBoxTemplateValidator(ContentValidator(keys))
                else -> null
            }
        }
    }
}