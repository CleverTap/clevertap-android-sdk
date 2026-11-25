package com.clevertap.android.sdk

@Suppress("MemberVisibilityCanBePrivate")
open class UTMDetail {

    private var _campaign: String? = null

    private var _medium: String? = null

    private var _source: String? = null

    open fun getCampaign(): String? = _campaign

    open fun setCampaign(campaign: String?) {
        this._campaign = campaign
    }

    /** Kotlin property syntax, equivalent to [getCampaign] / [setCampaign]. */
    var campaign: String?
        @JvmSynthetic @JvmName("ktGetCampaign") inline get() = getCampaign()
        @JvmSynthetic @JvmName("ktSetCampaign") inline set(value) = setCampaign(value)

    open fun getMedium(): String? = _medium

    open fun setMedium(medium: String?) {
        this._medium = medium
    }

    /** Kotlin property syntax, equivalent to [getMedium] / [setMedium]. */
    var medium: String?
        @JvmSynthetic @JvmName("ktGetMedium") inline get() = getMedium()
        @JvmSynthetic @JvmName("ktSetMedium") inline set(value) = setMedium(value)

    open fun getSource(): String? = _source

    open fun setSource(source: String?) {
        this._source = source
    }

    /** Kotlin property syntax, equivalent to [getSource] / [setSource]. */
    var source: String?
        @JvmSynthetic @JvmName("ktGetSource") inline get() = getSource()
        @JvmSynthetic @JvmName("ktSetSource") inline set(value) = setSource(value)
}
