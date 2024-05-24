package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.CLTAP_APP_VERSION
import com.clevertap.android.sdk.Constants.CLTAP_BLUETOOTH_ENABLED
import com.clevertap.android.sdk.Constants.CLTAP_BLUETOOTH_VERSION
import com.clevertap.android.sdk.Constants.CLTAP_CARRIER
import com.clevertap.android.sdk.Constants.CLTAP_CONNECTED_TO_WIFI
import com.clevertap.android.sdk.Constants.CLTAP_LATITUDE
import com.clevertap.android.sdk.Constants.CLTAP_LONGITUDE
import com.clevertap.android.sdk.Constants.CLTAP_NETWORK_TYPE
import com.clevertap.android.sdk.Constants.CLTAP_OS_VERSION
import com.clevertap.android.sdk.Constants.CLTAP_PROP_CAMPAIGN_ID
import com.clevertap.android.sdk.Constants.CLTAP_PROP_VARIANT
import com.clevertap.android.sdk.Constants.CLTAP_SDK_VERSION
import com.clevertap.android.sdk.Constants.INAPP_WZRK_PIVOT
import com.clevertap.android.sdk.Constants.NOTIFICATION_ID_TAG

/**
 * Represents an event and its associated properties.
 *
 * @property eventName The name of the event.
 * @property eventProperties A map of event properties, where keys are property names and values are property values.
 * @property items A list of items associated with the event (used for charged events). Defaults to an empty list.
 */
class EventAdapter(
    val eventName: String,
    val eventProperties: Map<String, Any>,
    val items: List<Map<String, Any>?> = listOf(), // for chargedEvent only
    val userLocation: Location? = null,
    val profileAttrName: String? = null
) {

    private val systemPropToKey = mapOf(
        "CT App Version" to CLTAP_APP_VERSION,
        "ct_app_version" to CLTAP_APP_VERSION,
        "CT Latitude" to CLTAP_LATITUDE,
        "ct_latitude" to CLTAP_LATITUDE,
        "CT Longitude" to CLTAP_LONGITUDE,
        "ct_longitude" to CLTAP_LONGITUDE,
        "CT OS Version" to CLTAP_OS_VERSION,
        "ct_os_version" to CLTAP_OS_VERSION,
        "CT SDK Version" to CLTAP_SDK_VERSION,
        "ct_sdk_version" to CLTAP_SDK_VERSION,
        "CT Network Carrier" to CLTAP_CARRIER,
        "ct_network_carrier" to CLTAP_CARRIER,
        "CT Network Type" to CLTAP_NETWORK_TYPE,
        "ct_network_type" to CLTAP_NETWORK_TYPE,
        "CT Connected To WiFi" to CLTAP_CONNECTED_TO_WIFI,
        "ct_connected_to_wifi" to CLTAP_CONNECTED_TO_WIFI,
        "CT Bluetooth Version" to CLTAP_BLUETOOTH_VERSION,
        "ct_bluetooth_version" to CLTAP_BLUETOOTH_VERSION,
        "CT Bluetooth Enabled" to CLTAP_BLUETOOTH_ENABLED,
        "ct_bluetooth_enabled" to CLTAP_BLUETOOTH_ENABLED,
        "CT App Name" to "appnId"
    )

    /**
     * Gets the property value for the specified property name.
     *
     * @param propertyName The name of the property to retrieve.
     * @return A [TriggerValue] representing the property value.
     */
    fun getPropertyValue(propertyName: String): TriggerValue {
        val propertyValue = getActualPropertyValue(propertyName)
        return TriggerValue(propertyValue)
    }

    /**
     * Gets the item value for the specified property name from the list of items.
     *
     * @param propertyName The name of the property to retrieve from the items.
     * @return A [TriggerValue] representing the item value.
     */
    fun getItemValue(propertyName: String): List<TriggerValue> {
        return items.filterNotNull().map { TriggerValue(it[propertyName]) }
    }

    /**
     * Checks if the event is a charged event.
     *
     * @return `true` if the event is a charged event; otherwise, `false`.
     */
    fun isChargedEvent(): Boolean {
        return eventName == Constants.CHARGED_EVENT
    }

    @VisibleForTesting
    internal fun getActualPropertyValue(propertyName: String): Any? {
        var value = eventProperties[propertyName]

        if (value == null) {
            value = when (propertyName) {
                CLTAP_PROP_CAMPAIGN_ID -> eventProperties[NOTIFICATION_ID_TAG]
                NOTIFICATION_ID_TAG -> eventProperties[CLTAP_PROP_CAMPAIGN_ID]
                CLTAP_PROP_VARIANT -> eventProperties[INAPP_WZRK_PIVOT]
                INAPP_WZRK_PIVOT -> eventProperties[CLTAP_PROP_VARIANT]
                else -> systemPropToKey[propertyName]?.let { eventProperties[it] }
            }
        }

        return value
    }
}
