package com.clevertap.android.sdk;

public enum EventGroup {

    REGULAR(""),
    PUSH_NOTIFICATION_VIEWED("-spiky");

    final String httpResource;

    EventGroup(String httpResource) {
        this.httpResource = httpResource;
    }
}
