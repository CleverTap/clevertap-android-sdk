package com.clevertap.android.sdk.displayunits;

import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;

import java.util.ArrayList;

/**
 * Listener for Display Unit Callbacks
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameter as a platform type, so wrapper SDKs
 * and apps can override it with a nullable or non-null type. Do not convert this file to Kotlin.
 */
public interface DisplayUnitListener {

    /**
     * Provides the list of currently running Display Unit campaigns
     *
     * @param units - list of Display units {@link CleverTapDisplayUnit}
     */
    void onDisplayUnitsLoaded(ArrayList<CleverTapDisplayUnit> units);
}
