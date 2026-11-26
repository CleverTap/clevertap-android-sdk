package com.clevertap.android.sdk

@Suppress("MemberVisibilityCanBePrivate")
public open class UTMDetail {

    private var campaignValue: String? = null

    private var mediumValue: String? = null

    private var sourceValue: String? = null

    public open fun getCampaign(): String? = campaignValue

    public open fun setCampaign(campaign: String?) {
        campaignValue = campaign
    }

    /** Kotlin property syntax, equivalent to [getCampaign] / [setCampaign]. */
    public var campaign: String?
        @JvmSynthetic @JvmName("ktGetCampaign") inline get() = getCampaign()
        @JvmSynthetic @JvmName("ktSetCampaign") inline set(value) = setCampaign(value)

    public open fun getMedium(): String? = mediumValue

    public open fun setMedium(medium: String?) {
        mediumValue = medium
    }

    /** Kotlin property syntax, equivalent to [getMedium] / [setMedium]. */
    public var medium: String?
        @JvmSynthetic @JvmName("ktGetMedium") inline get() = getMedium()
        @JvmSynthetic @JvmName("ktSetMedium") inline set(value) = setMedium(value)

    public open fun getSource(): String? = sourceValue

    public open fun setSource(source: String?) {
        sourceValue = source
    }

    /** Kotlin property syntax, equivalent to [getSource] / [setSource]. */
    public var source: String?
        @JvmSynthetic @JvmName("ktGetSource") inline get() = getSource()
        @JvmSynthetic @JvmName("ktSetSource") inline set(value) = setSource(value)
}
