package com.clevertap.android.sdk.ads;

import java.util.ArrayList;

/**
 * Listener for Ad Unit Callbacks
 */
public interface AdListener {

    /**
     * Provides the list of currently running ad campaigns
     *
     * @param units - list of ad units
     */
    void onAdUnitsLoaded(ArrayList<CTAdUnit> units);

}