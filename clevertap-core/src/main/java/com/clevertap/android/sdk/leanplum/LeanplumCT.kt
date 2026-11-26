package com.clevertap.android.sdk.leanplum

import android.content.Context
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger

/**
 * For Java compatibility we must have all of the function parameters as nullable.
 */
public object LeanplumCT {

  private var wrapper: CTWrapper? = null
    get() {
      if (field == null) {
        Logger.i("LeanplumCT", "Please initialize LeanplumCT before using it.")
      }
      return field
    }

  @JvmStatic
  public fun getPurchaseEventName(): String = "Purchase"

  /**
   * Initialization with Context would cause the CleverTap SDK to be initialized too.
   */
  @JvmStatic
  public fun initWithContext(context: Context?) {
    if (context != null) {
      val ctProvider = CleverTapProvider(context)
      wrapper = CTWrapper(ctProvider)
    }
  }

  @JvmStatic
  public fun initWithInstance(cleverTapInstance: CleverTapAPI?) {
    if (cleverTapInstance != null) {
      val ctProvider = CleverTapProvider(cleverTapInstance)
      wrapper = CTWrapper(ctProvider)
    }
  }

  @JvmStatic
  public fun advanceTo(state: String?) {
    advanceTo(state, null, null)
  }

  @JvmStatic
  public fun advanceTo(state: String?, info: String?) {
    advanceTo(state, info, null)
  }

  @JvmStatic
  public fun advanceTo(state: String?, params: Map<String, Any?>?) {
    advanceTo(state, null, params)
  }

  @JvmStatic
  public fun advanceTo(state: String?, info: String?, params: Map<String, Any?>?) {
    wrapper?.advanceTo(state, info, params)
  }

  @JvmStatic
  public fun setLogLevel(logLevel: CleverTapAPI.LogLevel) {
    CleverTapAPI.setDebugLevel(logLevel)
  }

  @JvmStatic
  public fun setTrafficSourceInfo(info: Map<String, String>?) {
    if (info != null) {
      wrapper?.setTrafficSourceInfo(info)
    }
  }

  @JvmStatic
  public fun setUserAttributes(attributes: Map<String, Any?>?) {
    wrapper?.setUserAttributes(attributes)
  }

  @JvmStatic
  public fun setUserAttributes(userId: String?, attributes: Map<String, Any?>?) {
    if (userId != null) {
      setUserId(userId)
    }
    setUserAttributes(attributes)
  }

  @JvmStatic
  public fun setUserId(userId: String?) {
    wrapper?.setUserId(userId)
  }

  @JvmStatic
  public fun track(event: String?) {
    track(event, .0, null, null)
  }

  @JvmStatic
  public fun track(event: String?, value: Double) {
    track(event, value, null, null)
  }

  @JvmStatic
  public fun track(event: String?, info: String?) {
    track(event, .0, info, null)
  }

  @JvmStatic
  public fun track(event: String?, params: Map<String, Any?>?) {
    track(event, .0, null, params)
  }

  @JvmStatic
  public fun track(event: String?, value: Double, params: Map<String, Any?>?) {
    track(event, value, null, params)
  }

  @JvmStatic
  public fun track(event: String?, value: Double, info: String?) {
    track(event, value, info, null)
  }

  @JvmStatic
  public fun track(event: String?, value: Double, info: String?, params: Map<String, Any?>?) {
    wrapper?.track(event, value, info, params)
  }

  @JvmStatic
  public fun trackGooglePlayPurchase(
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
  public fun trackGooglePlayPurchase(
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
  public fun trackGooglePlayPurchase(
    eventName: String?,
    item: String?,
    priceMicros: Long,
    currencyCode: String?,
    purchaseData: String?,
    dataSignature: String?,
    params: Map<String, Any?>?
  ) {
    if (eventName.isNullOrEmpty()) {
      Logger.i("LeanplumCT", "Failed to call trackGooglePlayPurchase, event name is null");
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
  public fun trackPurchase(
    event: String,
    value: Double,
    currencyCode: String?,
    params: Map<String, Any?>?
  ) {
    wrapper?.trackPurchase(event, value, currencyCode, params)
  }

}
