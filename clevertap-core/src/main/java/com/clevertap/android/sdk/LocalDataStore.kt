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
import java.util.Collections
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
public open class LocalDataStore internal constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val cryptHandler: ICryptHandler,
    private val deviceInfo: DeviceInfo,
    private val baseDatabaseManager: BaseDatabaseManager,
    private val profileStateTraverser: ProfileStateTraverser,
    private val nestedJsonBuilder: NestedJsonBuilder
) {

    private val profileFields = JSONObject()

    private val executor: ExecutorService = Executors.newFixedThreadPool(1)

    private val eventNamespace = "local_events"

    /** Prefs namespace of the deprecated local event store; non-default instances are account scoped. */
    private val accountEventNamespace: String
        get() = if (config.isDefaultInstance) eventNamespace else "$eventNamespace:${config.accountId}"

    private val logger: Logger
        get() = config.logger

    private val userNormalizedEventLogKeys: MutableSet<String?> = Collections.synchronizedSet(HashSet())
    private val normalizedEventNames: MutableMap<String?, String?> = HashMap()

    @WorkerThread
    public fun changeUser() {
        userNormalizedEventLogKeys.clear()
        resetLocalProfileSync()
    }

    /**
     * @deprecated since `v7.1.0`. Use [readUserEventLog]
     */
    @Deprecated("since v7.1.0. Use readUserEventLog(String)")
    public fun getEventDetail(eventName: String?): EventDetail? = try {
        if (!config.isPersonalizationEnabled) {
            null
        } else {
            decodeEventDetails(eventName, getStringFromPrefs(eventName, null, accountEventNamespace))
        }
    } catch (t: Throwable) {
        logger.verbose(config.accountId, "Failed to retrieve local event detail", t)
        null
    }

    /**
     * @deprecated since `v7.1.0`. Use [readUserEventLogs]
     */
    @Deprecated("since v7.1.0. Use readUserEventLogs()")
    public fun getEventHistory(context: Context?): MutableMap<String, EventDetail>? = try {
        // A null context throws here and is caught below, as in Java.
        val prefs = StorageHelper.getPreferences(requireNotNull(context), accountEventNamespace)
        prefs.all.mapValuesTo(HashMap<String, EventDetail>()) { (eventName, value) ->
            // Java dereferenced the value without a null check; a missing value fails here (caught below).
            // decodeEventDetails only returns null for a null input.
            checkNotNull(decodeEventDetails(eventName, checkNotNull(value).toString()))
        }
    } catch (t: Throwable) {
        logger.verbose(config.accountId, "Failed to retrieve local event history", t)
        null
    }

    /**
     * @deprecated since `v7.1.0`. Use [persistUserEventLog]
     */
    @Deprecated("since v7.1.0. Use persistUserEventLog(String)")
    @WorkerThread
    public fun persistEvent(context: Context, event: JSONObject?, type: Int) {
        if (event == null) {
            return
        }

        try {
            if (type == Constants.RAISED_EVENT) {
                persistEvent(context, event)
            }
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to sync with upstream", t)
        }
    }

    @WorkerThread
    public fun persistUserEventLogsInBulk(eventNames: Set<String?>): Boolean {
        val setOfActualAndNormalizedEventNamePair = eventNames.mapTo(HashSet()) { actualEventName ->
            Pair(actualEventName, getOrPutNormalizedEventName(actualEventName))
        }
        // Nulls flow through unchanged, exactly as with the former Java generics.
        @Suppress("UNCHECKED_CAST")
        return upsertUserEventLogsInBulk(setOfActualAndNormalizedEventNamePair as Set<Pair<String, String>>)
    }

    @WorkerThread
    public fun persistUserEventLog(eventName: String?): Boolean {
        if (eventName == null) {
            return false
        }

        val accountId = config.accountId
        return try {
            logger.verbose(accountId, "UserEventLog: Persisting EventLog for event $eventName")
            if (isUserEventLogExists(eventName)) {
                logger.verbose(accountId, "UserEventLog: Updating EventLog for event $eventName")
                updateUserEventLog(eventName)
            } else {
                logger.verbose(accountId, "UserEventLog: Inserting EventLog for event $eventName")
                insertUserEventLog(eventName)
            }
        } catch (t: Throwable) {
            logger.verbose(accountId, "UserEventLog: Failed to insert user event log: for event$eventName", t)
            false
        }
    }

    private fun userEventLogDAO() = baseDatabaseManager.loadDBAdapter(context).userEventLogDAO()

    // The DAO takes non-null Strings: a missing device id or event name fails at the DAO call,
    // as the former Java code did once it reached the DAO.

    @WorkerThread
    public fun updateUserEventLog(eventName: String?): Boolean {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        val updatedEventByDeviceID = userEventLogDAO()
            .updateEventByDeviceIdAndNormalizedEventName(checkNotNull(deviceID), checkNotNull(normalizedEventName))
        logger.verbose("updatedEventByDeviceID = $updatedEventByDeviceID")
        return updatedEventByDeviceID
    }

    @WorkerThread
    private fun upsertUserEventLogsInBulk(setOfActualAndNormalizedEventNamePair: Set<Pair<String, String>>): Boolean {
        val deviceID = deviceInfo.deviceID
        val upsertEventByDeviceID = userEventLogDAO()
            .upsertEventsByDeviceIdAndNormalizedEventName(checkNotNull(deviceID), setOfActualAndNormalizedEventNamePair)
        logger.verbose("upsertEventByDeviceID = $upsertEventByDeviceID")
        return upsertEventByDeviceID
    }

    private fun getOrPutNormalizedEventName(actualEventName: String?): String? =
        normalizedEventNames.getOrPut(actualEventName) { Utils.getNormalizedName(actualEventName) }

    @WorkerThread
    public fun insertUserEventLog(eventName: String?): Boolean {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        val rowId = userEventLogDAO().insertEvent(
            checkNotNull(deviceID),
            checkNotNull(eventName),
            checkNotNull(normalizedEventName)
        )
        logger.verbose("inserted rowId = $rowId")
        return rowId >= 0
    }

    @WorkerThread
    public fun isUserEventLogExists(eventName: String?): Boolean {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        val eventExists = userEventLogDAO()
            .eventExistsByDeviceIdAndNormalizedEventName(checkNotNull(deviceID), checkNotNull(normalizedEventName))
        logger.verbose("eventExists = $eventExists")
        return eventExists
    }

    @WorkerThread
    public fun isUserEventLogFirstTime(eventName: String?): Boolean {
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        if (normalizedEventName in userNormalizedEventLogKeys) {
            return false
        }

        val count = readEventCount(deviceInfo.deviceID, normalizedEventName)
        if (count > 1) {
            userNormalizedEventLogKeys.add(normalizedEventName)
        }
        return count == 1
    }

    @WorkerThread
    public fun cleanUpExtraEvents(threshold: Int, numberOfRowsToCleanup: Int): Boolean {
        val cleanUpExtraEvents = userEventLogDAO().cleanUpExtraEvents(threshold, numberOfRowsToCleanup)
        logger.verbose("cleanUpExtraEvents boolean= $cleanUpExtraEvents")
        return cleanUpExtraEvents
    }

    @WorkerThread
    public fun readUserEventLog(eventName: String?): UserEventLog? {
        val deviceID = deviceInfo.deviceID
        val normalizedEventName = getOrPutNormalizedEventName(eventName)
        return userEventLogDAO()
            .readEventByDeviceIdAndNormalizedEventName(checkNotNull(deviceID), checkNotNull(normalizedEventName))
    }

    @WorkerThread
    private fun readEventCount(deviceID: String?, normalizedEventName: String?): Int = userEventLogDAO()
        .readEventCountByDeviceIdAndNormalizedEventName(checkNotNull(deviceID), checkNotNull(normalizedEventName))

    @WorkerThread
    public fun readUserEventLogCount(eventName: String?): Int =
        readEventCount(deviceInfo.deviceID, getOrPutNormalizedEventName(eventName))

    @WorkerThread
    public fun readUserEventLogs(): List<UserEventLog> {
        val deviceID = deviceInfo.deviceID
        return userEventLogDAO().allEventsByDeviceID(checkNotNull(deviceID))
    }

    @WorkerThread
    public fun readEventLogsForAllUsers(): List<UserEventLog> = userEventLogDAO().allEvents()

    @WorkerThread
    public fun setDataSyncFlag(event: JSONObject) {
        try {
            // Check the personalisation flag
            if (!config.isPersonalizationEnabled) {
                event.put("dsync", false)
                return
            }

            // Always request a dsync when the App Launched event is recorded
            val eventType = event.getString("type")
            if ("event" == eventType && Constants.APP_LAUNCHED_EVENT == event.getString("evtName")) {
                logger.verbose(config.accountId, "Local cache needs to be updated (triggered by App Launched)")
                event.put("dsync", true)
                return
            }

            // If a profile event, then blindly set it to true
            if ("profile" == eventType) {
                event.put("dsync", true)
                logger.verbose(config.accountId, "Local cache needs to be updated (profile event)")
                return
            }

            // Default to expire in 20 minutes
            val now = (System.currentTimeMillis() / 1000).toInt()
            val expiresIn = getIntFromPrefs("local_cache_expires_in", 20 * 60)
            val lastUpdate = getIntFromPrefs("local_cache_last_update", now)

            if (lastUpdate + expiresIn < now) {
                event.put("dsync", true)
                logger.verbose(config.accountId, "Local cache needs to be updated")
            } else {
                event.put("dsync", false)
                logger.verbose(config.accountId, "Local cache doesn't need to be updated")
            }
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to sync with upstream", t)
        }
    }

    public val profile: JSONObject
        get() = try {
            synchronized(profileFields) {
                JSONObject(profileFields.toString())
            }
        } catch (e: JSONException) {
            // Handle exception or return empty object
            JSONObject()
        }

    public fun getProfileProperty(key: String?): Any? {
        if (key == null) {
            return null
        }

        synchronized(profileFields) {
            return processProfileTree(key, GET_MARKER, ProfileOperation.GET)[key]?.oldValue
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
        return EventDetail(parts[0].toInt(), parts[1].toInt(), parts[2].toInt(), name)
    }

    /**
     * @deprecated since `v7.1.0` in favor of DB. See [UserEventLog]
     */
    @Deprecated("since v7.1.0 in favor of DB. See UserEventLog")
    private fun encodeEventDetails(first: Int, last: Int, count: Int): String = "$count|$first|$last"

    private fun getIntFromPrefs(rawKey: String, defaultValue: Int): Int {
        if (!config.isDefaultInstance) {
            return StorageHelper.getInt(context, storageKeyWithSuffix(rawKey), defaultValue)
        }
        val dummy = -1000
        val stored = StorageHelper.getInt(context, storageKeyWithSuffix(rawKey), dummy)
        return if (stored != dummy) stored else StorageHelper.getInt(context, rawKey, defaultValue)
    }

    /**
     * @deprecated since `v7.1.0` in favor of DB. See [UserEventLog]
     */
    @Deprecated("since v7.1.0 in favor of DB. See UserEventLog")
    private fun getStringFromPrefs(rawKey: String?, defaultValue: String?, nameSpace: String): String? {
        val suffixed = StorageHelper.getString(context, nameSpace, storageKeyWithSuffix(rawKey), defaultValue)
        if (!config.isDefaultInstance) {
            return suffixed
        }
        // A null key fails here, as in Java (callers catch it).
        return suffixed ?: StorageHelper.getString(context, nameSpace, requireNotNull(rawKey), defaultValue)
    }

    // local cache/profile key expiry handling
    public fun inflateLocalProfileAsync(context: Context) {
        val accountID = config.accountId

        postAsyncSafely("LocalDataStore#inflateLocalProfileAsync") {
            val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
            synchronized(profileFields) {
                try {
                    val profile = dbAdapter.fetchUserProfileByAccountIdAndDeviceID(accountID, deviceInfo.deviceID)
                        ?: return@postAsyncSafely

                    for (key in profile.keys()) {
                        try {
                            when (val value = profile.get(key)) {
                                is JSONObject -> profileFields.put(key, profile.getJSONObject(key))
                                is JSONArray -> profileFields.put(key, profile.getJSONArray(key))
                                is String -> profileFields.put(key, cryptHandler.decryptSafe(value) ?: value)
                                else -> profileFields.put(key, value)
                            }
                        } catch (e: JSONException) {
                            // no-op
                        }
                    }

                    logger.verbose(config.accountId, "Local Data Store - Inflated local profile $profileFields")
                } catch (t: Throwable) {
                    //no-op
                }
            }
        }
    }

    /**
     * @deprecated since `v7.1.0`. Use [persistUserEventLog]
     */
    @Deprecated("since v7.1.0. Use persistUserEventLog(String)")
    @SuppressLint("CommitPrefEdits")
    private fun persistEvent(context: Context, event: JSONObject) {
        try {
            val evtName: String = event.getString("evtName") ?: return
            val namespace = accountEventNamespace
            val prefs = StorageHelper.getPreferences(context, namespace)

            val now = (System.currentTimeMillis() / 1000).toInt()

            val encoded = getStringFromPrefs(evtName, encodeEventDetails(now, now, 0), namespace)
            // Java dereferenced the decoded detail without a null check; that failure is caught below.
            val ed = checkNotNull(decodeEventDetails(evtName, encoded))

            val editor = prefs.edit()
            editor.putString(storageKeyWithSuffix(evtName), encodeEventDetails(ed.firstTime, now, ed.count + 1))
            StorageHelper.persist(editor)
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to persist event locally", t)
        }
    }

    private fun persistLocalProfileAsync() {
        val profileID = config.accountId

        postAsyncSafely("LocalDataStore#persistLocalProfileAsync") {
            synchronized(profileFields) {
                val profile = JSONObject()
                var encryptionFailed = false

                // Copy all fields from profileFields to profile
                try {
                    for (key in profileFields.keys()) {
                        profile.put(key, profileFields.get(key))
                    }
                } catch (e: JSONException) {
                    logger.verbose(config.accountId, "Failed to copy profile fields", e)
                }

                // Encrypts only the pii keys before storing to DB
                val isMediumEncryption = EncryptionLevel.fromInt(config.encryptionLevel) == EncryptionLevel.MEDIUM
                for (piiKey in piiDBKeys) {
                    try {
                        val value = profile.opt(piiKey)
                        if (!profile.has(piiKey) || value !is String || !isMediumEncryption) {
                            continue
                        }
                        val encrypted = cryptHandler.encryptSafe(value)
                        if (encrypted == null) {
                            encryptionFailed = true
                            // Don't update profile with null, keep original
                            continue
                        }
                        profile.put(piiKey, encrypted)
                    } catch (e: JSONException) {
                        logger.verbose(config.accountId, "Failed to encrypt pii key: $piiKey", e)
                        encryptionFailed = true
                    }
                }

                // Update migration failure count ONCE after processing all keys
                if (encryptionFailed) {
                    cryptHandler.updateMigrationFailureCount(false)
                }

                val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
                val status = dbAdapter.storeUserProfile(profileID, deviceInfo.deviceID, profile)
                logger.verbose(config.accountId, "Persist Local Profile complete with status $status for id $profileID")
            }
        }
    }

    private fun postAsyncSafely(name: String, runnable: Runnable) {
        try {
            if (Thread.currentThread().id == executorThreadId) {
                runnable.run()
                return
            }
            executor.submit(Runnable {
                executorThreadId = Thread.currentThread().id
                try {
                    logger.verbose(config.accountId, "Local Data Store Executor service: Starting task - $name")
                    runnable.run()
                } catch (t: Throwable) {
                    logger.verbose(config.accountId, "Executor service: Failed to complete the scheduled task", t)
                }
            })
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to submit task to the executor service", t)
        }
    }

    private fun resetLocalProfileSync() {
        synchronized(profileFields) {
            // Snapshot the keys first: removing while iterating keys() is not supported.
            profileFields.keys().asSequence().toList().forEach { profileFields.remove(it) }
        }

        // Load the older profile from cache into the db
        inflateLocalProfileAsync(context)
    }

    private fun storageKeyWithSuffix(key: String?): String = "$key:${config.accountId}"

    @WorkerThread
    internal fun processProfileTree(
        dotNotationKey: String,
        value: Any?,
        operation: ProfileOperation
    ): Map<String, ProfileChange> = try {
        processProfileTree(nestedJsonBuilder.buildFromPath(dotNotationKey, value), operation)
    } catch (e: JSONException) {
        emptyMap()
    }

    @WorkerThread
    internal fun processProfileTree(
        newJson: JSONObject,
        operation: ProfileOperation
    ): Map<String, ProfileChange> = synchronized(profileFields) {
        val result = profileStateTraverser.traverse(profileFields, newJson, operation)

        if (operation != ProfileOperation.GET) {
            persistLocalProfileAsync()
        }

        // Return the changes map
        result.changes
    }

    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    public companion object {
        // Written by the executor thread, read by callers; not volatile, exactly as in Java.
        private var executorThreadId: Long = 0
    }
}
