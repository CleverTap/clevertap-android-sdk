package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.db.DBAdapter;
import com.clevertap.android.sdk.events.EventDetail;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
@RestrictTo(Scope.LIBRARY)
public class LocalDataStore {

    private static long EXECUTOR_THREAD_ID = 0;

    /**
     * Whenever a profile field is updated, in the session, put it here.
     * The value must be an epoch until how long it is valid for (using existing TTL).
     * <p/>
     * When upstream updates come in, check whether or not to update the field.
     */
    private final HashMap<String, Integer> PROFILE_EXPIRY_MAP = new HashMap<>();

    private final HashMap<String, Object> PROFILE_FIELDS_IN_THIS_SESSION = new HashMap<>();

    private final CleverTapInstanceConfig config;

    private final Context context;

    private DBAdapter dbAdapter;

    private final ExecutorService es;

    private final String eventNamespace = "local_events";


    LocalDataStore(Context context, CleverTapInstanceConfig config) {
        this.context = context;
        this.config = config;
        this.es = Executors.newFixedThreadPool(1);
        inflateLocalProfileAsync(context);
    }

    @WorkerThread
    public void changeUser() {
        resetLocalProfileSync();
    }

    EventDetail getEventDetail(String eventName) {
        try {
            if (!isPersonalisationEnabled()) {
                return null;
            }
            String namespace;
            if (!this.config.isDefaultInstance()) {
                namespace = eventNamespace + ":" + this.config.getAccountId();
            } else {
                namespace = eventNamespace;
            }
            return decodeEventDetails(eventName, getStringFromPrefs(eventName, null, namespace));
        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to retrieve local event detail", t);
            return null;
        }
    }

    Map<String, EventDetail> getEventHistory(Context context) {
        try {
            String namespace;
            if (!this.config.isDefaultInstance()) {
                namespace = eventNamespace + ":" + this.config.getAccountId();
            } else {
                namespace = eventNamespace;
            }
            SharedPreferences prefs = StorageHelper.getPreferences(context, namespace);
            Map<String, ?> all = prefs.getAll();
            Map<String, EventDetail> out = new HashMap<>();
            for (String eventName : all.keySet()) {
                //noinspection ConstantConditions
                out.put(eventName, decodeEventDetails(eventName, all.get(eventName).toString()));
            }
            return out;
        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to retrieve local event history", t);
            return null;
        }
    }

    Object getProfileProperty(String key) {
        return getProfileValueForKey(key);
    }

    Object getProfileValueForKey(String key) {
        return _getProfileProperty(key);
    }

