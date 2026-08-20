package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.StorageHelper.getPreferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.inapp.ImpressionManager;
import com.clevertap.android.sdk.task.CTExecutors;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.Clock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Frequency-cap counter gate for the Native Display (ND / Display Units) channel.
 *
 * <p>This is the ND sibling of {@link InAppFCManager} and enforces the counter-based caps at the
 * point a unit would be surfaced to the host app:
 * <ul>
 *   <li>per-target lifetime ({@code tlc}) and daily ({@code tdc}) counts,</li>
 *   <li>per-target and global session caps ({@code mdc} / {@code ndmc}),</li>
 *   <li>global daily cap ({@code ndstc} shown-today vs {@code ndstmcd} ceiling).</li>
 * </ul>
 *
 * <p>ND is SS + legacy only, so unlike {@link InAppFCManager} there is no legacy pref-key migration
 * here. Counter state lives in its own namespaces (see {@link Constants#KEY_ND_COUNTS_PER_TARGET})
 * and never collides with in-app. The advanced {@code frequencyLimits}/{@code occurrenceLimits}
 * (whenLimits) are evaluated separately by the ND {@code LimitsMatcher}; their result is passed in as
 * {@code frequencyLimitsMaxedOut}.
 *
 * <p>The API is model-light (primitives) so it stays decoupled from the ND content/target model,
 * which is parsed elsewhere.
 */
@RestrictTo(Scope.LIBRARY)
public class NdFCManager {

    static final int UNCAPPED = -1;

    private static final String ND_DATE_KEY = "nd_ict_date";

    /**
     * Whether an ND unit/target carries any frequency-cap configuration. Only such units are gated at
     * delivery and counted at impression time; unmarked (legacy) units bypass ND capping entirely so
     * existing display units are never affected and never consume the ND global budget.
     */
    public static boolean isFcapManaged(JSONObject json) {
        return json != null
                && (json.has(Constants.KEY_EFC)
                || json.has(Constants.KEY_TLC)
                || json.has(Constants.KEY_TDC)
                || json.has(Constants.INAPP_MAX_DISPLAY_COUNT)
                || json.has(Constants.KEY_EXCLUDE_GLOBAL_CAPS));
    }

    private final SimpleDateFormat ddMMyyyy = new SimpleDateFormat("ddMMyyyy", Locale.US);

    private final CleverTapInstanceConfig config;

    private final Context context;

    private String deviceId;

    private final ImpressionManager impressionManager;

    private final CTExecutors executors;

    private final Clock clock;

    NdFCManager(Context context,
                CleverTapInstanceConfig config,
                String deviceId,
                ImpressionManager impressionManager,
                CTExecutors executors,
                Clock clock) {
        this.config = config;
        this.context = context;
        this.deviceId = deviceId;
        this.impressionManager = impressionManager;
        this.executors = executors;
        this.clock = clock;

        Task<Void> task = executors.postAsyncSafelyTask();
        task.execute("initNdFCManager", () -> {
            init(deviceId);
            return null;
        });
    }

    /**
     * Whether the given ND target can currently be surfaced under all counter caps.
     *
     * @param id                       ND target id ({@code ti}).
     * @param excludeFromCaps          {@code efc == 1} — exempt from all caps.
     * @param totalLifetimeCount       {@code tlc}; {@link #UNCAPPED} means uncapped.
     * @param totalDailyCount          {@code tdc}; {@link #UNCAPPED} means uncapped.
     * @param maxPerSession            {@code mdc}; negative means the per-target session default.
     * @param frequencyLimitsMaxedOut  Result of the ND whenLimits (advanced) re-check by LimitsMatcher.
     */
    public boolean canShow(String id,
                           boolean excludeFromCaps,
                           int totalLifetimeCount,
                           int totalDailyCount,
                           int maxPerSession,
                           boolean frequencyLimitsMaxedOut) {
        try {
            if (id == null || id.isEmpty()) {
                return true;
            }

            // Re-check advanced whenLimits (without Nth triggers), mirrors in-app canShow.
            if (frequencyLimitsMaxedOut) {
                return false;
            }

            // Exclude from all counter caps?
            if (excludeFromCaps) {
                return true;
            }

            if (!hasSessionCapacityMaxedOut(id, maxPerSession)
                    && !hasLifetimeCapacityMaxedOut(id, totalLifetimeCount)
                    && !hasDailyCapacityMaxedOut(id, totalDailyCount)) {
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    public void changeUser(String deviceId) {
        impressionManager.clearSessionData();
        this.deviceId = deviceId;
        init(deviceId);
    }

    /**
     * Records a surfaced ND impression: bumps the per-target today/lifetime counts, the global
     * shown-today counter, and the in-memory session impression state.
     */
    public void didShow(final Context context, final String id) {
        if (id == null || id.isEmpty()) {
            return;
        }

        Task<Void> task = executors.ioTask();
        task.execute("recordNdImpressionsAndCounts", () -> {
            impressionManager.recordImpression(id);

            incrementNdCountsInPersistentStore(id);

            int shownToday = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId), 0);
            StorageHelper.putInt(context,
                    storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId)),
                    ++shownToday);
            return null;
        });
    }

    /**
     * @return the SDK's total ND render count today ({@code ndmp} request value).
     */
    public int getShownTodayCount() {
        return getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId), 0);
    }

    /**
     * @return the {@code ndtlc} array: {@code [[targetId, todayCount, lifetimeCount], ...]}.
     */
    public JSONArray getNdCounts(final Context context) {
        try {
            JSONArray arr = new JSONArray();
            final SharedPreferences prefs = getPreferences(context,
                    storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)));
            final Map<String, ?> all = prefs.getAll();
            for (Map.Entry<String, ?> target : all.entrySet()) {
                // impressions share this file under the "__impressions_" prefix — skip them.
                if (target.getKey().startsWith(com.clevertap.android.sdk.inapp.store.preference.ImpressionStore.PREF_PREFIX)) {
                    continue;
                }
                if (target.getValue() instanceof String) {
                    final String[] parts = ((String) target.getValue()).split(",");
                    if (parts.length == 2) {
                        JSONArray a = new JSONArray();
                        a.put(0, target.getKey());
                        a.put(1, Integer.parseInt(parts[0]));
                        a.put(2, Integer.parseInt(parts[1]));
                        arr.put(a);
                    }
                }
            }
            return arr;
        } catch (Throwable t) {
            Logger.v("Failed to get ND counts", t);
            return null;
        }
    }

    /**
     * Purges dead-target ({@code adUnit_stale}) counter state from the ND counts store.
     */
    public void processResponse(final Context context, final JSONArray staleIds) {
        try {
            if (staleIds == null) {
                return;
            }

            final SharedPreferences prefs = getPreferences(context,
                    storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)));
            final SharedPreferences.Editor editor = prefs.edit();

            for (int i = 0; i < staleIds.length(); i++) {
                final Object o = staleIds.get(i);
                if (o instanceof Integer) {
                    editor.remove("" + o);
                    Logger.d("Purged stale ND target - " + o);
                } else if (o instanceof String) {
                    editor.remove((String) o);
                    Logger.d("Purged stale ND target - " + o);
                }
            }

            StorageHelper.persist(editor);
        } catch (Throwable t) {
            Logger.v("Failed to purge stale ND targets", t);
        }
    }

    /**
     * Stores the account-level ND ceilings pushed on every response ({@code ndmp}/{@code ndmc}).
     */
    public synchronized void updateLimits(final Context context, int perDay, int perSession) {
        StorageHelper.putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_MAX_PER_DAY, deviceId)),
                perDay);
        StorageHelper.putInt(context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.ND_MAX_PER_SESSION_KEY, deviceId)),
                perSession);
    }

    private boolean hasDailyCapacityMaxedOut(String id, int totalDailyCount) {
        // 1. Global daily cap.
        int shownTodayCount = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId), 0);
        int maxPerDayCount = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_ND_MAX_PER_DAY, deviceId), 1);
        if (shownTodayCount >= maxPerDayCount) {
            return true;
        }

        // 2. Per-target daily cap.
        try {
            if (totalDailyCount == UNCAPPED) {
                return false;
            }
            final int[] counts = getNdCountsFromPersistentStore(id);
            if (counts[0] >= totalDailyCount) {
                return true;
            }
        } catch (Throwable t) {
            return true;
        }

        return false;
    }

    private boolean hasLifetimeCapacityMaxedOut(String id, int totalLifetimeCount) {
        if (totalLifetimeCount == UNCAPPED) {
            return false;
        }
        try {
            final int[] counts = getNdCountsFromPersistentStore(id);
            if (counts[1] >= totalLifetimeCount) {
                return true;
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    private boolean hasSessionCapacityMaxedOut(String id, int maxPerSession) {
        // 1. Per-target session cap.
        try {
            final int perSession = maxPerSession >= 0 ? maxPerSession : 1000;
            if (impressionManager.perSession(id) >= perSession) {
                return true;
            }
        } catch (Throwable t) {
            return true;
        }

        // 2. Global per-session cap (ndmc, default 1).
        final int c = getIntFromPrefs(getKeyWithDeviceId(Constants.ND_MAX_PER_SESSION_KEY, deviceId), 1);
        int sessionTotal = impressionManager.perSessionTotal();
        return (sessionTotal >= c);
    }

    private int[] getNdCountsFromPersistentStore(String id) {
        final SharedPreferences prefs = getPreferences(context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)));
        final String str = prefs.getString(id, null);
        if (str == null) {
            return new int[]{0, 0};
        }
        try {
            final String[] parts = str.split(",");
            if (parts.length != 2) {
                return new int[]{0, 0};
            }
            // protocol: todayCount,lifeTimeCount
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (Throwable t) {
            return new int[]{0, 0};
        }
    }

    private void incrementNdCountsInPersistentStore(String id) {
        int[] current = getNdCountsFromPersistentStore(id);
        current[0] = current[0] + 1;
        current[1] = current[1] + 1;

        final SharedPreferences prefs = getPreferences(context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)));
        final SharedPreferences.Editor editor = prefs.edit();

        // protocol: todayCount,lifeTimeCount
        editor.putString(id, current[0] + "," + current[1]);
        StorageHelper.persist(editor);
    }

    private void init(String deviceId) {
        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "NdFCManager init() called");
        try {
            final String today = ddMMyyyy.format(clock.newDate());
            final String lastUpdated = getStringFromPrefs(getKeyWithDeviceId(ND_DATE_KEY, deviceId), "20140428");
            if (!today.equals(lastUpdated)) {
                StorageHelper.putString(context, storageKeyWithSuffix(getKeyWithDeviceId(ND_DATE_KEY, deviceId)), today);

                // Reset global shown-today count.
                StorageHelper.putInt(context,
                        storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId)), 0);

                // Reset per-target today counts (keep lifetime). Impression keys share this file under
                // the "__impressions_" prefix — leave them untouched.
                final SharedPreferences prefs = getPreferences(context,
                        storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)));
                final SharedPreferences.Editor editor = prefs.edit();
                final Map<String, ?> all = prefs.getAll();
                for (String target : all.keySet()) {
                    if (target.startsWith(com.clevertap.android.sdk.inapp.store.preference.ImpressionStore.PREF_PREFIX)) {
                        continue;
                    }
                    Object ov = all.get(target);
                    if (!(ov instanceof String)) {
                        continue;
                    }
                    String[] oldValues = ((String) ov).split(",");
                    if (oldValues.length != 2) {
                        continue;
                    }
                    // protocol: todayCount,lifeTimeCount
                    try {
                        editor.putString(target, "0," + oldValues[1]);
                    } catch (Throwable t) {
                        getConfigLogger().verbose(getConfigAccountId(),
                                "Failed to reset todayCount for ND target " + target, t);
                    }
                }
                StorageHelper.persist(editor);
            }
        } catch (Exception e) {
            getConfigLogger().verbose(getConfigAccountId(),
                    "Failed to init ND FC manager " + e.getLocalizedMessage());
        }
    }

    private String getConfigAccountId() {
        return this.config.getAccountId();
    }

    private Logger getConfigLogger() {
        return this.config.getLogger();
    }

    private int getIntFromPrefs(String rawKey, int defaultValue) {
        if (this.config.isDefaultInstance()) {
            int dummy = -1000;
            int _new = StorageHelper.getInt(this.context, storageKeyWithSuffix(rawKey), dummy);
            return _new != dummy ? _new : StorageHelper.getInt(this.context, rawKey, defaultValue);
        } else {
            return StorageHelper.getInt(this.context, storageKeyWithSuffix(rawKey), defaultValue);
        }
    }

    private String getStringFromPrefs(String rawKey, String defaultValue) {
        if (this.config.isDefaultInstance()) {
            String _new = StorageHelper.getString(this.context, storageKeyWithSuffix(rawKey), defaultValue);
            return _new != null ? _new : StorageHelper.getString(this.context, rawKey, defaultValue);
        } else {
            return StorageHelper.getString(this.context, storageKeyWithSuffix(rawKey), defaultValue);
        }
    }

    private String getKeyWithDeviceId(String key, String deviceId) {
        return key + ":" + deviceId;
    }

    private String storageKeyWithSuffix(String key) {
        return key + ":" + getConfigAccountId();
    }
}
