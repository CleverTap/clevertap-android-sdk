package com.clevertap.android.sdk.ads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

public interface CTAdConstants {

    //supported AD Types
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

        /**
         * Returns the ad-type instance using the string value
         *
         * @param type - string value of the type provided from the feed.
         * @return CtAdType
         */
        @Nullable
        public static CtAdType type(String type) {

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
            Log.d("CtAdType", "Unsupported AdType");
            return null;
        }

        @NonNull
        @Override
        public String toString() {
            return adType;
        }
    }
}