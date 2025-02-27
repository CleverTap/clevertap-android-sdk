package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;

public enum PushType {
    FCM(PushConstants.FCM_DELIVERY_TYPE, PushConstants.FCM_PROPERTY_REG_ID, PushConstants.CT_FIREBASE_PROVIDER_CLASS, PushConstants.FIREBASE_SDK_CLASS);

    private final String ctProviderClassName;

    private final String messagingSDKClassName;

    private final String tokenPrefKey;

    private final String type;

    PushType(
            String type,
            String prefKey,
            String className,
            String messagingSDKClassName
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

    public String getTokenPrefKey() {
        return tokenPrefKey;
    }

    public String getType() {
        return type;
    }

    @NonNull
    @Override
    public String toString() {
        return " [PushType:" + name() + "] ";
    }
}
