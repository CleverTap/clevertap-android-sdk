package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builder class which creates an JSON object to display local in-app. Only supports half-interstitial
 * and alert local in-apps.
 *
 * Start with [builder]; each step returns the next builder, so the required values are set in order
 * (type, title, message, orientation, positive and negative button) before the optional ones in
 * [Builder.Builder6]. Pass the result of [Builder.Builder6.build] to
 * [com.clevertap.android.sdk.CleverTapAPI.promptPushPrimer]. The positive button asks for the push
 * permission (see [Builder.Builder6.setFallbackToSettings]) and the in-app is dismissed when that flow
 * ends; the negative button cancels the request and dismisses the in-app.
 */
public class CTLocalInApp private constructor() {

    /**
     * The templates a local in-app can use.
     *
     * @property type the template identifier written to the in-app JSON
     */
    public enum class InAppType(public val type: String) {
        ALERT(CTInAppType.CTInAppTypeAlert.toString()),
        HALF_INTERSTITIAL(CTInAppType.CTInAppTypeHalfInterstitial.toString())
    }

    public companion object {

        /**
         * Starts building a local in-app.
         *
         * @return the first builder step
         */
        @JvmStatic
        public fun builder(): Builder = Builder()
        /** JSON key, set to `true` by [Builder.setInAppType], that marks an in-app as local. */
        public const val IS_LOCAL_INAPP: String = "isLocalInApp"
        /** JSON key written by [Builder.Builder6.setFallbackToSettings]. */
        public const val FALLBACK_TO_NOTIFICATION_SETTINGS: String = "fallbackToNotificationSettings"
    }

    /** First step of the local in-app builder: choose the template. */
    public class Builder internal constructor() {

        private val jsonObject = JSONObject()

        /**
         * Sets the template. The in-app is marked as local and its `close` flag is set, so the half
         * interstitial shows its close button; the alert template has no close button.
         *
         * @param inAppType the template to use
         * @return the next step, which sets the title
         */
        public fun setInAppType(inAppType: InAppType): Builder1 =
            jsonObject.run {
                put(Constants.KEY_TYPE, inAppType.type)
                put(IS_LOCAL_INAPP, true)
                put(Constants.KEY_HIDE_CLOSE, true)
                Builder1(this)
            }

        /** Second step of the local in-app builder: set the title. */
        public class Builder1(private var jsonObject: JSONObject) {

            /**
             * Sets the title text.
             *
             * @param titleText the title
             * @return the next step, which sets the message
             */
            public fun setTitleText(titleText: String): Builder2 =
                jsonObject.run {
                    put(Constants.KEY_TITLE, JSONObject().put(Constants.KEY_TEXT, titleText))
                    Builder2(this)
                }
        }

        /** Third step of the local in-app builder: set the message. */
        public class Builder2(private var jsonObject: JSONObject) {

            /**
             * Sets the message text.
             *
             * @param messageText the message
             * @return the next step, which sets the orientation
             */
            public fun setMessageText(messageText: String): Builder3 =
                jsonObject.run {
                    put(Constants.KEY_MESSAGE, JSONObject().put(Constants.KEY_TEXT, messageText))
                    Builder3(this)
                }
        }

        /** Fourth step of the local in-app builder: set the supported orientations. */
        public class Builder3(private var jsonObject: JSONObject) {

            /**
             * Sets the supported orientations. Portrait is always supported.
             *
             * @param deviceOrientation `true` to also support landscape
             * @return the next step, which sets the positive button
             */
            public fun followDeviceOrientation(deviceOrientation: Boolean): Builder4 =
                jsonObject.run {
                    put(Constants.KEY_PORTRAIT, true)
                    put(Constants.KEY_LANDSCAPE, deviceOrientation)
                    Builder4(this)
                }
        }

        /** Fifth step of the local in-app builder: set the positive button. */
        public class Builder4(private var jsonObject: JSONObject) {

            /**
             * Sets the text of the positive (first) button.
             *
             * @param positiveBtnText the button label
             * @return the next step, which sets the negative button
             */
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

        /** Sixth step of the local in-app builder: set the negative button. */
        public class Builder5(private var jsonObject: JSONObject) {

