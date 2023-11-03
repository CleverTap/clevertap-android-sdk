package com.clevertap.android.sdk.network

import org.json.JSONObject

interface NetworkHeadersListener {

    fun onAttachHeaders(endpointId: EndpointId): JSONObject?
    fun onSentHeaders(allHeaders: JSONObject, endpointId: EndpointId)
}

enum class EndpointId(val identifier: String) {
    ENDPOINT_SPIKY("-spiky"),
    ENDPOINT_A1("/a1"),
    ENDPOINT_HELLO("/hello"),
    ENDPOINT_DEFINE_VARS("/defineVars");

    companion object {

        @JvmStatic
        fun fromString(identifier: String): EndpointId {
            return values().find { identifier.contains(it.identifier) } ?: ENDPOINT_A1
        }
    }
}