    @WorkerThread
    public void persistEvent(Context context, JSONObject event, int type) {

        if (event == null) {
            return;
        }

        try {
            if (type == Constants.RAISED_EVENT) {
                persistEvent(context, event);
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to sync with upstream", t);
        }
    }

    @WorkerThread
    void removeProfileField(String key) {
        removeProfileField(key, false, true);
    }

    void removeProfileFields(ArrayList<String> fields) {
        if (fields == null) {
            return;
        }
        removeProfileFields(fields, false);
    }

    @WorkerThread
    public void setDataSyncFlag(JSONObject event) {
        try {
            // Check the personalisation flag
            boolean enablePersonalisation = this.config.isPersonalizationEnabled();
            if (!enablePersonalisation) {
                event.put("dsync", false);
                return;
            }

            // Always request a dsync when the App Launched event is recorded
            final String eventType = event.getString("type");
            if ("event".equals(eventType)) {
                final String evtName = event.getString("evtName");
                if (Constants.APP_LAUNCHED_EVENT.equals(evtName)) {
                    getConfigLogger().verbose(getConfigAccountId(),
                            "Local cache needs to be updated (triggered by App Launched)");
                    event.put("dsync", true);
                    return;
                }
            }

            // If a profile event, then blindly set it to true
            if ("profile".equals(eventType)) {
                event.put("dsync", true);
                getConfigLogger().verbose(getConfigAccountId(), "Local cache needs to be updated (profile event)");
                return;
            }

            // Default to expire in 20 minutes
            final int now = (int) (System.currentTimeMillis() / 1000);

            int expiresIn = getLocalCacheExpiryInterval(20 * 60);

            int lastUpdate = getIntFromPrefs("local_cache_last_update", now);

            if (lastUpdate + expiresIn < now) {
                event.put("dsync", true);
                getConfigLogger().verbose(getConfigAccountId(), "Local cache needs to be updated");
            } else {
                event.put("dsync", false);
                getConfigLogger().verbose(getConfigAccountId(), "Local cache doesn't need to be updated");
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to sync with upstream", t);
        }
    }

    void setProfileField(String key, Object value) {
        setProfileField(key, value, false, true);
    }

    void setProfileFields(JSONObject fields) {
        setProfileFields(fields, false);
    }

    //Not used.Remove later
    @SuppressWarnings("rawtypes")
    public void syncWithUpstream(Context context, JSONObject response) {
        try {
            JSONObject eventUpdates = null;
            JSONObject profileUpdates = null;

            if (!response.has("evpr")) {
                return;
            }

            JSONObject evpr = response.getJSONObject("evpr");
            if (evpr.has("profile")) {
                JSONObject profile = evpr.getJSONObject("profile");
                if (profile.has("_custom")) {
                    JSONObject custom = profile.getJSONObject("_custom");
                    profile.remove("_custom");
                    Iterator keys = custom.keys();
                    while (keys.hasNext()) {
                        String next = keys.next().toString();

                        Object value = null;
                        try {
                            value = custom.getJSONArray(next);
                        } catch (Throwable t) {
                            try {
                                value = custom.get(next);
                            } catch (JSONException e) {
                                //no-op
                            }
                        }

                        if (value != null) {
                            profile.put(next, value);
                        }
                    }
                }

                profileUpdates = syncProfile(profile);
            }

            if (evpr.has("events")) {
                eventUpdates = syncEventsFromUpstream(context, evpr.getJSONObject("events"));
            }

            if (evpr.has("expires_in")) {
                int expiresIn = evpr.getInt("expires_in");
                setLocalCacheExpiryInterval(context, expiresIn);
            }

            StorageHelper.putInt(context, storageKeyWithSuffix("local_cache_last_update"),
                    (int) (System.currentTimeMillis() / 1000));

            Boolean profileUpdatesNotEmpty = (profileUpdates != null && profileUpdates.length() > 0);
            Boolean eventsUpdatesNotEmpty = (eventUpdates != null && eventUpdates.length() > 0);
            if (profileUpdatesNotEmpty || eventsUpdatesNotEmpty) {
                JSONObject updates = new JSONObject();

                if (profileUpdatesNotEmpty) {
                    updates.put("profile", profileUpdates);
                }

                if (eventsUpdatesNotEmpty) {
                    updates.put("events", eventUpdates);
                }
                SyncListener syncListener = null;
                try {
                    CleverTapAPI ct = CleverTapAPI.getDefaultInstance(context);
                    if (ct != null) {
                        syncListener = ct.getSyncListener();
                    }
                } catch (Throwable t) {
                    // no-op
                }
                if (syncListener != null) {
                    try {
                        syncListener.profileDataUpdated(updates);
                    } catch (Throwable t) {
                        getConfigLogger().verbose(getConfigAccountId(), "Execution of sync listener failed", t);
                    }
                }
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to sync with upstream", t);
        }
    }

    private Object _getProfileProperty(String key) {

        if (key == null) {
            return null;
        }

        synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
            try {
                return PROFILE_FIELDS_IN_THIS_SESSION.get(key);

            } catch (Throwable t) {
                getConfigLogger().verbose(getConfigAccountId(), "Failed to retrieve local profile property", t);
                return null;
            }
        }
    }

    private void _removeProfileField(String key) {

        if (key == null) {
            return;
        }

        synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
            try {
                PROFILE_FIELDS_IN_THIS_SESSION.remove(key);

            } catch (Throwable t) {
                getConfigLogger()
                        .verbose(getConfigAccountId(), "Failed to remove local profile value for key " + key, t);
            }
        }
    }

    private void _setProfileField(String key, Object value) {
        if (key == null || value == null) {
            return;
        }

        synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
            PROFILE_FIELDS_IN_THIS_SESSION.put(key, value);
        }
    }

