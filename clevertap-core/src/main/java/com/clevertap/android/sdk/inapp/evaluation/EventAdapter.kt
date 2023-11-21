package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import com.clevertap.android.sdk.Constants

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
    val userLocation: Location? = null
) {

    /**
     * Gets the property value for the specified property name.
     *
     * @param propertyName The name of the property to retrieve.
     * @return A [TriggerValue] representing the property value.
     */
    fun getPropertyValue(propertyName: String): TriggerValue =
        TriggerValue(eventProperties[propertyName])

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
}
