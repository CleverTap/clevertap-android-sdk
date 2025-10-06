package com.clevertap.android.sdk.inapp.store.db

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.DelayedLegacyInAppDAO
import com.clevertap.android.sdk.db.DelayedLegacyInAppData
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.iterator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class DelayedLegacyInAppStore(
    private val delayedLegacyInAppDAO: DelayedLegacyInAppDAO,
    private val logger: Logger,
    private val accountId : String
) {

    @WorkerThread
    fun saveDelayedInApp(inAppId: String, delay: Int, inAppJson: JSONObject): Boolean {
        val data = DelayedLegacyInAppData(
            inAppId = inAppId,
            delay = delay,
            inAppData = inAppJson.toString()
        )

        val result = delayedLegacyInAppDAO.insert(data)
        return result > 0
    }

    fun saveDelayedInAppsBatch(delayedInApps: JSONArray): Boolean {
        if (delayedInApps.length() == 0) return true

        val dataList = mutableListOf<DelayedLegacyInAppData>()

        delayedInApps.iterator<JSONObject>
        { inAppJson ->
            try {
                val inAppId = inAppJson.optString(Constants.INAPP_ID_IN_PAYLOAD)
                val delay = inAppJson.optInt(INAPP_DELAY_AFTER_TRIGGER)
                dataList.add(
                    DelayedLegacyInAppData(
                        inAppId = inAppId,
                        delay = delay,
                        inAppData = inAppJson.toString()
                    )
                )
            } catch (e: JSONException) {
                logger.verbose(accountId, "Error parsing delayed in-app", e)
            }
        }

        return delayedLegacyInAppDAO.insertBatch(dataList)
    }

    @WorkerThread
    fun getDelayedInApp(inAppId: String): JSONObject? {
        val inAppDataString = delayedLegacyInAppDAO.fetchSingleInApp(inAppId)

        return inAppDataString?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @WorkerThread
    fun removeDelayedInApp(inAppId: String): Boolean {
        return delayedLegacyInAppDAO.remove(inAppId)
    }

    @WorkerThread
    fun removeDelayedInAppsBatch(inAppIds: List<String>): Int {
        var removedCount = 0
        inAppIds.forEach { inAppId ->
            if (delayedLegacyInAppDAO.remove(inAppId)) {
                removedCount++
            }
        }
        return removedCount
    }

    @WorkerThread
    fun hasDelayedInApp(inAppId: String): Boolean {
        return delayedLegacyInAppDAO.fetchSingleInApp(inAppId) != null
    }
}