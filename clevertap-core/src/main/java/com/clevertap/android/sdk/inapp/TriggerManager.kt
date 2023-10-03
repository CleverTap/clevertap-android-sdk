package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import java.lang.ref.WeakReference

/**
 * Responsible for storing triggers count for a given campaign ID.
 * It stores triggers in the shared preferences named "WizRocket_triggers_per_inapp:<<account_id>>:<<device_id>>"
 * with keys in the format "__triggers_<<campaign_id>>".
 */
class TriggerManager(
    context: Context, accountId: String, deviceId: String
) {
    companion object {
        const val PREF_PREFIX = "__triggers"
    }

    var contextRef = WeakReference(context)
    val prefName = "${Constants.KEY_TRIGGERS_PER_INAPP}:$deviceId:$accountId"

    /**
     * Retrieves the trigger count for a given campaign ID.
     *
     * @param campaignId The identifier of the In-App campaign.
     * @return The trigger count for the specified campaign, or 0 if not found.
     */
    fun getTriggers(campaignId: String): Int {
        val prefs = sharedPrefs() ?: return 0
        return read(prefs, getTriggersKey(campaignId))
    }

    /**
     * Increments the trigger count for a given campaign ID.
     *
     * @param campaignId The identifier of the In-App campaign.
     */
    fun increment(campaignId: String) {
        val prefs = sharedPrefs() ?: return
        var savedTriggers = getTriggers(campaignId)
        savedTriggers++
        write(prefs, getTriggersKey(campaignId), savedTriggers)
    }

    /**
     * Removes the trigger count for a given campaign ID.
     *
     * @param campaignId The identifier of the In-App campaign.
     */
    fun removeTriggers(campaignId: String) { // TODO handle inappStale from server to clear data per inapp
        val prefs = sharedPrefs() ?: return
        prefs.edit().remove(getTriggersKey(campaignId)).apply()
    }

    /**
     * Reads an integer value from SharedPreferences for the specified storage key.
     *
     * @param prefs      The SharedPreferences instance to read from.
     * @param storageKey The key to retrieve the integer value.
     * @return The integer value associated with the storage key, or 0 if not found.
     */
    private fun read(prefs: SharedPreferences, storageKey: String): Int {
        return prefs.getInt(storageKey, 0)
    }

    /**
     * Writes an integer value to SharedPreferences for the specified storage key.
     *
     * @param prefs        The SharedPreferences instance to write to.
     * @param storageKey   The key to store the integer value.
     * @param triggerCount The integer value to store.
     */
    private fun write(prefs: SharedPreferences, storageKey: String, triggerCount: Int) {
        prefs.edit().putInt(storageKey, triggerCount).apply()
    }

    /**
     * Retrieves the shared preferences instance for storing trigger counts.
     *
     * @return The SharedPreferences instance, or null if the context reference is null.
     */
    fun sharedPrefs(): SharedPreferences? {
        val context = contextRef.get() ?: return null
        return StorageHelper.getPreferences(context, prefName)
    }

    /**
     * Generates the storage key for trigger counts for a given campaign ID.
     *
     * @param campaignId The identifier of the In-App campaign.
     * @return The generated storage key in the format "__triggers_campaignId".
     */
    fun getTriggersKey(campaignId: String): String {
        return "${PREF_PREFIX}_$campaignId"
    }
}
