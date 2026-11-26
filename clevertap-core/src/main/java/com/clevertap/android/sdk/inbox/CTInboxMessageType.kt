package com.clevertap.android.sdk.inbox

/**
 * The App Inbox message templates, as returned by [CTInboxMessage.getType]. [toString] returns the
 * template's identifier in the inbox payload, for example `message-icon`.
 */
public enum class CTInboxMessageType(private val inboxMessageType: String) {

    SimpleMessage("simple"),
    IconMessage("message-icon"),
    CarouselMessage("carousel"),
    CarouselImageMessage("carousel-image");

    override fun toString(): String = inboxMessageType

    public companion object {

        internal fun fromString(type: String): CTInboxMessageType? = entries.firstOrNull { it.inboxMessageType == type }
    }
}
