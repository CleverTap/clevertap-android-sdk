package com.clevertap.android.sdk.inapp.matchers

class EventAdapter(
  private val eventName: String,
  private val eventProperties: Map<String, Any>,
  private val items: List<Map<String, Any>> = listOf(), // for chargedEvent only
) {

  fun getEventName(): String {
    return eventName
  }

  fun getPropertyValue(propertyName: String): TriggerValue? {
    // TODO return null when property is missing
    return TriggerValue()
  }

  fun getItemValue(propertyName: String): TriggerValue? {
    val itemValues = mutableListOf<Any>()
    items.forEach {
      val value = it[propertyName]
      if (value != null) {
        itemValues.add(value)
      }
    }
    return if (itemValues.isEmpty()) null else TriggerValue(itemValues)
  }

}
