package com.clevertap.android.sdk.pushnotification;

import androidx.annotation.NonNull;

public class PushType {

    public static PushType FCM = new PushType(
            PushConstants.FCM_DELIVERY_TYPE,
            PushConstants.FCM_PROPERTY_REG_ID,
            PushConstants.CT_FIREBASE_PROVIDER_CLASS,
            PushConstants.FIREBASE_SDK_CLASS,
            PushConstants.FCM_PUSH_TYPE_LOG_NAME
    );

    private final String ctProviderClassName;

    private final String messagingSDKClassName;

    private final String tokenPrefKey;

    private final String type;

    private final String logName;

    public PushType(
            String type,
            String prefKey,
            String className,
            String messagingSDKClassName,
            String logName
    ) {
        this.type = type;
        this.tokenPrefKey = prefKey;
        this.ctProviderClassName = className;
        this.messagingSDKClassName = messagingSDKClassName;
        this.logName = logName;
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

    public String name() {
        return logName;
    }

    @NonNull
    @Override
    public String toString() {
        return " [PushType:" + name() + "] ";
    }
}
