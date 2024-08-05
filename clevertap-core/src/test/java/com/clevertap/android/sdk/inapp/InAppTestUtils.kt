package com.clevertap.android.sdk.inapp

import org.json.JSONObject

fun createCtInAppNotification(json: JSONObject, videoSupported: Boolean = false): CTInAppNotification {
    return CTInAppNotification().initWithJSON(json, videoSupported)
}
