package com.clevertap.android.sdk.inapp.matchers

import org.json.JSONArray
import org.json.JSONObject

class TriggersMatcher {

  fun matchEvent(whenTriggers: JSONArray, eventName: String, eventProperties: Map<String, Any>): Boolean {
    val event = EventAdapter(eventName, eventProperties)

    // events in array are OR-ed
    for (i in 0 until whenTriggers.length()) {
      val trigger = TriggerAdapter(whenTriggers[i] as JSONObject)
      if (match(trigger, event)) {
        return true
      }
    }
    return false
  }

  fun matchChargedEvent(
    whenTriggers: JSONArray,
    eventName: String,
    details: Map<String, Any>,
    items: List<Map<String, Any>>,
  ): Boolean {
    val event = EventAdapter(eventName, details, items)

    // events in array are OR-ed
    for (i in 0 until whenTriggers.length()) {
      val trigger = TriggerAdapter(whenTriggers[i] as JSONObject)
      if (match(trigger, event)) {
        return true
      }
    }
    return false
  }

  private fun match(trigger: TriggerAdapter, event: EventAdapter): Boolean {
    if (event.eventName != trigger.eventName) {
      return false
    }

    // property conditions are AND-ed
    val propCount = trigger.propertyCount
    for (i in 0 until propCount) {
      val condition = trigger.propertyAtIndex(i) ?: continue

      val matched = evaluate(
        condition.op,
        condition.value,
        event.getPropertyValue(condition.propertyName)
      )
      if (!matched) {
        return false
      }
    }

    // (chargedEvent only) property conditions for items are AND-ed
    val itemsCount = trigger.itemsCount
    if (itemsCount > 0) {
      for (i in 0 until itemsCount) {
        val triggerItem = trigger.itemAtIndex(i) ?: continue
        val eventValues = event.getItemValue(triggerItem.propertyName)

        val matched = evaluate(triggerItem.op, triggerItem.value, eventValues)
        if (!matched) {
          return false
        }
      }
    }

    return true
  }

  private fun evaluate(op: TriggerOperator, expected: TriggerValue, actual: TriggerValue?): Boolean {
    if (actual == null) {
      if (op == TriggerOperator.NotSet) {
        return true
      } else {
        return false
      }
    }

    when (op) {
      TriggerOperator.LessThan -> {
        return expected.numberValue().toDouble() < actual.numberValue().toDouble()
      }
      else -> Unit // TODO implement all cases as per the backed evaluation and remove `else` clause
    }
    return false
  }
}
