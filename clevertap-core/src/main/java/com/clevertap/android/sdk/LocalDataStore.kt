package com.clevertap.android.sdk

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants.GET_MARKER
import com.clevertap.android.sdk.Constants.piiDBKeys
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.events.EventDetail
import com.clevertap.android.sdk.profile.ProfileStateTraverser
import com.clevertap.android.sdk.profile.traversal.ProfileChange
import com.clevertap.android.sdk.profile.traversal.ProfileOperation
import com.clevertap.android.sdk.usereventlogs.UserEventLog
import com.clevertap.android.sdk.utils.NestedJsonBuilder
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Bucket E: Leanplum-Android-SDK uses this type, so it stays public (restricted) and every
 * formerly public member keeps its JVM signature.
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
open class LocalDataStore internal constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val cryptHandler: ICryptHandler,
    private val deviceInfo: DeviceInfo,
    private val baseDatabaseManager: BaseDatabaseManager,
    private val profileStateTraverser: ProfileStateTraverser,
    private val nestedJsonBuilder: NestedJsonBuilder
) {

    private val PROFILE_FIELDS_IN_THIS_SESSION = JSONObject()

    private val es: ExecutorService = Executors.newFixedThreadPool(1)

    private val eventNamespace = "local_events"

    private val userNormalizedEventLogKeys: MutableSet<String?> = Collections.synchronizedSet(HashSet())
    private val normalizedEventNames: MutableMap<String?, String?> = HashMap()

    @WorkerThread
    fun changeUser() {
        userNormalizedEventLogKeys.clear()
        resetLocalProfileSync()
    }

    /**
     * @deprecated since `v7.1.0`. Use [readUserEventLog]
     */
    @Deprecated("since v7.1.0. Use readUserEventLog(String)")
    fun getEventDetail(eventName: String?): EventDetail? {
        try {
            if (!isPersonalisationEnabled()) {
                return null
            }
            val namespace: String = if (!this.config.isDefaultInstance) {
                eventNamespace + ":" + this.config.accountId
            } else {
                eventNamespace
            }
            return decodeEventDetails(eventName, getStringFromPrefs(eventName, null, namespace))
        } catch (t: Throwable) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to retrieve local event detail", t)
            return null
        }
    }

    /**
     * @deprecated since `v7.1.0`. Use [readUserEventLogs]
     */
    @Deprecated("since v7.1.0. Use readUserEventLogs()")
    fun getEventHistory(context: Context?): MutableMap<String, EventDetail>? {
        try {
            val namespace: String = if (!this.config.isDefaultInstance) {
                eventNamespace + ":" + this.config.accountId
            } else {
                eventNamespace
            }
            // A null context throws here and is caught below, as in Java.
            val prefs = StorageHelper.getPreferences(context!!, namespace)
            val all = prefs.all
            val out: MutableMap<String, EventDetail> = HashMap()
            for (eventName in all.keys) {
                // Java dereferenced the value without a null check; keep the same NPE (caught below).
                // decodeEventDetails only returns null for a null input.
                out[eventName] = decodeEventDetails(eventName, all[eventName]!!.toString())!!
            }
            return out
        } catch (t: Throwable) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to retrieve local event history", t)
            return null
        }
    }

    /**
     * @deprecated since `v7.1.0`. Use [persistUserEventLog]
     */
    @Deprecated("since v7.1.0. Use persistUserEventLog(String)")
    @WorkerThread
    fun persistEvent(context: Context, event: JSONObject?, type: Int) {
        if (event == null) {
            return
        }

        try {
            if (type == Constants.RAISED_EVENT) {
                persistEvent(context, event)
            }
        } catch (t: Throwable) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to sync with upstream", t)
        }
    }

    @WorkerThread
    fun persistUserEventLogsInBulk(eventNames: Set<String?>): Boolean {
        val setOfActualAndNormalizedEventNamePair: MutableSet<Pair<String?, String?>> = HashSet()
        eventNames.mapTo(setOfActualAndNormalizedEventNamePair) { actualEventName ->
            Pair(actualEventName, getOrPutNormalizedEventName(actualEventName))
        }
        // Nulls flow through unchanged, exactly as with the former Java generics.
        @Suppress("UNCHECKED_CAST")
        return upsertUserEventLogsInBulk(setOfActualAndNormalizedEventNamePair as Set<Pair<String, String>>)
    }

    @WorkerThread
    fun persistUserEventLog(eventName: String?): Boolean {
        if (eventName == null) {
            return false
        }

        val logger = config.logger
        val accountId = config.accountId
        try {
            logger.verbose(accountId, "UserEventLog: Persisting EventLog for event $eventName")
            if (isUserEventLogExists(eventName)) {
                logger.verbose(accountId, "UserEventLog: Updating EventLog for event $eventName")
                return updateUserEventLog(eventName)
            } else {
                logger.verbose(accountId, "UserEventLog: Inserting EventLog for event $eventName")
                return insertUserEventLog(eventName)
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
        } catch (t: Throwable) {
            logger.verbose(accountId, "UserEventLog: Failed to insert user event log: for event$eventName", t)
            return false
        }
    }

    // The DAO takes non-null Strings; a null device id / normalized name failed there in Java too.
    @WorkerThread
    private fun updateEventByDeviceIdAndNormalizedEventName(deviceID: String?, normalizedEventName: String?): Boolean {
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        val updatedEventByDeviceID =
            dbAdapter.userEventLogDAO().updateEventByDeviceIdAndNormalizedEventName(deviceID!!, normalizedEventName!!)
        getConfigLogger().verbose("updatedEventByDeviceID = $updatedEventByDeviceID")
        return updatedEventByDeviceID
    }

    @WorkerThread
    fun updateUserEventLog(eventName: String?): Boolean {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        return updateEventByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName)
    }

    @WorkerThread
    private fun upsertUserEventLogsInBulk(setOfActualAndNormalizedEventNamePair: Set<Pair<String, String>>): Boolean {
        val deviceID = deviceInfo.deviceID
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        val upsertEventByDeviceID = dbAdapter.userEventLogDAO()
            .upsertEventsByDeviceIdAndNormalizedEventName(deviceID!!, setOfActualAndNormalizedEventNamePair)
        getConfigLogger().verbose("upsertEventByDeviceID = $upsertEventByDeviceID")
        return upsertEventByDeviceID
    }

    @WorkerThread
    private fun insertEvent(deviceID: String?, actualEventName: String?, normalizedEventName: String?): Long {
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        val rowId = dbAdapter.userEventLogDAO().insertEvent(deviceID!!, actualEventName!!, normalizedEventName!!)
        getConfigLogger().verbose("inserted rowId = $rowId")
        return rowId
    }

    private fun getOrPutNormalizedEventName(actualEventName: String?): String? {
        return normalizedEventNames.getOrPut(actualEventName) { Utils.getNormalizedName(actualEventName) }
    }

    @WorkerThread
    fun insertUserEventLog(eventName: String?): Boolean {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        val rowId = insertEvent(deviceID, eventName, normalizedEventName)
        return rowId >= 0
    }

    @WorkerThread
    private fun eventExistsByDeviceIdAndNormalizedEventName(deviceID: String?, normalizedEventName: String?): Boolean {
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        val eventExists =
            dbAdapter.userEventLogDAO().eventExistsByDeviceIdAndNormalizedEventName(deviceID!!, normalizedEventName!!)
        getConfigLogger().verbose("eventExists = $eventExists")
        return eventExists
    }

    @WorkerThread
    fun isUserEventLogExists(eventName: String?): Boolean {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        return eventExistsByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName)
    }

    @WorkerThread
    fun isUserEventLogFirstTime(eventName: String?): Boolean {
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        if (userNormalizedEventLogKeys.contains(normalizedEventName)) {
            return false
        }

        val deviceID = deviceInfo.deviceID
        val count = readEventCountByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName)
        if (count > 1) {
            userNormalizedEventLogKeys.add(normalizedEventName)
        }
        return count == 1
    }

    @WorkerThread
    fun cleanUpExtraEvents(threshold: Int, numberOfRowsToCleanup: Int): Boolean {
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        val cleanUpExtraEvents = dbAdapter.userEventLogDAO().cleanUpExtraEvents(threshold, numberOfRowsToCleanup)
        getConfigLogger().verbose("cleanUpExtraEvents boolean= $cleanUpExtraEvents")
        return cleanUpExtraEvents
    }

    @WorkerThread
    private fun readEventByDeviceIdAndNormalizedEventName(deviceID: String?, normalizedEventName: String?): UserEventLog? {
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        return dbAdapter.userEventLogDAO().readEventByDeviceIdAndNormalizedEventName(deviceID!!, normalizedEventName!!)
    }

    @WorkerThread
    fun readUserEventLog(eventName: String?): UserEventLog? {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        return readEventByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName)
    }

    @WorkerThread
    private fun readEventCountByDeviceIdAndNormalizedEventName(deviceID: String?, normalizedEventName: String?): Int {
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        return dbAdapter.userEventLogDAO().readEventCountByDeviceIdAndNormalizedEventName(deviceID!!, normalizedEventName!!)
    }

    @WorkerThread
    fun readUserEventLogCount(eventName: String?): Int {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        return readEventCountByDeviceIdAndNormalizedEventName(deviceID, normalizedEventName)
    }

    @WorkerThread
    private fun allEventsByDeviceID(deviceID: String?): List<UserEventLog> {
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        return dbAdapter.userEventLogDAO().allEventsByDeviceID(deviceID!!)
    }

    @WorkerThread
    fun readUserEventLogs(): List<UserEventLog> {
        val deviceID = deviceInfo.deviceID
        return allEventsByDeviceID(deviceID)
    }

    @WorkerThread
    fun readEventLogsForAllUsers(): List<UserEventLog> {
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        return dbAdapter.userEventLogDAO().allEvents()
    }

    @WorkerThread
    fun setDataSyncFlag(event: JSONObject) {
        try {
            // Check the personalisation flag
            val enablePersonalisation = this.config.isPersonalizationEnabled
            if (!enablePersonalisation) {
                event.put("dsync", false)
                return
            }

            // Always request a dsync when the App Launched event is recorded
            val eventType = event.getString("type")
            if ("event" == eventType) {
                val evtName = event.getString("evtName")
                if (Constants.APP_LAUNCHED_EVENT == evtName) {
                    getConfigLogger().verbose(
                        getConfigAccountId(),
                        "Local cache needs to be updated (triggered by App Launched)"
                    )
                    event.put("dsync", true)
                    return
                }
            }

            // If a profile event, then blindly set it to true
            if ("profile" == eventType) {
                event.put("dsync", true)
                getConfigLogger().verbose(getConfigAccountId(), "Local cache needs to be updated (profile event)")
                return
            }

            // Default to expire in 20 minutes
            val now = (System.currentTimeMillis() / 1000).toInt()

            val expiresIn = getLocalCacheExpiryInterval(20 * 60)

            val lastUpdate = getIntFromPrefs("local_cache_last_update", now)

            if (lastUpdate + expiresIn < now) {
                event.put("dsync", true)
                getConfigLogger().verbose(getConfigAccountId(), "Local cache needs to be updated")
            } else {
                event.put("dsync", false)
                getConfigLogger().verbose(getConfigAccountId(), "Local cache doesn't need to be updated")
            }
        } catch (t: Throwable) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to sync with upstream", t)
        }
    }

    val profile: JSONObject
        get() {
            try {
                synchronized(PROFILE_FIELDS_IN_THIS_SESSION) {
                    return JSONObject(PROFILE_FIELDS_IN_THIS_SESSION.toString())
                }
            } catch (e: JSONException) {
                // Handle exception or return empty object
                return JSONObject()
            }
        }

    fun getProfileProperty(key: String?): Any? {
        if (key == null) {
            return null
        }

        synchronized(PROFILE_FIELDS_IN_THIS_SESSION) {
            val profileChange = processProfileTree(key, GET_MARKER, ProfileOperation.GET)[key]
            return profileChange?.oldValue
        }
    }

    /**
     * @deprecated since `v7.1.0` in favor of DB. See [UserEventLog]
     */
    @Deprecated("since v7.1.0 in favor of DB. See UserEventLog")
    private fun decodeEventDetails(name: String?, encoded: String?): EventDetail? {
        if (encoded == null) {
            return null
        }

        // Pattern#split matches the former String#split(regex) semantics.
        val parts = Pattern.compile("\\|").split(encoded)
        return EventDetail(
            Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]), name
        )
    }

    /**
     * @deprecated since `v7.1.0` in favor of DB. See [UserEventLog]
     */
    @Deprecated("since v7.1.0 in favor of DB. See UserEventLog")
    private fun encodeEventDetails(first: Int, last: Int, count: Int): String {
        return "$count|$first|$last"
    }

    private fun getConfigAccountId(): String? {
        return this.config.accountId
    }

    private fun getConfigLogger(): Logger {
        return this.config.logger
    }

    private fun getIntFromPrefs(rawKey: String, defaultValue: Int): Int {
        if (this.config.isDefaultInstance) {
            val dummy = -1000
            val _new = StorageHelper.getInt(this.context, storageKeyWithSuffix(rawKey), dummy)
            return if (_new != dummy) _new else StorageHelper.getInt(this.context, rawKey, defaultValue)
        } else {
            return StorageHelper.getInt(this.context, storageKeyWithSuffix(rawKey), defaultValue)
        }
    }

    private fun getLocalCacheExpiryInterval(defaultInterval: Int): Int {
        return getIntFromPrefs("local_cache_expires_in", defaultInterval)
    }

    /**
     * @deprecated since `v7.1.0` in favor of DB. See [UserEventLog]
     */
    @Deprecated("since v7.1.0 in favor of DB. See UserEventLog")
    private fun getStringFromPrefs(rawKey: String?, defaultValue: String?, nameSpace: String): String? {
        if (this.config.isDefaultInstance) {
            val _new = StorageHelper
                .getString(this.context, nameSpace, storageKeyWithSuffix(rawKey), defaultValue)
            return _new ?: StorageHelper.getString(this.context, nameSpace, rawKey!!, defaultValue)
        } else {
            return StorageHelper.getString(this.context, nameSpace, storageKeyWithSuffix(rawKey), defaultValue)
        }
    }

    private fun getUserProfileID(): String? {
        return this.config.accountId
    }

    // local cache/profile key expiry handling
    fun inflateLocalProfileAsync(context: Context) {
        val accountID = this.config.accountId

        this.postAsyncSafely("LocalDataStore#inflateLocalProfileAsync") {
            val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
            synchronized(PROFILE_FIELDS_IN_THIS_SESSION) {
                try {
                    val profile = dbAdapter.fetchUserProfileByAccountIdAndDeviceID(accountID, deviceInfo.deviceID)
                        ?: return@postAsyncSafely

                    val keys: Iterator<*> = profile.keys()
                    while (keys.hasNext()) {
                        try {
                            val key = keys.next() as String
                            val value = profile.get(key)
                            if (value is JSONObject) {
                                val jsonObject = profile.getJSONObject(key)
                                PROFILE_FIELDS_IN_THIS_SESSION.put(key, jsonObject)
                            } else if (value is JSONArray) {
                                val jsonArray = profile.getJSONArray(key)
                                PROFILE_FIELDS_IN_THIS_SESSION.put(key, jsonArray)
                            } else {
                                var decrypted: Any? = value
                                if (value is String) {
                                    decrypted = cryptHandler.decryptSafe(value)
                                    if (decrypted == null) {
                                        decrypted = value
                                    }
                                }
                                PROFILE_FIELDS_IN_THIS_SESSION.put(key, decrypted)
                            }
                        } catch (e: JSONException) {
                            // no-op
                        }
                    }

                    getConfigLogger().verbose(
                        getConfigAccountId(),
                        "Local Data Store - Inflated local profile $PROFILE_FIELDS_IN_THIS_SESSION"
                    )
                } catch (t: Throwable) {
                    //no-op
                }
            }
        }
    }

    private fun isPersonalisationEnabled(): Boolean {
        return this.config.isPersonalizationEnabled
    }

    /**
     * @deprecated since `v7.1.0`. Use [persistUserEventLog]
     */
    @Deprecated("since v7.1.0. Use persistUserEventLog(String)")
    @SuppressLint("CommitPrefEdits")
    private fun persistEvent(context: Context, event: JSONObject) {
        try {
            val evtName: String? = event.getString("evtName")
            if (evtName == null) {
                return
            }
            val namespace: String = if (!this.config.isDefaultInstance) {
                eventNamespace + ":" + this.config.accountId
            } else {
                eventNamespace
            }
            val prefs = StorageHelper.getPreferences(context, namespace)

            val now = (System.currentTimeMillis() / 1000).toInt()

            val encoded = getStringFromPrefs(evtName, encodeEventDetails(now, now, 0), namespace)
            // Java dereferenced the decoded detail without a null check; the NPE is caught below.
            val ed = decodeEventDetails(evtName, encoded)!!

            val updateEncoded = encodeEventDetails(ed.firstTime, now, ed.count + 1)
            val editor = prefs.edit()
            editor.putString(storageKeyWithSuffix(evtName), updateEncoded)
            StorageHelper.persist(editor)
        } catch (t: Throwable) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to persist event locally", t)
        }
    }

    private fun persistLocalProfileAsync() {
        val profileID = this.config.accountId

        this.postAsyncSafely("LocalDataStore#persistLocalProfileAsync") {
            synchronized(PROFILE_FIELDS_IN_THIS_SESSION) {
                val profile = JSONObject()
                var encryptionFailed = false

                // Copy all fields from PROFILE_FIELDS_IN_THIS_SESSION to profile
                try {
                    val keys = PROFILE_FIELDS_IN_THIS_SESSION.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        profile.put(key, PROFILE_FIELDS_IN_THIS_SESSION.get(key))
                    }
                } catch (e: JSONException) {
                    getConfigLogger().verbose(getConfigAccountId(), "Failed to copy profile fields", e)
                }

                // Encrypts only the pii keys before storing to DB
                val isMediumEncryption = EncryptionLevel.fromInt(config.encryptionLevel) == EncryptionLevel.MEDIUM
                for (piiKey in piiDBKeys) {
                    try {
                        if (profile.has(piiKey)) {
                            var value: Any? = profile.opt(piiKey)
                            if (value is String) {
                                if (isMediumEncryption) {
                                    value = cryptHandler.encryptSafe(value)
                                    if (value == null) {
                                        encryptionFailed = true
                                        // Don't update profile with null, keep original
                                        continue
                                    }
                                    profile.put(piiKey, value)
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        getConfigLogger().verbose(getConfigAccountId(), "Failed to encrypt pii key: $piiKey", e)
                        encryptionFailed = true
                    }
                }

                // Update migration failure count ONCE after processing all keys
                if (encryptionFailed) {
                    cryptHandler.updateMigrationFailureCount(false)
                }

                val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
                val status = dbAdapter.storeUserProfile(profileID, deviceInfo.deviceID, profile)
                getConfigLogger().verbose(
                    getConfigAccountId(),
                    "Persist Local Profile complete with status $status for id $profileID"
                )
            }
        }
    }

    private fun postAsyncSafely(name: String, runnable: Runnable) {
        try {
            val executeSync = Thread.currentThread().id == EXECUTOR_THREAD_ID

            if (executeSync) {
                runnable.run()
            } else {
                es.submit(Runnable {
                    EXECUTOR_THREAD_ID = Thread.currentThread().id
                    try {
                        getConfigLogger().verbose(
                            getConfigAccountId(),
                            "Local Data Store Executor service: Starting task - $name"
                        )
                        runnable.run()
                    } catch (t: Throwable) {
                        getConfigLogger().verbose(
                            getConfigAccountId(),
                            "Executor service: Failed to complete the scheduled task", t
                        )
                    }
                })
            }
        } catch (t: Throwable) {
            getConfigLogger().verbose(getConfigAccountId(), "Failed to submit task to the executor service", t)
        }
    }

    private fun profileValueIsEmpty(value: Any?): Boolean {
        if (value == null) {
            return true
        }

        var isEmpty = false

        if (value is String) {
            isEmpty = value.trim { it <= ' ' }.isEmpty()
        }

        if (value is JSONArray) {
            isEmpty = value.length() <= 0
        }

        return isEmpty
    }

    private fun profileValuesAreEqual(value1: Any?, value2: Any?): Boolean {
        // convert to strings and compare
        // stringify handles null values
        return stringify(value1) == stringify(value2)
    }

    private fun resetLocalProfileSync() {
        synchronized(PROFILE_FIELDS_IN_THIS_SESSION) {
            val keys = PROFILE_FIELDS_IN_THIS_SESSION.keys()
            val keysToRemove: MutableList<String> = ArrayList()
            while (keys.hasNext()) {
                keysToRemove.add(keys.next())
            }
            for (key in keysToRemove) {
                PROFILE_FIELDS_IN_THIS_SESSION.remove(key)
            }
        }

        // Load the older profile from cache into the db
        inflateLocalProfileAsync(context)
    }

    private fun storageKeyWithSuffix(key: String?): String {
        return key + ":" + this.config.accountId
    }

    private fun stringify(value: Any?): String {
        return value?.toString() ?: ""
    }

    @WorkerThread
    fun processProfileTree(
        dotNotationKey: String,
        value: Any?,
        operation: ProfileOperation
    ): Map<String, ProfileChange> {
        return try {
            val nestedProfile = nestedJsonBuilder.buildFromPath(dotNotationKey, value)
            processProfileTree(nestedProfile, operation)
        } catch (e: JSONException) {
            Collections.emptyMap()
        }
    }

    @WorkerThread
    fun processProfileTree(
        newJson: JSONObject,
        operation: ProfileOperation
    ): Map<String, ProfileChange> {
        synchronized(PROFILE_FIELDS_IN_THIS_SESSION) {
            val result = profileStateTraverser.traverse(
                PROFILE_FIELDS_IN_THIS_SESSION,
                newJson,
                operation
            )

            if (operation != ProfileOperation.GET) {
                persistLocalProfileAsync()
            }

            // Return the changes map
            return result.changes
        }
    }

    companion object {
        private var EXECUTOR_THREAD_ID: Long = 0
    }
}
