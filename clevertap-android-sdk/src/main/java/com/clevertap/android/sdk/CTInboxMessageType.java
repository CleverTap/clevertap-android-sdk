package com.clevertap.android.sdk;

import androidx.annotation.NonNull;

enum CTInboxMessageType {

    SimpleMessage("simple"),
    IconMessage("message-icon"),
    CarouselMessage("carousel"),
    CarouselImageMessage("carousel-image");

    private final String inboxMessageType;
    CTInboxMessageType(String type) {
        this.inboxMessageType = type;
    }

    static CTInboxMessageType fromString(String type){
        switch (type){
            case "simple" : return SimpleMessage;

            case "message-icon" : return IconMessage;

            case "carousel" : return CarouselMessage;

            case "carousel-image" : return CarouselImageMessage;

            default: return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return inboxMessageType;
    }
}
