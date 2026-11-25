package com.clevertap.android.sdk.displayunits

import android.text.TextUtils
import android.util.Log
import com.clevertap.android.sdk.Constants

/**
 * supported Display Unit Types
 */
enum class CTDisplayUnitType(@JvmField val type: String) {

    SIMPLE("simple"),
    SIMPLE_WITH_IMAGE("simple-image"),
    CAROUSEL("carousel"),
    CAROUSEL_WITH_IMAGE("carousel-image"),
    MESSAGE_WITH_ICON("message-icon"),
    CUSTOM_KEY_VALUE("custom-key-value");

    override fun toString(): String {
        return type
    }

    companion object {

        /**
         * Returns the display type instance using the string value
         *
         * @param type - string value of the type provided from the feed.
         * @return CTDisplayUnitType
         */
        @JvmStatic
        fun type(type: String): CTDisplayUnitType? {
            if (!TextUtils.isEmpty(type)) {
                when (type) {
                    "simple" -> return SIMPLE
                    "simple-image" -> return SIMPLE_WITH_IMAGE
                    "carousel" -> return CAROUSEL
                    "carousel-image" -> return CAROUSEL_WITH_IMAGE
                    "message-icon" -> return MESSAGE_WITH_ICON
                    "custom-key-value" -> return CUSTOM_KEY_VALUE
                }
            }
            Log.d(Constants.FEATURE_DISPLAY_UNIT, "Unsupported Display Unit Type")
            return null
        }
    }
}
