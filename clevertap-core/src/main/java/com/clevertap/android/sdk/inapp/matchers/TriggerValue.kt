package com.clevertap.android.sdk.inapp.matchers

class TriggerValue(listValue: List<Any> = listOf()) {

  fun numberValue(): Double {
    return .0
  }

  fun stringValue(): String {
    return ""
  }

  fun listValue(): List<Any> { // TODO set type of list to something meaningful
    return listOf()
  }

  fun isArray(): Boolean = false

  fun isList(): Boolean = false

}
