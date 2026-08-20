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
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager;
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore;
import com.clevertap.android.sdk.inapp.store.preference.NdStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Ingests the Native Display (ND) frequency-cap <b>meta</b> from the response (SDK-6055), none of
 * which is user-visible on its own:
 * <ul>
 *   <li>{@code ndmc}/{@code ndmp} — session/daily ceilings → {@link NdFCManager#updateLimits},</li>
 *   <li>{@code adUnit_stale} — dead-target GC → clear ND impressions/triggers/counters,</li>
 *   <li>{@code adUnit_notifs_ss} — advanced-rule metadata bundle → {@link NdStore} (for local eval),</li>
 *   <li>CG-suppressed stubs inside {@code adUnit_notifs_applaunched} → {@code adUnit_suppressed} ack.</li>
 * </ul>
 *
 * <p>This processor is registered <b>before</b> {@link DisplayUnitResponse} so ceilings and the
 * stale-purge land before the delivery cap gate runs. It deliberately does <b>not</b> write display
 * content: both {@code adUnit_notifs} and the non-stub {@code adUnit_notifs_applaunched} entries are
 * delivered by {@link DisplayUnitResponse} in a single merged, gated cache write, so one response
 * carrying both keys can't wipe the other. No-ops for old (V1) responses that carry none of these keys.
 */
public class AdUnitResponse extends CleverTapResponse {

    private final CleverTapInstanceConfig config;

    private final StoreRegistry storeRegistry;

    private final ControllerManager controllerManager;

    private final TriggerManager ndTriggerManager;

    private final NdEvaluationManager ndEvaluationManager;

    private final Logger logger;

    public AdUnitResponse(
            CleverTapInstanceConfig config,
            StoreRegistry storeRegistry,
            ControllerManager controllerManager,
            TriggerManager ndTriggerManager,
            NdEvaluationManager ndEvaluationManager
    ) {
        this.config = config;
        this.storeRegistry = storeRegistry;
        this.controllerManager = controllerManager;
        this.ndTriggerManager = ndTriggerManager;
        this.ndEvaluationManager = ndEvaluationManager;
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

            // 1. Account-level ceilings. Per contract §5.1 `ndmc` is emitted on every V2 response;
            //    `ndmp` only when the account has a daily cap (absent => uncapped daily, hence
            //    Integer.MAX_VALUE rather than a numeric default that would wrongly cap it).
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

            // 3. Advanced-rule metadata bundle (rules only) for local evaluation. Full replace,
            //    including an empty array: the bundle is emitted only on App-Launched / ND-meta-fetch
            //    and is always the complete current set (contract §5.2), so [] legitimately means "clear".
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

            // 4. Ack CG-suppressed App-Launched stubs (the real content is delivered by
            //    DisplayUnitResponse). App-Launched path only — regular-event CG is server-decided.
            ackCgSuppressedStubs(response);
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "Failed to process ND response", t);
        }
    }

    /**
     * Records an {@code adUnit_suppressed} ack for every CG stub ({@code suppressed:true}) inside
     * {@code adUnit_notifs_applaunched}. Non-stub entries are ignored here — they are delivered as
     * content by {@link DisplayUnitResponse}.
     */
    private void ackCgSuppressedStubs(final JSONObject response) {
        final JSONArray appLaunched = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_APP_LAUNCHED_KEY);
        if (appLaunched == null) {
            return;
        }
        for (int i = 0; i < appLaunched.length(); i++) {
            final JSONObject entry = appLaunched.optJSONObject(i);
            if (entry != null && entry.optBoolean(Constants.INAPP_SUPPRESSED, false)) {
                ndEvaluationManager.recordCgSuppressed(entry);
            }
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
