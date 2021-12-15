package com.clevertap.android.pushtemplates

internal enum class TemplateType(private val templateType: String) {
    BASIC("pt_basic"), AUTO_CAROUSEL("pt_carousel"),
    MANUAL_CAROUSEL("pt_manual_carousel"), RATING("pt_rating"),
    FIVE_ICONS("pt_five_icons"), PRODUCT_DISPLAY("pt_product_display"),
    ZERO_BEZEL("pt_zero_bezel"), TIMER("pt_timer"),
    INPUT_BOX("pt_input"), VIDEO("pt_video"), CANCEL("pt_cancel");

    override fun toString(): String {
        return templateType
    }

    companion object {

        @JvmStatic
        fun fromString(type: String?): TemplateType? {
            return when (type) {
                "pt_basic" -> BASIC
                "pt_carousel" -> AUTO_CAROUSEL
                "pt_manual_carousel" -> MANUAL_CAROUSEL
                "pt_rating" -> RATING
                "pt_five_icons" -> FIVE_ICONS
                "pt_product_display" -> PRODUCT_DISPLAY
                "pt_zero_bezel" -> ZERO_BEZEL
                "pt_timer" -> TIMER
                "pt_input" -> INPUT_BOX
                "pt_video" -> VIDEO
                "pt_cancel" -> CANCEL
                else -> null
            }
        }
    }
}