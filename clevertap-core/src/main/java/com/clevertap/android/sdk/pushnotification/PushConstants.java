package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface PushConstants {

    @StringDef({FCM_DELIVERY_TYPE})
    @Retention(RetentionPolicy.SOURCE)
    @interface DeliveryType {

    }

    @StringDef({CT_FIREBASE_PROVIDER_CLASS})
    @Retention(RetentionPolicy.SOURCE)
    @interface CTPushProviderClass {

    }

    @StringDef({FIREBASE_SDK_CLASS})
    @Retention(RetentionPolicy.SOURCE)
    @interface PushMessagingClass {

    }

    @StringDef({FCM_PROPERTY_REG_ID})
    @Retention(RetentionPolicy.SOURCE)
    @interface RegKeyType {

    }

    enum PushType {
        FCM(FCM_DELIVERY_TYPE, FCM_PROPERTY_REG_ID, CT_FIREBASE_PROVIDER_CLASS, FIREBASE_SDK_CLASS);

        private final String ctProviderClassName;

        private final String messagingSDKClassName;

        private final String tokenPrefKey;

        private final String type;

        PushType(
                @DeliveryType String type,
                @RegKeyType String prefKey,
                @CTPushProviderClass String className,
                @PushMessagingClass String messagingSDKClassName
        ) {
            this.type = type;
            this.tokenPrefKey = prefKey;
            this.ctProviderClassName = className;
            this.messagingSDKClassName = messagingSDKClassName;
        }

        public String getCtProviderClassName() {
            return ctProviderClassName;
        }

        public String getMessagingSDKClassName() {
            return messagingSDKClassName;
        }

        public @RegKeyType
        String getTokenPrefKey() {
            return tokenPrefKey;
        }

        public String getType() {
            return type;
        }

        @NonNull
        @Override
        public @DeliveryType String toString() {
            return " [PushType:" + name() + "] ";
        }
    }

    String LOG_TAG = "PushProvider";

    String FCM_LOG_TAG = PushType.FCM.toString();

    @NonNull
    String FCM_DELIVERY_TYPE = "fcm";
    @NonNull
    String CT_FIREBASE_PROVIDER_CLASS = "com.clevertap.android.sdk.pushnotification.fcm.FcmPushProvider";
    String FIREBASE_SDK_CLASS = "com.google.firebase.messaging.FirebaseMessagingService";
    String FCM_PROPERTY_REG_ID = "fcm_token";
}