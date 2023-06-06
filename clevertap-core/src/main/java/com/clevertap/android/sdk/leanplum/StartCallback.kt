package com.clevertap.android.sdk.leanplum

// TODO Think about replacing this one with callback after App Launched is triggered
interface StartCallback {
  fun onResponse(success: Boolean)
}
