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

  /**
   * Returns the default event name used by [trackGooglePlayPurchase].
   *
   * @return `"Purchase"`
   */
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

  /**
   * Initialises LeanplumCT with an existing CleverTap instance; later calls report to it.
   * A `null` instance is ignored.
   *
   * @param cleverTapInstance the CleverTap instance to use
   */
  @JvmStatic
  public fun initWithInstance(cleverTapInstance: CleverTapAPI?) {
    if (cleverTapInstance != null) {
      val ctProvider = CleverTapProvider(cleverTapInstance)
      wrapper = CTWrapper(ctProvider)
    }
  }

  /**
   * Tracks entering an app state as the event `state_<state>`. Does nothing until LeanplumCT has
   * been initialised.
   *
   * @param state the state name; `null` is ignored
   */
  @JvmStatic
  public fun advanceTo(state: String?) {
    advanceTo(state, null, null)
  }

  /**
   * Tracks entering an app state as the event `state_<state>`, with an `info` property.
   * Does nothing until LeanplumCT has been initialised.
   *
   * @param state the state name; `null` is ignored
   * @param info sent as the `info` property, if not `null`
   */
  @JvmStatic
  public fun advanceTo(state: String?, info: String?) {
    advanceTo(state, info, null)
  }

  /**
   * Tracks entering an app state as the event `state_<state>`, with properties. Does nothing until
   * LeanplumCT has been initialised.
   *
   * @param state the state name; `null` is ignored
   * @param params the event properties. Iterable values in [params] are sent as a `[a,b]` string,
   * Byte and Short values as Int.
   */
  @JvmStatic
  public fun advanceTo(state: String?, params: Map<String, Any?>?) {
    advanceTo(state, null, params)
  }

  /**
   * Tracks entering an app state as the event `state_<state>`, with properties and an `info`
   * property. Does nothing until LeanplumCT has been initialised.
   *
   * @param state the state name; `null` is ignored
   * @param info sent as the `info` property, if not `null`
   * @param params the event properties. Iterable values in [params] are sent as a `[a,b]` string,
   * Byte and Short values as Int.
   */
  @JvmStatic
  public fun advanceTo(state: String?, info: String?, params: Map<String, Any?>?) {
    wrapper?.advanceTo(state, info, params)
  }

  /**
   * Sets the global CleverTap log level (same as `CleverTapAPI.setDebugLevel`).
   *
   * @param logLevel the log level
   */
  @JvmStatic
  public fun setLogLevel(logLevel: CleverTapAPI.LogLevel) {
    CleverTapAPI.setDebugLevel(logLevel)
  }

  /**
   * Reports the install referrer: `publisherName` is sent as the source, `publisherSubPublisher` as
   * the medium and `publisherSubCampaign` as the campaign. Does nothing until LeanplumCT has been
   * initialised.
   *
   * @param info the traffic source values; `null` is ignored
   */
  @JvmStatic
  public fun setTrafficSourceInfo(info: Map<String, String>?) {
    if (info != null) {
      wrapper?.setTrafficSourceInfo(info)
    }
  }

  /**
   * Updates the user profile with the given attributes; an attribute with a `null` value is removed
   * from the profile. Does nothing until LeanplumCT has been initialised.
   *
   * @param attributes the attributes; `null` or empty is ignored. Iterable values are sent as a
   * `[a,b]`
   * string, Byte and Short values as Int.
   */
  @JvmStatic
  public fun setUserAttributes(attributes: Map<String, Any?>?) {
    wrapper?.setUserAttributes(attributes)
  }

  /**
   * Calls [setUserId] (if [userId] is not `null`), then [setUserAttributes].
   *
   * @param userId the user ID, or `null` to keep the current user
   * @param attributes the attributes to set
   */
  @JvmStatic
  public fun setUserAttributes(userId: String?, attributes: Map<String, Any?>?) {
    if (userId != null) {
      setUserId(userId)
    }
    setUserAttributes(attributes)
  }

  /**
   * Logs the user in with `onUserLogin`, using [userId] as the `Identity`. Does nothing until
   * LeanplumCT has been initialised.
   *
   * @param userId the user ID; `null` or empty is ignored
   */
  @JvmStatic
  public fun setUserId(userId: String?) {
    wrapper?.setUserId(userId)
  }

  /**
   * Tracks an event with a `value` property of 0. Does nothing until LeanplumCT has been
   * initialised.
   *
   * @param event the event name; `null` is ignored
   */
  @JvmStatic
  public fun track(event: String?) {
    track(event, .0, null, null)
  }

  /**
   * Tracks an event with a `value` property. Does nothing until LeanplumCT has been initialised.
   *
   * @param event the event name; `null` is ignored
   * @param value sent as the `value` property
   */
  @JvmStatic
  public fun track(event: String?, value: Double) {
    track(event, value, null, null)
  }

  /**
   * Tracks an event with an `info` property and a `value` property of 0. Does nothing until
   * LeanplumCT has been initialised.
   *
   * @param event the event name; `null` is ignored
   * @param info sent as the `info` property, if not `null`
   */
  @JvmStatic
  public fun track(event: String?, info: String?) {
    track(event, .0, info, null)
  }

  /**
   * Tracks an event with properties and a `value` property of 0. Does nothing until LeanplumCT has
   * been initialised.
   *
   * @param event the event name; `null` is ignored
   * @param params the event properties. Iterable values in [params] are sent as a `[a,b]` string,
   * Byte and Short values as Int.
   */
  @JvmStatic
  public fun track(event: String?, params: Map<String, Any?>?) {
    track(event, .0, null, params)
  }

  /**
   * Tracks an event with properties and a `value` property. Does nothing until LeanplumCT has been
   * initialised.
   *
   * @param event the event name; `null` is ignored
   * @param value sent as the `value` property
   * @param params the event properties. Iterable values in [params] are sent as a `[a,b]` string,
   * Byte and Short values as Int.
   */
  @JvmStatic
  public fun track(event: String?, value: Double, params: Map<String, Any?>?) {
    track(event, value, null, params)
  }

  /**
   * Tracks an event with `value` and `info` properties. Does nothing until LeanplumCT has been
   * initialised.
   *
   * @param event the event name; `null` is ignored
   * @param value sent as the `value` property
   * @param info sent as the `info` property, if not `null`
   */
  @JvmStatic
  public fun track(event: String?, value: Double, info: String?) {
    track(event, value, info, null)
  }

  /**
   * Tracks an event with properties plus `value` and `info` properties. Does nothing until
   * LeanplumCT has been initialised.
   *
   * @param event the event name; `null` is ignored
   * @param value sent as the `value` property
   * @param info sent as the `info` property, if not `null`
   * @param params the event properties. Iterable values in [params] are sent as a `[a,b]` string,
   * Byte and Short values as Int.
   */
  @JvmStatic
  public fun track(event: String?, value: Double, info: String?, params: Map<String, Any?>?) {
    wrapper?.track(event, value, info, params)
  }

  /**
   * Tracks a Google Play purchase as a Charged event whose `event` property is [getPurchaseEventName].
   *
   * @param item the purchased item
   * @param priceMicros the price in micro-units; sent divided by 1,000,000
   * @param currencyCode the currency code
   * @param purchaseData the Google Play purchase data
   * @param dataSignature the signature of the purchase data
   */
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

  /**
   * Tracks a Google Play purchase as a Charged event whose `event` property is [getPurchaseEventName],
   * with extra properties.
   *
   * @param item the purchased item
   * @param priceMicros the price in micro-units; sent divided by 1,000,000
   * @param currencyCode the currency code
   * @param purchaseData the Google Play purchase data
   * @param dataSignature the signature of the purchase data
   * @param params extra properties of the Charged event. Iterable values in [params] are sent as a
   * `[a,b]` string, Byte and Short values as Int.
   */
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

  /**
   * Tracks a Google Play purchase as a Charged event whose `event` property is [eventName].
   * Does nothing until LeanplumCT has been initialised.
   *
   * @param eventName the purchase event name; `null` or empty is ignored (and logged)
   * @param item the purchased item
   * @param priceMicros the price in micro-units; sent divided by 1,000,000
   * @param currencyCode the currency code
   * @param purchaseData the Google Play purchase data
   * @param dataSignature the signature of the purchase data
   * @param params extra properties of the Charged event. Iterable values in [params] are sent as a
   * `[a,b]` string, Byte and Short values as Int.
   */
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

  /**
   * Tracks a purchase as a Charged event with `event`, `value` and (if not `null`) `currencyCode`
   * properties. Does nothing until LeanplumCT has been initialised.
   *
   * @param event the purchase event name, sent as the `event` property
   * @param value the purchase value
   * @param currencyCode the currency code, or `null`
   * @param params extra properties of the Charged event. Iterable values in [params] are sent as a
   * `[a,b]` string, Byte and Short values as Int.
   */
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
