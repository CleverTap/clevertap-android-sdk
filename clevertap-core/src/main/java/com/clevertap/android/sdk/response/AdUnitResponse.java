package com.clevertap.android.sdk.response;

import android.content.Context;

import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.NdFCManager;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.inapp.TriggerManager;
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore;
import com.clevertap.android.sdk.inapp.store.preference.NdStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Ingests the Native Display (ND) frequency-cap response keys (SDK-6055, Phase 2).
 *
 * <p>ND content itself still flows through {@link DisplayUnitResponse} ({@code adUnit_notifs}); this
 * processor handles only the new SS/fcap surface, none of which is user-visible on its own:
 * <ul>
 *   <li>{@code adUnit_notifs_ss} — advanced-rule metadata bundle → {@link NdStore} (for local eval),</li>
 *   <li>{@code adUnit_stale} — dead-target GC → clear ND impressions/triggers/counters,</li>
 *   <li>{@code ndmc}/{@code ndmp} — session/daily ceilings → {@link NdFCManager#updateLimits}.</li>
 * </ul>
 *
 * <p>Content delivery for {@code adUnit_notifs_applaunched} + the cap gate are added in a later phase
 * (they must ship together with enforcement, not ungated). No-ops for old (V1) responses that carry
 * none of these keys.
 */
public class AdUnitResponse extends CleverTapResponse {

    private final CleverTapInstanceConfig config;

    private final StoreRegistry storeRegistry;

    private final ControllerManager controllerManager;

    private final TriggerManager ndTriggerManager;

    private final Logger logger;

    public AdUnitResponse(
            CleverTapInstanceConfig config,
            StoreRegistry storeRegistry,
            ControllerManager controllerManager,
            TriggerManager ndTriggerManager
    ) {
        this.config = config;
        this.storeRegistry = storeRegistry;
        this.controllerManager = controllerManager;
        this.ndTriggerManager = ndTriggerManager;
        this.logger = config.getLogger();
    }

    @Override
    @WorkerThread
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        if (config.isAnalyticsOnly()) {
            return;
        }
        if (response == null) {
            return;
        }

        try {
            final NdFCManager ndFCManager = controllerManager.getNdFCManager();

            // 1. Account-level ceilings. ndmc is emitted on every V2 response; ndmp only when the
            //    account has a daily cap (absent => uncapped daily).
            if (response.has(Constants.ND_MAX_PER_SESSION_KEY) && ndFCManager != null) {
                final int perSession = response.optInt(Constants.ND_MAX_PER_SESSION_KEY, 1);
                final int perDay = response.has(Constants.ND_MAX_PER_DAY_KEY)
                        ? response.optInt(Constants.ND_MAX_PER_DAY_KEY, Integer.MAX_VALUE)
                        : Integer.MAX_VALUE; // uncapped
                ndFCManager.updateLimits(context, perDay, perSession);
            }

            // 2. Dead-target GC.
            final JSONArray staleIds = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_STALE_KEY);
            if (staleIds != null) {
                clearStaleNdCache(staleIds);
                if (ndFCManager != null) {
                    ndFCManager.processResponse(context, staleIds);
                }
            }

            // 3. Advanced-rule metadata bundle (rules only) for local evaluation.
            if (response.has(Constants.DISPLAY_UNIT_NOTIFS_SS_KEY)) {
                final NdStore ndStore = storeRegistry.getNdStore();
                final JSONArray ssArray = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_SS_KEY);
                if (ndStore != null && ssArray != null) {
                    final List<JSONObject> meta = Utils.toJSONObjectList(ssArray);
                    ndStore.storeServerSideNdMetaData(meta);
                    logger.verbose(config.getAccountId(),
                            Constants.FEATURE_DISPLAY_UNIT + "Stored " + meta.size() + " ND SS metadata entries");
                }
            }
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "Failed to process ND response", t);
        }
    }

    /**
     * Wipes the CS/SS cap state (impression timestamps + trigger counts) for stale ND target ids.
     * The legacy per-target counters are purged separately by {@link NdFCManager#processResponse}.
     */
    private void clearStaleNdCache(final JSONArray staleIds) {
        final ImpressionStore impressionStore = storeRegistry.getNdImpressionStore();
        for (int i = 0; i < staleIds.length(); i++) {
            final String staleId = staleIds.optString(i);
            if (staleId == null || staleId.isEmpty()) {
                continue;
            }
            if (impressionStore != null) {
                impressionStore.clear(staleId);
            }
            if (ndTriggerManager != null) {
                ndTriggerManager.removeTriggers(staleId);
            }
        }
    }
}