    private JSONObject buildChangeFromOldValueToNewValue(Object oldValue, Object newValue) {

        if (oldValue == null && newValue == null) {
            return null;
        }

        JSONObject keyUpdates = new JSONObject();

        try {
            // if newValue is null means its been removed, represent that as -1
            Object _newVal = (newValue != null) ? newValue : -1;
            keyUpdates.put("newValue", _newVal);

            if (oldValue != null) {
                keyUpdates.put("oldValue", oldValue);
            }

        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to create profile changed values object", t);
            return null;
        }

        return keyUpdates;
    }

    private int calculateLocalKeyExpiryTime() {
        final int now = (int) (System.currentTimeMillis() / 1000);
        return (now + getLocalCacheExpiryInterval(0));
    }

    private EventDetail decodeEventDetails(String name, String encoded) {
        if (encoded == null) {
            return null;
        }

        String[] parts = encoded.split("\\|");
        return new EventDetail(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]), name);
    }

    private String encodeEventDetails(int first, int last, int count) {
        return count + "|" + first + "|" + last;
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

    private int getLocalCacheExpiryInterval(int defaultInterval) {
        return getIntFromPrefs("local_cache_expires_in", defaultInterval);
    }

    private Integer getLocalProfileKeyExpiryTimeForKey(String key) {
        if (key == null) {
            return 0;
        }

        synchronized (PROFILE_EXPIRY_MAP) {
            return PROFILE_EXPIRY_MAP.get(key);
        }
    }

    private String getStringFromPrefs(String rawKey, String defaultValue, String nameSpace) {
        if (this.config.isDefaultInstance()) {
            String _new = StorageHelper
                    .getString(this.context, nameSpace, storageKeyWithSuffix(rawKey), defaultValue);
            return _new != null ? _new : StorageHelper.getString(this.context, nameSpace, rawKey, defaultValue);
        } else {
            return StorageHelper.getString(this.context, nameSpace, storageKeyWithSuffix(rawKey), defaultValue);
        }
    }

    private String getUserProfileID() {
        return this.config.getAccountId();
    }

    // local cache/profile key expiry handling
    private void inflateLocalProfileAsync(final Context context) {

        final String accountID = this.config.getAccountId();

        this.postAsyncSafely("LocalDataStore#inflateLocalProfileAsync", new Runnable() {
            @Override
            public void run() {
                if (dbAdapter == null) {
                    dbAdapter = new DBAdapter(context, config);
                }
                synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
                    try {
                        JSONObject profile = dbAdapter.fetchUserProfileById(accountID);

                        if (profile == null) {
                            return;
                        }

                        Iterator<?> keys = profile.keys();
                        while (keys.hasNext()) {
                            try {
                                String key = (String) keys.next();
                                Object value = profile.get(key);
                                if (value instanceof JSONObject) {
                                    JSONObject jsonObject = profile.getJSONObject(key);
                                    PROFILE_FIELDS_IN_THIS_SESSION.put(key, jsonObject);
                                } else if (value instanceof JSONArray) {
                                    JSONArray jsonArray = profile.getJSONArray(key);
                                    PROFILE_FIELDS_IN_THIS_SESSION.put(key, jsonArray);
                                } else {
                                    PROFILE_FIELDS_IN_THIS_SESSION.put(key, value);
                                }
                            } catch (JSONException e) {
                                // no-op
                            }
                        }

                        getConfigLogger().verbose(getConfigAccountId(),
                                "Local Data Store - Inflated local profile " + PROFILE_FIELDS_IN_THIS_SESSION
                                        .toString());

                    } catch (Throwable t) {
                        //no-op
                    }
                }
            }
        });

    }

    private boolean isPersonalisationEnabled() {
        return this.config.isPersonalizationEnabled();
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("CommitPrefEdits")
    private void persistEvent(Context context, JSONObject event) {
        try {
            String evtName = event.getString("evtName");
            if (evtName == null) {
                return;
            }
            String namespace;
            if (!this.config.isDefaultInstance()) {
                namespace = eventNamespace + ":" + this.config.getAccountId();
            } else {
                namespace = eventNamespace;
            }
            SharedPreferences prefs = StorageHelper.getPreferences(context, namespace);

            int now = (int) (System.currentTimeMillis() / 1000);

            String encoded = getStringFromPrefs(evtName, encodeEventDetails(now, now, 0), namespace);
            EventDetail ed = decodeEventDetails(evtName, encoded);

            String updateEncoded = encodeEventDetails(ed.getFirstTime(), now, ed.getCount() + 1);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(storageKeyWithSuffix(evtName), updateEncoded);
            StorageHelper.persist(editor);
        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to persist event locally", t);
        }
    }

    private void persistLocalProfileAsync() {

        final String profileID = this.config.getAccountId();

        this.postAsyncSafely("LocalDataStore#persistLocalProfileAsync", new Runnable() {
            @Override
            public void run() {
                synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
                    long status = dbAdapter
                            .storeUserProfile(profileID, new JSONObject(PROFILE_FIELDS_IN_THIS_SESSION));
                    getConfigLogger().verbose(getConfigAccountId(),
                            "Persist Local Profile complete with status " + status + " for id " + profileID);
                }
            }
        });
    }

    private void postAsyncSafely(final String name, final Runnable runnable) {
        try {
            final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;

            if (executeSync) {
                runnable.run();
            } else {
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                        try {
                            getConfigLogger().verbose(getConfigAccountId(),
                                    "Local Data Store Executor service: Starting task - " + name);
                            runnable.run();
                        } catch (Throwable t) {
                            getConfigLogger().verbose(getConfigAccountId(),
                                    "Executor service: Failed to complete the scheduled task", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to submit task to the executor service", t);
        }
    }

    private boolean profileValueIsEmpty(Object value) {

        if (value == null) {
            return true;
        }

        boolean isEmpty = false;

        if (value instanceof String) {
            isEmpty = ((String) value).trim().length() == 0;
        }

        if (value instanceof JSONArray) {
            isEmpty = ((JSONArray) value).length() <= 0;
        }

        return isEmpty;
    }

    private Boolean profileValuesAreEqual(Object value1, Object value2) {
        // convert to strings and compare
        // stringify handles null values
        return stringify(value1).equals(stringify(value2));
    }

    private void removeLocalProfileKeyExpiryTime(String key) {
        if (key == null) {
            return;
        }

        synchronized (PROFILE_EXPIRY_MAP) {
            PROFILE_EXPIRY_MAP.remove(key);
        }
    }

    private void removeProfileField(String key, Boolean fromUpstream, boolean persist) {

        if (key == null) {
            return;
        }

        try {
            _removeProfileField(key);

            // even though its a remove add an expiration time for the local key, as we still need it in the sync
            if (!fromUpstream) {
                updateLocalProfileKeyExpiryTime(key);
            }

        } catch (Throwable t) {
            // no-op
        }

        if (persist) {
            persistLocalProfileAsync();
        }

    }

    @SuppressWarnings("SameParameterValue")
    private void removeProfileFields(ArrayList<String> fields, Boolean fromUpstream) {
        if (fields == null) {
            return;
        }

        for (String key : fields) {
            removeProfileField(key, fromUpstream, false);
        }
        persistLocalProfileAsync();
    }

    private void resetLocalProfileSync() {

        synchronized (PROFILE_EXPIRY_MAP) {
            PROFILE_EXPIRY_MAP.clear();
        }

        synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
            PROFILE_FIELDS_IN_THIS_SESSION.clear();
        }

        final String accountID = getUserProfileID();
        dbAdapter.removeUserProfile(accountID);
    }

    private void setLocalCacheExpiryInterval(final Context context, final int ttl) {
        StorageHelper.putInt(context, storageKeyWithSuffix("local_cache_expires_in"), ttl);
    }


    private void setProfileField(String key, Object value, Boolean fromUpstream, boolean persist) {
        if (key == null || value == null) {
            return;
        }

        try {
            _setProfileField(key, value);

            if (!fromUpstream) {
                updateLocalProfileKeyExpiryTime(key);
            }
        } catch (Throwable t) {
            // no-op
        }
        if (persist) {
            persistLocalProfileAsync();
        }
    }

    @SuppressWarnings("rawtypes")
    private void setProfileFields(JSONObject fields, Boolean fromUpstream) {
        if (fields == null) {
            return;
        }

        try {
            final Iterator keys = fields.keys();

            while (keys.hasNext()) {
                String key = keys.next().toString();
                setProfileField(key, fields.get(key), fromUpstream, false);
            }
            persistLocalProfileAsync();

        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to set profile fields", t);
        }
    }

    private Boolean shouldPreferLocalProfileUpdateForKeyForTime(String key, int time) {
        final int now = (time <= 0) ? (int) (System.currentTimeMillis() / 1000) : time;
        Integer keyValidUntil = getLocalProfileKeyExpiryTimeForKey(key);
        return (keyValidUntil != null && keyValidUntil > now);
    }

    private String storageKeyWithSuffix(String key) {
        return key + ":" + this.config.getAccountId();
    }

    private String stringify(Object value) {
        return (value == null) ? "" : value.toString();
    }

    //Not used.Remove later
    @SuppressWarnings({"rawtypes", "ConstantConditions"})
    private JSONObject syncEventsFromUpstream(Context context, JSONObject events) {
        try {
            JSONObject eventUpdates = null;
            String namespace;
            if (!this.config.isDefaultInstance()) {
                namespace = eventNamespace + ":" + this.config.getAccountId();
            } else {
                namespace = eventNamespace;
            }
            SharedPreferences prefs = StorageHelper.getPreferences(context, namespace);
            Iterator keys = events.keys();
            SharedPreferences.Editor editor = prefs.edit();

            while (keys.hasNext()) {
                String event = keys.next().toString();
                String encoded = getStringFromPrefs(event, encodeEventDetails(0, 0, 0), namespace);

                EventDetail ed = decodeEventDetails(event, encoded);

                JSONArray upstream = events.getJSONArray(event);
                if (upstream == null || upstream.length() < 3) {
                    getConfigLogger().verbose(getConfigAccountId(), "Corrupted upstream event detail");
                    continue;
                }

                int upstreamCount, first, last;
                try {
                    upstreamCount = upstream.getInt(0);
                    first = upstream.getInt(1);
                    last = upstream.getInt(2);
                } catch (Throwable t) {
                    getConfigLogger().verbose(getConfigAccountId(),
                            "Failed to parse upstream event message: " + upstream.toString());
                    continue;
                }

                if (upstreamCount > ed.getCount()) {
                    editor.putString(storageKeyWithSuffix(event), encodeEventDetails(first, last, upstreamCount));
                    getConfigLogger()
                            .verbose(getConfigAccountId(), "Accepted update for event " + event + " from upstream");

                    try {
                        if (eventUpdates == null) {
                            eventUpdates = new JSONObject();
                        }

                        JSONObject evUpdate = new JSONObject();

                        JSONObject countUpdate = new JSONObject();
                        countUpdate.put("oldValue", ed.getCount());
                        countUpdate.put("newValue", upstreamCount);
                        evUpdate.put("count", countUpdate);

                        JSONObject firstUpdate = new JSONObject();
                        firstUpdate.put("oldValue", ed.getFirstTime());
                        firstUpdate.put("newValue", upstream.getInt(1));
                        evUpdate.put("firstTime", firstUpdate);

                        JSONObject lastUpdate = new JSONObject();
                        lastUpdate.put("oldValue", ed.getLastTime());
                        lastUpdate.put("newValue", upstream.getInt(2));
                        evUpdate.put("lastTime", lastUpdate);

                        eventUpdates.put(event, evUpdate);

                    } catch (Throwable t) {
                        getConfigLogger().verbose(getConfigAccountId(), "Couldn't set event updates", t);
                    }

                } else {
                    getConfigLogger()
                            .verbose(getConfigAccountId(), "Rejected update for event " + event + " from upstream");
                }
            }
            StorageHelper.persist(editor);
            return eventUpdates;
        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Couldn't sync events from upstream", t);
            return null;
        }
    }
    //Not used.Remove later
    @SuppressWarnings("rawtypes")
    private JSONObject syncProfile(JSONObject remoteProfile) {

        // Will hold the changes to be returned
        JSONObject profileUpdates = new JSONObject();

        if (remoteProfile == null || remoteProfile.length() <= 0) {
            return profileUpdates;
        }

        try {

            // will hold the updated fields that need to be written to the local profile
            JSONObject fieldsToUpdateLocally = new JSONObject();

            // cache the current time for shouldPreferLocalUpdateForKey check
            final int now = (int) (System.currentTimeMillis() / 1000);

            // walk the remote profile and compare values against the local profile values
            // prefer the remote profile value unless we have set a still-valid expiration time for the local profile value
            final Iterator keys = remoteProfile.keys();

            while (keys.hasNext()) {
                try {

                    String key = keys.next().toString();

                    if (shouldPreferLocalProfileUpdateForKeyForTime(key, now)) {
                        // We shouldn't accept the upstream value, as our map
                        // forces us to use the local
                        getConfigLogger()
                                .verbose(getConfigAccountId(), "Rejecting upstream value for key " + key + " " +
                                        "because our local cache prohibits it");
                        continue;
                    }

                    Object localValue = getProfileValueForKey(key);

                    Object remoteValue = remoteProfile.get(key);

                    // if remoteValue is empty (empty string or array) treat it as removed, so null it out here
                    // all later tests handle null values
                    if (profileValueIsEmpty(remoteValue)) {
                        remoteValue = null;
                    }

                    // handles null values
                    if (!profileValuesAreEqual(remoteValue, localValue)) {
                        try {
                            // Update required as we prefer the remote value once we've passed the local expiration time check

                            // add the new value to be written to the local profile
                            // if empty send a remove message
                            if (remoteValue != null) {
                                fieldsToUpdateLocally.put(key, remoteValue);
                            } else {
                                removeProfileField(key, true, true);
                            }

                            // add the changed values to the dictionary to be returned
                            // handles null values
                            JSONObject changesObject = buildChangeFromOldValueToNewValue(localValue, remoteValue);
                            if (changesObject != null) {
                                profileUpdates.put(key, changesObject);
                            }

                        } catch (Throwable t) {
                            getConfigLogger().verbose(getConfigAccountId(), "Failed to set profile updates", t);
                        }
                    }

                } catch (Throwable t) {
                    getConfigLogger().verbose(getConfigAccountId(), "Failed to update profile field", t);
                }
            }

            // save the changed fields locally
            if (fieldsToUpdateLocally.length() > 0) {
                setProfileFields(fieldsToUpdateLocally, true);
            }

            return profileUpdates;

        } catch (Throwable t) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to sync remote profile", t);
            return null;
        }
    }

    private void updateLocalProfileKeyExpiryTime(String key) {
        if (key == null) {
            return;
        }

        synchronized (PROFILE_EXPIRY_MAP) {
            PROFILE_EXPIRY_MAP.put(key, calculateLocalKeyExpiryTime());
        }
    }
}
