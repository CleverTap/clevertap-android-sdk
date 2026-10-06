package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.store.preference.NdCountsStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.Clock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Frequency-cap policy for the Native Display (ND / Display Units) channel.
 *
 * This is the ND sibling of [InAppFCManager]. It owns the ND counter state (per-target today/lifetime
 * counts → `ndtlc`, global shown-today → `ndmp`, session impressions) and the daily-rollover trigger;
 * all persistence lives in [NdCountsStore].
 *
 * At delivery, [canShow] enforces **session caps only** (per-target `mdc` and the account `ndmc`). The
 * per-target daily/lifetime caps (`tdc`/`tlc`) and the global daily ceiling are the server's job on
 * regular events (V2); the App-Launched remaining-budget trim uses [globalCapRemaining] instead — so
 * re-applying any of them in [canShow] would double-cap. The advanced `frequencyLimits`/`occurrenceLimits`
 * (whenLimits) are evaluated separately by the ND `LimitsMatcher`; their result is passed into [canShow]
 * as `frequencyLimitsMaxedOut`.
 *
 * The API is model-light (primitives) so it stays decoupled from the ND content/target model, which is
 * parsed elsewhere.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class NdFCManager internal constructor(
    private val config: CleverTapInstanceConfig,
    private val storeRegistry: StoreRegistry,
    private val impressionManager: ImpressionManager,
    private val executors: CTExecutors,
    private val clock: Clock,
) {

    // Read through the registry so the store repoints with the user via NdStoreProvider (no per-user ref
    // held here). Nullable until the device id resolves; every read degrades gracefully rather than throw.
    private val countsStore: NdCountsStore?
        get() = storeRegistry.ndCountsStore

    companion object {

        private const val SESSION_CAP_DEFAULT = 1000

        /**
         * Whether an ND unit/target carries any frequency-cap configuration. Used by [NdFcapGate] to decide
         * which units to counter-cap at delivery; unmarked (legacy) units pass through the gate unchanged.
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
     * Whether the given ND target can currently be surfaced under the **session** caps — the only counter
     * caps the SDK owns at delivery (daily/lifetime are server-owned on V2; see the class doc).
     *
     * @param id ND target id (`ti`).
     * @param excludeFromCaps `efc == 1` || `excludeGlobalFCaps == 1` — exempt from caps.
     * @param maxPerSession `mdc`; negative means the per-target session default.
     * @param frequencyLimitsMaxedOut Result of the ND whenLimits (advanced) re-check by LimitsMatcher.
     */
    fun canShow(
        id: String?,
        excludeFromCaps: Boolean,
        maxPerSession: Int,
        frequencyLimitsMaxedOut: Boolean,
    ): Boolean {
        return try {
            when {
                id.isNullOrEmpty() -> true
                // Re-check advanced whenLimits (without Nth triggers), mirrors in-app canShow.
                frequencyLimitsMaxedOut -> false
                // Exclude from caps?
                excludeFromCaps -> true
                else -> !hasSessionCapacityMaxedOut(id, maxPerSession)
            }
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Clears in-memory session state and re-checks the daily rollover for the new user. Must be called
     * after the new device id is set — the counts store repoints itself via [NdStoreProvider].
     */
    fun changeUser() {
        impressionManager.clearSessionData()
        resetDailyStateIfNewDay()
    }

    /**
     * Records a surfaced ND impression.
     *
     * The in-memory session impression is **always** recorded — session caps (account `ndmc` and per-target)
     * are SDK-owned for every ND unit, and advanced `frequencyLimits` need the impression history regardless
     * of the global budget.
     *
     * The persisted counters that the SDK reports to the server ([NdCountsStore.increment] → `ndtlc`, and
     * `shownToday` → `ndmp`) are bumped **only when the unit counts toward the global budget**
     * ([countsTowardCaps]). This diverges from in-app (which counts unconditionally): the ND server reads
     * `ndmp`/`ndtlc` straight from the request as the cap basis and itself increments only on
     * `DELIVER_COUNTED` — so an exempt unit (ND fcap regime off, or `excludeGlobalFCaps`) must not spend a
     * slot, or it would starve ceiling-bound campaigns. Journeys (flag absent) also fall outside, by design
     * of the agnostic flag gate.
     *
     * @param countsTowardCaps `isNdFcapEnabled == true && excludeGlobalFCaps != 1` — computed at the call
     *   site from the unit payload.
     */
    fun didShow(id: String?, countsTowardCaps: Boolean) {
        if (id.isNullOrEmpty()) {
            return
        }
        // Single-thread executor (not the IO pool): increment / shownToday are read-modify-write over
        // SharedPreferences, so concurrent viewed events on a pool would lose counts and undercount ndtlc/ndmp.
        executors.postAsyncSafelyTask<Unit>().execute("recordNdImpressionsAndCounts") {
            impressionManager.recordImpression(id)
            if (countsTowardCaps) {
                countsStore?.let {
                    it.increment(id)
                    it.shownToday += 1
                }
            }
        }
    }

    /** The SDK's total ND render count today (`ndmp` request value); 0 before the store is ready. */
    val shownTodayCount: Int
        get() = countsStore?.shownToday ?: 0

    /**
     * Remaining account-level global budget: the lower of the daily (`ndmp` − shownToday) and session
     * (`ndmc` − session-total) headroom, floored at 0. Used by the App-Launched content-in-advance path —
     * where the SDK (not the server) decides how many units to surface — to trim a batch to the remaining
     * global allowance. Returns [Int.MAX_VALUE] (no cap) before the store is ready.
     */
    fun globalCapRemaining(): Int {
        val store = countsStore ?: return Int.MAX_VALUE
        val dailyRemaining = store.maxPerDay - store.shownToday
        val sessionRemaining = store.maxPerSession - impressionManager.perSessionTotal()
        return maxOf(0, minOf(dailyRemaining, sessionRemaining))
    }

    /** The `ndtlc` array: `[[targetId, todayCount, lifetimeCount], ...]`, or null on failure. */
    fun getNdCounts(): JSONArray? {
        return try {
            val arr = JSONArray()
            countsStore?.allTargetCounts()?.forEach { (targetId, counts) ->
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
            config.logger.verbose(config.accountId, "Failed to get ND counts", t)
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
                countsStore?.remove(targetId)
                config.logger.debug(config.accountId, "Purged stale ND target - $targetId")
            }
        }
    }

    /** Stores the account-level ND ceilings pushed on every response (`ndmp`/`ndmc`). */
    fun updateLimits(perDay: Int, perSession: Int) {
        countsStore?.let {
            it.maxPerDay = perDay
            it.maxPerSession = perSession
        }
    }

    private fun hasSessionCapacityMaxedOut(id: String, maxPerSession: Int): Boolean {
        // 1. Per-target session cap (in-memory).
        val perSession = if (maxPerSession >= 0) maxPerSession else SESSION_CAP_DEFAULT
        if (impressionManager.perSession(id) >= perSession) {
            return true
        }
        // 2. Global per-session cap (ndmc).
        val store = countsStore ?: return false // store not ready -> not capped
        return impressionManager.perSessionTotal() >= store.maxPerSession
    }

    /** Resets the daily counters when the stored date differs from today (keeps lifetime counts). */
    private fun resetDailyStateIfNewDay() {
        try {
            val store = countsStore ?: return
            val today = ddMMyyyy.format(clock.newDate())
            if (store.lastResetDate == today) {
                return
            }
            store.lastResetDate = today
            store.resetDailyKeepingLifetime()
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Failed to reset ND daily state: ${e.localizedMessage}")
        }
    }
}
