package com.clevertap.android.sdk;

class CTLockManager {

    private final Boolean eventLock = true;

    private final Object inboxControllerLock = new Object();

    Boolean getEventLock() {
        return eventLock;
    }

    Object getInboxControllerLock() {
        return inboxControllerLock;
    }
}
