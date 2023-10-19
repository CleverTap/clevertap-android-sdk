package com.clevertap.android.sdk.leanplum

import android.content.Context
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger

/**
 * For Java compatibility we must have all of the function parameters as nullable.
 */
object LeanplumCT {

  private var wrapper: CTWrapper? = null
    get() {
      if (field == null) {
        Logger.info("LeanplumCT", "Please initialize LeanplumCT before using it.")
      }
      return field
    }

  @JvmStatic
  fun getPurchaseEventName() = "Purchase"

  /**
   * Initialization with Context would cause the CleverTap SDK to be initialized too.
   */
  @JvmStatic
  fun initWithContext(context: Context?) {
    if (context != null) {
      val ctProvider = CleverTapProvider(context)
      wrapper = CTWrapper(ctProvider)
    }
  }

  @JvmStatic
  fun initWithInstance(cleverTapInstance: CleverTapAPI?) {
    if (cleverTapInstance != null) {
      val ctProvider = CleverTapProvider(cleverTapInstance)
      wrapper = CTWrapper(ctProvider)
    }
  }

  @JvmStatic
  fun advanceTo(state: String?) {
    advanceTo(state, null, null)
  }

  @JvmStatic
  fun advanceTo(state: String?, info: String?) {
    advanceTo(state, info, null)
  }

  @JvmStatic
  fun advanceTo(state: String?, params: Map<String, Any?>?) {
    advanceTo(state, null, params)
  }

  @JvmStatic
  fun advanceTo(state: String?, info: String?, params: Map<String, Any?>?) {
    wrapper?.advanceTo(state, info, params)
  }

  @JvmStatic
  fun setLogLevel(logLevel: CleverTapAPI.LogLevel) {
    CleverTapAPI.setDebugLevel(logLevel)
  }

  @JvmStatic
  fun setTrafficSourceInfo(info: Map<String, String>?) {
    if (info != null) {
      wrapper?.setTrafficSourceInfo(info)
    }
  }

  @JvmStatic
  fun setUserAttributes(attributes: Map<String, Any?>?) {
    wrapper?.setUserAttributes(attributes)
  }

  @JvmStatic
  fun setUserAttributes(userId: String?, attributes: Map<String, Any?>?) {
    if (userId != null) {
      setUserId(userId)
    }
    setUserAttributes(attributes)
  }

  @JvmStatic
  fun setUserId(userId: String?) {
    wrapper?.setUserId(userId)
  }

  @JvmStatic
  fun track(event: String?) {
    track(event, .0, null, null)
  }

  @JvmStatic
  fun track(event: String?, value: Double) {
    track(event, value, null, null)
  }

  @JvmStatic
  fun track(event: String?, info: String?) {
    track(event, .0, info, null)
  }

  @JvmStatic
  fun track(event: String?, params: Map<String, Any?>?) {
    track(event, .0, null, params)
  }

  @JvmStatic
  fun track(event: String?, value: Double, params: Map<String, Any?>?) {
    track(event, value, null, params)
  }

  @JvmStatic
  fun track(event: String?, value: Double, info: String?) {
    track(event, value, info, null)
  }

  @JvmStatic
  fun track(event: String?, value: Double, info: String?, params: Map<String, Any?>?) {
    wrapper?.track(event, value, info, params)
  }

  @JvmStatic
  fun trackGooglePlayPurchase(
    item: String?,
    priceMicros: Long,
    currencyCode: String?,
    purchaseData: String?,
    dataSignature: String?
  ) {
    trackGooglePlayPurchase(
      getPurchaseEventName(),
      item,
      priceMicros,
      currencyCode,
      purchaseData,
      dataSignature,
      null
    )
  }

  @JvmStatic
  fun trackGooglePlayPurchase(
    item: String,
    priceMicros: Long,
    currencyCode: String,
    purchaseData: String,
    dataSignature: String,
    params: Map<String, Any?>?
  ) {
    trackGooglePlayPurchase(
      getPurchaseEventName(),
      item,
      priceMicros,
      currencyCode,
      purchaseData,
      dataSignature,
      params
    )
  }

  @JvmStatic
  fun trackGooglePlayPurchase(
    eventName: String?,
    item: String?,
    priceMicros: Long,
    currencyCode: String?,
    purchaseData: String?,
    dataSignature: String?,
    params: Map<String, Any?>?
  ) {
    if (eventName.isNullOrEmpty()) {
      Logger.info("LeanplumCT", "Failed to call trackGooglePlayPurchase, event name is null");
      return
    }

    wrapper?.trackGooglePlayPurchase(
      eventName,
      item,
      priceMicros / 1000000.0,
      currencyCode,
      purchaseData,
      dataSignature,
      params
    )
  }

  @JvmStatic
  fun trackPurchase(
    event: String,
    value: Double,
    currencyCode: String?,
    params: Map<String, Any?>?
  ) {
    wrapper?.trackPurchase(event, value, currencyCode, params)
  }

}
