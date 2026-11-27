package com.clevertap.android.sdk

/**
 * The UTM parameters (source, medium, campaign) the SDK attributed the current session to, as
 * returned by [CleverTapAPI.getUTMDetails]. Each value is `null` when it is not known.
 */
@Suppress("MemberVisibilityCanBePrivate")
public open class UTMDetail {

    private var campaignValue: String? = null

    private var mediumValue: String? = null

    private var sourceValue: String? = null

    /**
     * Returns the UTM campaign (`utm_campaign`).
     *
     * @return the campaign, or `null` if not known
     */
    public open fun getCampaign(): String? = campaignValue

    /**
     * Sets the UTM campaign.
     *
     * @param campaign the campaign, or `null`
     */
    public open fun setCampaign(campaign: String?) {
        campaignValue = campaign
    }

    /** Kotlin property syntax, equivalent to [getCampaign] / [setCampaign]. */
    public var campaign: String?
        @JvmSynthetic @JvmName("ktGetCampaign") inline get() = getCampaign()
        @JvmSynthetic @JvmName("ktSetCampaign") inline set(value) = setCampaign(value)

    /**
     * Returns the UTM medium (`utm_medium`).
     *
     * @return the medium, or `null` if not known
     */
    public open fun getMedium(): String? = mediumValue

    /**
     * Sets the UTM medium.
     *
     * @param medium the medium, or `null`
     */
    public open fun setMedium(medium: String?) {
        mediumValue = medium
    }

    /** Kotlin property syntax, equivalent to [getMedium] / [setMedium]. */
    public var medium: String?
        @JvmSynthetic @JvmName("ktGetMedium") inline get() = getMedium()
        @JvmSynthetic @JvmName("ktSetMedium") inline set(value) = setMedium(value)

    /**
     * Returns the UTM source (`utm_source`).
     *
     * @return the source, or `null` if not known
     */
    public open fun getSource(): String? = sourceValue

    /**
     * Sets the UTM source.
     *
     * @param source the source, or `null`
     */
    public open fun setSource(source: String?) {
        sourceValue = source
    }

    /** Kotlin property syntax, equivalent to [getSource] / [setSource]. */
    public var source: String?
        @JvmSynthetic @JvmName("ktGetSource") inline get() = getSource()
        @JvmSynthetic @JvmName("ktSetSource") inline set(value) = setSource(value)
}
