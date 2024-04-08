package com.clevertap.android.sdk;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

@RestrictTo(Scope.LIBRARY)
public class CTLockManager {

    private final Object eventLock = new Object();

    private final Object inboxControllerLock = new Object();

    public Object getEventLock() {
        return eventLock;
    }

    public Object getInboxControllerLock() {
        return inboxControllerLock;
    }
}
