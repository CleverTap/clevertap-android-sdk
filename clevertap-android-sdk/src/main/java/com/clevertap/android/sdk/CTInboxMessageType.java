package com.clevertap.android.sdk;

enum CTInboxMessageType {

    SimpleMessage("simple-message"),
    IconMessage("icon-message"),
    CarouselMessage("carousel"),
    CarouselImageMessage("carousel-image");

    private final String inboxMessageType;
    CTInboxMessageType(String type) {
        this.inboxMessageType = type;
    }

    static CTInboxMessageType fromString(String type){
        switch (type){
            case "simple-message" : return SimpleMessage;

            case "icon-message" : return IconMessage;

            case "carousel" : return CarouselMessage;

            case "carousel-image" : return CarouselImageMessage;

            default: return null;
        }
    }

    @Override
    public String toString() {
        return inboxMessageType;
    }
}
