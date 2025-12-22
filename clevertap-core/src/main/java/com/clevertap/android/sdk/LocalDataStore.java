package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.Constants.GET_MARKER;
import static com.clevertap.android.sdk.Constants.piiDBKeys;

import static java.util.Collections.emptyMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.cryption.EncryptionLevel;
import com.clevertap.android.sdk.cryption.ICryptHandler;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.db.DBAdapter;
import com.clevertap.android.sdk.events.EventDetail;
import com.clevertap.android.sdk.profile.ProfileStateTraverser;
import com.clevertap.android.sdk.profile.traversal.ProfileOperation;
import com.clevertap.android.sdk.profile.traversal.ProfileChange;
import com.clevertap.android.sdk.usereventlogs.UserEventLog;
import com.clevertap.android.sdk.utils.NestedJsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;

@SuppressWarnings("unused")
@RestrictTo(Scope.LIBRARY)
public class LocalDataStore {

    private static long EXECUTOR_THREAD_ID = 0;

    private final JSONObject PROFILE_FIELDS_IN_THIS_SESSION = new JSONObject();

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final ICryptHandler cryptHandler;
    private final BaseDatabaseManager baseDatabaseManager;

    private final ExecutorService es;

    private final String eventNamespace = "local_events";

    private final DeviceInfo deviceInfo;
    private final Set<String> userNormalizedEventLogKeys = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, String> normalizedEventNames = new HashMap<>();

    private final ProfileStateTraverser profileStateTraverser;
    private final NestedJsonBuilder nestedJsonBuilder;

    LocalDataStore(Context context, CleverTapInstanceConfig config, ICryptHandler cryptHandler, DeviceInfo deviceInfo, BaseDatabaseManager baseDatabaseManager, ProfileStateTraverser profileStateTraverser, NestedJsonBuilder nestedJsonBuilder) {
        this.context = context;
        this.config = config;
        this.es = Executors.newFixedThreadPool(1);
        this.cryptHandler = cryptHandler;
        this.deviceInfo = deviceInfo;
        this.baseDatabaseManager = baseDatabaseManager;
        this.profileStateTraverser = profileStateTraverser;
        this.nestedJsonBuilder = nestedJsonBuilder;
    }

    @WorkerThread
    public void changeUser() {
        userNormalizedEventLogKeys.clear();
        resetLocalProfileSync();
    }

    /**
     * @deprecated since <code>v7.1.0</code>. Use {@link #readUserEventLog(String)}
     */
    @Deprecated(since = "7.1.0")
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
    /**
     * @deprecated since <code>v7.1.0</code>. Use {@link #readUserEventLogs()}
     */
    @Deprecated(since = "7.1.0")
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

    /**
     * @deprecated since <code>v7.1.0</code>. Use {@link #persistUserEventLog(String)}
     */
    @Deprecated(since = "7.1.0")
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
    public boolean persistUserEventLogsInBulk(Set<String> eventNames){
        Set<Pair<String, String>> setOfActualAndNormalizedEventNamePair = new HashSet<>();
        CollectionsKt.mapTo(eventNames, setOfActualAndNormalizedEventNamePair,
                (actualEventName) -> new Pair<>(actualEventName, getOrPutNormalizedEventName(actualEventName)));
        return upsertUserEventLogsInBulk(setOfActualAndNormalizedEventNamePair);
    }

