package com.clevertap.android.sdk.ads;

import com.clevertap.android.sdk.ads.model.CTAdUnit;

import java.util.ArrayList;

/**
 * Listener for Ad Unit Callbacks
 */
public interface AdListener {

    /**
     * Provides the list of currently running ad campaigns
     *
     * @param units - list of ad units {@link CTAdUnit}
     */
    void onAdUnitsLoaded(ArrayList<CTAdUnit> units);

}