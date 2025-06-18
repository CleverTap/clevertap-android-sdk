package com.clevertap.android.sdk.inapp

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.CTInAppNotificationMedia.Companion.create
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData.CREATOR.createFromJson
import com.clevertap.android.sdk.utils.getStringOrNull
import org.json.JSONException
import org.json.JSONObject
import kotlin.reflect.KClass

internal class CTInAppNotification : Parcelable {

    var actionExtras: JSONObject? = null

    var backgroundColor: String = Constants.WHITE
        private set

    var buttonCount: Int = 0
        private set

    private var _buttons = ArrayList<CTInAppNotificationButton>()
    val buttons: List<CTInAppNotificationButton>
        get() = _buttons

    var campaignId: String? = null
        private set

    var customExtras: JSONObject? = null
        private set

    var customInAppUrl: String? = null
        private set

    var isDarkenScreen: Boolean = false
        private set

    var error: String? = null

    var isExcludeFromCaps: Boolean = false
        private set

    var height: Int = 0
        private set

    var heightPercentage: Int = 0
        private set

    var aspectRatio: Double = HTML_DEFAULT_ASPECT_RATIO
        private set

    var isHideCloseButton: Boolean = false
        private set

    var html: String? = null
        private set

    var id: String? = null
        private set

    var inAppType: CTInAppType? = null
        private set

    var isLandscape: Boolean = false
        private set

    var isPortrait: Boolean = false
        private set

    var isTablet: Boolean = false
        private set

    var isJsEnabled: Boolean = false
        private set

    var jsonDescription: JSONObject
        private set

    var landscapeImageUrl: String? = null
        private set

    var maxPerSession: Int = 0
        private set

    private var _mediaList = ArrayList<CTInAppNotificationMedia>()
    val mediaList: List<CTInAppNotificationMedia>
        get() = _mediaList

    var message: String? = null
        private set

    var messageColor: String = Constants.BLACK
        private set

    var position: Char = 0.toChar()
        private set

    var isShowClose: Boolean = false
        private set

    var timeToLive: Long = 0
        private set

    var title: String? = null
        private set

    var titleColor: String = Constants.BLACK
        private set

    var totalDailyCount: Int = 0
        private set

    var totalLifetimeCount: Int = 0
        private set

    var type: String? = null
        private set

    var isVideoSupported: Boolean = false
        private set

    var width: Int = 0
        private set

    var widthPercentage: Int = 0
        private set

    var isLocalInApp: Boolean = false
        private set

    var fallBackToNotificationSettings = false
        private set

    var isRequestForPushPermission: Boolean = false
        private set

    var customTemplateData: CustomTemplateInAppData? = null
        private set

    constructor(jsonObject: JSONObject, videoSupported: Boolean) {
        isVideoSupported = videoSupported
        jsonDescription = jsonObject
        try {
            type = jsonObject.getStringOrNull(Constants.KEY_TYPE)
            if (type == null || type == Constants.KEY_CUSTOM_HTML) {
                legacyConfigureWithJson(jsonObject)
            } else {
                configureWithJson(jsonObject)
            }
        } catch (e: JSONException) {
            error = "Invalid JSON: ${e.localizedMessage}"
        }
    }

