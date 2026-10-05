package com.clevertap.android.sdk.displayunits;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Controller class for caching & supplying the Display Units to the client.
 * Default implementation of {@link DisplayUnitCache}.
 */
public class CTDisplayUnitController implements DisplayUnitCache {

    final HashMap<String, CleverTapDisplayUnit> items = new HashMap<>();

    /**
     * Getter for retrieving all the running Display Units in the cache.
     *
     * @return ArrayList<CleverTapDisplayUnit> - Could be null in case no Display Units are there in the cache
     */
    @Override
    @Nullable
    public synchronized ArrayList<CleverTapDisplayUnit> getAllDisplayUnits() {
        if (!items.isEmpty()) {
            return new ArrayList<>(items.values());
        } else {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Failed to return Display Units, nothing found in the cache");
            return null;
        }
    }

    /**
     * Getter to retrieve the Display Unit using the unitID
     *
     * @param unitId - unitID of the Display Unit {@link CleverTapDisplayUnit#getUnitID()}
     * @return CleverTapDisplayUnit - Could be null in case no Display Units with the ID is found
     */
    @Override
    @Nullable
    public synchronized CleverTapDisplayUnit getDisplayUnitForID(@Nullable String unitId) {
        if (!TextUtils.isEmpty(unitId)) {
            return items.get(unitId);
        } else {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Can't return Display Unit, id was null");
            return null;
        }
    }

    /**
     * clears the existing Display Units
     */
    @Override
    public synchronized void reset() {
        items.clear();
        Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Cleared Display Units Cache");
    }

    /**
     * Upserts the supplied Display Units into the cache, keyed by unitID: a unit with a known unitID
     * replaces the existing one, a new unitID is added, and units not in this list are left in place.
     * This does NOT clear the cache, so a partial response (e.g. a /content subset) adds to rather
     * than wipes the units delivered by /a1. Call {@link #reset()} explicitly to clear.
     *
     * @param displayUnits parsed display units; may be {@code null} or empty (then a no-op).
     */
    @Override
    public synchronized void updateDisplayUnits(@Nullable List<CleverTapDisplayUnit> displayUnits) {
        if (displayUnits == null || displayUnits.isEmpty()) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Empty Display Units list, nothing to update");
            return;
        }
        for (CleverTapDisplayUnit unit : displayUnits) {
            if (unit != null && !TextUtils.isEmpty(unit.getUnitID())) {
                items.put(unit.getUnitID(), unit);
            }
        }
    }
}