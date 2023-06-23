package com.clevertap.android.sdk.leanplum

import android.content.Context
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger

internal class CleverTapProvider {

  private var customInstance: CleverTapAPI? = null
  private var defaultInstance: CleverTapAPI? = null

  constructor(context: Context) {
    this.defaultInstance = CleverTapAPI.getDefaultInstance(context)
  }

  constructor(customInstance: CleverTapAPI) {
    this.customInstance = customInstance
  }

  fun getCleverTap(): CleverTapAPI? {
    if (customInstance != null) {
      return customInstance
    } else if (defaultInstance != null) {
      return defaultInstance
    }
    Logger.i("CTWrapper", "Please initialize LeanplumCT, because CleverTap instance is missing.")
    return null
  }

}