    private constructor(parcel: Parcel) {
        id = parcel.readString()
        campaignId = parcel.readString()
        inAppType = parcel.readValue(CTInAppType::class.java.getClassLoader()) as? CTInAppType?
        html = parcel.readString()
        isExcludeFromCaps = parcel.readByte().toInt() != 0x00
        isShowClose = parcel.readByte().toInt() != 0x00
        isDarkenScreen = parcel.readByte().toInt() != 0x00
        maxPerSession = parcel.readInt()
        totalLifetimeCount = parcel.readInt()
        totalDailyCount = parcel.readInt()
        position = parcel.readInt().toChar()
        height = parcel.readInt()
        heightPercentage = parcel.readInt()
        width = parcel.readInt()
        widthPercentage = parcel.readInt()
        jsonDescription = JSONObject(parcel.readString() ?: EMPTY_JSON)
        error = parcel.readString()
        customExtras = if (parcel.readByte().toInt() == 0x00) {
            null
        } else {
            JSONObject(parcel.readString() ?: EMPTY_JSON)
        }
        actionExtras = if (parcel.readByte().toInt() == 0x00) {
            null
        } else {
            JSONObject(parcel.readString() ?: EMPTY_JSON)
        }
        type = parcel.readString()
        title = parcel.readString()
        titleColor = parcel.readString() ?: titleColor
        backgroundColor = parcel.readString() ?: backgroundColor
        message = parcel.readString()
        messageColor = parcel.readString() ?: messageColor
        try {
            _buttons =
                parcel.createTypedArrayList<CTInAppNotificationButton>(CTInAppNotificationButton.CREATOR)
                    ?: ArrayList()
        } catch (_: Throwable) {
            // no-op
        }
        try {
            _mediaList =
                parcel.createTypedArrayList<CTInAppNotificationMedia>(CTInAppNotificationMedia.CREATOR)
                    ?: ArrayList()
        } catch (_: Throwable) {
            // no-op
        }
        isHideCloseButton = parcel.readByte().toInt() != 0x00
        buttonCount = parcel.readInt()
        isTablet = parcel.readByte().toInt() != 0x00
        customInAppUrl = parcel.readString()
        isJsEnabled = parcel.readByte().toInt() != 0x00
        isPortrait = parcel.readByte().toInt() != 0x00
        isLandscape = parcel.readByte().toInt() != 0x00
        isLocalInApp = parcel.readByte().toInt() != 0x00
        fallBackToNotificationSettings = parcel.readByte().toInt() != 0x00
        landscapeImageUrl = parcel.readString()
        timeToLive = parcel.readLong()
        customTemplateData =
            parcel.readParcelable<CustomTemplateInAppData?>(CustomTemplateInAppData::class.java.getClassLoader())
        aspectRatio = parcel.readDouble()
        isRequestForPushPermission = parcel.readByte().toInt() != 0x00
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(campaignId)
        dest.writeValue(inAppType)
        dest.writeString(html)
        dest.writeByte((if (this.isExcludeFromCaps) 0x01 else 0x00).toByte())
        dest.writeByte((if (this.isShowClose) 0x01 else 0x00).toByte())
        dest.writeByte((if (this.isDarkenScreen) 0x01 else 0x00).toByte())
        dest.writeInt(maxPerSession)
        dest.writeInt(totalLifetimeCount)
        dest.writeInt(totalDailyCount)
        dest.writeInt(position.code)
        dest.writeInt(height)
        dest.writeInt(heightPercentage)
        dest.writeInt(width)
        dest.writeInt(widthPercentage)
        dest.writeString(jsonDescription.toString())
        dest.writeString(error)
        if (customExtras == null) {
            dest.writeByte((0x00).toByte())
        } else {
            dest.writeByte((0x01).toByte())
            dest.writeString(customExtras.toString())
        }
        if (actionExtras == null) {
            dest.writeByte((0x00).toByte())
        } else {
            dest.writeByte((0x01).toByte())
            dest.writeString(actionExtras.toString())
        }
        dest.writeString(type)
        dest.writeString(title)
        dest.writeString(titleColor)
        dest.writeString(backgroundColor)
        dest.writeString(message)
        dest.writeString(messageColor)
        dest.writeTypedList<CTInAppNotificationButton?>(_buttons)
        dest.writeTypedList<CTInAppNotificationMedia?>(_mediaList)
        dest.writeByte((if (this.isHideCloseButton) 0x01 else 0x00).toByte())
        dest.writeInt(buttonCount)
        dest.writeByte((if (isTablet) 0x01 else 0x00).toByte())
        dest.writeString(customInAppUrl)
        dest.writeByte((if (this.isJsEnabled) 0x01 else 0x00).toByte())
        dest.writeByte((if (isPortrait) 0x01 else 0x00).toByte())
        dest.writeByte((if (isLandscape) 0x01 else 0x00).toByte())
        dest.writeByte((if (isLocalInApp) 0x01 else 0x00).toByte())
        dest.writeByte((if (fallBackToNotificationSettings) 0x01 else 0x00).toByte())
        dest.writeString(landscapeImageUrl)
        dest.writeLong(timeToLive)
        dest.writeParcelable(customTemplateData, flags)
        dest.writeDouble(aspectRatio)
        dest.writeByte((if (isRequestForPushPermission) 0x01 else 0x00).toByte())
    }

