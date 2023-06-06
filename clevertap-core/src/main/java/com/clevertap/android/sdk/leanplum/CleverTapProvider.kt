package com.clevertap.android.sdk.leanplum

import android.content.Context
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger
import java.lang.ref.WeakReference

internal class CleverTapProvider {

  private var contextRef: WeakReference<Context>? = null
  private var customInstance: CleverTapAPI? = null

  constructor(context: Context) {
    this.contextRef = WeakReference(context)
    CleverTapAPI.getDefaultInstance(context) // create default instance
  }

  constructor(customInstance: CleverTapAPI) {
    this.customInstance = customInstance
  }

  fun getCleverTap(): CleverTapAPI? {
    if (customInstance != null) {
      return customInstance
    } else {
      val context = contextRef?.get()
      if (context != null) {
        return CleverTapAPI.getDefaultInstance(context)
      }
    }
    Logger.i("CTWrapper", "Please initialize LeanplumCT, because CleverTap instance is missing.")
    return null
  }

}
