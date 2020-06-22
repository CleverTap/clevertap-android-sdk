package com.clevertap.android.sdk;

import androidx.annotation.NonNull;

public enum PushType {
    FCM("fcm"),
    HPS("hps"),
    XPS("xps"),
    BPS("bps");

    private final String type;

    PushType(String type) {
        this.type = type;
    }

    @SuppressWarnings("unused")
    static PushType fromString(String type) {
        if ("fcm".equals(type)) {
            return PushType.FCM;
        } else if ("hps".equals(type)) {
            return PushType.HPS;
        } else if ("xps".equals(type)) {
            return PushType.XPS;
        } else if ("bps".equals(type)) {
            return PushType.BPS;
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
