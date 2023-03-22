package com.clevertap.android.sdk.inbox;

import androidx.annotation.NonNull;

public enum CTInboxMessageType {

    SimpleMessage("simple"),
    IconMessage("message-icon"),
    CarouselMessage("carousel"),
    CarouselImageMessage("carousel-image");

    private final String inboxMessageType;

    CTInboxMessageType(String type) {
        this.inboxMessageType = type;
    }

    @NonNull
    @Override
    public String toString() {
        return inboxMessageType;
    }

    static CTInboxMessageType fromString(String type) {
        switch (type) {
            case "simple":
                return SimpleMessage;

            case "message-icon":
                return IconMessage;

            case "carousel":
                return CarouselMessage;

            case "carousel-image":
                return CarouselImageMessage;

            default:
                return null;
        }
    }
}
