package com.clevertap.android.sdk

@Suppress("MemberVisibilityCanBePrivate")
open class UTMDetail {

    private var campaignValue: String? = null

    private var mediumValue: String? = null

    private var sourceValue: String? = null

    open fun getCampaign(): String? = campaignValue

    open fun setCampaign(campaign: String?) {
        campaignValue = campaign
    }

    /** Kotlin property syntax, equivalent to [getCampaign] / [setCampaign]. */
    var campaign: String?
        @JvmSynthetic @JvmName("ktGetCampaign") inline get() = getCampaign()
        @JvmSynthetic @JvmName("ktSetCampaign") inline set(value) = setCampaign(value)

    open fun getMedium(): String? = mediumValue

    open fun setMedium(medium: String?) {
        mediumValue = medium
    }

    /** Kotlin property syntax, equivalent to [getMedium] / [setMedium]. */
    var medium: String?
        @JvmSynthetic @JvmName("ktGetMedium") inline get() = getMedium()
        @JvmSynthetic @JvmName("ktSetMedium") inline set(value) = setMedium(value)

    open fun getSource(): String? = sourceValue

    open fun setSource(source: String?) {
        sourceValue = source
    }

    /** Kotlin property syntax, equivalent to [getSource] / [setSource]. */
    var source: String?
        @JvmSynthetic @JvmName("ktGetSource") inline get() = getSource()
        @JvmSynthetic @JvmName("ktSetSource") inline set(value) = setSource(value)
}
