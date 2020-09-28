package com.clevertap.android.sdk.displayunits;

import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
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