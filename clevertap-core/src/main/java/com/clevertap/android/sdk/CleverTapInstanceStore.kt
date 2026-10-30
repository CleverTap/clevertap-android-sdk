package com.clevertap.android.sdk

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry of live [CleverTapAPI] instances, keyed by `accountId`.
 *
 * Replaces the previously unsynchronised `static HashMap` that lived in [CleverTapAPI]. That map
 * was read and written from several threads at once — app launch on the main thread, an incoming
 * push on an FCM background thread, geofence worker threads — with no locking. Two threads racing
 * on the same account could each miss the lookup and construct a second [CleverTapAPI] for that
 * account (duplicate CoreState, DB adapter and serial queue), with one silently overwriting the
 * other in the map.
 *
 * [getOrCreate] makes the check-then-create atomic so exactly one instance exists per account.
 */
internal object CleverTapInstanceStore {

    private val instances = ConcurrentHashMap<String, CleverTapAPI>()

    /**
     * Returns the existing instance for [accountId], or atomically creates one via [factory] and
     * stores it. [factory] is invoked at most once per account; concurrent callers for the same
     * account all receive the single instance the winner created.
     */
    @JvmStatic
    fun getOrCreate(accountId: String, factory: () -> CleverTapAPI): CleverTapAPI =
        synchronized(this) {
            instances[accountId] ?: factory().also { instances[accountId] = it }
        }

    @JvmStatic
    fun get(accountId: String): CleverTapAPI? = instances[accountId]

    /** Live, weakly-consistent view of all instances — safe to iterate without external locking. */
    @JvmStatic
    fun all(): Collection<CleverTapAPI> = instances.values

    @JvmStatic
    fun isEmpty(): Boolean = instances.isEmpty()

    /** Defensive copy, backing the public [CleverTapAPI.getInstances] API. */
    @JvmStatic
    fun snapshot(): HashMap<String, CleverTapAPI> = HashMap(instances)

    /** Replaces the whole registry; a null [newInstances] clears it. Backs [CleverTapAPI.setInstances]. */
    @JvmStatic
    fun replaceAll(newInstances: Map<String, CleverTapAPI>?) = synchronized(this) {
        instances.clear()
        newInstances?.let { instances.putAll(it) }
    }
}