    @WorkerThread
    public boolean persistUserEventLog(String eventName) {

        if (eventName == null) {
            return false;
        }

        Logger logger = config.getLogger();
        String accountId = config.getAccountId();
        try {
            logger.verbose(accountId,"UserEventLog: Persisting EventLog for event "+eventName);
            if (isUserEventLogExists(eventName)){
                logger.verbose(accountId,"UserEventLog: Updating EventLog for event "+eventName);
                return updateUserEventLog(eventName);
            } else {
                logger.verbose(accountId,"UserEventLog: Inserting EventLog for event "+eventName);
                return insertUserEventLog(eventName );
            }
            /*
             * ==========TESTING BLOCK START ==========
             */
            /*cleanUpExtraEvents(50);

            UserEventLog userEventLog = readUserEventLog(eventName);
            logger.verbose(accountId,"UserEventLog: EventLog for event "+eventName+" = "+userEventLog);

            List<UserEventLog> list = readUserEventLogs();
            logger.verbose(accountId,"UserEventLog: All EventLog list for User "+list);

            List<UserEventLog> list1 = readEventLogsForAllUsers();
            logger.verbose(accountId,"UserEventLog: All user EventLog list "+list1);

            int count = readUserEventLogCount(eventName);
            logger.verbose(accountId,"UserEventLog: EventLog count for event "+eventName+" = "+count);

            long logFirstTs = readUserEventLogFirstTs(eventName);
            logger.verbose(accountId,"UserEventLog: EventLog firstTs for event "+eventName+" = "+logFirstTs);

            long logLastTs = readUserEventLogLastTs(eventName);
            logger.verbose(accountId,"UserEventLog: EventLog lastTs for event "+eventName+" = "+logLastTs);

            boolean isUserEventLogFirstTime = isUserEventLogFirstTime(eventName);
            logger.verbose(accountId,"UserEventLog: EventLog isUserEventLogFirstTime for event "+eventName+" = "+isUserEventLogFirstTime);*/
            /*
             * ==========TESTING BLOCK END ==========
             */
        } catch (Throwable t) {
            logger.verbose(accountId, "UserEventLog: Failed to insert user event log: for event" + eventName, t);
            return false;
        }
    }

    @WorkerThread
    private boolean updateEventByDeviceIdAndNormalizedEventName(String deviceID, String normalizedEventName) {
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        boolean updatedEventByDeviceID = dbAdapter.userEventLogDAO().updateEventByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
        getConfigLogger().verbose("updatedEventByDeviceID = " + updatedEventByDeviceID);
        return updatedEventByDeviceID;
    }

    @WorkerThread
    public boolean updateUserEventLog(String eventName) {
        String deviceID = deviceInfo.getDeviceID();
        String normalizedEventName = getOrPutNormalizedEventName(eventName);
        return updateEventByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
    }

    @WorkerThread
    private boolean upsertUserEventLogsInBulk(Set<Pair<String, String>> setOfActualAndNormalizedEventNamePair) {
        String deviceID = deviceInfo.getDeviceID();
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        boolean upsertEventByDeviceID = dbAdapter.userEventLogDAO()
                .upsertEventsByDeviceIdAndNormalizedEventName(deviceID, setOfActualAndNormalizedEventNamePair);
        getConfigLogger().verbose("upsertEventByDeviceID = " + upsertEventByDeviceID);
        return upsertEventByDeviceID;
    }

    @WorkerThread
    private long insertEvent(String deviceID, String actualEventName, String normalizedEventName) {
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        long rowId = dbAdapter.userEventLogDAO().insertEvent(deviceID, actualEventName, normalizedEventName);
        getConfigLogger().verbose("inserted rowId = " + rowId);
        return rowId;
    }

    private String getOrPutNormalizedEventName(String actualEventName) {
        return MapsKt.getOrPut(normalizedEventNames, actualEventName,
                () -> Utils.getNormalizedName(actualEventName));
    }

    @WorkerThread
    public boolean insertUserEventLog(String eventName) {
        String deviceID = deviceInfo.getDeviceID();
        String normalizedEventName = getOrPutNormalizedEventName(eventName);
        long rowId = insertEvent(deviceID, eventName, normalizedEventName);
        return rowId >= 0;
    }

    @WorkerThread
    private boolean eventExistsByDeviceIdAndNormalizedEventName(String deviceID, String normalizedEventName) {
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        boolean eventExists = dbAdapter.userEventLogDAO().eventExistsByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
        getConfigLogger().verbose("eventExists = "+eventExists);
        return eventExists;
    }

    @WorkerThread
    public boolean isUserEventLogExists(String eventName) {
        String deviceID = deviceInfo.getDeviceID();
        String normalizedEventName = getOrPutNormalizedEventName(eventName);
        return eventExistsByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
    }

