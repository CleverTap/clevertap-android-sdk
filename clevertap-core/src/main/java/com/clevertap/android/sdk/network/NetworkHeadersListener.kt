package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup.REGULAR
import com.clevertap.android.sdk.events.EventGroup.VARIABLES
import com.clevertap.android.sdk.inapp.evaluation.EventType
import org.json.JSONObject

interface NetworkHeadersListener {

    fun onAttachHeaders(endpointId: EndpointId, eventType: EventType): JSONObject?
    fun onSentHeaders(allHeaders: JSONObject, endpointId: EndpointId, eventType: EventType)
}

enum class EndpointId(val identifier: String) {
    ENDPOINT_SPIKY("-spiky"),
    ENDPOINT_A1("/a1"),
    ENDPOINT_HELLO("/hello"),
    ENDPOINT_DEFINE_VARS("/defineVars"),
    CONTENT_FETCH("/content");

    companion object {

        @JvmStatic
        fun fromString(identifier: String): EndpointId {
            return values().find { identifier.contains(it.identifier) } ?: ENDPOINT_A1
        }

        @JvmStatic
        fun fromEventGroup(eventGroup: EventGroup): EndpointId {
            return when (eventGroup) {
                PUSH_NOTIFICATION_VIEWED -> ENDPOINT_SPIKY
                REGULAR -> ENDPOINT_A1
                VARIABLES -> ENDPOINT_DEFINE_VARS
            }
        }
    }
}