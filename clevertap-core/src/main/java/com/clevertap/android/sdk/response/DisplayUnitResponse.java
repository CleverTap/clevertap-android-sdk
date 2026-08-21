package com.clevertap.android.sdk.response;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.NdFCManager;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.displayunits.DisplayUnitCache;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.inapp.TriggerManager;
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager;
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore;
import com.clevertap.android.sdk.inapp.store.preference.NdStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Single owner of the Display Units / Native Display (ND) channel (SDK-6055 Phase 10).
 *
 * <p>Handles the whole channel in one pass, in order, so there is no cross-processor ordering
 * dependency (the meta must land before the content gate reads ceilings):
 * <ol>
 *   <li>ND fcap <b>meta</b> — {@code ndmc}/{@code ndmp} ceilings, {@code adUnit_stale} GC,
 *       {@code adUnit_notifs_ss} rule bundle, and CG-suppression acks from
 *       {@code adUnit_notifs_applaunched} stubs. Ingested on <b>every</b> response, including a
 *       user switch (ceilings/rules are per-account and should stay current).</li>
 *   <li>ND <b>content</b> — {@code adUnit_notifs} + non-stub {@code adUnit_notifs_applaunched},
 *       merged, frequency-cap gated, written to the cache once, and delivered via the callback.
 *       <b>Skipped on a user switch</b> (matches the legacy behavior of not surfacing display units
 *       to the just-switched-in user on that response).</li>
 * </ol>
 */
public class DisplayUnitResponse extends CleverTapResponseDecorator {

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final ControllerManager controllerManager;

    @Nullable
    private final StoreRegistry storeRegistry;

    @Nullable
    private final TriggerManager ndTriggerManager;

    @Nullable
    private final NdEvaluationManager ndEvaluationManager;

    private final Logger logger;

    public DisplayUnitResponse(
            CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager,
            ControllerManager controllerManager,
            @Nullable StoreRegistry storeRegistry,
            @Nullable TriggerManager ndTriggerManager,
            @Nullable NdEvaluationManager ndEvaluationManager
    ) {
        this.config = config;
        this.logger = this.config.getLogger();
        this.callbackManager = callbackManager;
        this.controllerManager = controllerManager;
        this.storeRegistry = storeRegistry;
        this.ndTriggerManager = ndTriggerManager;
        this.ndEvaluationManager = ndEvaluationManager;
    }

    /**
     * Content-only constructor for the send-test / push-preview path, which carries a single display
     * unit and no ND fcap meta. ND meta ingestion is skipped (deps null).
     */
    public DisplayUnitResponse(
            CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager,
            ControllerManager controllerManager
    ) {
        this(config, callbackManager, controllerManager, null, null, null);
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        processResponse(response, stringBody, context, false);
    }

    @WorkerThread
    public void processResponse(
            final JSONObject response,
            final String stringBody,
            final Context context,
            final boolean isUserSwitching
    ) {
        if (config.isAnalyticsOnly()) {
            logger.verbose(config.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing Display Unit response");
            return;
        }
        if (response == null) {
            logger.verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT
                    + "Can't parse Display Unit Response, JSON response object is null");
            return;
        }

        // 1. ND fcap meta — always, even on a user switch.
        ingestNdMeta(response, context);

        // 2. ND content — skipped on a user switch.
        if (!isUserSwitching) {
            deliverContent(response);
        }
    }

    // ---- ND fcap meta ----------------------------------------------------------------------------

