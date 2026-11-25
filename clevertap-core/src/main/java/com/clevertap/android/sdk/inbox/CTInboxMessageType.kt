package com.clevertap.android.sdk.inbox

enum class CTInboxMessageType(private val inboxMessageType: String) {

    SimpleMessage("simple"),
    IconMessage("message-icon"),
    CarouselMessage("carousel"),
    CarouselImageMessage("carousel-image");

    override fun toString(): String {
        return inboxMessageType
    }

    companion object {

        internal fun fromString(type: String): CTInboxMessageType? {
            return when (type) {
                "simple" -> SimpleMessage

                "message-icon" -> IconMessage

                "carousel" -> CarouselMessage

                "carousel-image" -> CarouselImageMessage

                else -> null
            }
        }
    }
}
