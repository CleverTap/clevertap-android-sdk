package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.matchers.EventAdapter
import org.json.JSONObject

class EvaluationManager {

  fun evaluateOnEvent(eventName: String, eventProperties: Map<String, Any>) {
    if (eventName != "App Launched") {
      val event = EventAdapter(eventName, eventProperties)
      evaluateServerSide(event)
      evaluateClientSide(event)
    }
  }

  fun evaluateOnChargedEvent(
    eventName: String,
    details: Map<String, Any>,
    items: List<Map<String, Any>>
  ) {
    val event = EventAdapter(eventName, details, items)
    evaluateServerSide(event)
    evaluateClientSide(event)
  }

  // onBatchSent with App Launched event in batch
  fun evaluateOnAppLaunchedClientSide() {
    val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
    evaluateClientSide(event)
  }

  fun evaluateOnAppLaunchedServerSide(appLaunchedNotifs: List<JSONObject>) {
    val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, emptyMap())
    evaluate(event, appLaunchedNotifs)
    // TODO handle supressed inapps
    // TODO eligibleInapps.sort().first().display();
  }

  private fun evaluateServerSide(event: EventAdapter) {
    // evaluate(event, getServerSideNotifsFromStore())
    // TODO add to meta inapp_evals : eligibleInapps.addToMeta();
  }

  private fun evaluateClientSide(event: EventAdapter) {
    // evaluate(event, getClientSideNotifsFromStore())
    // TODO handle supressed inapps
    // TODO calculate TTL field and put it in the json based on ttlOffset parameter
    // TODO eligibleInapps.sort().first().display();
  }

  private fun evaluate(event: EventAdapter, inappNotifs: List<JSONObject>): List<JSONObject> {
    return listOf() // returns eligible inapps
  }

}