    @WorkerThread
    private boolean eventExistsByDeviceIdAndNormalizedEventNameAndCount(String deviceID, String normalizedEventName, int count) {
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        boolean eventExistsByDeviceIDAndCount = dbAdapter.userEventLogDAO()
                .eventExistsByDeviceIdAndNormalizedEventNameAndCount(deviceID, normalizedEventName, count);

        getConfigLogger().verbose("eventExistsByDeviceIDAndCount = " + eventExistsByDeviceIDAndCount);
        return eventExistsByDeviceIDAndCount;
    }

    @WorkerThread
    public boolean isUserEventLogFirstTime(String eventName) {
        String normalizedEventName = getOrPutNormalizedEventName(eventName);
        if (userNormalizedEventLogKeys.contains(normalizedEventName)) {
            return false;
        }

        String deviceID = deviceInfo.getDeviceID();
        int count = readEventCountByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
        if (count > 1) {
            userNormalizedEventLogKeys.add(normalizedEventName);
        }
        return count == 1;
    }

    @WorkerThread
    public boolean cleanUpExtraEvents(int threshold, int numberOfRowsToCleanup){
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        boolean cleanUpExtraEvents = dbAdapter.userEventLogDAO().cleanUpExtraEvents(threshold, numberOfRowsToCleanup);
        getConfigLogger().verbose("cleanUpExtraEvents boolean= "+cleanUpExtraEvents);
        return cleanUpExtraEvents;
    }

    @WorkerThread
    private UserEventLog readEventByDeviceIdAndNormalizedEventName(String deviceID, String normalizedEventName) {
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        return dbAdapter.userEventLogDAO().readEventByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
    }

    @WorkerThread
    public UserEventLog readUserEventLog(String eventName) {
        String deviceID = deviceInfo.getDeviceID();
        String normalizedEventName = getOrPutNormalizedEventName(eventName);
        return readEventByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
    }

    @WorkerThread
    private int readEventCountByDeviceIdAndNormalizedEventName(String deviceID, String normalizedEventName) {
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        return dbAdapter.userEventLogDAO().readEventCountByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
    }

