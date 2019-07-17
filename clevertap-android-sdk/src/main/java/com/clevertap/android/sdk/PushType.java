package com.clevertap.android.sdk;

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

    @Override
    public String toString() {
        return type;
    }
}
