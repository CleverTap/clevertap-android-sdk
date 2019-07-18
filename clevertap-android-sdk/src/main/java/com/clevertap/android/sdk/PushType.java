package com.clevertap.android.sdk;

import android.support.annotation.NonNull;

public enum PushType {
    FCM("fcm");

    private final String type;

    PushType(String type) {
        this.type = type;
    }

    @SuppressWarnings("unused")
    static PushType fromString(String type) {
        if ("fcm".equals(type)) {
            return PushType.FCM;
        } else {
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return type;
    }
}
