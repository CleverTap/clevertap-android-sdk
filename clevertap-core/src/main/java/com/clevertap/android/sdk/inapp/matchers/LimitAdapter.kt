package com.clevertap.android.sdk.inapp.matchers

import org.json.JSONObject

enum class LimitType {
  Ever,
  Session,
  Seconds,
  Minutes,
  Hours,
  Days,
  Weeks,
  OnEvery,
  OnExactly,
}

class LimitAdapter(limitJson: JSONObject) {

  fun getType(): LimitType {
    return LimitType.Ever
  }

  fun getLimit(): Int {
    return 0
  }

  fun getFrequency(): Int {
    return 0
  }
}