    private void ingestNdMeta(final JSONObject response, final Context context) {
        // Content-only (send-test/preview) path has no ND stores wired — skip meta entirely.
        if (storeRegistry == null || ndEvaluationManager == null) {
            return;
        }
        try {
            final NdFCManager ndFCManager = controllerManager.getNdFCManager();

            // Account-level ceilings. Per contract §5.1 `ndmc` is emitted on every V2 response; `ndmp`
            // only when the account has a daily cap (absent => uncapped daily, hence Integer.MAX_VALUE).
            if (response.has(Constants.ND_MAX_PER_SESSION_KEY) && ndFCManager != null) {
                final int perSession = response.optInt(Constants.ND_MAX_PER_SESSION_KEY, 1);
                final int perDay = response.has(Constants.ND_MAX_PER_DAY_KEY)
                        ? response.optInt(Constants.ND_MAX_PER_DAY_KEY, Integer.MAX_VALUE)
                        : Integer.MAX_VALUE;
                ndFCManager.updateLimits(context, perDay, perSession);
            }

            // Dead-target GC.
            final JSONArray staleIds = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_STALE_KEY);
            if (staleIds != null) {
                clearStaleNdCache(staleIds);
                if (ndFCManager != null) {
                    ndFCManager.processResponse(context, staleIds);
                }
            }

            // Advanced-rule metadata bundle (rules only) for local evaluation. Full replace, including
            // an empty array: the bundle is emitted only on App-Launched / ND-meta-fetch and is always
            // the complete current set (contract §5.2), so [] legitimately means "clear".
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

            // Ack CG-suppressed App-Launched stubs (the real content is delivered below). App-Launched
            // path only — regular-event CG is server-decided.
            ackCgSuppressedStubs(response);
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "Failed to process ND meta", t);
        }
    }

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

    // ---- ND content ------------------------------------------------------------------------------

    private void deliverContent(final JSONObject response) {
        final JSONArray notifs = response.optJSONArray(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY);
        final JSONArray appLaunched = response.optJSONArray(Constants.DISPLAY_UNIT_NOTIFS_APP_LAUNCHED_KEY);
        final boolean hasNotifs = notifs != null && notifs.length() > 0;
        final boolean hasAppLaunched = appLaunched != null && appLaunched.length() > 0;
        if (!hasNotifs && !hasAppLaunched) {
            return;
        }
        try {
            parseDisplayUnits(notifs, appLaunched);
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "Failed to parse content", t);
        }
    }

    /**
     * Parses Display Units from both {@code adUnit_notifs} and (non-CG-suppressed)
     * {@code adUnit_notifs_applaunched}, merges them, applies the ND frequency-cap gate, and writes the
     * cache once — {@link com.clevertap.android.sdk.displayunits.CTDisplayUnitController#updateDisplayUnits}
     * replaces (not merges) the cache, so two writes for one response would wipe each other.
     * CG stubs ({@code suppressed:true}) are skipped here; they are acked in {@link #ackCgSuppressedStubs}.
     */
    private void parseDisplayUnits(JSONArray notifs, JSONArray appLaunched) {
        final ArrayList<CleverTapDisplayUnit> parsed = new ArrayList<>();
        if (notifs != null) {
            parsed.addAll(parseDisplayUnitsFromJson(notifs, false));
        }
        if (appLaunched != null) {
            parsed.addAll(parseDisplayUnitsFromJson(appLaunched, true));
        }
        if (parsed.isEmpty()) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "No valid Display Units to process");
            return;
        }

        DisplayUnitCache cache = controllerManager.getOrCreateDisplayUnitCache();
        if (cache == null) {
            logger.verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "No display-unit cache available");
            return;
        }

        ArrayList<CleverTapDisplayUnit> displayUnits = NdFcapGate.filter(
                parsed, controllerManager.getNdFCManager(), logger, config.getAccountId());
        cache.updateDisplayUnits(displayUnits);
        if (!displayUnits.isEmpty()) {
            callbackManager.notifyDisplayUnitsLoaded(displayUnits);
        }
    }

    @NonNull
    private ArrayList<CleverTapDisplayUnit> parseDisplayUnitsFromJson(@NonNull JSONArray messages, boolean skipSuppressed) {
        final ArrayList<CleverTapDisplayUnit> list = new ArrayList<>();
        for (int i = 0; i < messages.length(); i++) {
            try {
                JSONObject json = messages.getJSONObject(i);
                if (skipSuppressed && json.optBoolean(Constants.INAPP_SUPPRESSED, false)) {
                    continue;
                }
                CleverTapDisplayUnit unit = CleverTapDisplayUnit.toDisplayUnit(json);
                if (TextUtils.isEmpty(unit.getError())) {
                    list.add(unit);
                } else {
                    logger.verbose(config.getAccountId(),
                            Constants.FEATURE_DISPLAY_UNIT + "Failed to convert JsonArray item at index:" + i
                                    + " to Display Unit");
                }
            } catch (JSONException e) {
                logger.verbose(config.getAccountId(),
                        Constants.FEATURE_DISPLAY_UNIT + "Failed to parse Display Unit at index " + i
                                + ": " + e.getLocalizedMessage());
            }
        }
        return list;
    }
}