            /**
             * Sets the text of the negative (second) button.
             *
             * @param negativeBtnText the button label
             * @return the final step, which sets optional styling and builds the JSON
             */
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

        /** Final step of the local in-app builder: optional settings and [build]. */
        public class Builder6(private var jsonObject: JSONObject) {

            /**
             * Sets whether to open the app's notification settings when the push permission was
             * already denied and can no longer be requested.
             *
             * @param fallbackToSettings `true` to fall back to the notification settings
             * @return this builder
             */
            public fun setFallbackToSettings(fallbackToSettings: Boolean): Builder6 =
                apply { jsonObject.put(FALLBACK_TO_NOTIFICATION_SETTINGS, fallbackToSettings) }

            /**
             * Sets the background color.
             *
             * @param backgroundColor a hex color string, for example `#FFFFFF`
             * @return this builder
             */
            public fun setBackgroundColor(backgroundColor: String): Builder6 =
                apply { jsonObject.put(Constants.KEY_BG, backgroundColor) }

            /**
             * Sets an image without accessibility text. Same as `setImageUrl(imageUrl, null)`.
             *
             * @param imageUrl the image URL
             * @return this builder
             */
            public fun setImageUrl(imageUrl: String): Builder6 = setImageUrl(imageUrl, null)

            /**
             * Sets an image. If landscape was enabled with [Builder3.followDeviceOrientation], the same
             * image is used for landscape too.
             *
             * @param imageUrl the image URL
             * @param contentDescription accessibility text for the image, or `null` for none
             * @return this builder
             */
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

            /**
             * Sets the title text color.
             *
             * @param titleTextColor a hex color string, for example `#000000`
             * @return this builder
             */
            public fun setTitleTextColor(titleTextColor: String): Builder6 =
                apply { jsonObject.getJSONObject(Constants.KEY_TITLE).put(Constants.KEY_COLOR, titleTextColor) }

            /**
             * Sets the message text color.
             *
             * @param messageTextColor a hex color string, for example `#000000`
             * @return this builder
             */
            public fun setMessageTextColor(messageTextColor: String): Builder6 =
                apply { jsonObject.getJSONObject(Constants.KEY_MESSAGE).put(Constants.KEY_COLOR, messageTextColor) }

            /**
             * Sets the text color of both buttons.
             *
             * @param btnTextColor a hex color string, for example `#000000`
             * @return this builder
             */
            public fun setBtnTextColor(btnTextColor: String): Builder6 =
                apply { updateActionButtonArray(Constants.KEY_COLOR, btnTextColor) }

            /**
             * Sets the background color of both buttons.
             *
             * @param btnBackgroundColor a hex color string, for example `#000000`
             * @return this builder
             */
            public fun setBtnBackgroundColor(btnBackgroundColor: String): Builder6 =
                apply { updateActionButtonArray(Constants.KEY_BG, btnBackgroundColor) }

            /**
             * Sets the border color of both buttons.
             *
             * @param btnBorderColor a hex color string, for example `#000000`
             * @return this builder
             */
            public fun setBtnBorderColor(btnBorderColor: String): Builder6 =
                apply { updateActionButtonArray(Constants.KEY_BORDER, btnBorderColor) }

            /**
             * Sets the corner radius of both buttons (default `"2"`).
             *
             * @param btnBorderRadius the radius, as a string
             * @return this builder
             */
            public fun setBtnBorderRadius(btnBorderRadius: String): Builder6 =
                apply { updateActionButtonArray(Constants.KEY_RADIUS, btnBorderRadius) }

            private val updateActionButtonArray: (String, String) -> Unit = { key, value ->
                arrayOf(0, 1).forEach {
                    jsonObject.getJSONArray(Constants.KEY_BUTTONS).getJSONObject(it).put(key, value)
                }
            }

            /**
             * Returns the in-app JSON built so far.
             *
             * @return the local in-app JSON, to pass to
             * [com.clevertap.android.sdk.CleverTapAPI.promptPushPrimer]
             */
            public fun build(): JSONObject = jsonObject
        }
    }
}
