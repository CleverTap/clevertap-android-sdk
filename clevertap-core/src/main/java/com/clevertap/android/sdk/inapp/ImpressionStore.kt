package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.ICTPreference

/**
 * Responsible for storing impressions count for a given campaign ID.
 * It stores impressions in the shared preferences named "WizRocket_counts_per_inapp:<<account_id>>:<<device_id>>"
 * with keys in the format "__impression_<<campaign_id>>".
 */
class ImpressionStore(
    private val ctPreference: ICTPreference,
) {
    companion object {
        const val PREF_PREFIX = "__impressions"
    }

    /**
     * Reads the impressions for a given campaign ID.
     *
     * @param campaignId The campaign ID for which to read the impressions.
     * @return A list of impressions for the given campaign ID.
     */
    fun read(campaignId: String): List<Long> {
        return getLongListFromPrefs("${PREF_PREFIX}_$campaignId")
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

        saveLongListToPrefs("${PREF_PREFIX}_$campaignId", records)
    }

    /**
     * Clears the impressions for a given campaign ID.
     *
     * @param campaignId The campaign ID for which to clear the impressions.
     */
    fun clear(campaignId: String) { // TODO handle inappStale from server to clear data per inapp
        ctPreference.remove("${PREF_PREFIX}_$campaignId")
    }

    /**
     * Saves a list of timestamp values to shared preferences as a serialized string.
     *
     * @param list   The list of long timestamp values to be stored.
     * @param prefs  The SharedPreferences instance.
     * @param key    The key under which to store the data.
     */
    private fun saveLongListToPrefs(key: String, list: List<Long>) {
        val serialized = list.joinToString(",")
        ctPreference.writeString(key, serialized)
    }

    /**
     * Retrieves a list of timestamp values from shared preferences for a given key.
     *
     * @param prefs  The SharedPreferences instance.
     * @param key    The key under which the data is stored.
     * @return A list of long timestamp values, or an empty list if no data is found.
     */
    private fun getLongListFromPrefs(key: String): List<Long> {
        val serialized = ctPreference.readString(key, "")
        if (serialized.isNullOrBlank()) return emptyList()
        return serialized.split(",").map { it.toLong() }
    }
}
