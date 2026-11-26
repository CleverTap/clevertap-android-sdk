package com.clevertap.android.sdk.network

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface BatchListener {
    fun onBatchSent(batch: JSONArray, success: Boolean)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CompositeBatchListener : BatchListener {
    private val listeners = mutableListOf<BatchListener>()

    fun addListener(listener: BatchListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: BatchListener) {
        listeners.remove(listener)
    }

    override fun onBatchSent(batch: JSONArray, success: Boolean) {
        listeners.forEach { it.onBatchSent(batch, success) }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AppLaunchListener : BatchListener {

    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    override fun onBatchSent(batch: JSONArray, success: Boolean) {
        for (i in 0 until batch.length()) {
            val item = batch.getJSONObject(i)
            if (item.optString("evtName") == Constants.APP_LAUNCHED_EVENT && success) {
                onAppLaunchedFound()
                return
            }
        }
    }

    private fun onAppLaunchedFound() {
        listeners.forEach { listener -> listener() }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class FetchInAppListener(private val callbackManager: BaseCallbackManager) : BatchListener {

    override fun onBatchSent(batch: JSONArray, success: Boolean) {

        if (batch.length() == 0) {
            callbackManager.fetchInAppsCallback?.onInAppsFetched(success)
            return
        }
        for (i in 0 until batch.length()) {
            val batchItem = batch.optJSONObject(i) ?: JSONObject()
            val batchItemEvtData = batchItem.optJSONObject(Constants.KEY_EVT_DATA) ?: JSONObject()

            if (batchItem.optString(Constants.KEY_EVT_NAME) == Constants.WZRK_FETCH
                && batchItemEvtData.optInt(Constants.KEY_T) == Constants.FETCH_TYPE_IN_APPS
            ) {
                callbackManager.fetchInAppsCallback?.onInAppsFetched(success)
                return
            }
        }
    }

}
