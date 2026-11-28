package com.clevertap.android.sdk.displayunits;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;

import java.util.ArrayList;

/**
 * Listener for Display Unit Callbacks
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameter
 * carries androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin
 * override must declare the same nullability. Do not convert this file to Kotlin.
 */
public interface DisplayUnitListener {

    /**
     * Provides the list of currently running Display Unit campaigns
     *
     * @param units - list of Display units {@link CleverTapDisplayUnit}
     */
    void onDisplayUnitsLoaded(@NonNull ArrayList<CleverTapDisplayUnit> units);
}
