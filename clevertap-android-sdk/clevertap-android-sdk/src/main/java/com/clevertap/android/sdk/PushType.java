package com.clevertap.android.sdk;

public enum PushType {
    FCM("fcm"),
    GCM("gcm");

    private final String type;

    PushType(String type) {
        this.type = type;
    }

    @SuppressWarnings("unused")
    static PushType fromString(String type) {
        if ("fcm".equals(type)) {
            return PushType.FCM;
        } else if ("gcm".equals(type)) {
            return PushType.GCM;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return type;
    }
}
