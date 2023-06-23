package com.clevertap.android.sdk.leanplum

import com.clevertap.android.sdk.Logger

internal class CTWrapper(private val ctProvider: CleverTapProvider) {

  fun setUserId(userId: String?) {
    if (userId.isNullOrEmpty()) return

    val profile = mapOf(Constants.IDENTITY to userId)

    Logger.d("CTWrapper", "setUserId will call onUserLogin with $profile")
    ctProvider.getCleverTap()?.onUserLogin(profile)
  }

  /**
   * LP doesn't allow iterables in params.
   */
  fun track(
    event: String?,
    value: Double,
    info: String?,
    params: Map<String, Any?>?
  ) {
    if (event == null) return

    val properties =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    properties[Constants.VALUE_PARAM] = value

    if (info != null) {
      properties[Constants.INFO_PARAM] = info
    }

    Logger.d("CTWrapper", "track(...) will call pushEvent with $event and $properties")
    ctProvider.getCleverTap()?.pushEvent(event, properties)
  }

  /**
   * LP doesn't allow iterables in params.
   */
  fun trackPurchase(
    event: String,
    value: Double,
    currencyCode: String?,
    params: Map<String, Any?>?
  ) {
    val filteredParams =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    val details = HashMap<String, Any?>(filteredParams).apply {
      this[Constants.CHARGED_EVENT_PARAM] = event
      this[Constants.VALUE_PARAM] = value
      if (currencyCode != null) {
        this[Constants.CURRENCY_CODE_PARAM] = currencyCode
      }
    }

    val items = arrayListOf<HashMap<String, Any?>>()

    Logger.d("CTWrapper", "trackPurchase will call pushChargedEvent with $details and $items")
    ctProvider.getCleverTap()?.pushChargedEvent(details, items)
  }

  /**
   * LP doesn't allow iterables in params.
   */
  fun trackGooglePlayPurchase(
    event: String,
    item: String?,
    value: Double,
    currencyCode: String?,
    purchaseData: String?,
    dataSignature: String?,
    params: Map<String, Any?>?
  ) {
    val filteredParams =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    val details = HashMap<String, Any?>(filteredParams).apply {
      this[Constants.CHARGED_EVENT_PARAM] = event
      this[Constants.VALUE_PARAM] = value
      this[Constants.CURRENCY_CODE_PARAM] = currencyCode
      this[Constants.GP_PURCHASE_DATA_PARAM] = purchaseData
      this[Constants.GP_PURCHASE_DATA_SIGNATURE_PARAM] = dataSignature
      this[Constants.IAP_ITEM_PARAM] = item
    }

    val items = arrayListOf<HashMap<String, Any?>>()

    Logger.d("CTWrapper", "trackGooglePlayPurchase will call pushChargedEvent with $details and $items")
    ctProvider.getCleverTap()?.pushChargedEvent(details, items)
  }

  /**
   * LP doesn't allow iterables in params.
   */
  fun advanceTo(state: String?, info: String?, params: Map<String, Any?>?) {
    if (state == null) return;

    val event = Constants.STATE_PREFIX + state
    val properties =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    if (info != null) {
      properties[Constants.INFO_PARAM] = info
    }

    Logger.d("CTWrapper", "advance(...) will call pushEvent with $event and $properties")
    ctProvider.getCleverTap()?.pushEvent(event, properties)
  }

  /**
   * To remove an attribute CT.removeValueForKey is used.
   */
  fun setUserAttributes(attributes: Map<String, Any?>?) {
    if (attributes.isNullOrEmpty()) {
      return
    }

    val profile = attributes
      .filterValues { value -> value != null }
      .mapValues(::mapNotSupportedValues)

    Logger.d("CTWrapper", "setUserAttributes will call pushProfile with $profile")
    ctProvider.getCleverTap()?.pushProfile(profile)

    attributes
      .filterValues { value -> value == null}
      .forEach {
        Logger.d("CTWrapper", "setUserAttributes will call removeValueForKey with ${it.key}")
        ctProvider.getCleverTap()?.removeValueForKey(it.key)
      }
  }

  private fun mapNotSupportedValues(entry: Map.Entry<String, Any?>): Any? {
    return when (val value = entry.value) {
      is Iterable<*> ->
        value
          .filterNotNull()
          .joinToString(separator = ",", prefix = "[", postfix = "]")
      is Byte -> value.toInt()
      is Short -> value.toInt()
      else -> value
    }
  }

  fun setTrafficSourceInfo(info: Map<String, String>) {
    val source = info["publisherName"]
    val medium = info["publisherSubPublisher"]
    val campaign = info["publisherSubCampaign"]
    Logger.d("CTWrapper", "setTrafficSourceInfo will call pushInstallReferrer with " +
        "$source, $medium, and $campaign")
    ctProvider.getCleverTap()?.pushInstallReferrer(source, medium, campaign)
  }

}
