package com.clevertap.android.sdk.display_units;

import com.clevertap.android.sdk.display_units.model.CleverTapDisplayUnit;

import java.util.ArrayList;

/**
 * Listener for Display Unit Callbacks
 */
public interface DisplayUnitListener {

    /**
     * Provides the list of currently running Display Unit campaigns
     *
     * @param units - list of Display units {@link CleverTapDisplayUnit}
     */
    void onDisplayUnitsLoaded(ArrayList<CleverTapDisplayUnit> units);

}