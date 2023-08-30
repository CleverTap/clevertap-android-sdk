package com.clevertap.android.sdk.inapp.matchers

import org.json.JSONObject

data class TriggerCondition(
  val propertyName: String,
  val op: TriggerOperator,
  val value: TriggerValue,
)

enum class TriggerOperator {
  Contains,
  NotContains,
  LessThan,
  GreaterThan,
  Between,
  Equals,
  NotEquals,
  Set, // exists
  NotSet, // not exists
}

class TriggerAdapter(triggerJson: JSONObject) {

  fun getEventName(): String {
    return ""
  }

  fun getPropertyCount(): Int {
    return 0
  }

  fun getProperty(pos: Int): TriggerCondition {
    return TriggerCondition("", TriggerOperator.Contains, TriggerValue())
  }

  fun getItemsCount(): Int { // for chargedEvent
    return 0
  }

  fun getItem(pos: Int): TriggerCondition {
    return TriggerCondition("", TriggerOperator.Contains, TriggerValue())
  }

}
