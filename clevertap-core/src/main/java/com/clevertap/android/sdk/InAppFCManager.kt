package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.clevertap.android.sdk.StorageHelper.getPreferences
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.SharedPreferencesMigration
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONObject

@RestrictTo(Scope.LIBRARY)
internal open class InAppFCManager(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private var deviceId: String?,
    private val storeRegistry: StoreRegistry,
    private val impressionManager: ImpressionManager,
    private val executors: CTExecutors,
    private val clock: Clock
) {

    private val ddMMyyyy = SimpleDateFormat("ddMMyyyy", Locale.US)

    init {
        val initDeviceId = deviceId
        executors.postAsyncSafelyTask<Void?>().execute("initInAppFCManager") {
            init(initDeviceId)
            null
        }
    }

    fun canShow(
        inapp: CTInAppNotification?,
        hasInAppFrequencyLimitsMaxedOut: (JSONObject, String) -> Boolean
    ): Boolean {
        try {
            if (inapp == null) {
                return false
            }

            val id = getInAppID(inapp) ?: return true

            /* Evaluate frequency limits again (without Nth triggers)
               in case the message was added multiple times before being displayed,
               or queue was paused and the message was added multiple times in the meantime
            */
            if (hasInAppFrequencyLimitsMaxedOut.invoke(inapp.jsonDescription, id)) {
                return false
            }

            // Exclude from all caps?
            if (inapp.isExcludeFromCaps) {
                return true
            }

            return !hasSessionCapacityMaxedOut(inapp) &&
                !hasLifetimeCapacityMaxedOut(inapp) &&
                !hasDailyCapacityMaxedOut(inapp)
        } catch (t: Throwable) {
            return false
        }
    }

    fun changeUser(newDeviceId: String?) {
        // reset counters
        impressionManager.clearSessionData()
        deviceId = newDeviceId
        init(newDeviceId)
    }

    fun didShow(context: Context, inapp: CTInAppNotification) {
        val id = getInAppID(inapp) ?: return

        executors.ioTask<Void?>().execute("recordInAppImpressionsAndCounts") {
            impressionManager.recordImpression(id)

            incrementInAppCountsInPersistentStore(id)

            val shownToday = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0)
            StorageHelper.putInt(
                context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)),
                shownToday + 1
            )
            null
        }
    }

    val shownTodayCount: Int
        get() = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0)

    fun getInAppsCount(context: Context): JSONArray? = try {
        // tlc: [[targetID, todayCount, lifetime]]
        val arr = JSONArray()
        val prefs =
            getPreferences(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)))
        for ((key, value) in prefs.all) {
            //inApp: "targetID" -> "todayCount,lifetime"
            if (value !is String) {
                continue
            }
            val parts = javaSplit(value, ",")
            if (parts.size == 2) {
                arr.put(
                    JSONArray()
                        .put(0, key)
                        .put(1, parts[0].toInt())
                        .put(2, parts[1].toInt())
                )
            }
        }
        arr
    } catch (t: Throwable) {
        Logger.v("Failed to get in apps count", t)
        null
    }

    fun processResponse(context: Context, response: JSONObject) {
        try {
            if (!response.has("inapp_stale")) {
                return
            }

            val arr = response.getJSONArray("inapp_stale")

            val prefs = getPreferences(
                context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId))
            )
            val editor = prefs.edit()

            for (i in 0 until arr.length()) {
                when (val o = arr.get(i)) {
                    is Int, is String -> {
                        editor.remove(o.toString())
                        Logger.d("Purged stale in-app - $o")
                    }
                }
            }

            StorageHelper.persist(editor)
        } catch (t: Throwable) {
            Logger.v("Failed to purge out stale targets", t)
        }
    }

    @Synchronized
    fun updateLimits(context: Context, perDay: Int, perSession: Int) {
        StorageHelper.putInt(
            context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_MAX_PER_DAY, deviceId)),
            perDay
        )
        StorageHelper.putInt(
            context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION_KEY, deviceId)),
            perSession
        )
    }

    private fun getInAppCountsFromPersistentStore(inappID: String): IntArray {
        val prefs = getPreferences(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId))
        )
        val str = prefs.getString(inappID, null) ?: return intArrayOf(0, 0)

        return try {
            val parts = javaSplit(str, ",")
            if (parts.size != 2) {
                intArrayOf(0, 0)
            } else {
                // protocol: todayCount,lifeTimeCount
                intArrayOf(parts[0].toInt(), parts[1].toInt())
            }
        } catch (t: Throwable) {
            intArrayOf(0, 0)
        }
    }

    fun getInAppID(inapp: CTInAppNotification): String? = inapp.id?.takeIf { it.isNotEmpty() }

    private fun getIntFromPrefs(rawKey: String, defaultValue: Int): Int {
        if (!config.isDefaultInstance) {
            return StorageHelper.getInt(context, storageKeyWithSuffix(rawKey), defaultValue)
        }
        val dummy = -1000
        val stored = StorageHelper.getInt(context, storageKeyWithSuffix(rawKey), dummy)
        return if (stored != dummy) { stored } else { StorageHelper.getInt(context, rawKey, defaultValue) }
    }

    private fun getKeyWithDeviceId(key: String, deviceId: String?): String = "$key:$deviceId"

    private fun getStringFromPrefs(rawKey: String, defaultValue: String?): String? {
        if (!config.isDefaultInstance) {
            return StorageHelper.getString(context, storageKeyWithSuffix(rawKey), defaultValue)
        }
        return StorageHelper.getString(context, storageKeyWithSuffix(rawKey), defaultValue)
            ?: StorageHelper.getString(context, rawKey, defaultValue)
    }

    private fun hasDailyCapacityMaxedOut(inapp: CTInAppNotification): Boolean {
        val id = getInAppID(inapp) ?: return false

        // 1. Has the daily count maxed out globally?
        val shownTodayCount = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0)
        val maxPerDayCount = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_MAX_PER_DAY, deviceId), 1)
        if (shownTodayCount >= maxPerDayCount) {
            return true
        }

        // 2. Has the daily count been maxed out for this inapp?
        return try {
            val maxPerDay = inapp.totalDailyCount
            maxPerDay != -1 && getInAppCountsFromPersistentStore(id)[0] >= maxPerDay
        } catch (t: Throwable) {
            true
        }
    }

    private fun hasLifetimeCapacityMaxedOut(inapp: CTInAppNotification): Boolean {
        val id = getInAppID(inapp) ?: return false

        if (inapp.totalLifetimeCount == -1) {
            return false
        }

        return try {
            getInAppCountsFromPersistentStore(id)[1] >= inapp.totalLifetimeCount
        } catch (e: Exception) {
            true
        }
    }

    private fun hasSessionCapacityMaxedOut(inapp: CTInAppNotification): Boolean {
        val id = getInAppID(inapp) ?: return false

        // 2. Has the session max count for this inapp been breached?
        try {
            val maxPerSession = if (inapp.maxPerSession >= 0) { inapp.maxPerSession } else { 1000 }
            if (impressionManager.perSession(id) >= maxPerSession) {
                return true
            }
        } catch (t: Throwable) {
            return true
        }

        // 3. Have we shown enough of in-apps this session?
        val maxPerSessionTotal = getIntFromPrefs(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION_KEY, deviceId), 1)
        return impressionManager.perSessionTotal() >= maxPerSessionTotal
    }

    private fun incrementInAppCountsInPersistentStore(inappID: String) {
        val (todayCount, lifetimeCount) = getInAppCountsFromPersistentStore(inappID)

        val editor = getPreferences(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId))
        ).edit()

        // protocol: todayCount,lifeTimeCount
        editor.putString(inappID, "${todayCount + 1},${lifetimeCount + 1}")
        StorageHelper.persist(editor)
    }

    private fun init(deviceId: String?) {
        config.logger.verbose("${config.accountId}:async_deviceID", "InAppFCManager init() called")
        try {
            migrateToNewPrefsKey(deviceId)
            val today = ddMMyyyy.format(clock.newDate())
            val lastUpdated = getStringFromPrefs(getKeyWithDeviceId("ict_date", deviceId), "20140428")
            if (today != lastUpdated) {
                StorageHelper
                    .putString(context, storageKeyWithSuffix(getKeyWithDeviceId("ict_date", deviceId)), today)

                // Reset today count
                StorageHelper.putInt(
                    context,
                    storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)), 0
                )

                // Reset the counts for each inapp
                val prefs = getPreferences(
                    context,
                    storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId))
                )
                val editor = prefs.edit()
                for ((inapp, ov) in prefs.all) {
                    val oldValues = (ov as? String)?.let { javaSplit(it, ",") }
                    if (oldValues == null || oldValues.size != 2) {
                        editor.remove(inapp)
                        continue
                    }

                    // protocol: todayCount,lifeTimeCount
                    try {
                        editor.putString(inapp, "0,${oldValues[1]}")
                    } catch (t: Throwable) {
                        config.logger.verbose(config.accountId, "Failed to reset todayCount for inapp $inapp", t)
                    }
                }

                StorageHelper.persist(editor)
            }
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Failed to init inapp manager ${e.localizedMessage}")
        }
    }

    private fun migrateToNewPrefsKey(deviceId: String?) {
        // without account id and device id
        val spCountsPerInAppV1 = getPreferences(context, Constants.KEY_COUNTS_PER_INAPP)

        // with device id
        val spCountsPerInAppV2 = getPreferences(
            context,
            getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)
        )

        // with account id and device id
        val spCountsPerInAppV3 = getPreferences(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId))
        )

        val countsPerInAppMigrationCondition: (String) -> Boolean = { javaSplit(it, ",").size == 2 }

        if (spCountsPerInAppV2.hasData()) {
            Logger.d("migrating shared preference countsPerInApp from V2 to V3...")
            val countsPerInAppMigrationV2ToV3 = SharedPreferencesMigration(
                spCountsPerInAppV2, spCountsPerInAppV3, String::class.java, countsPerInAppMigrationCondition
            )

            countsPerInAppMigrationV2ToV3.migrate()
            Logger.d("Finished migrating shared preference countsPerInApp from V2 to V3.")
        } else if (spCountsPerInAppV1.hasData()) {
            Logger.d("migrating shared preference countsPerInApp from V1 to V3...")
            val countsPerInAppMigrationV1ToV3 = SharedPreferencesMigration(
                spCountsPerInAppV1, spCountsPerInAppV3, String::class.java, countsPerInAppMigrationCondition
            )

            countsPerInAppMigrationV1ToV3.migrate()
            Logger.d("Finished migrating shared preference countsPerInApp from V1 to V3.")
        }

        val inAppStore = storeRegistry.inAppStore
        val legacyInAppStore = storeRegistry.legacyInAppStore
        if (inAppStore != null && legacyInAppStore != null) {
            val accountBasedInApps = legacyInAppStore.readInApps()
            if (accountBasedInApps.length() > 0) {
                Logger.d("migrating in-apps from account id to device id based preference.")
                inAppStore.storeServerSideInApps(Utils.toJSONObjectList(accountBasedInApps))
                legacyInAppStore.removeInApps()
                Logger.d("Finished migrating in-apps from account id to device id based preference.")
            }
        }

        // Migrate only when the device-scoped date is missing and a legacy one exists.
        if (getStringFromPrefs(getKeyWithDeviceId("ict_date", deviceId), null) != null ||
            getStringFromPrefs("ict_date", null) == null
        ) {
            return
        }

        Logger.v("Migrating InAppFC Prefs")

        val ictDate = getStringFromPrefs("ict_date", "20140428")
        StorageHelper.putString(context, storageKeyWithSuffix(getKeyWithDeviceId("ict_date", deviceId)), ictDate)

        val keyCountsShownToday = getIntFromPrefs(storageKeyWithSuffix(Constants.KEY_COUNTS_SHOWN_TODAY), 0)
        StorageHelper.putInt(
            context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)),
            keyCountsShownToday
        )
    }

    private fun storageKeyWithSuffix(key: String): String = "$key:${config.accountId}"

    // String#split(regex) semantics (trailing empty strings removed), unlike Kotlin's split.
    private fun javaSplit(value: String, regex: String): Array<String> = Pattern.compile(regex).split(value)
}
