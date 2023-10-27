package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import org.json.JSONArray

interface BatchListener {
    fun onBatchSent(batch: JSONArray, success: Boolean)
}

class CompositeBatchListener : BatchListener {
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

class AppLaunchListener(private val evaluationManager: EvaluationManager) : BatchListener {

    override fun onBatchSent(batch: JSONArray, success: Boolean) {
        for (i in 0 until batch.length()) {
            val item = batch.getJSONObject(i)
            if (item.optString("evtName") == Constants.APP_LAUNCHED_EVENT && success) { // TODO check success?
                onAppLaunchedFound()
                return
            }
        }
    }

  private fun onAppLaunchedFound() {
    evaluationManager.onAppLaunchedWithSuccess()
  }
}

class FetchInAppListener(private val callbackManager: BaseCallbackManager) : BatchListener {

    override fun onBatchSent(batch: JSONArray, success: Boolean) {

        if (batch.length() == 0) {
            callbackManager.fetchInAppsCallback?.onInAppsFetched(success)
            return
        }
        for (i in 0 until batch.length()) {
            val item = batch.getJSONObject(i)
            if (item.optString(Constants.KEY_EVT_NAME) == Constants.WZRK_FETCH
                && item.get(Constants.KEY_T) == Constants.FETCH_TYPE_IN_APPS
            ) {
                callbackManager.fetchInAppsCallback?.onInAppsFetched(success)
                return
            }
        }
    }

}
