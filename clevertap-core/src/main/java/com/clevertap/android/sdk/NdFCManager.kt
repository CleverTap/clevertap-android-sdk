package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.StorageHelper.getPreferences
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Frequency-cap counter gate for the Native Display (ND / Display Units) channel.
 *
 * This is the ND sibling of [InAppFCManager] and enforces the counter-based caps at the point a unit
 * would be surfaced to the host app:
 * - per-target lifetime (`tlc`) and daily (`tdc`) counts,
 * - per-target and global session caps (`mdc` / `ndmc`),
 * - global daily cap (`ndstc` shown-today vs `ndstmcd` ceiling).
 *
 * ND is SS + legacy only, so unlike [InAppFCManager] there is no legacy pref-key migration here.
 * Counter state lives in its own namespaces (see [Constants.KEY_ND_COUNTS_PER_TARGET]) and never
 * collides with in-app. The advanced `frequencyLimits`/`occurrenceLimits` (whenLimits) are evaluated
 * separately by the ND `LimitsMatcher`; their result is passed in as `frequencyLimitsMaxedOut`.
 *
 * The API is model-light (primitives) so it stays decoupled from the ND content/target model, which is
 * parsed elsewhere.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class NdFCManager internal constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private var deviceId: String,
    private val impressionManager: ImpressionManager,
    private val executors: CTExecutors,
    private val clock: Clock,
) {

    companion object {

        private const val UNCAPPED = -1

        private const val ND_DATE_KEY = "nd_ict_date"

        private const val SESSION_CAP_DEFAULT = 1000

        /**
         * Whether an ND unit/target carries any frequency-cap configuration. Only such units are gated
         * at delivery and counted at impression time; unmarked (legacy) units bypass ND capping
         * entirely so existing display units are never affected and never consume the ND global budget.
         */
        @JvmStatic
        fun isFcapManaged(json: JSONObject?): Boolean =
            json != null && (
                json.has(Constants.KEY_EFC) ||
                    json.has(Constants.KEY_TLC) ||
                    json.has(Constants.KEY_TDC) ||
                    json.has(Constants.INAPP_MAX_DISPLAY_COUNT) ||
                    json.has(Constants.KEY_EXCLUDE_GLOBAL_CAPS)
                )
    }

    private val ddMMyyyy = SimpleDateFormat("ddMMyyyy", Locale.US)

    init {
        executors.postAsyncSafelyTask<Unit>().execute("initNdFCManager") {
            initDailyState(deviceId)
        }
    }

    /**
     * Whether the given ND target can currently be surfaced under all counter caps.
     *
     * @param id ND target id (`ti`).
     * @param excludeFromCaps `efc == 1` — exempt from all caps.
     * @param totalLifetimeCount `tlc`; [UNCAPPED] means uncapped.
     * @param totalDailyCount `tdc`; [UNCAPPED] means uncapped.
     * @param maxPerSession `mdc`; negative means the per-target session default.
     * @param frequencyLimitsMaxedOut Result of the ND whenLimits (advanced) re-check by LimitsMatcher.
     */
    fun canShow(
        id: String?,
        excludeFromCaps: Boolean,
        totalLifetimeCount: Int,
        totalDailyCount: Int,
        maxPerSession: Int,
        frequencyLimitsMaxedOut: Boolean,
    ): Boolean {
        return try {
            when {
                id.isNullOrEmpty() -> true
                // Re-check advanced whenLimits (without Nth triggers), mirrors in-app canShow.
                frequencyLimitsMaxedOut -> false
                // Exclude from all counter caps?
                excludeFromCaps -> true
                else -> !hasSessionCapacityMaxedOut(id, maxPerSession) &&
                    !hasLifetimeCapacityMaxedOut(id, totalLifetimeCount) &&
                    !hasDailyCapacityMaxedOut(id, totalDailyCount)
            }
        } catch (t: Throwable) {
            false
        }
    }

    fun changeUser(deviceId: String) {
        impressionManager.clearSessionData()
        this.deviceId = deviceId
        initDailyState(deviceId)
    }

    /**
     * Records a surfaced ND impression: bumps the per-target today/lifetime counts, the global
     * shown-today counter, and the in-memory session impression state.
     */
    fun didShow(context: Context, id: String?) {
        if (id.isNullOrEmpty()) {
            return
        }
        executors.ioTask<Unit>().execute("recordNdImpressionsAndCounts") {
            impressionManager.recordImpression(id)
            incrementNdCountsInPersistentStore(id)

            val shownToday = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId), 0)
            StorageHelper.putInt(
                context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId)),
                shownToday + 1,
            )
        }
    }

    /** The SDK's total ND render count today (`ndmp` request value). */
    val shownTodayCount: Int
        get() = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId), 0)

    /** The `ndtlc` array: `[[targetId, todayCount, lifetimeCount], ...]`, or null on failure. */
    fun getNdCounts(context: Context): JSONArray? {
        return try {
            val arr = JSONArray()
            val prefs = getPreferences(
                context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)),
            )
            for ((key, value) in prefs.all) {
                // impressions share this file under the "__impressions_" prefix — skip them.
                if (key.startsWith(ImpressionStore.PREF_PREFIX) || value !is String) {
                    continue
                }
                val parts = value.split(",")
                if (parts.size == 2) {
                    arr.put(
                        JSONArray().apply {
                            put(0, key)
                            put(1, parts[0].toInt())
                            put(2, parts[1].toInt())
                        },
                    )
                }
            }
            arr
        } catch (t: Throwable) {
            Logger.v("Failed to get ND counts", t)
            null
        }
    }

    /** Purges dead-target (`adUnit_stale`) counter state from the ND counts store. */
    fun processResponse(context: Context, staleIds: JSONArray?) {
        if (staleIds == null) {
            return
        }
        try {
            val prefs = getPreferences(
                context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)),
            )
            val editor = prefs.edit()
            for (i in 0 until staleIds.length()) {
                when (val o = staleIds.get(i)) {
                    is Int -> editor.remove(o.toString()).also { Logger.d("Purged stale ND target - $o") }
                    is String -> editor.remove(o).also { Logger.d("Purged stale ND target - $o") }
                }
            }
            StorageHelper.persist(editor)
        } catch (t: Throwable) {
            Logger.v("Failed to purge stale ND targets", t)
        }
    }

    /** Stores the account-level ND ceilings pushed on every response (`ndmp`/`ndmc`). */
    @Synchronized
    fun updateLimits(context: Context, perDay: Int, perSession: Int) {
        StorageHelper.putInt(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_MAX_PER_DAY, deviceId)),
            perDay,
        )
        StorageHelper.putInt(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.ND_MAX_PER_SESSION_KEY, deviceId)),
            perSession,
        )
    }

    private fun hasDailyCapacityMaxedOut(id: String, totalDailyCount: Int): Boolean {
        // 1. Global daily cap.
        val shownTodayCount = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId), 0)
        val maxPerDayCount = getIntFromPrefs(getKeyWithDeviceId(Constants.KEY_ND_MAX_PER_DAY, deviceId), 1)
        if (shownTodayCount >= maxPerDayCount) {
            return true
        }

        // 2. Per-target daily cap.
        return try {
            if (totalDailyCount == UNCAPPED) false else getNdCountsFromPersistentStore(id)[0] >= totalDailyCount
        } catch (t: Throwable) {
            true
        }
    }

    private fun hasLifetimeCapacityMaxedOut(id: String, totalLifetimeCount: Int): Boolean {
        if (totalLifetimeCount == UNCAPPED) {
            return false
        }
        return try {
            getNdCountsFromPersistentStore(id)[1] >= totalLifetimeCount
        } catch (t: Throwable) {
            true
        }
    }

    private fun hasSessionCapacityMaxedOut(id: String, maxPerSession: Int): Boolean {
        // 1. Per-target session cap.
        try {
            val perSession = if (maxPerSession >= 0) maxPerSession else SESSION_CAP_DEFAULT
            if (impressionManager.perSession(id) >= perSession) {
                return true
            }
        } catch (t: Throwable) {
            return true
        }

        // 2. Global per-session cap (ndmc, default 1).
        val globalCap = getIntFromPrefs(getKeyWithDeviceId(Constants.ND_MAX_PER_SESSION_KEY, deviceId), 1)
        return impressionManager.perSessionTotal() >= globalCap
    }

    /** @return `[todayCount, lifetimeCount]`, defaulting to `[0, 0]` when absent/malformed. */
    private fun getNdCountsFromPersistentStore(id: String): IntArray {
        val prefs = getPreferences(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)),
        )
        val str = prefs.getString(id, null) ?: return intArrayOf(0, 0)
        return try {
            val parts = str.split(",")
            if (parts.size != 2) intArrayOf(0, 0) else intArrayOf(parts[0].toInt(), parts[1].toInt())
        } catch (t: Throwable) {
            intArrayOf(0, 0)
        }
    }

    private fun incrementNdCountsInPersistentStore(id: String) {
        val current = getNdCountsFromPersistentStore(id)
        val prefs = getPreferences(
            context,
            storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)),
        )
        // protocol: todayCount,lifeTimeCount
        prefs.edit().putString(id, "${current[0] + 1},${current[1] + 1}").also { StorageHelper.persist(it) }
    }

    /** Resets the daily counters when the stored date differs from today (keeps lifetime counts). */
    private fun initDailyState(deviceId: String) {
        configLogger.verbose("${config.accountId}:async_deviceID", "NdFCManager init() called")
        try {
            val today = ddMMyyyy.format(clock.newDate())
            val lastUpdated = getStringFromPrefs(getKeyWithDeviceId(ND_DATE_KEY, deviceId), "20140428")
            if (today == lastUpdated) {
                return
            }
            StorageHelper.putString(context, storageKeyWithSuffix(getKeyWithDeviceId(ND_DATE_KEY, deviceId)), today)

            // Reset global shown-today count.
            StorageHelper.putInt(
                context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_SHOWN_TODAY, deviceId)),
                0,
            )

            // Reset per-target today counts (keep lifetime). Impression keys share this file under the
            // "__impressions_" prefix — leave them untouched.
            val prefs = getPreferences(
                context,
                storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_ND_COUNTS_PER_TARGET, deviceId)),
            )
            val editor = prefs.edit()
            for ((target, value) in prefs.all) {
                if (target.startsWith(ImpressionStore.PREF_PREFIX) || value !is String) {
                    continue
                }
                val oldValues = value.split(",")
                if (oldValues.size != 2) {
                    continue
                }
                // protocol: todayCount,lifeTimeCount
                runCatching { editor.putString(target, "0,${oldValues[1]}") }
                    .onFailure { configLogger.verbose(config.accountId, "Failed to reset todayCount for ND target $target", it) }
            }
            StorageHelper.persist(editor)
        } catch (e: Exception) {
            configLogger.verbose(config.accountId, "Failed to init ND FC manager ${e.localizedMessage}")
        }
    }

    private val configLogger: Logger
        get() = config.logger

    private fun getIntFromPrefs(rawKey: String, defaultValue: Int): Int {
        if (!config.isDefaultInstance) {
            return StorageHelper.getInt(context, storageKeyWithSuffix(rawKey), defaultValue)
        }
        val dummy = -1000
        val scoped = StorageHelper.getInt(context, storageKeyWithSuffix(rawKey), dummy)
        return if (scoped != dummy) scoped else StorageHelper.getInt(context, rawKey, defaultValue)
    }

    private fun getStringFromPrefs(rawKey: String, defaultValue: String): String {
        if (!config.isDefaultInstance) {
            return StorageHelper.getString(context, storageKeyWithSuffix(rawKey), defaultValue) ?: defaultValue
        }
        val scoped = StorageHelper.getString(context, storageKeyWithSuffix(rawKey), defaultValue)
        return scoped ?: StorageHelper.getString(context, rawKey, defaultValue) ?: defaultValue
    }

    private fun getKeyWithDeviceId(key: String, deviceId: String): String = "$key:$deviceId"

    private fun storageKeyWithSuffix(key: String): String = "$key:${config.accountId}"
}
