package com.clevertap.android.pushtemplates.content

import android.content.Context
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TimerTemplateData

internal class TimerBigContentView(
    context: Context,
    timer_end: Long?,
    renderer: TemplateRenderer,
    data: TimerTemplateData
) :
    TimerSmallContentView(context, timer_end, renderer, data, R.layout.timer) {

    init {
        val baseContent = data.baseContent
        setCustomBackgroundColour(baseContent.colorData.backgroundColor, R.id.content_view_big)
        setCustomContentViewMessageSummary(baseContent.textData.messageSummary)
        setCustomContentViewMedia(
            R.layout.image_view_dynamic_linear,
            data.mediaData.gif.url,
            data.mediaData.bigImage.url,
            data.mediaData.scaleType,
            data.mediaData.bigImage.altText,
            data.mediaData.gif.numberOfFrames
        )
    }
}