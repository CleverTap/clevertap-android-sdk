package com.clevertap.android.sdk;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

@RestrictTo(Scope.LIBRARY)
public class CTLockManager {

    private final Boolean eventLock = true;

    private final Object inboxControllerLock = new Object();

    Boolean getEventLock() {
        return eventLock;
    }

    public Object getInboxControllerLock() {
        return inboxControllerLock;
    }
}
