package com.clevertap.android.sdk.inapp.matchers

class EventAdapter(
  private val eventName: String,
  private val eventProperties: Map<String, Any>,
  private val items: List<Map<String, Any>> = listOf(),
) {

  fun getEventName(): String {
    return eventName
  }

  fun getPropertyValue(propertyName: String): TriggerValue? {
    // TODO return null when property is missing
    return TriggerValue()
  }

}
