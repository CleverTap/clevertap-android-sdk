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
class ImpressionStore(context: Context, deviceId: String) {

  companion object {
    private const val PREF_PREFIX = "__impressions"
  }

  private val contextRef = WeakReference(context)
  private val prefName = "${Constants.KEY_COUNTS_PER_INAPP}:$deviceId"

  fun read(campaignId: String): List<Long> {
    val prefs = sharedPrefs() ?: return listOf()
    return getLongListFromPrefs(prefs, "${PREF_PREFIX}_$campaignId")
  }

  fun write(campaignId: String, timestamp: Long) {
    // TODO put some limit on the numbers of impressions written per inapp

    val records = read(campaignId).toMutableList()
    records.add(timestamp)

    val prefs = sharedPrefs() ?: return
    saveLongListToPrefs(records, prefs, "${PREF_PREFIX}_$campaignId")
  }

  private fun sharedPrefs(): SharedPreferences? {
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

  // TODO handle inappStale from server to clear data per inapp
}
