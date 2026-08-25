package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.STORE_TYPE_ND_COUNTS
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.store.preference.ICTPreference

/**
 * DAO for the Native Display (ND) frequency-cap **counter** state (SDK-6055).
 *
 * Owns everything counter-related in a single prefs namespace (`nd_counts_per_target:<deviceId>:<accountId>`):
 * - per-target counts, keyed by target id (`ti`), value `"today,lifetime"`,
 * - the global shown-today counter, the daily/session ceilings, and the daily-reset date.
 *
 * This is the persistence half of the ND cap gate; [com.clevertap.android.sdk.NdFCManager] holds the
 * policy and delegates all reads/writes here. It mirrors the DAO split already used by
 * [ImpressionStore]/[InAppStore], so the manager no longer hand-rolls prefs access, key building or
 * CSV (de)serialization. ND impression timestamps live in a *separate* namespace, so no key here needs
 * an `__impressions_` guard.
 */
internal class NdCountsStore(
    private val ctPreference: ICTPreference,
) {

    /** Per-target today + lifetime render counts. */
    data class Counts(val today: Int, val lifetime: Int)

    companion object {
        // Global (non-per-target) keys in this store, excluded when enumerating per-target counts.
        private val GLOBAL_KEYS = setOf(
            Constants.KEY_ND_COUNTS_SHOWN_TODAY,
            Constants.KEY_ND_MAX_PER_DAY,
            Constants.ND_MAX_PER_SESSION_KEY,
            Constants.KEY_ND_LAST_RESET_DATE,
        )
        private const val DEFAULT_DATE = "20140428"
    }

    // ---- per-target counts ----

    /** Returns `[today, lifetime]` for a target, or `Counts(0, 0)` when absent/malformed. */
    fun counts(targetId: String): Counts = parseOrNull(ctPreference.readString(targetId, "")) ?: Counts(0, 0)

    /** Bumps both today and lifetime by one for a target. */
    fun increment(targetId: String) {
        val current = counts(targetId)
        ctPreference.writeString(targetId, "${current.today + 1},${current.lifetime + 1}")
    }

    /** Removes a target's counts (dead-target GC). */
    fun remove(targetId: String) = ctPreference.remove(targetId)

    /** All valid per-target counts, keyed by target id (global keys excluded). */
    fun allTargetCounts(): Map<String, Counts> {
        val all = ctPreference.readAll() ?: return emptyMap()
        return all.entries
            .filter { it.key !in GLOBAL_KEYS }
            .mapNotNull { entry -> parseOrNull(entry.value as? String)?.let { entry.key to it } }
            .toMap()
    }

    /** Resets every target's today count to 0 (keeps lifetime) and clears the global shown-today. */
    fun resetDailyKeepingLifetime() {
        allTargetCounts().forEach { (targetId, counts) ->
            ctPreference.writeString(targetId, "0,${counts.lifetime}")
        }
        shownToday = 0
    }

    // ---- global counters / ceilings / date ----

    /** ND render count today (drives the `ndmp` request value and the global daily check). */
    var shownToday: Int
        get() = ctPreference.readInt(Constants.KEY_ND_COUNTS_SHOWN_TODAY, 0)
        set(value) = ctPreference.writeInt(Constants.KEY_ND_COUNTS_SHOWN_TODAY, value)

    /** Account global daily ceiling (`ndstmcd`), default 1 until the server pushes it. */
    var maxPerDay: Int
        get() = ctPreference.readInt(Constants.KEY_ND_MAX_PER_DAY, 1)
        set(value) = ctPreference.writeInt(Constants.KEY_ND_MAX_PER_DAY, value)

    /** Account global session ceiling (`ndmc`), default 1 until the server pushes it. */
    var maxPerSession: Int
        get() = ctPreference.readInt(Constants.ND_MAX_PER_SESSION_KEY, 1)
        set(value) = ctPreference.writeInt(Constants.ND_MAX_PER_SESSION_KEY, value)

    /** `ddMMyyyy` of the last daily reset. */
    var lastResetDate: String
        get() = ctPreference.readString(Constants.KEY_ND_LAST_RESET_DATE, DEFAULT_DATE) ?: DEFAULT_DATE
        set(value) = ctPreference.writeString(Constants.KEY_ND_LAST_RESET_DATE, value)

    fun onChangeUser(deviceId: String, accountId: String) {
        ctPreference.changePreferenceName(
            StoreProvider.getInstance().constructStorePreferenceName(STORE_TYPE_ND_COUNTS, deviceId, accountId),
        )
    }

    /** Parses a `"today,lifetime"` value, or null if absent/malformed. */
    private fun parseOrNull(raw: String?): Counts? {
        if (raw.isNullOrEmpty()) return null
        val parts = raw.split(",")
        if (parts.size != 2) return null
        val today = parts[0].toIntOrNull() ?: return null
        val lifetime = parts[1].toIntOrNull() ?: return null
        return Counts(today, lifetime)
    }
}
