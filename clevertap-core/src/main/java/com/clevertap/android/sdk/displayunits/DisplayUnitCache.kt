package com.clevertap.android.sdk.displayunits

import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit

/**
 * In-memory storage contract for [CleverTapDisplayUnit]s.
 * The default implementation ([CTDisplayUnitController]) is
 * server-pipeline-driven. Hosts may install their own implementation via
 * `CleverTapAPI#setDisplayUnitCache(DisplayUnitCache)`
 * to expose units produced outside the standard server-response pipeline
 * (for example, server-driven UI SDKs that fetch units through their own
 * pipeline).
 *
 * Implementors must be thread-safe — methods may be invoked from any thread.
 *
 * The display unit listener registered via
 * `CleverTapAPI#setDisplayUnitListener` only
 * fires for server-pipeline activity. Replacing the cache or mutating its
 * contents from outside the SDK does not synthesise a listener fire.
 */
interface DisplayUnitCache {

    /**
     * @return all units currently held; `null` or empty if none.
     */
    fun getAllDisplayUnits(): ArrayList<CleverTapDisplayUnit>?

    /**
     * @param unitID the unit identifier; implementations must tolerate
     *               `null` or empty input by returning `null`.
     * @return the unit with the given id, or `null` if absent.
     */
    fun getDisplayUnitForID(unitID: String?): CleverTapDisplayUnit?

    /**
     * Called by the SDK when a server response delivers an updated set of
     * display units. The default implementation replaces the cache contents;
     * hosts may choose merge semantics for their own implementations.
     *
     * @param displayUnits parsed display units; may be `null` or empty.
     */
    // @JvmSuppressWildcards keeps the Java signature `List<CleverTapDisplayUnit>` (not
    // `List<? extends CleverTapDisplayUnit>`) so existing Java implementations still @Override it.
    fun updateDisplayUnits(displayUnits: List<@JvmSuppressWildcards CleverTapDisplayUnit>?)

    /**
     * Clears all units. Called by the SDK on logout / reset flows.
     */
    fun reset()
}
