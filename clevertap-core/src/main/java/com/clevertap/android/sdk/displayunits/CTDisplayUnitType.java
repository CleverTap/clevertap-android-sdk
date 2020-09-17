package com.clevertap.android.sdk.displayunits;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clevertap.android.sdk.Constants;

/**
 * supported Display Unit Types
 */
public enum CTDisplayUnitType {

    SIMPLE("simple"),
    SIMPLE_WITH_IMAGE("simple-image"),
    CAROUSEL("carousel"),
    CAROUSEL_WITH_IMAGE("carousel-image"),
    MESSAGE_WITH_ICON("message-icon"),
    CUSTOM_KEY_VALUE("custom-key-value");

    private final String type;

    CTDisplayUnitType(String type) {
        this.type = type;
    }

    /**
     * Returns the display type instance using the string value
     *
     * @param type - string value of the type provided from the feed.
     * @return CTDisplayUnitType
     */
    @Nullable
    public static CTDisplayUnitType type(String type) {

        if (!TextUtils.isEmpty(type)) {
            switch (type) {
                case "simple":
                    return SIMPLE;
                case "simple-image":
                    return SIMPLE_WITH_IMAGE;
                case "carousel":
                    return CAROUSEL;
                case "carousel-image":
                    return CAROUSEL_WITH_IMAGE;
                case "message-icon":
                    return MESSAGE_WITH_ICON;
                case "custom-key-value":
                    return CUSTOM_KEY_VALUE;
            }
        }
        Log.d(Constants.FEATURE_DISPLAY_UNIT, "Unsupported Display Unit Type");
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return type;
    }
}