    @WorkerThread
    public int readUserEventLogCount(String eventName) {
        String deviceID = deviceInfo.getDeviceID();
        String normalizedEventName = getOrPutNormalizedEventName(eventName);
        return readEventCountByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName);
    }

    @WorkerThread
    private List<UserEventLog> allEventsByDeviceID(String deviceID) {
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        return dbAdapter.userEventLogDAO().allEventsByDeviceID(deviceID);
    }

    @WorkerThread
    public List<UserEventLog> readUserEventLogs(){
        String deviceID = deviceInfo.getDeviceID();
        return allEventsByDeviceID(deviceID);
    }

    @WorkerThread
    public List<UserEventLog> readEventLogsForAllUsers() {
        DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
        return dbAdapter.userEventLogDAO().allEvents();
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

    public JSONObject getProfile() {
        try {
            synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
                return new JSONObject(PROFILE_FIELDS_IN_THIS_SESSION.toString());
            }
        } catch (JSONException e) {
            // Handle exception or return empty object
            return new JSONObject();
        }
    }

    public Object getProfileProperty(String key) {
        if (key == null) {
            return null;
        }

        synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
            ProfileChange profileChange = processProfileTree(key, GET_MARKER, ProfileOperation.GET).get(key);
            return profileChange == null ? null : profileChange.getOldValue();
        }
    }

    /**
     * @deprecated since <code>v7.1.0</code> in favor of DB. See {@link UserEventLog}
     */
    @Deprecated(since = "7.1.0")
    private EventDetail decodeEventDetails(String name, String encoded) {
        if (encoded == null) {
            return null;
        }

        String[] parts = encoded.split("\\|");
        return new EventDetail(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]), name);
    }

    /**
     * @deprecated since <code>v7.1.0</code> in favor of DB. See {@link UserEventLog}
     */
    @Deprecated(since = "7.1.0")
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

    /**
     * @deprecated since <code>v7.1.0</code> in favor of DB. See {@link UserEventLog}
     */
    @Deprecated(since = "7.1.0")
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
    void inflateLocalProfileAsync(final Context context) {

        final String accountID = this.config.getAccountId();

        this.postAsyncSafely("LocalDataStore#inflateLocalProfileAsync", new Runnable() {
            @Override
            public void run() {
                DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
                synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
                    try {
                        JSONObject profile = dbAdapter.fetchUserProfileByAccountIdAndDeviceID(accountID, deviceInfo.getDeviceID());

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
                                    Object decrypted = value;
                                    if (value instanceof String) {
                                        decrypted = cryptHandler.decryptSafe((String) value);
                                        if (decrypted == null)
                                            decrypted = value;
                                    }
                                    PROFILE_FIELDS_IN_THIS_SESSION.put(key, decrypted);
                                }
                            } catch (JSONException e) {
                                // no-op
                            }
                        }

                        getConfigLogger().verbose(getConfigAccountId(),
                                "Local Data Store - Inflated local profile " + PROFILE_FIELDS_IN_THIS_SESSION);

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

    /**
     * @deprecated since <code>v7.1.0</code>. Use {@link #persistUserEventLog(String)}
     */
    @Deprecated(since = "7.1.0")
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
                    JSONObject profile = new JSONObject();
                    boolean encryptionFailed = false;

                    // Copy all fields from PROFILE_FIELDS_IN_THIS_SESSION to profile
                    try {
                        Iterator<String> keys = PROFILE_FIELDS_IN_THIS_SESSION.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            profile.put(key, PROFILE_FIELDS_IN_THIS_SESSION.get(key));
                        }
                    } catch (JSONException e) {
                        getConfigLogger().verbose(getConfigAccountId(), "Failed to copy profile fields", e);
                    }

                    // Encrypts only the pii keys before storing to DB
                    boolean isMediumEncryption = EncryptionLevel.fromInt(config.getEncryptionLevel()) == EncryptionLevel.MEDIUM;
                    for (String piiKey : piiDBKeys) {
                        try {
                            if (profile.has(piiKey)) {
                                Object value = profile.opt(piiKey);
                                if (value instanceof String) {
                                    if (isMediumEncryption) {
                                        value = cryptHandler.encryptSafe((String) value);
                                        if (value == null) {
                                            encryptionFailed = true;
                                            // Don't update profile with null, keep original
                                            continue;
                                        }
                                        profile.put(piiKey, value);
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            getConfigLogger().verbose(getConfigAccountId(), "Failed to encrypt pii key: " + piiKey, e);
                            encryptionFailed = true;
                        }
                    }

                    // Update migration failure count ONCE after processing all keys
                    if (encryptionFailed) {
                        cryptHandler.updateMigrationFailureCount(false);
                    }

                    DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
                    long status = dbAdapter.storeUserProfile(profileID, deviceInfo.getDeviceID(), profile);
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

    private void resetLocalProfileSync() {
        synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
            Iterator<String> keys = PROFILE_FIELDS_IN_THIS_SESSION.keys();
            List<String> keysToRemove = new ArrayList<>();
            while (keys.hasNext()) {
                keysToRemove.add(keys.next());
            }
            for (String key : keysToRemove) {
                PROFILE_FIELDS_IN_THIS_SESSION.remove(key);
            }
        }

        // Load the older profile from cache into the db
        inflateLocalProfileAsync(context);
    }

    private String storageKeyWithSuffix(String key) {
        return key + ":" + this.config.getAccountId();
    }

    private String stringify(Object value) {
        return (value == null) ? "" : value.toString();
    }

    @WorkerThread
    public Map<String, ProfileChange> processProfileTree(
            String dotNotationKey,
            Object value,
            ProfileOperation operation
    ) {
        try {
            JSONObject nestedProfile = nestedJsonBuilder.buildFromPath(dotNotationKey, value);
            return processProfileTree(nestedProfile, operation);
        } catch (JSONException e) {
            return emptyMap();
        }
    }


    @WorkerThread
    public Map<String, ProfileChange> processProfileTree(
            JSONObject newJson,
            ProfileOperation operation
    ) {
        synchronized (PROFILE_FIELDS_IN_THIS_SESSION) {
            ProfileStateTraverser.ProfileTraversalResult result = profileStateTraverser.traverse(
                    PROFILE_FIELDS_IN_THIS_SESSION,
                    newJson,
                    operation
            );

            if (operation != ProfileOperation.GET) {
                persistLocalProfileAsync();
            }

            // Return the changes map
            return result.getChanges();
        }
    }
}