package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import java.lang.ref.WeakReference

/**
 * Writing data to shared preferences: "WizRocket_counts_per_inapp:<<device_id>>"
 * with keys "__impression_<<campaign_id>>".
 *
 *
 */
class ImpressionStore(
    context: Context, accountId: String, deviceId: String
) {
    companion object {
        const val PREF_PREFIX = "__impressions"
    }

    var contextRef = WeakReference(context)
    val prefName = "${Constants.KEY_COUNTS_PER_INAPP}:$accountId:$deviceId"

    fun read(campaignId: String): List<Long> {
        val prefs = sharedPrefs() ?: return listOf()
        return getLongListFromPrefs(prefs, "${PREF_PREFIX}_$campaignId")
    }

    fun write(campaignId: String, timestamp: Long) {
        val records = read(campaignId).toMutableList()
        records.add(timestamp)

        val prefs = sharedPrefs() ?: return
        saveLongListToPrefs(records, prefs, "${PREF_PREFIX}_$campaignId")
    }

    // TODO handle inappStale from server to clear data per inapp
    fun clear(campaignId: String) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().remove("${PREF_PREFIX}_$campaignId").apply()
    }

    //TODO handle case where contextRef.get() returns null? possibly due to GC collected
    fun sharedPrefs(): SharedPreferences? {
        val context = contextRef.get() ?: return null
        return StorageHelper.getPreferences(context, prefName)
    }

    private fun saveLongListToPrefs(list: List<Long>, prefs: SharedPreferences, key: String) {
        val serialized = list.joinToString(",")
        prefs.edit().putString(key, serialized).apply()
    }

    private fun getLongListFromPrefs(prefs: SharedPreferences, key: String): List<Long> {
        val serialized = prefs.getString(key, "") ?: ""
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(",").map { it.toLong() }
    }
}
