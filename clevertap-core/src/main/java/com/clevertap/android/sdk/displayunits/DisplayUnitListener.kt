package com.clevertap.android.sdk.displayunits

import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit

/**
 * Listener for Display Unit Callbacks
 */
fun interface DisplayUnitListener {

    /**
     * Provides the list of currently running Display Unit campaigns
     *
     * @param units - list of Display units [CleverTapDisplayUnit]
     */
    fun onDisplayUnitsLoaded(units: ArrayList<CleverTapDisplayUnit>)
}
