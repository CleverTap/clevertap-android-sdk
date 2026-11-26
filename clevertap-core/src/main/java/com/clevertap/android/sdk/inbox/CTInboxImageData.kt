package com.clevertap.android.sdk.inbox

/**
 * One carousel image of an App Inbox message, as returned by
 * [CTInboxMessage.getCarouselImagesData].
 *
 * @property url the image URL
 * @property contentDescription the accessibility text (alt text) set for the image
 */
public data class CTInboxImageData(
    val url: String,
    val contentDescription: String
)