    fun fallBackToNotificationSettings(): Boolean {
        return fallBackToNotificationSettings
    }

    fun getInAppMediaForOrientation(orientation: Int): CTInAppNotificationMedia? {
        var returningMedia: CTInAppNotificationMedia? = null
        for (inAppNotificationMedia in _mediaList) {
            if (orientation == inAppNotificationMedia.orientation) {
                returningMedia = inAppNotificationMedia
                break
            }
        }
        return returningMedia
    }

    fun createNotificationForAction(actionData: CustomTemplateInAppData?): CTInAppNotification? {
        try {
            val notificationJson = JSONObject()
            notificationJson.put(Constants.INAPP_ID_IN_PAYLOAD, id)
            notificationJson.put(Constants.NOTIFICATION_ID_TAG, campaignId)
            notificationJson.put(Constants.KEY_TYPE, InAppActionType.CUSTOM_CODE.toString())
            notificationJson.put(Constants.KEY_EFC, 1)
            notificationJson.put(Constants.KEY_EXCLUDE_GLOBAL_CAPS, 1)
            notificationJson.put(Constants.KEY_WZRK_TTL, timeToLive)
            if (jsonDescription.has(Constants.INAPP_WZRK_PIVOT)) {
                notificationJson.put(
                    Constants.INAPP_WZRK_PIVOT, jsonDescription.optString(
                        Constants.INAPP_WZRK_PIVOT
                    )
                )
            }
            if (jsonDescription.has(Constants.INAPP_WZRK_CGID)) {
                notificationJson.put(
                    Constants.INAPP_WZRK_CGID, jsonDescription.optString(
                        Constants.INAPP_WZRK_CGID
                    )
                )
            }
            val notification = CTInAppNotification(notificationJson, isVideoSupported)
            notification.setCustomTemplateData(actionData)
            return notification
        } catch (_: JSONException) {
            return null
        }
    }

    fun setCustomTemplateData(inAppData: CustomTemplateInAppData?) {
        customTemplateData = inAppData
        inAppData?.writeFieldsToJson(jsonDescription)
    }

    fun hasStreamMedia(): Boolean {
        return !_mediaList.isEmpty() && _mediaList[0].isMediaStreamable()
    }

