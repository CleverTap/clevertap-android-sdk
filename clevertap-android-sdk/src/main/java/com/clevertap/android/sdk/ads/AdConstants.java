package com.clevertap.android.sdk.ads;

import android.support.annotation.NonNull;
import android.text.TextUtils;

public interface AdConstants {

    enum CtAdType {
        SIMPLE("simple"),
        SIMPLE_WITH_IMAGE("simple-image"),
        CAROUSEL("carousel"),
        CAROUSEL_WITH_IMAGE("carousel-image"),
        MESSAGE_WITH_ICON("message-icon"),
        CUSTOM_KEY_VALUE("custom-key-value");

        private final String adType;

        CtAdType(String type) {
            this.adType = type;
        }

        public static CtAdType type(String type) {

            if (TextUtils.isEmpty(type))
                return null;

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
            return null;
        }

        @NonNull
        @Override
        public String toString() {
            return adType;
        }
    }
}