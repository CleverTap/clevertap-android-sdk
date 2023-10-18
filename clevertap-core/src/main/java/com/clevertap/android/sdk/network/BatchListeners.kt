package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.EvaluationManager
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
      if (item.has("evtName") && Constants.APP_LAUNCHED_EVENT == item.get("evtName")) { // TODO check success?
        onAppLaunchedFound()
        return
      }
    }
  }

  private fun onAppLaunchedFound() {
    evaluationManager.onAppLaunchedSent()
  }
}
