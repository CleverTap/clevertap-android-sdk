package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.StorageHelper.getPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import org.json.JSONArray;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class InAppFCManager {

    private final SimpleDateFormat ddMMyyyy = new SimpleDateFormat("ddMMyyyy", Locale.US);

    private final CleverTapInstanceConfig config;

    private final Context context;

    private String deviceId;

    private final ArrayList<String> mDismissedThisSession = new ArrayList<>();

    private final HashMap<String, Integer> mShownThisSession = new HashMap<>();

    private int mShownThisSessionCount = 0;


    InAppFCManager(Context context, CleverTapInstanceConfig config, String deviceId) {
        this.config = config;
        this.context = context;
        this.deviceId = deviceId;

        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("initInAppFCManager",new Callable<Void>() {
            @Override
            public Void call() {
                init(InAppFCManager.this.deviceId);
                return null;
            }
        });
    }

    public boolean canShow(CTInAppNotification inapp) {
        try {
            if (inapp == null) {
                return false;
            }

            final String id = getInAppID(inapp);
            if (id == null) {
                return true;
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
        mShownThisSession.clear();
        mShownThisSessionCount = 0;
        mDismissedThisSession.clear();
        this.deviceId = deviceId;
        init(deviceId);
    }

    public void didDismiss(CTInAppNotification inapp) {
        final Object id = inapp.getId();
        if (id != null) {
            mDismissedThisSession.add(id.toString());
        }
    }

    public void didShow(final Context context, CTInAppNotification inapp) {
        final String id = getInAppID(inapp);
        if (id == null) {
            return;
        }

        mShownThisSessionCount++;

        Integer count = mShownThisSession.get(id);
        if (count == null) {
            count = 1;
        }

        mShownThisSession.put(id, ++count);

        incrementInAppCountsInPersistentStore(id);

        int shownToday = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0);
        StorageHelper
                .putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)),
                        ++shownToday);
    }

    public void attachToHeader(final Context context, JSONObject header) {
        try {
            // Trigger reset for dates

            header.put("imp", getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0));

            // tlc: [[targetID, todayCount, lifetime]]
            JSONArray arr = new JSONArray();
            final SharedPreferences prefs = StorageHelper
                    .getPreferences(context, getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId));
            final Map<String, ?> all = prefs.getAll();
            for (String inapp : all.keySet()) {
                final Object o = all.get(inapp);
                if (o instanceof String) {
                    final String[] parts = ((String) o).split(",");
                    if (parts.length == 2) {
                        JSONArray a = new JSONArray();
                        a.put(0, inapp);
                        a.put(1, Integer.parseInt(parts[0]));
                        a.put(2, Integer.parseInt(parts[1]));
                        arr.put(a);
                    }
                }
            }

            header.put("tlc", arr);
        } catch (Throwable t) {
            Logger.v("Failed to attach FC to header", t);
        }
    }

    public void processResponse(final Context context, final JSONObject response) {
        try {
            if (!response.has("inapp_stale")) {
                return;
            }

            final JSONArray arr = response.getJSONArray("inapp_stale");

            final SharedPreferences prefs = getPreferences(context,
                    getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId));
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
                .putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION, deviceId)),
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
                getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId));
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

    private String getInAppID(CTInAppNotification inapp) {
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

        // 1. Has this been dismissed?
        if (mDismissedThisSession.contains(id)) {
            return true;
        }

        // 2. Has the session max count for this inapp been breached?
        try {
            final int maxPerSession = inapp.getMaxPerSession() >= 0 ? inapp.getMaxPerSession() : 1000;

            Integer c = mShownThisSession.get(id);
            if (c != null && c >= maxPerSession) {
                return true;
            }
        } catch (Throwable t) {
            return true;
        }

        // 3. Have we shown enough of in-apps this session?
        final int c = getIntFromPrefs(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION, deviceId), 1);
        return (mShownThisSessionCount >= c);
    }

    private void incrementInAppCountsInPersistentStore(String inappID) {
        int[] current = getInAppCountsFromPersistentStore(inappID);
        current[0] = current[0] + 1;
        current[1] = current[1] + 1;

        final SharedPreferences prefs = getPreferences(context,
                getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId));
        final SharedPreferences.Editor editor = prefs.edit();

        // protocol: todayCount,lifeTimeCount
        editor.putString(inappID, current[0] + "," + current[1]);
        StorageHelper.persist(editor);
    }

    private void init(String deviceId) {
        getConfigLogger()
                .verbose(config.getAccountId() + ":async_deviceID", "InAppFCManager init() called");
        try {
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
                        getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId));
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

        if (getStringFromPrefs(storageKeyWithSuffix(getKeyWithDeviceId("ict_date", deviceId)), null) != null
                || getStringFromPrefs("ict_date", null) == null) {
            return;
        }

        Logger.v("Migrating InAppFC Prefs");

        String ict_date = getStringFromPrefs("ict_date", "20140428");
        StorageHelper.putString(context, storageKeyWithSuffix(getKeyWithDeviceId("ict_date", deviceId)), ict_date);

        int keyCountsShownToday = getIntFromPrefs(storageKeyWithSuffix(Constants.KEY_COUNTS_SHOWN_TODAY), 0);
        StorageHelper
                .putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)),
                        keyCountsShownToday);

        final SharedPreferences oldPrefs = getPreferences(context, Constants.KEY_COUNTS_PER_INAPP);
        final SharedPreferences.Editor editor = oldPrefs.edit();

        final SharedPreferences newPrefs = getPreferences(context,
                getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId));
        final SharedPreferences.Editor newEditor = newPrefs.edit();

        final Map<String, ?> all = oldPrefs.getAll();
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
            newEditor.putString(inapp, ov.toString());
        }
        StorageHelper.persist(newEditor);
        editor.clear().apply();
    }

    private String storageKeyWithSuffix(String key) {
        return key + ":" + getConfigAccountId();
    }
}
