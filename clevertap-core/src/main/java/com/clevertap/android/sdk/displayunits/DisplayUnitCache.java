package com.clevertap.android.sdk.displayunits;

import androidx.annotation.Nullable;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import java.util.ArrayList;
import org.json.JSONArray;

/**
 * In-memory storage contract for {@link CleverTapDisplayUnit}s.
 * The default implementation ({@link CTDisplayUnitController}) is
 * server-pipeline-driven. Hosts may install their own implementation via
 * {@link com.clevertap.android.sdk.CleverTapAPI#setDisplayUnitCache(DisplayUnitCache)}
 * to expose units produced outside the standard server-response pipeline
 * (for example, server-driven UI SDKs that fetch units through their own
 * pipeline).
 *
 * <p>Implementors must be thread-safe — methods may be invoked from any thread.
 *
 * <p>The display unit listener registered via
 * {@link com.clevertap.android.sdk.CleverTapAPI#setDisplayUnitListener} only
 * fires for server-pipeline activity. Replacing the cache or mutating its
 * contents from outside the SDK does not synthesise a listener fire.
 */
public interface DisplayUnitCache {

    /**
     * @return all units currently held; {@code null} or empty if none.
     */
    @Nullable
    ArrayList<CleverTapDisplayUnit> getAllDisplayUnits();

    /**
     * @param unitID the unit identifier; implementations must tolerate
     *               {@code null} or empty input by returning {@code null}.
     * @return the unit with the given id, or {@code null} if absent.
     */
    @Nullable
    CleverTapDisplayUnit getDisplayUnitForID(@Nullable String unitID);

    /**
     * Called by the SDK when a server response delivers an updated set of
     * display units. The default implementation replaces the cache contents;
     * hosts may choose merge semantics for their own implementations.
     *
     * @param messages parsed server payload; may be {@code null} or empty.
     * @return the parsed units that were applied, or {@code null} if none.
     */
    @Nullable
    ArrayList<CleverTapDisplayUnit> updateDisplayUnits(@Nullable JSONArray messages);

    /**
     * Clears all units. Called by the SDK on logout / reset flows.
     */
    void reset();
}
