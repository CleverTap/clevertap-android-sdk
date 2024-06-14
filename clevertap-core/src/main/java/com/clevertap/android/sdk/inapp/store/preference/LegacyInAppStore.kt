package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.concatIfNotNull
import com.clevertap.android.sdk.store.preference.ICTPreference
import org.json.JSONArray
import org.json.JSONException

/**
 * The `LegacyInAppStore` class manages the storage and retrieval of legacy In-App messages.
 * Legacy In-App messages are stored in the shared preferences named "WizRocket" with key "inApp" concatenated
 * with the provided account identifier. This class is used for handling In-App messages in a legacy manner.
 *
 * @property ctPreference The instance of the preference manager interface (`ICTPreference`) for handling preferences.
 * @property accountId The unique account identifier used for creating a specific preference key.
 */
class LegacyInAppStore(private val ctPreference: ICTPreference, accountId: String) {

    companion object {

        private const val ASSETS_CLEANUP_TS_KEY = "last_assets_cleanup"
    }

    // Key for storing and retrieving legacy In-App messages
    private val inAppKey = Constants.INAPP_KEY.concatIfNotNull(accountId, ":")

    /**
     * Stores legacy In-App messages in the shared preferences.
     *
     * @param inApps The array of legacy In-App messages to be stored.
     */
    fun storeInApps(inApps: JSONArray) {
        ctPreference.writeStringImmediate(inAppKey!!, inApps.toString())
    }

    /**
     * Reads and retrieves legacy In-App messages from the shared preferences.
     *
     * @return An array of legacy In-App messages.
     */
    fun readInApps(): JSONArray {
        val storedInApps = ctPreference.readString(inAppKey!!, "[]")
        return try {
            JSONArray(storedInApps)
        } catch (e: JSONException) {
            JSONArray()
        }
    }

    /**
     * Removes the stored legacy In-App messages from the shared preferences.
     */
    fun removeInApps() {
        ctPreference.remove(inAppKey!!)
    }

    fun updateAssetCleanupTs(ts: Long) {
        ctPreference.writeLong(ASSETS_CLEANUP_TS_KEY, ts)
    }

    fun lastCleanupTs(): Long {
        return ctPreference.readLong(ASSETS_CLEANUP_TS_KEY, 0)
    }
}