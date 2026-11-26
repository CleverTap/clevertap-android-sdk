package com.clevertap.android.sdk.displayunits

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import androidx.annotation.RestrictTo

/**
 * Controller class for caching & supplying the Display Units to the client.
 * Default implementation of [DisplayUnitCache].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class CTDisplayUnitController : DisplayUnitCache {

    internal val items = HashMap<String, CleverTapDisplayUnit>()

    /**
     * Getter for retrieving all the running Display Units in the cache.
     *
     * @return ArrayList<CleverTapDisplayUnit> - Could be null in case no Display Units are there in the cache
     */
    @Synchronized
    override fun getAllDisplayUnits(): ArrayList<CleverTapDisplayUnit>? {
        if (items.isEmpty()) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Failed to return Display Units, nothing found in the cache")
            return null
        }
        return ArrayList(items.values)
    }

    /**
     * Getter to retrieve the Display Unit using the unitID
     *
     * @param unitID - unitID of the Display Unit [CleverTapDisplayUnit.getUnitID]
     * @return CleverTapDisplayUnit - Could be null in case no Display Units with the ID is found
     */
    @Synchronized
    override fun getDisplayUnitForID(unitID: String?): CleverTapDisplayUnit? {
        if (unitID.isNullOrEmpty()) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Can't return Display Unit, id was null")
            return null
        }
        return items[unitID]
    }

    /**
     * clears the existing Display Units
     */
    @Synchronized
    override fun reset() {
        items.clear()
        Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Cleared Display Units Cache")
    }

    /**
     * Replaces the old Display Units with the supplied list.
     *
     * @param displayUnits parsed display units; may be `null` or empty.
     */
    @Synchronized
    override fun updateDisplayUnits(displayUnits: List<CleverTapDisplayUnit>?) {
        reset()
        if (displayUnits.isNullOrEmpty()) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Empty Display Units list, cache cleared")
            return
        }
        // filterNotNull: Java implementors/callers may hand over null elements
        for (unit in displayUnits.filterNotNull()) {
            val unitID = unit.getUnitID()
            if (!unitID.isNullOrEmpty()) {
                items[unitID] = unit
            }
        }
    }
}
