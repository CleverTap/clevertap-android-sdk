package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builder class which creates an JSON object to display local in-app. Only supports half-interstitial
 * and alert local in-apps.
 */
public class CTLocalInApp private constructor() {

    public enum class InAppType(public val type: String) {
        ALERT(CTInAppType.CTInAppTypeAlert.toString()),
        HALF_INTERSTITIAL(CTInAppType.CTInAppTypeHalfInterstitial.toString())
    }

    public companion object {

        @JvmStatic
        public fun builder(): Builder = Builder()
        public const val IS_LOCAL_INAPP: String = "isLocalInApp"
        public const val FALLBACK_TO_NOTIFICATION_SETTINGS: String = "fallbackToNotificationSettings"
    }

    public class Builder internal constructor() {

        private var jsonObject = JSONObject()

        public fun setInAppType(inAppType: InAppType): Builder1 =
            jsonObject.run {
                put(Constants.KEY_TYPE, inAppType.type)
                put(IS_LOCAL_INAPP, true)
                put(Constants.KEY_HIDE_CLOSE, true)
                Builder1(this)
            }

        public class Builder1(private var jsonObject: JSONObject) {

            public fun setTitleText(titleText: String): Builder2 =
                jsonObject.run {
                    put(Constants.KEY_TITLE, JSONObject().put(Constants.KEY_TEXT, titleText))
                    Builder2(this)
                }
        }

        public class Builder2(private var jsonObject: JSONObject) {

            public fun setMessageText(messageText: String): Builder3 =
                jsonObject.run {
                    put(Constants.KEY_MESSAGE, JSONObject().put(Constants.KEY_TEXT, messageText))
                    Builder3(this)
                }
        }

        public class Builder3(private var jsonObject: JSONObject) {

            public fun followDeviceOrientation(deviceOrientation: Boolean): Builder4 =
                jsonObject.run {
                    put(Constants.KEY_PORTRAIT, true)
                    put(Constants.KEY_LANDSCAPE, deviceOrientation)
                    Builder4(this)
                }
        }

        public class Builder4(private var jsonObject: JSONObject) {

            public fun setPositiveBtnText(positiveBtnText: String): Builder5 =
                jsonObject.run {
                    val positiveButtonObject = JSONObject().apply {
                        put(Constants.KEY_TEXT, positiveBtnText)
                        put(Constants.KEY_RADIUS, "2")
                        put(Constants.KEY_ACTIONS, JSONObject().apply {
                            put(Constants.KEY_TYPE, InAppActionType.CLOSE)
                        })
                    }
                    put(Constants.KEY_BUTTONS, JSONArray().put(0, positiveButtonObject))
                    Builder5(this)
                }
        }

        public class Builder5(private var jsonObject: JSONObject) {

            public fun setNegativeBtnText(negativeBtnText: String): Builder6 =
                jsonObject.run {
                    val negativeButtonObject = JSONObject().apply {
                        put(Constants.KEY_TEXT, negativeBtnText)
                        put(Constants.KEY_RADIUS, "2")
                        put(Constants.KEY_ACTIONS, JSONObject().apply {
                            put(Constants.KEY_TYPE, InAppActionType.CLOSE)
                        })
                    }
                    getJSONArray(Constants.KEY_BUTTONS).put(1, negativeButtonObject)
                    Builder6(this)
                }
        }

        public class Builder6(private var jsonObject: JSONObject) {

            public fun setFallbackToSettings(fallbackToSettings: Boolean): Builder6 =
                apply { jsonObject.put(FALLBACK_TO_NOTIFICATION_SETTINGS, fallbackToSettings) }

            public fun setBackgroundColor(backgroundColor: String): Builder6 =
                apply { jsonObject.put(Constants.KEY_BG, backgroundColor) }

            public fun setImageUrl(imageUrl: String): Builder6 = setImageUrl(imageUrl, null)

            public fun setImageUrl(imageUrl: String, contentDescription: String?): Builder6 =
                apply {
                    val mediaObject = JSONObject().apply {
                        put(Constants.KEY_URL, imageUrl)
                        put(Constants.KEY_CONTENT_TYPE, "image")
                        if (contentDescription != null) {
                            put(Constants.KEY_ALT_TEXT, contentDescription)
                        }
                    }
                    jsonObject.apply {
                        put(Constants.KEY_MEDIA, mediaObject)
                        if (getBoolean(Constants.KEY_LANDSCAPE)) {
                            put(Constants.KEY_MEDIA_LANDSCAPE, mediaObject)
                        }
                    }
                }

            public fun setTitleTextColor(titleTextColor: String): Builder6 =
                apply { jsonObject.getJSONObject(Constants.KEY_TITLE).put(Constants.KEY_COLOR, titleTextColor) }

            public fun setMessageTextColor(messageTextColor: String): Builder6 =
                apply { jsonObject.getJSONObject(Constants.KEY_MESSAGE).put(Constants.KEY_COLOR, messageTextColor) }

            public fun setBtnTextColor(btnTextColor: String): Builder6 =
                apply { updateActionButtonArray(Constants.KEY_COLOR, btnTextColor) }

            public fun setBtnBackgroundColor(btnBackgroundColor: String): Builder6 =
                apply { updateActionButtonArray(Constants.KEY_BG, btnBackgroundColor) }

            public fun setBtnBorderColor(btnBorderColor: String): Builder6 =
                apply { updateActionButtonArray(Constants.KEY_BORDER, btnBorderColor) }

            public fun setBtnBorderRadius(btnBorderRadius: String): Builder6 =
                apply { updateActionButtonArray(Constants.KEY_RADIUS, btnBorderRadius) }

            private val updateActionButtonArray: (String, String) -> Unit = { key, value ->
                arrayOf(0, 1).forEach {
                    jsonObject.getJSONArray(Constants.KEY_BUTTONS).getJSONObject(it).put(key, value)
                }
            }

            public fun build(): JSONObject = jsonObject
        }
    }
}
