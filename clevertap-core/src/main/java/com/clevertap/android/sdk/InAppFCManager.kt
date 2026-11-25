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
import java.util.concurrent.Callable
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
        val task = executors.postAsyncSafelyTask<Void?>()
        val initDeviceId = deviceId
        task.execute("initInAppFCManager", Callable<Void?> {
            init(initDeviceId)
            null
        })
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

            if (!hasSessionCapacityMaxedOut(inapp)
                && !hasLifetimeCapacityMaxedOut(inapp)
                && !hasDailyCapacityMaxedOut(inapp)
            ) {
                return true
            }
        } catch (t: Throwable) {
            return false
        }
        return false
    }

    fun changeUser(deviceId: String?) {
        // reset counters
        impressionManager.clearSessionData()
        this.deviceId = deviceId
        init(deviceId)
    }

    fun didShow(context: Context, inapp: CTInAppNotification) {
        val id = getInAppID(inapp) ?: return

        val task = executors.ioTask<Void?>()
        task.execute("recordInAppImpressionsAndCounts", Callable<Void?> {
            impressionManager.recordImpression(id)

            incrementInAppCountsInPersistentStore(id)

            var shownToday = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0)
            StorageHelper
                .putInt(
                    context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)),
                    ++shownToday
                )
            null
        })
    }

    val shownTodayCount: Int
        get() = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId), 0)

    fun getInAppsCount(context: Context): JSONArray? {
        try {
            // tlc: [[targetID, todayCount, lifetime]]
            val arr = JSONArray()
            val prefs = StorageHelper
                .getPreferences(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)))
            val all = prefs.all
            for ((key, value) in all) {
                //inApp: "targetID" -> "todayCount,lifetime"
                if (value is String) {
                    val parts = javaSplit(value, ",")
                    if (parts.size == 2) {
                        val a = JSONArray()
                        a.put(0, key)
                        a.put(1, Integer.parseInt(parts[0]))
                        a.put(2, Integer.parseInt(parts[1]))
                        arr.put(a)
                    }
                }
            }
            return arr
        } catch (t: Throwable) {
            Logger.v("Failed to get in apps count", t)
            return null
        }
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
                val o = arr.get(i)
                if (o is Int) {
                    editor.remove("" + o)
                    Logger.d("Purged stale in-app - $o")
                } else if (o is String) {
                    editor.remove(o)
                    Logger.d("Purged stale in-app - $o")
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
        StorageHelper
            .putInt(
                context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION_KEY, deviceId)),
                perSession
            )
    }

    private fun getConfigAccountId(): String? {
        return this.config.accountId
    }

    private fun getConfigLogger(): Logger {
        return this.config.logger
    }

    private fun getInAppCountsFromPersistentStore(inappID: String): IntArray {
        val prefs = getPreferences(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId))
        )
        val str = prefs.getString(inappID, null) ?: return intArrayOf(0, 0)

        try {
            val parts = javaSplit(str, ",")
            if (parts.size != 2) {
                return intArrayOf(0, 0)
            }

            // protocol: todayCount,lifeTimeCount
            return intArrayOf(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]))
        } catch (t: Throwable) {
            return intArrayOf(0, 0)
        }
    }

    fun getInAppID(inapp: CTInAppNotification): String? {
        val inappId = inapp.id ?: return null

        if (inappId.isNotEmpty()) {
            try {
                return inapp.id
            } catch (ignored: Throwable) {
                return null
            }
        }
        return null
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

    private fun getKeyWithDeviceId(key: String, deviceId: String?): String {
        return "$key:$deviceId"
    }

    private fun getStringFromPrefs(rawKey: String, defaultValue: String?): String? {
        if (this.config.isDefaultInstance) {
            val _new = StorageHelper.getString(this.context, storageKeyWithSuffix(rawKey), defaultValue)
            return _new ?: StorageHelper.getString(this.context, rawKey, defaultValue)
        } else {
            return StorageHelper.getString(this.context, storageKeyWithSuffix(rawKey), defaultValue)
        }
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
        try {
            val maxPerDay = inapp.totalDailyCount
            if (maxPerDay == -1) {
                return false
            }

            val counts = getInAppCountsFromPersistentStore(id)
            if (counts[0] >= maxPerDay) {
                return true
            }
        } catch (t: Throwable) {
            return true
        }

        return false
    }

    private fun hasLifetimeCapacityMaxedOut(inapp: CTInAppNotification): Boolean {
        val id = getInAppID(inapp) ?: return false

        if (inapp.totalLifetimeCount == -1) {
            return false
        }

        try {
            val counts = getInAppCountsFromPersistentStore(id)
            if (counts[1] >= inapp.totalLifetimeCount) {
                return true
            }
        } catch (e: Exception) {
            return true
        }

        return false
    }

    private fun hasSessionCapacityMaxedOut(inapp: CTInAppNotification): Boolean {
        val id = getInAppID(inapp) ?: return false

        // 2. Has the session max count for this inapp been breached?
        try {
            val maxPerSession = if (inapp.maxPerSession >= 0) inapp.maxPerSession else 1000

            val c = impressionManager.perSession(id)
            if (c >= maxPerSession) {
                return true
            }
        } catch (t: Throwable) {
            return true
        }

        // 3. Have we shown enough of in-apps this session?
        val c = getIntFromPrefs(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION_KEY, deviceId), 1)
        val sessionTotal = impressionManager.perSessionTotal()
        return (sessionTotal >= c)
    }

    private fun incrementInAppCountsInPersistentStore(inappID: String) {
        val current = getInAppCountsFromPersistentStore(inappID)
        current[0] = current[0] + 1
        current[1] = current[1] + 1

        val prefs = getPreferences(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId))
        )
        val editor = prefs.edit()

        // protocol: todayCount,lifeTimeCount
        editor.putString(inappID, current[0].toString() + "," + current[1])
        StorageHelper.persist(editor)
    }

    private fun init(deviceId: String?) {
        getConfigLogger()
            .verbose(config.accountId + ":async_deviceID", "InAppFCManager init() called")
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
                val all = prefs.all
                for (inapp in all.keys) {
                    val ov = all[inapp]
                    if (ov !is String) {
                        editor.remove(inapp)
                        continue
                    }

                    val oldValues = javaSplit(ov, ",")
                    if (oldValues.size != 2) {
                        editor.remove(inapp)
                        continue
                    }

                    // protocol: todayCount,lifeTimeCount
                    try {
                        editor.putString(inapp, "0," + oldValues[1])
                    } catch (t: Throwable) {
                        getConfigLogger()
                            .verbose(getConfigAccountId(), "Failed to reset todayCount for inapp $inapp", t)
                    }
                }

                StorageHelper.persist(editor)
            }
        } catch (e: Exception) {
            getConfigLogger()
                .verbose(getConfigAccountId(), "Failed to init inapp manager " + e.localizedMessage)
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
                val accountBasedInAppsList = Utils.toJSONObjectList(accountBasedInApps)
                inAppStore.storeServerSideInApps(accountBasedInAppsList)
                legacyInAppStore.removeInApps()
                Logger.d("Finished migrating in-apps from account id to device id based preference.")
            }
        }

        if (getStringFromPrefs(getKeyWithDeviceId("ict_date", deviceId), null) != null //F
            || getStringFromPrefs("ict_date", null) == null
        ) /*T | F*/ {
            return
        }

        Logger.v("Migrating InAppFC Prefs")

        val ict_date = getStringFromPrefs("ict_date", "20140428")
        StorageHelper.putString(context, storageKeyWithSuffix(getKeyWithDeviceId("ict_date", deviceId)), ict_date)

        val keyCountsShownToday = getIntFromPrefs(storageKeyWithSuffix(Constants.KEY_COUNTS_SHOWN_TODAY), 0)
        StorageHelper
            .putInt(
                context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_SHOWN_TODAY, deviceId)),
                keyCountsShownToday
            )
    }

    private fun storageKeyWithSuffix(key: String): String {
        return key + ":" + getConfigAccountId()
    }

    // String#split(regex) semantics (trailing empty strings removed), unlike Kotlin's split.
    private fun javaSplit(value: String, regex: String): Array<String> {
        return Pattern.compile(regex).split(value)
    }
}
