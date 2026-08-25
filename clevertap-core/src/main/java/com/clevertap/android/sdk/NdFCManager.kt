package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.store.preference.NdCountsStore
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Frequency-cap policy for the Native Display (ND / Display Units) channel.
 *
 * This is the ND sibling of [InAppFCManager] and enforces the counter-based caps at the point a unit
 * would be surfaced to the host app:
 * - per-target lifetime (`tlc`) and daily (`tdc`) counts,
 * - per-target and global session caps (`mdc` / `ndmc`),
 * - global daily cap (shown-today vs the account ceiling).
 *
 * It holds only the cap *policy* and the daily-rollover trigger; all persistence lives in
 * [NdCountsStore]. The advanced `frequencyLimits`/`occurrenceLimits` (whenLimits) are evaluated
 * separately by the ND `LimitsMatcher`; their result is passed in as `frequencyLimitsMaxedOut`.
 *
 * The API is model-light (primitives) so it stays decoupled from the ND content/target model, which is
 * parsed elsewhere.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class NdFCManager internal constructor(
    private val config: CleverTapInstanceConfig,
    private val countsStore: NdCountsStore,
    private val impressionManager: ImpressionManager,
    private val executors: CTExecutors,
    private val clock: Clock,
) {

    companion object {

        private const val UNCAPPED = -1

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
            resetDailyStateIfNewDay()
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
        countsStore.onChangeUser(deviceId, config.accountId)
        resetDailyStateIfNewDay()
    }

    /**
     * Records a surfaced ND impression: bumps the per-target today/lifetime counts, the global
     * shown-today counter, and the in-memory session impression state.
     */
    fun didShow(id: String?) {
        if (id.isNullOrEmpty()) {
            return
        }
        executors.ioTask<Unit>().execute("recordNdImpressionsAndCounts") {
            impressionManager.recordImpression(id)
            countsStore.increment(id)
            countsStore.shownToday += 1
        }
    }

    /** The SDK's total ND render count today (`ndmp` request value). */
    val shownTodayCount: Int
        get() = countsStore.shownToday

    /** The `ndtlc` array: `[[targetId, todayCount, lifetimeCount], ...]`, or null on failure. */
    fun getNdCounts(): JSONArray? {
        return try {
            val arr = JSONArray()
            countsStore.allTargetCounts().forEach { (targetId, counts) ->
                arr.put(
                    JSONArray().apply {
                        put(0, targetId)
                        put(1, counts.today)
                        put(2, counts.lifetime)
                    },
                )
            }
            arr
        } catch (t: Throwable) {
            Logger.v("Failed to get ND counts", t)
            null
        }
    }

    /** Purges dead-target (`adUnit_stale`) counter state from the ND counts store. */
    fun processResponse(staleIds: JSONArray?) {
        if (staleIds == null) {
            return
        }
        for (i in 0 until staleIds.length()) {
            val targetId = staleIds.optString(i)
            if (targetId.isNotEmpty()) {
                countsStore.remove(targetId)
                Logger.d("Purged stale ND target - $targetId")
            }
        }
    }

    /** Stores the account-level ND ceilings pushed on every response (`ndmp`/`ndmc`). */
    fun updateLimits(perDay: Int, perSession: Int) {
        countsStore.maxPerDay = perDay
        countsStore.maxPerSession = perSession
    }

    private fun hasDailyCapacityMaxedOut(id: String, totalDailyCount: Int): Boolean {
        // 1. Global daily cap.
        if (countsStore.shownToday >= countsStore.maxPerDay) {
            return true
        }
        // 2. Per-target daily cap.
        return totalDailyCount != UNCAPPED && countsStore.counts(id).today >= totalDailyCount
    }

    private fun hasLifetimeCapacityMaxedOut(id: String, totalLifetimeCount: Int): Boolean =
        totalLifetimeCount != UNCAPPED && countsStore.counts(id).lifetime >= totalLifetimeCount

    private fun hasSessionCapacityMaxedOut(id: String, maxPerSession: Int): Boolean {
        // 1. Per-target session cap (in-memory).
        val perSession = if (maxPerSession >= 0) maxPerSession else SESSION_CAP_DEFAULT
        if (impressionManager.perSession(id) >= perSession) {
            return true
        }
        // 2. Global per-session cap (ndmc).
        return impressionManager.perSessionTotal() >= countsStore.maxPerSession
    }

    /** Resets the daily counters when the stored date differs from today (keeps lifetime counts). */
    private fun resetDailyStateIfNewDay() {
        try {
            val today = ddMMyyyy.format(clock.newDate())
            if (countsStore.lastResetDate == today) {
                return
            }
            countsStore.lastResetDate = today
            countsStore.resetDailyKeepingLifetime()
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Failed to reset ND daily state: ${e.localizedMessage}")
        }
    }
}
