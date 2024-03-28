package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.StorageHelper.getPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.inapp.ImpressionManager;
import com.clevertap.android.sdk.inapp.SharedPreferencesMigration;
import com.clevertap.android.sdk.inapp.store.preference.InAppStore;
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.json.JSONArray;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class InAppFCManager {

    private final SimpleDateFormat ddMMyyyy = new SimpleDateFormat("ddMMyyyy", Locale.US);

    private final CleverTapInstanceConfig config;

    private final Context context;

    private String deviceId;

    private ImpressionManager impressionManager;

    private final StoreRegistry storeRegistry;

    InAppFCManager(Context context, CleverTapInstanceConfig config, String deviceId,
            StoreRegistry storeRegistry, ImpressionManager impressionManager) {
        this.config = config;
        this.context = context;
        this.deviceId = deviceId;
        this.storeRegistry = storeRegistry;
        this.impressionManager = impressionManager;

        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("initInAppFCManager", new Callable<Void>() {
            @Override
            public Void call() {
                init(InAppFCManager.this.deviceId);
                return null;
            }
        });
    }


    public boolean canShow(CTInAppNotification inapp,
            Function2<JSONObject, String, Boolean> hasInAppFrequencyLimitsMaxedOut) {
        try {
            if (inapp == null) {
                return false;
            }

            final String id = getInAppID(inapp);
            if (id == null) {
                return true;
            }


            /* Evaluate frequency limits again (without Nth triggers)
               in case the message was added multiple times before being displayed,
               or queue was paused and the message was added multiple times in the meantime
            */
            if (hasInAppFrequencyLimitsMaxedOut.invoke(inapp.getJsonDescription(), id)) {
                return false;
            }

            // Exclude from all caps?
            if (inapp.isExcludeFromCaps()) {
                return true;
            }

            if (!hasSessionCapacityMaxedOut(inapp)
                    && !hasLifetimeCapacityMaxedOut(inapp)
                    && !hasDailyCapacityMaxedOut(inapp)) {
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    public void changeUser(String deviceId) {
        // reset counters
        impressionManager.clearSessionData();
        this.deviceId = deviceId;
        init(deviceId);
    }

    public void didDismiss(CTInAppNotification inapp) {
        // NO-OP
    }

    public void didShow(final Context context, CTInAppNotification inapp) {
        final String id = getInAppID(inapp);
        if (id == null) {
            return;
        }

        impressionManager.recordImpression(id);

        incrementInAppCountsInPersistentStore(id);

        int shownToday = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0);
        StorageHelper
                .putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)),
                        ++shownToday);
    }

    public int getShownTodayCount() {
        return getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0);
    }

    public JSONArray getInAppsCount(final Context context) {
        try {
            // tlc: [[targetID, todayCount, lifetime]]
            JSONArray arr = new JSONArray();
            final SharedPreferences prefs = StorageHelper
                    .getPreferences(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)));
            final Map<String, ?> all = prefs.getAll();
            for (Map.Entry<String, ?> inApp : all.entrySet()) {
                //inApp: "targetID" -> "todayCount,lifetime"
                if (inApp.getValue() instanceof String) {
                    final String[] parts = ((String) inApp.getValue()).split(",");
                    if (parts.length == 2) {
                        JSONArray a = new JSONArray();
                        a.put(0, inApp.getKey());
                        a.put(1, Integer.parseInt(parts[0]));
                        a.put(2, Integer.parseInt(parts[1]));
                        arr.put(a);
                    }
                }
            }
            return arr;
        } catch (Throwable t) {
            Logger.v("Failed to get in apps count", t);
            return null;
        }
    }

    public void processResponse(final Context context, final JSONObject response) {
        try {
            if (!response.has("inapp_stale")) {
                return;
            }

            final JSONArray arr = response.getJSONArray("inapp_stale");

            final SharedPreferences prefs = getPreferences(context,
                    storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)));
            final SharedPreferences.Editor editor = prefs.edit();

            for (int i = 0; i < arr.length(); i++) {
                final Object o = arr.get(i);
                if (o instanceof Integer) {
                    editor.remove("" + o);
                    Logger.d("Purged stale in-app - " + o);
                } else if (o instanceof String) {
                    editor.remove((String) o);
                    Logger.d("Purged stale in-app - " + o);
                }
            }

            StorageHelper.persist(editor);
        } catch (Throwable t) {
            Logger.v("Failed to purge out stale targets", t);
        }
    }

    public synchronized void updateLimits(final Context context, int perDay, int perSession) {
        StorageHelper.putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_MAX_PER_DAY, deviceId)),
                perDay);
        StorageHelper
                .putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION_KEY, deviceId)),
                        perSession);
    }

    private String getConfigAccountId() {
        return this.config.getAccountId();
    }

    private Logger getConfigLogger() {
        return this.config.getLogger();
    }

    private int[] getInAppCountsFromPersistentStore(String inappID) {
        final SharedPreferences prefs = getPreferences(context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)));
        final String str = prefs.getString(inappID, null);
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

    public String getInAppID(CTInAppNotification inapp) {
        if (inapp.getId() == null) {
            return null;
        }

        if (!inapp.getId().isEmpty()) {
            try {
                return inapp.getId();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
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

    private String getKeyWithDeviceId(String key, String deviceId) {
        return key + ":" + deviceId;
    }

    @SuppressWarnings("SameParameterValue")
    private String getStringFromPrefs(String rawKey, String defaultValue) {
        if (this.config.isDefaultInstance()) {
            String _new = StorageHelper.getString(this.context, storageKeyWithSuffix(rawKey), defaultValue);
            return _new != null ? _new : StorageHelper.getString(this.context, rawKey, defaultValue);
        } else {
            return StorageHelper.getString(this.context, storageKeyWithSuffix(rawKey), defaultValue);
        }
    }

    private boolean hasDailyCapacityMaxedOut(CTInAppNotification inapp) {
        final String id = getInAppID(inapp);
        if (id == null) {
            return false;
        }

        // 1. Has the daily count maxed out globally?
        int shownTodayCount = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0);
        int maxPerDayCount = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_MAX_PER_DAY, deviceId), 1);
        if (shownTodayCount >= maxPerDayCount) {
            return true;
        }

        // 2. Has the daily count been maxed out for this inapp?
        try {
            int maxPerDay = inapp.getTotalDailyCount();
            if (maxPerDay == -1) {
                return false;
            }

            final int[] counts = getInAppCountsFromPersistentStore(id);
            if (counts[0] >= maxPerDay) {
                return true;
            }
        } catch (Throwable t) {
            return true;
        }

        return false;
    }

    private boolean hasLifetimeCapacityMaxedOut(CTInAppNotification inapp) {

        final String id = getInAppID(inapp);
        if (id == null) {
            return false;
        }

        if (inapp.getTotalLifetimeCount() == -1) {
            return false;
        }

        try {
            final int[] counts = getInAppCountsFromPersistentStore(id);
            if (counts[1] >= inapp.getTotalLifetimeCount()) {
                return true;
            }
        } catch (Exception e) {
            return true;
        }

        return false;
    }

    private boolean hasSessionCapacityMaxedOut(CTInAppNotification inapp) {
        final String id = getInAppID(inapp);
        if (id == null) {
            return false;
        }

        // 2. Has the session max count for this inapp been breached?
        try {
            final int maxPerSession = inapp.getMaxPerSession() >= 0 ? inapp.getMaxPerSession() : 1000;

            int c = impressionManager.perSession(id);
            if (c >= maxPerSession) {
                return true;
            }
        } catch (Throwable t) {
            return true;
        }

        // 3. Have we shown enough of in-apps this session?
        final int c = getIntFromPrefs(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION_KEY, deviceId), 1);
        int sessionTotal = impressionManager.perSessionTotal();
        return (sessionTotal >= c);
    }

    private void incrementInAppCountsInPersistentStore(String inappID) {
        int[] current = getInAppCountsFromPersistentStore(inappID);
        current[0] = current[0] + 1;
        current[1] = current[1] + 1;

        final SharedPreferences prefs = getPreferences(context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)));
        final SharedPreferences.Editor editor = prefs.edit();

        // protocol: todayCount,lifeTimeCount
        editor.putString(inappID, current[0] + "," + current[1]);
        StorageHelper.persist(editor);
    }

    private void init(String deviceId) {
        getConfigLogger()
                .verbose(config.getAccountId() + ":async_deviceID", "InAppFCManager init() called");
        try {
            //================= Testing START==================TODO: remove after testing

            /*SharedPreferences spCountsPerInAppV3 = getPreferences(context,
                    storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)));

            spCountsPerInAppV3.edit().clear().commit();

            *//*SharedPreferences spCountsPerInAppV1 = getPreferences(context, Constants.KEY_COUNTS_PER_INAPP);
            SharedPreferences.Editor edit = spCountsPerInAppV1.edit();*//*
            SharedPreferences spCountsPerInAppV2 = getPreferences(context, getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP,deviceId));
            SharedPreferences.Editor edit = spCountsPerInAppV2.edit();
            edit.putString("inapp1","1,1");
            edit.putString("inapp2","1");
            edit.putString("inapp3","1,1");
            edit.putString("inapp4","hello");
            edit.putInt("inapp5",10);
            edit.putBoolean("inapp6",true);
            edit.commit();*/

            //================= Testing END==================TODO: remove after testing
            migrateToNewPrefsKey(deviceId);
            final String today = ddMMyyyy.format(new Date());
            final String lastUpdated = getStringFromPrefs(getKeyWithDeviceId("ict_date", deviceId), "20140428");
            if (!today.equals(lastUpdated)) {
                StorageHelper
                        .putString(context, storageKeyWithSuffix(getKeyWithDeviceId("ict_date", deviceId)), today);

                // Reset today count
                StorageHelper.putInt(context,
                        storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)), 0);

                // Reset the counts for each inapp
                final SharedPreferences prefs = getPreferences(context,
                        storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)));
                final SharedPreferences.Editor editor = prefs.edit();
                final Map<String, ?> all = prefs.getAll();
                for (String inapp : all.keySet()) {
                    Object ov = all.get(inapp);
                    if (!(ov instanceof String)) {
                        editor.remove(inapp);
                        continue;
                    }

                    String[] oldValues = ((String) ov).split(",");
                    if (oldValues.length != 2) {
                        editor.remove(inapp);
                        continue;
                    }

                    // protocol: todayCount,lifeTimeCount
                    try {
                        editor.putString(inapp, "0," + oldValues[1]);
                    } catch (Throwable t) {
                        getConfigLogger()
                                .verbose(getConfigAccountId(), "Failed to reset todayCount for inapp " + inapp, t);
                    }
                }

                StorageHelper.persist(editor);
            }
        } catch (Exception e) {
            getConfigLogger()
                    .verbose(getConfigAccountId(), "Failed to init inapp manager " + e.getLocalizedMessage());
        }
    }

    private void migrateToNewPrefsKey(String deviceId) {

        // without account id and device id
        SharedPreferences spCountsPerInAppV1 = getPreferences(context, Constants.KEY_COUNTS_PER_INAPP);

        // with device id
        SharedPreferences spCountsPerInAppV2 = getPreferences(context,
                getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId));

        // with account id and device id
        SharedPreferences spCountsPerInAppV3 = getPreferences(context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)));

        Function1<String, Boolean> countsPerInAppMigrationCondition =
                (it) -> it.split(",").length == 2;

        if (CTXtensions.hasData(spCountsPerInAppV2)) {
            Logger.d("migrating shared preference countsPerInApp from V2 to V3...");
            SharedPreferencesMigration<String> countsPerInAppMigrationV2ToV3 = new SharedPreferencesMigration<>(
                    spCountsPerInAppV2, spCountsPerInAppV3, String.class, countsPerInAppMigrationCondition);

            countsPerInAppMigrationV2ToV3.migrate();
            Logger.d("Finished migrating shared preference countsPerInApp from V2 to V3.");
        } else if (CTXtensions.hasData(spCountsPerInAppV1)) {
            Logger.d("migrating shared preference countsPerInApp from V1 to V3...");
            SharedPreferencesMigration<String> countsPerInAppMigrationV1ToV3 = new SharedPreferencesMigration<>(
                    spCountsPerInAppV1, spCountsPerInAppV3, String.class, countsPerInAppMigrationCondition);

            countsPerInAppMigrationV1ToV3.migrate();
            Logger.d("Finished migrating shared preference countsPerInApp from V1 to V3.");
        }

        final InAppStore inAppStore = storeRegistry.getInAppStore();
        final LegacyInAppStore legacyInAppStore = storeRegistry.getLegacyInAppStore();
        if (inAppStore != null && legacyInAppStore != null) {
            final JSONArray accountBasedInApps = legacyInAppStore.readInApps();
            if (accountBasedInApps.length() > 0) {
                Logger.d("migrating in-apps from account id to device id based preference.");
                inAppStore.storeServerSideInApps(accountBasedInApps);
                legacyInAppStore.removeInApps();
                Logger.d("Finished migrating in-apps from account id to device id based preference.");
            }
        }

        if (getStringFromPrefs(getKeyWithDeviceId("ict_date", deviceId), null) != null//F
                || getStringFromPrefs("ict_date", null) == null)/*T | F*/ {
            return;
        }

        Logger.v("Migrating InAppFC Prefs");

        String ict_date = getStringFromPrefs("ict_date", "20140428");
        StorageHelper.putString(context, storageKeyWithSuffix(getKeyWithDeviceId("ict_date", deviceId)), ict_date);

        int keyCountsShownToday = getIntFromPrefs(storageKeyWithSuffix(Constants.KEY_COUNTS_SHOWN_TODAY), 0);
        StorageHelper
                .putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)),
                        keyCountsShownToday);
    }

    private String storageKeyWithSuffix(String key) {
        return key + ":" + getConfigAccountId();
    }
}
