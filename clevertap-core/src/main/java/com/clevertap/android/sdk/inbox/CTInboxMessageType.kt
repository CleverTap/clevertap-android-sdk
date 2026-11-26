package com.clevertap.android.sdk.inbox

enum class CTInboxMessageType(private val inboxMessageType: String) {

    SimpleMessage("simple"),
    IconMessage("message-icon"),
    CarouselMessage("carousel"),
    CarouselImageMessage("carousel-image");

    override fun toString(): String = inboxMessageType

    companion object {

        internal fun fromString(type: String): CTInboxMessageType? = entries.firstOrNull { it.inboxMessageType == type }
    }
}