    private fun configureWithJson(jsonObject: JSONObject) {
        try {
            id = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD, "")
            campaignId = jsonObject.optString(Constants.NOTIFICATION_ID_TAG, "")
            type = jsonObject.getString(Constants.KEY_TYPE) // won't be null based on constructor
            isLocalInApp = jsonObject.optBoolean(CTLocalInApp.Companion.IS_LOCAL_INAPP, false)
            fallBackToNotificationSettings = jsonObject.optBoolean(
                CTLocalInApp.Companion.FALLBACK_TO_NOTIFICATION_SETTINGS, false
            )
            isExcludeFromCaps = jsonObject.optInt(Constants.KEY_EFC, -1) == 1
                    || jsonObject.optInt(Constants.KEY_EXCLUDE_GLOBAL_CAPS, -1) == 1
            totalLifetimeCount = jsonObject.optInt(Constants.KEY_TLC, -1)
            totalDailyCount = jsonObject.optInt(Constants.KEY_TDC, -1)
            maxPerSession = jsonObject.optInt(Constants.INAPP_MAX_DISPLAY_COUNT, -1)
            inAppType = CTInAppType.fromString(type)
            isTablet = jsonObject.optBoolean(Constants.KEY_IS_TABLET, false)
            backgroundColor = jsonObject.optString(Constants.KEY_BG, backgroundColor)
            isPortrait = !jsonObject.has(Constants.KEY_PORTRAIT) || jsonObject.getBoolean(
                Constants.KEY_PORTRAIT
            )
            isLandscape = jsonObject.optBoolean(Constants.KEY_LANDSCAPE, false)
            timeToLive = jsonObject.optLong(Constants.WZRK_TIME_TO_LIVE, defaultTtl())

            val titleObject = jsonObject.optJSONObject(Constants.KEY_TITLE)
            if (titleObject != null) {
                title = titleObject.optString(Constants.KEY_TEXT, "")
                titleColor = titleObject.optString(Constants.KEY_COLOR, titleColor)
            }

            val msgObject = jsonObject.optJSONObject(Constants.KEY_MESSAGE)
            if (msgObject != null) {
                message = msgObject.optString(Constants.KEY_TEXT, "")
                messageColor = msgObject.optString(Constants.KEY_COLOR, messageColor)
            }

            isHideCloseButton = jsonObject.optBoolean(Constants.KEY_HIDE_CLOSE, false)

            val media = jsonObject.optJSONObject(Constants.KEY_MEDIA)
            if (media != null) {
                val portraitMedia = create(media, Configuration.ORIENTATION_PORTRAIT)
                if (portraitMedia != null) {
                    _mediaList.add(portraitMedia)
                }
            }

            val mediaLandscape = jsonObject.optJSONObject(Constants.KEY_MEDIA_LANDSCAPE)
            if (mediaLandscape != null) {
                val landscapeMedia = create(mediaLandscape, Configuration.ORIENTATION_LANDSCAPE)
                if (landscapeMedia != null) {
                    _mediaList.add(landscapeMedia)
                }
            }

            val buttonArray = jsonObject.optJSONArray(Constants.KEY_BUTTONS)
            if (buttonArray != null) {
                for (i in 0..<buttonArray.length()) {
                    val buttonJson = buttonArray.optJSONObject(i)
                    if (buttonJson != null) {
                        _buttons.add(CTInAppNotificationButton(buttonJson))
                        buttonCount++
                    }
                }
            }
            isRequestForPushPermission =
                jsonObject.optBoolean(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION, false)
            customTemplateData = createFromJson(jsonObject)

            when (inAppType) {
                CTInAppType.CTInAppTypeFooter,
                CTInAppType.CTInAppTypeHeader,
                CTInAppType.CTInAppTypeCover,
                CTInAppType.CTInAppTypeHalfInterstitial -> {
                    for (inAppMedia in _mediaList) {
                        if (inAppMedia.isGIF() || inAppMedia.isAudio() || inAppMedia.isVideo()) {
                            inAppMedia.mediaUrl = ""
                            Logger.d("Unable to download to media. Wrong media type for template")
                        }
                    }
                }

                CTInAppType.CTInAppTypeCoverImageOnly,
                CTInAppType.CTInAppTypeHalfInterstitialImageOnly,
                CTInAppType.CTInAppTypeInterstitialImageOnly -> {
                    if (!_mediaList.isEmpty()) {
                        for (inAppMedia in _mediaList) {
                            if (inAppMedia.isGIF() || inAppMedia.isAudio() || inAppMedia.isVideo() || !inAppMedia.isImage()) {
                                error = "Wrong media type for template"
                                break // Exit the loop early if an error is found
                            }
                        }
                    } else {
                        error = "No media type for template"
                    }
                }

                else -> {
                    //do nothing
                }
            }
        } catch (e: JSONException) {
            error = "Invalid JSON: ${e.localizedMessage}"
        }
    }

    private fun legacyConfigureWithJson(jsonObject: JSONObject) {
        val b: Bundle = getBundleFromJsonObject(jsonObject)
        if (!validateNotifBundle(b)) {
            error = "Invalid JSON"
            return
        }
        try {
            id = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD, "")
            campaignId = jsonObject.optString(Constants.NOTIFICATION_ID_TAG, "")
            isExcludeFromCaps =
                jsonObject.optInt(Constants.KEY_EFC, -1) == 1 || jsonObject.optInt(
                    Constants.KEY_EXCLUDE_GLOBAL_CAPS, -1
                ) == 1
            totalLifetimeCount = jsonObject.optInt(Constants.KEY_TLC, -1)
            totalDailyCount = jsonObject.optInt(Constants.KEY_TDC, -1)
            isJsEnabled = jsonObject.optBoolean(Constants.INAPP_JS_ENABLED, false)
            timeToLive = jsonObject.optLong(Constants.WZRK_TIME_TO_LIVE, defaultTtl())
            isRequestForPushPermission =
                jsonObject.optBoolean(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION, false)

            val data = jsonObject.optJSONObject(Constants.INAPP_DATA_TAG)
            if (data != null) {
                html = data.getString(Constants.INAPP_HTML_TAG)
                customInAppUrl = data.optString(Constants.KEY_URL, "")
                customExtras =
                    if (data.optJSONObject(Constants.KEY_KV) != null) data.getJSONObject(
                        Constants.KEY_KV
                    ) else JSONObject()

                val displayParams = jsonObject.optJSONObject(Constants.INAPP_WINDOW)
                if (displayParams != null) {
                    isDarkenScreen =
                        displayParams.getBoolean(Constants.INAPP_NOTIF_DARKEN_SCREEN)
                    isShowClose = displayParams.getBoolean(Constants.INAPP_NOTIF_SHOW_CLOSE)
                    position = displayParams.getString(Constants.INAPP_POSITION).get(0)
                    width = displayParams.optInt(Constants.INAPP_X_DP, 0)
                    widthPercentage = displayParams.optInt(Constants.INAPP_X_PERCENT, 0)
                    height = displayParams.optInt(Constants.INAPP_Y_DP, 0)
                    heightPercentage = displayParams.optInt(Constants.INAPP_Y_PERCENT, 0)
                    maxPerSession = displayParams.optInt(Constants.INAPP_MAX_DISPLAY_COUNT, -1)
                    aspectRatio = displayParams.optDouble(
                        Constants.INAPP_ASPECT_RATIO, HTML_DEFAULT_ASPECT_RATIO
                    )
                    if (aspectRatio <= 0.0f) {
                        aspectRatio = HTML_DEFAULT_ASPECT_RATIO
                    }
                }

                if (html != null) {
                    when (position) {
                        't' -> {
                            if (aspectRatio != -1.0 || (widthPercentage == 100 && heightPercentage <= 30)) {
                                inAppType = CTInAppType.CTInAppTypeHeaderHTML
                            }
                        }

                        'b' -> {
                            if (aspectRatio != -1.0 || (widthPercentage == 100 && heightPercentage <= 30)) {
                                inAppType = CTInAppType.CTInAppTypeFooterHTML
                            }
                        }

                        'c' -> {
                            if (widthPercentage == 90 && heightPercentage == 85) {
                                inAppType = CTInAppType.CTInAppTypeInterstitialHTML
                            } else if (widthPercentage == 100 && heightPercentage == 100) {
                                inAppType = CTInAppType.CTInAppTypeCoverHTML
                            } else if (widthPercentage == 90 && heightPercentage == 50) {
                                inAppType = CTInAppType.CTInAppTypeHalfInterstitialHTML
                            }
                        }
                    }
                }
            }
        } catch (_: JSONException) {
            error = "Invalid JSON"
        }
    }

    private fun isKeyValid(b: Bundle, key: String?, type: KClass<*>): Boolean {
        return b.containsKey(key) && type.isInstance(b.get(key))
    }

    private fun validateNotifBundle(notif: Bundle): Boolean {
        try {
            val w = notif.getBundle(Constants.INAPP_WINDOW)
            val d = notif.getBundle("d")
            if (w == null || d == null) {
                return false
            }

            // Check that either xdp or xp is set
            if (!isKeyValid(w, Constants.INAPP_X_DP, Integer::class)) {
                if (!isKeyValid(w, Constants.INAPP_X_PERCENT, Integer::class)) {
                    return false
                }
            }

            // Check that either ydp or yp is set
            if (!isKeyValid(w, Constants.INAPP_Y_DP, Integer::class)) {
                if (!isKeyValid(w, Constants.INAPP_Y_PERCENT, Integer::class)) {
                    return false
                }
            }

            // Check that dk is set
            if (!(isKeyValid(w, Constants.INAPP_NOTIF_DARKEN_SCREEN, Boolean::class))) {
                return false
            }

            // Check that sc is set
            if (!(isKeyValid(w, Constants.INAPP_NOTIF_SHOW_CLOSE, Boolean::class))) {
                return false
            }

            // Check that html is set
            if (!(isKeyValid(d, Constants.INAPP_HTML_TAG, String::class))) {
                return false
            }

            // Check that pos contains the right value
            if ((isKeyValid(w, Constants.INAPP_POSITION, String::class))) {
                val pos = w.getString(Constants.INAPP_POSITION)!![0]
                when (pos) {
                    Constants.INAPP_POSITION_TOP,
                    Constants.INAPP_POSITION_RIGHT,
                    Constants.INAPP_POSITION_BOTTOM,
                    Constants.INAPP_POSITION_LEFT,
                    Constants.INAPP_POSITION_CENTER -> {
                    }

                    else -> return false
                }
            } else {
                return false
            }

            return true
        } catch (t: Throwable) {
            Logger.v("Failed to parse in-app notification!", t)
            return false
        }
    }

    companion object {
        const val HTML_DEFAULT_ASPECT_RATIO: Double = -1.0

        private const val EMPTY_JSON = "{}"

        @JvmField
        val CREATOR: Parcelable.Creator<CTInAppNotification> =
            object : Parcelable.Creator<CTInAppNotification> {
                override fun createFromParcel(`in`: Parcel): CTInAppNotification {
                    return CTInAppNotification(`in`)
                }

                override fun newArray(size: Int): Array<CTInAppNotification?> {
                    return arrayOfNulls<CTInAppNotification>(size)
                }
            }

        fun defaultTtl(): Long {
            return (System.currentTimeMillis() + 2 * Constants.ONE_DAY_IN_MILLIS) / 1000
        }

        private fun getBundleFromJsonObject(notif: JSONObject): Bundle {
            val b = Bundle()
            val iterator: MutableIterator<*> = notif.keys()
            while (iterator.hasNext()) {
                val key = iterator.next() as String
                try {
                    val value = notif.get(key)
                    if (value is String) {
                        b.putString(key, value)
                    } else if (value is Char) {
                        b.putChar(key, value)
                    } else if (value is Int) {
                        b.putInt(key, value)
                    } else if (value is Float) {
                        b.putFloat(key, value)
                    } else if (value is Double) {
                        b.putDouble(key, value)
                    } else if (value is Long) {
                        b.putLong(key, value)
                    } else if (value is Boolean) {
                        b.putBoolean(key, value)
                    } else if (value is JSONObject) {
                        b.putBundle(key, getBundleFromJsonObject(value))
                    }
                } catch (_: JSONException) {
                    Logger.v("Key had unknown object. Discarding")
                }
            }
            return b
        }
    }
}
