package com.clevertap.android.sdk.inapp

import android.content.Context
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DeviceInfo
import org.json.JSONArray
import org.json.JSONObject

class CTLocalInApp private constructor() {

    enum class InAppType(val type: String) {
        ALERT(CTInAppType.CTInAppTypeAlert.toString()),
        HALF_INTERSTITIAL(CTInAppType.CTInAppTypeHalfInterstitial.toString())
    }

    companion object {

        @JvmStatic
        fun builder(): Builder = Builder()
        const val IS_LOCAL_INAPP = "isLocalInApp"
        const val FALLBACK_TO_NOTIFICATION_SETTINGS = "fallbackToNotificationSettings"
    }

    class Builder internal constructor() {

        private var jsonObject = JSONObject()

        fun setInAppType(inAppType: InAppType) =
            jsonObject.run {
                put(Constants.KEY_TYPE, inAppType.type)
                put(IS_LOCAL_INAPP, true)
                put(Constants.KEY_HIDE_CLOSE, true)
                Builder1(this)
            }

        class Builder1(private var jsonObject: JSONObject) {

            fun setTitleText(titleText: String) =
                jsonObject.run {
                    put(Constants.KEY_TITLE, JSONObject().put(Constants.KEY_TEXT, titleText))
                    Builder2(this)
                }
        }

        class Builder2(private var jsonObject: JSONObject) {

            fun setMessageText(messageText: String) =
                jsonObject.run {
                    put(Constants.KEY_MESSAGE, JSONObject().put(Constants.KEY_TEXT, messageText))
                    Builder3(this)
                }
        }

        class Builder3(private var jsonObject: JSONObject) {

            fun followDeviceOrientation(deviceOrientation: Boolean) =
                jsonObject.run {
                    put(Constants.KEY_PORTRAIT, true)
                    put(Constants.KEY_LANDSCAPE, deviceOrientation)
                    Builder4(this)
                }
        }

        class Builder4(private var jsonObject: JSONObject) {

            fun setPositiveBtnText(positiveBtnText: String) =
                jsonObject.run {
                    val positiveButtonObject = JSONObject().apply {
                        put(Constants.KEY_TEXT, positiveBtnText)
                        put(Constants.KEY_RADIUS, "2")
                    }
                    put(Constants.KEY_BUTTONS, JSONArray().put(0, positiveButtonObject))
                    Builder5(this)
                }
        }

        class Builder5(private var jsonObject: JSONObject) {

            fun setNegativeBtnText(negativeBtnText: String) =
                jsonObject.run {
                    val negativeButtonObject = JSONObject().apply {
                        put(Constants.KEY_TEXT, negativeBtnText)
                        put(Constants.KEY_RADIUS, "2")
                    }
                    getJSONArray(Constants.KEY_BUTTONS).put(1, negativeButtonObject)
                    Builder6(this)
                }
        }

        class Builder6(private var jsonObject: JSONObject) {

            fun setFallbackToSettings(fallbackToSettings: Boolean) =
                apply { jsonObject.put(FALLBACK_TO_NOTIFICATION_SETTINGS, fallbackToSettings) }

            fun setBackgroundColor(backgroundColor: String) =
                apply { jsonObject.put(Constants.KEY_BG, backgroundColor) }

            fun setImageUrl(imageUrl: String) =
                apply {
                    val mediaObject = JSONObject().apply {
                        put(Constants.KEY_URL, imageUrl)
                        put(Constants.KEY_CONTENT_TYPE, "image")
                    }
                    jsonObject.apply {
                        put(Constants.KEY_MEDIA, mediaObject)
                        if (getBoolean(Constants.KEY_LANDSCAPE)) {
                            put(Constants.KEY_MEDIA_LANDSCAPE, mediaObject)
                        }
                    }
                }

            fun setTitleTextColor(titleTextColor: String) =
                apply { jsonObject.getJSONObject(Constants.KEY_TITLE).put(Constants.KEY_COLOR, titleTextColor) }

            fun setMessageTextColor(messageTextColor: String) =
                apply { jsonObject.getJSONObject(Constants.KEY_MESSAGE).put(Constants.KEY_COLOR, messageTextColor) }

            fun setBtnTextColor(btnTextColor: String) =
                apply { updateActionButtonArray(Constants.KEY_COLOR, btnTextColor) }

            fun setBtnBackgroundColor(btnBackgroundColor: String) =
                apply { updateActionButtonArray(Constants.KEY_BG, btnBackgroundColor) }

            fun setBtnBorderColor(btnBorderColor: String) =
                apply { updateActionButtonArray(Constants.KEY_BORDER, btnBorderColor) }

            fun setBtnBorderRadius(btnBorderRadius: String) =
                apply { updateActionButtonArray(Constants.KEY_RADIUS, btnBorderRadius) }

            private val updateActionButtonArray: (String, String) -> Unit = { key, value ->
                arrayOf(0, 1).forEach {
                    jsonObject.getJSONArray(Constants.KEY_BUTTONS).getJSONObject(it).put(key, value)
                }
            }

            fun build() = jsonObject
        }
    }
}
