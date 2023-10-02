package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import java.lang.ref.WeakReference

/**
 * Responsible for storing impressions for a given campaign ID.
 * It stores impressions the shared preferences name "WizRocket_counts_per_inapp:<<account_id>>:<<device_id>>"
 * with keys in the format "__impression_<<campaign_id>>".
 */
class ImpressionStore(
    context: Context, accountId: String, deviceId: String
) {
    companion object {
        const val PREF_PREFIX = "__impressions"
    }

    var contextRef = WeakReference(context)
    val prefName = "${Constants.KEY_COUNTS_PER_INAPP}:$accountId:$deviceId"

    /**
     * Reads the impressions for a given campaign ID.
     *
     * @param campaignId The campaign ID for which to read the impressions.
     * @return A list of impressions for the given campaign ID.
     */
    fun read(campaignId: String): List<Long> {
        val prefs = sharedPrefs() ?: return listOf()
        return getLongListFromPrefs(prefs, "${PREF_PREFIX}_$campaignId")
    }

    /**
     * Writes an impression for a given campaign ID.
     *
     * @param campaignId The campaign ID for which to write the impression.
     * @param timestamp The timestamp of the impression.
     */
    fun write(campaignId: String, timestamp: Long) { //TODO Migrate inAppFcManager's count per device key to new prefName key?
        val records = read(campaignId).toMutableList()
        records.add(timestamp)

        val prefs = sharedPrefs() ?: return
        saveLongListToPrefs(records, prefs, "${PREF_PREFIX}_$campaignId")
    }

    /**
     * Clears the impressions for a given campaign ID.
     *
     * @param campaignId The campaign ID for which to clear the impressions.
     */
    fun clear(campaignId: String) { // TODO handle inappStale from server to clear data per inapp
        val prefs = sharedPrefs() ?: return
        prefs.edit().remove("${PREF_PREFIX}_$campaignId").apply()
    }

    /**
     * Retrieves the shared preferences instance for storing data.
     *
     * @return The SharedPreferences instance, or null if the context reference is null.
     */
    fun sharedPrefs(): SharedPreferences? {
        val context = contextRef.get() ?: return null
        return StorageHelper.getPreferences(context, prefName)
    }

    /**
     * Saves a list of timestamp values to shared preferences as a serialized string.
     *
     * @param list   The list of long timestamp values to be stored.
     * @param prefs  The SharedPreferences instance.
     * @param key    The key under which to store the data.
     */
    private fun saveLongListToPrefs(list: List<Long>, prefs: SharedPreferences, key: String) {
        val serialized = list.joinToString(",")
        prefs.edit().putString(key, serialized).apply()
    }

    /**
     * Retrieves a list of timestamp values from shared preferences for a given key.
     *
     * @param prefs  The SharedPreferences instance.
     * @param key    The key under which the data is stored.
     * @return A list of long timestamp values, or an empty list if no data is found.
     */
    private fun getLongListFromPrefs(prefs: SharedPreferences, key: String): List<Long> {
        val serialized = prefs.getString(key, "") ?: ""
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(",").map { it.toLong() }
    }
}
