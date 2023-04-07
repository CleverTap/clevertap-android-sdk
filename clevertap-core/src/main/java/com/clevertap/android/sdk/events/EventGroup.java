package com.clevertap.android.sdk.events;

public enum EventGroup {

    REGULAR("",""),
    PUSH_NOTIFICATION_VIEWED("-spiky",""),
    VARIABLES("","/defineVars");

    public final String httpResource;
    public final String additionalPath;

    EventGroup(String httpResource, String additionalPath) {
        this.httpResource = httpResource;
        this.additionalPath = additionalPath;
    }
}
