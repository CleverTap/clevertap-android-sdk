package com.clevertap.android.sdk.ads;

import android.support.annotation.NonNull;

public interface AdConstants {

    enum CtAdType {

        CUSTOM_KEY_VALUE("custom-key-value");

        private final String adType;

        CtAdType(String type) {
            this.adType = type;
        }

        public static CtAdType type(String type) {

            if ("custom-key-value".equalsIgnoreCase(type)) {
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