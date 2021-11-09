package com.clevertap.android.pushtemplates

internal data class PushTemplate (
    internal val basicTemplate: PushTemplateType.BasicTemplate,
    internal val manualCarouselTemplate: PushTemplateType.ManualCarouselTemplate,
    internal val autoCarouselTemplate: PushTemplateType.AutoCarouselTemplate
)