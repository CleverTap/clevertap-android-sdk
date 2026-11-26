package com.clevertap.android.sdk.displayunits

import android.util.Log
import com.clevertap.android.sdk.Constants

/**
 * supported Display Unit Types
 */
public enum class CTDisplayUnitType(@JvmField public val type: String) {

    SIMPLE("simple"),
    SIMPLE_WITH_IMAGE("simple-image"),
    CAROUSEL("carousel"),
    CAROUSEL_WITH_IMAGE("carousel-image"),
    MESSAGE_WITH_ICON("message-icon"),
    CUSTOM_KEY_VALUE("custom-key-value");

    override fun toString(): String = type

    public companion object {

        /**
         * Returns the display type instance using the string value
         *
         * @param type - string value of the type provided from the feed.
         * @return CTDisplayUnitType
         */
        @JvmStatic
        public fun type(type: String): CTDisplayUnitType? {
            val displayUnitType = entries.firstOrNull { it.type == type }
            if (displayUnitType == null) {
                Log.d(Constants.FEATURE_DISPLAY_UNIT, "Unsupported Display Unit Type")
            }
            return displayUnitType
        }
    }
}
