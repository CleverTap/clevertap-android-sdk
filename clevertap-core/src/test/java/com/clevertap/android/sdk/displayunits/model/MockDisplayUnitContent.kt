package com.clevertap.android.sdk.displayunits.model

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.displayunits.CTDisplayUnitType
import org.json.JSONObject

class MockDisplayUnitContent {

    fun getContent(): JSONObject {
        val contentObj = JSONObject()
        contentObj.put(Constants.KEY_TYPE, CTDisplayUnitType.SIMPLE.name)
        val titleObject = JSONObject()
        contentObj.put(Constants.KEY_TITLE, titleObject)
        titleObject.put(Constants.KEY_TEXT, "mock-content-title")
        titleObject.put(Constants.KEY_COLOR, "mock-content-title-color")

        val msgObject = JSONObject()
        contentObj.put(Constants.KEY_MESSAGE, msgObject)
        msgObject.put(Constants.KEY_TEXT, "mock-msg-text")
        msgObject.put(Constants.KEY_COLOR, "mock-content-msg-color")

        val mediaObject = JSONObject()
        contentObj.put(Constants.KEY_MEDIA, mediaObject)
        mediaObject.put(Constants.KEY_URL, "mock-content-icon-url")
        mediaObject.put(Constants.KEY_CONTENT_TYPE, "mock-content-media-type")
        mediaObject.put(Constants.KEY_POSTER_URL, "mock-content-poster-url")

        val actionObject = JSONObject()
        contentObj.put(Constants.KEY_ACTION, actionObject)

        val iconObject = JSONObject()
        contentObj.put(Constants.KEY_ICON, iconObject)
        iconObject.put(Constants.KEY_URL, "mock-content-icon-url")

        val urlObject = JSONObject()
        actionObject.put(Constants.KEY_URL, urlObject)
        val androidObject = JSONObject()
        urlObject.put(Constants.KEY_ANDROID, androidObject)
        androidObject.put(Constants.KEY_TEXT, "mock-content-android-action-url")
        return contentObj
    }
}