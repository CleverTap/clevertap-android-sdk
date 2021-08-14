package com.clevertap.android.pushtemplates

import android.graphics.Bitmap

sealed class PushTemplateType {

    data class BasicTemplate(
        var title: CharSequence,
        var message: CharSequence,
        var backgroundColor: Int,
        var titleColor: Int,
        var messageColor: Int,
        var messageSummary: CharSequence,
        var smallIcon: Bitmap,
        var dotSeparator: Bitmap,
        var bigImage: Bitmap,
        var largeIcon: Bitmap
    )

    data class ManualCarouselTemplate(
        var title: CharSequence,
        var message: CharSequence,
        var backgroundColor: Int,
        var titleColor: Int,
        var messageColor: Int,
        var messageSummary: CharSequence,
        var smallIcon: Bitmap,
        var dotSeparator: Bitmap,
        var bigImage: Bitmap,
        var largeIcon: Bitmap,
        var imageList: ArrayList<String>
    )


    data class AutoCarouselTemplate(
        var title: CharSequence,
        var message: CharSequence,
        var backgroundColor: Int,
        var titleColor: Int,
        var messageColor: Int,
        var messageSummary: CharSequence,
        var smallIcon: Bitmap,
        var dotSeparator: Bitmap,
        var bigImage: Bitmap,
        var largeIcon: Bitmap,
        var imageList: ArrayList<String>
    )
}