package com.clevertap.android.directcall.events;

public enum CTSystemEvent {
    DC_OUTGOING("DCOutgoing"),
    DC_INCOMING("DCIncoming"),
    DC_END("DCEnd");

    private final String eventName;

    CTSystemEvent(String name) {
        eventName = name;
    }

    public String getName() {
        return eventName;
    }
}
