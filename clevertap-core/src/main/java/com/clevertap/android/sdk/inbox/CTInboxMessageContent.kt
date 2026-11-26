package com.clevertap.android.sdk.inbox

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Public model class for the "msg" object from notification inbox payload
 */
public open class CTInboxMessageContent : Parcelable {

    private var _actionUrl: String? = null

    /**
     * Return the action URL of the body of the inbox message
     */
    public open fun getActionUrl(): String? = _actionUrl

    /** Kotlin property syntax, equivalent to [getActionUrl]. */
    public val actionUrl: String?
        @JvmSynthetic @JvmName("ktGetActionUrl") inline get() = getActionUrl()

    private var _contentType: String? = null

    /**
     * Returns the content type of the media
     */
    public open fun getContentType(): String? = _contentType

    /** Kotlin property syntax, equivalent to [getContentType]. */
    public val contentType: String?
        @JvmSynthetic @JvmName("ktGetContentType") inline get() = getContentType()

    private var hasLinks: Boolean? = null

    private var hasUrl: Boolean? = null

    private var _icon: String? = null

    /**
     * Returns the URL as String for the icon in case of Icon Message template
     */
    public open fun getIcon(): String? = _icon

    /** Kotlin property syntax, equivalent to [getIcon]. */
    public val icon: String?
        @JvmSynthetic @JvmName("ktGetIcon") inline get() = getIcon()

    private var _iconContentDescription: String? = null

    /**
     * Returns the content description for the icon in case of an Icon Message template.
     */
    public open fun getIconContentDescription(): String? = _iconContentDescription

    /** Kotlin property syntax, equivalent to [getIconContentDescription]. */
    public val iconContentDescription: String?
        @JvmSynthetic @JvmName("ktGetIconContentDescription") inline get() = getIconContentDescription()

    private var _links: JSONArray? = null

    /**
     * Returns a JSONArray of Call to Action buttons
     */
    public open fun getLinks(): JSONArray? = _links

    /** Kotlin property syntax, equivalent to [getLinks]. */
    public val links: JSONArray?
        @JvmSynthetic @JvmName("ktGetLinks") inline get() = getLinks()

    private var _media: String? = null

    /**
     * Returns the media URL of the inbox message
     */
    public open fun getMedia(): String? = _media

    /** Kotlin property syntax, equivalent to [getMedia]. */
    public val media: String?
        @JvmSynthetic @JvmName("ktGetMedia") inline get() = getMedia()

    private var _mediaContentDescription: String? = null

    /**
     * Returns the content description of the media in the inbox message.
     */
    public open fun getMediaContentDescription(): String? = _mediaContentDescription

    /** Kotlin property syntax, equivalent to [getMediaContentDescription]. */
    public val mediaContentDescription: String?
        @JvmSynthetic @JvmName("ktGetMediaContentDescription") inline get() = getMediaContentDescription()

    private var _message: String? = null

    /**
     * Returns the message section of the inbox message
     */
    public open fun getMessage(): String? = _message

    /** Kotlin property syntax, equivalent to [getMessage]. */
    public val message: String?
        @JvmSynthetic @JvmName("ktGetMessage") inline get() = getMessage()

    private var _messageColor: String? = null

    /**
     * Returns the hexcode value of the message color as String
     */
    public open fun getMessageColor(): String? = _messageColor

    /** Kotlin property syntax, equivalent to [getMessageColor]. */
    public val messageColor: String?
        @JvmSynthetic @JvmName("ktGetMessageColor") inline get() = getMessageColor()

    private var _posterUrl: String? = null

    /**
     * Returns URL for the thumbnail of the video
     */
    public open fun getPosterUrl(): String? = _posterUrl

    public open fun setPosterUrl(posterUrl: String?) {
        this._posterUrl = posterUrl
    }

    /** Kotlin property syntax, equivalent to [getPosterUrl] / [setPosterUrl]. */
    public var posterUrl: String?
        @JvmSynthetic @JvmName("ktGetPosterUrl") inline get() = getPosterUrl()
        @JvmSynthetic @JvmName("ktSetPosterUrl") inline set(value) = setPosterUrl(value)

    private var _title: String? = null

    /**
     * Returns the title section of the inbox message
     */
    public open fun getTitle(): String? = _title

    /** Kotlin property syntax, equivalent to [getTitle]. */
    public val title: String?
        @JvmSynthetic @JvmName("ktGetTitle") inline get() = getTitle()

    private var _titleColor: String? = null

    /**
     * Returns the hexcode value of the title color as String
     */
    public open fun getTitleColor(): String? = _titleColor

    /** Kotlin property syntax, equivalent to [getTitleColor]. */
    public val titleColor: String?
        @JvmSynthetic @JvmName("ktGetTitleColor") inline get() = getTitleColor()

    internal constructor()

    protected constructor(`in`: Parcel) {
        _title = `in`.readString()
        _titleColor = `in`.readString()
        _message = `in`.readString()
        _messageColor = `in`.readString()
        _media = `in`.readString()
        _mediaContentDescription = `in`.readString()
        hasUrl = `in`.readByte().toInt() != 0x00
        hasLinks = `in`.readByte().toInt() != 0x00
        _actionUrl = `in`.readString()
        _icon = `in`.readString()
        _iconContentDescription = `in`.readString()
        try {
            _links = if (`in`.readByte().toInt() == 0x00) { null } else { JSONArray(`in`.readString()) }
        } catch (e: JSONException) {
            Logger.v("Unable to init CTInboxMessageContent with Parcel - ${e.localizedMessage}")
        }
        _contentType = `in`.readString()
        _posterUrl = `in`.readString()
    }

    override fun describeContents(): Int = 0

    /**
     * Returns the background color for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String
     */
    public open fun getLinkBGColor(jsonObject: JSONObject?): String? {
        if (jsonObject == null) {
            return null
        }
        return try {
            if (jsonObject.has(Constants.KEY_BG)) {
                jsonObject.getString(Constants.KEY_BG)
            } else {
                ""
            }
        } catch (e: JSONException) {
            Logger.v("Unable to get Link Text Color with JSON - ${e.localizedMessage}")
            null
        }
    }

    /**
     * Returns the text color for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String
     */
    public open fun getLinkColor(jsonObject: JSONObject?): String? {
        if (jsonObject == null) {
            return null
        }
        return try {
            if (jsonObject.has(Constants.KEY_COLOR)) {
                jsonObject.getString(Constants.KEY_COLOR)
            } else {
                ""
            }
        } catch (e: JSONException) {
            Logger.v("Unable to get Link Text Color with JSON - ${e.localizedMessage}")
            null
        }
    }

    /**
     * Returns the text for the JSONObject of Link provided
     * The JSONObject of Link provided should be of the type "copy"
     *
     * @param jsonObject of Link
     * @return String
     */
    public open fun getLinkCopyText(jsonObject: JSONObject?): String {
        if (jsonObject == null) {
            return ""
        }
        return try {
            val copyObject = if (jsonObject.has("copyText")) { jsonObject.getJSONObject("copyText") } else { null }
            if (copyObject != null) {
                if (copyObject.has(Constants.KEY_TEXT)) {
                    copyObject.getString(Constants.KEY_TEXT)
                } else {
                    ""
                }
            } else {
                ""
            }
        } catch (e: JSONException) {
            Logger.v("Unable to get Link Text with JSON - ${e.localizedMessage}")
            ""
        }
    }

    /**
     * Returns the Key Value pair with for the JSONObject of Link provided
     */
    public open fun getLinkKeyValue(jsonObject: JSONObject?): HashMap<String, String>? {
        if (jsonObject == null || !jsonObject.has(Constants.KEY_KV)) {
            return null
        }
        return try {
            val keyValues = jsonObject.getJSONObject(Constants.KEY_KV)
            val keyValuesMap = HashMap<String, String>()
            for (key in keyValues.keys()) {
                val value = keyValues.getString(key)
                if (key.isNotEmpty()) {
                    keyValuesMap[key] = value
                }
            }
            keyValuesMap.ifEmpty { null }
        } catch (e: JSONException) {
            Logger.v("Unable to get Link Key Value with JSON - ${e.localizedMessage}")
            null
        }
    }

    /**
     * Returns the text for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String
     */
    public open fun getLinkText(jsonObject: JSONObject?): String? {
        if (jsonObject == null) {
            return null
        }
        return try {
            if (jsonObject.has(Constants.KEY_TEXT)) {
                jsonObject.getString(Constants.KEY_TEXT)
            } else {
                ""
            }
        } catch (e: JSONException) {
            Logger.v("Unable to get Link Text with JSON - ${e.localizedMessage}")
            null
        }
    }

    /**
     * Returns the text for the JSONObject of Link provided
     * The JSONObject of Link provided should be of the type "url"
     *
     * @param jsonObject of Link
     * @return String
     */
    public open fun getLinkUrl(jsonObject: JSONObject?): String? {
        if (jsonObject == null) {
            return null
        }
        return try {
            val urlObject = (if (jsonObject.has(Constants.KEY_URL)) {
                jsonObject.getJSONObject(Constants.KEY_URL)
            } else {
                null
            }) ?: return null
            val androidObject = if (urlObject.has(Constants.KEY_ANDROID)) {
                urlObject.getJSONObject(Constants.KEY_ANDROID)
            } else {
                null
            }
            if (androidObject != null) {
                if (androidObject.has(Constants.KEY_TEXT)) {
                    androidObject.getString(Constants.KEY_TEXT)
                } else {
                    ""
                }
            } else {
                ""
            }
        } catch (e: JSONException) {
            Logger.v("Unable to get Link URL with JSON - ${e.localizedMessage}")
            null
        }
    }

    /**
     * Returns the type for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String "copy" for Copy Text
     * String "url" for URLs
     */
    public open fun getLinktype(jsonObject: JSONObject?): String? {
        if (jsonObject == null) {
            return null
        }
        return try {
            if (jsonObject.has(Constants.KEY_TYPE)) {
                jsonObject.getString(Constants.KEY_TYPE)
            } else {
                ""
            }
        } catch (e: JSONException) {
            Logger.v("Unable to get Link Type with JSON - ${e.localizedMessage}")
            null
        }
    }

    public open fun isFallbackSettingsEnabled(jsonObject: JSONObject?): Boolean {
        if (jsonObject == null) {
            return false
        }
        return try {
            if (jsonObject.has(Constants.KEY_FALLBACK_NOTIFICATION_SETTINGS)) {
                jsonObject.getBoolean(Constants.KEY_FALLBACK_NOTIFICATION_SETTINGS)
            } else {
                false
            }
        } catch (e: JSONException) {
            Logger.v("Unable to get fallback settings key with JSON - ${e.localizedMessage}")
            false
        }
    }

    /**
     * Method to check whether media in the [CTInboxMessageContent] object is an audio.
     *
     * @return true if the media type is audio
     * false if the media type is not an audio
     */
    public open fun mediaIsAudio(): Boolean {
        val contentType = this.getContentType()
        return contentType != null && this._media != null && contentType.startsWith("audio")
    }

    /**
     * Method to check whether media in the [CTInboxMessageContent] object is an GIF.
     *
     * @return true if the media type is GIF
     * false if the media type is not an GIF
     */
    public open fun mediaIsGIF(): Boolean {
        val contentType = this.getContentType()
        return contentType != null && this._media != null && contentType == "image/gif"
    }

    /**
     * Method to check whether media in the [CTInboxMessageContent] object is an image.
     *
     * @return true if the media type is image
     * false if the media type is not an image
     */
    public open fun mediaIsImage(): Boolean {
        val contentType = this.getContentType()
        return contentType != null && this._media != null && contentType.startsWith("image") &&
                contentType != "image/gif"
    }

    /**
     * Method to check whether media in the [CTInboxMessageContent] object is a video.
     *
     * @return true if the media type is video
     * false if the media type is not a video
     */
    public open fun mediaIsVideo(): Boolean {
        val contentType = this.getContentType()
        return contentType != null && this._media != null && contentType.startsWith("video")
    }

    public open fun mediaIsStreamable(): Boolean = mediaIsAudio() || mediaIsVideo()

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(_title)
        dest.writeString(_titleColor)
        dest.writeString(_message)
        dest.writeString(_messageColor)
        dest.writeString(_media)
        dest.writeString(_mediaContentDescription)
        // Unboxes like the Java code did: throws (NullPointerException) if the payload had no "action" object.
        val hasUrl = hasUrl ?: throw NullPointerException("CTInboxMessageContent without an action object")
        val hasLinks = hasLinks ?: throw NullPointerException("CTInboxMessageContent without an action object")
        dest.writeByte((if (hasUrl) { 0x01 } else { 0x00 }).toByte())
        dest.writeByte((if (hasLinks) { 0x01 } else { 0x00 }).toByte())
        dest.writeString(_actionUrl)
        dest.writeString(_icon)
        dest.writeString(_iconContentDescription)
        val links = _links
        if (links == null) {
            dest.writeByte(0x00.toByte())
        } else {
            dest.writeByte(0x01.toByte())
            dest.writeString(links.toString())
        }
        dest.writeString(_contentType)
        dest.writeString(_posterUrl)
    }

    internal fun initWithJSON(contentObject: JSONObject): CTInboxMessageContent {
        try {
            val titleObject = if (contentObject.has(Constants.KEY_TITLE)) {
                contentObject.getJSONObject(Constants.KEY_TITLE)
            } else {
                null
            }
            if (titleObject != null) {
                this._title = if (titleObject.has(Constants.KEY_TEXT)) {
                    titleObject.getString(Constants.KEY_TEXT)
                } else {
                    ""
                }
                this._titleColor = if (titleObject.has(Constants.KEY_COLOR)) {
                    titleObject.getString(Constants.KEY_COLOR)
                } else {
                    ""
                }
            }
            val msgObject = if (contentObject.has(Constants.KEY_MESSAGE)) {
                contentObject.getJSONObject(Constants.KEY_MESSAGE)
            } else {
                null
            }
            if (msgObject != null) {
                this._message = if (msgObject.has(Constants.KEY_TEXT)) {
                    msgObject.getString(Constants.KEY_TEXT)
                } else {
                    ""
                }
                this._messageColor = if (msgObject.has(Constants.KEY_COLOR)) {
                    msgObject.getString(Constants.KEY_COLOR)
                } else {
                    ""
                }
            }
            val iconObject = if (contentObject.has(Constants.KEY_ICON)) {
                contentObject.getJSONObject(Constants.KEY_ICON)
            } else {
                null
            }
            if (iconObject != null) {
                this._icon = if (iconObject.has(Constants.KEY_URL)) {
                    iconObject.getString(Constants.KEY_URL)
                } else {
                    ""
                }
                this._iconContentDescription = iconObject.optString(Constants.KEY_ALT_TEXT, "")
            }
            val mediaObject = if (contentObject.has(Constants.KEY_MEDIA)) {
                contentObject.getJSONObject(Constants.KEY_MEDIA)
            } else {
                null
            }
            if (mediaObject != null) {
                this._media = if (mediaObject.has(Constants.KEY_URL)) {
                    mediaObject.getString(Constants.KEY_URL)
                } else {
                    ""
                }
                this._mediaContentDescription = mediaObject.optString(Constants.KEY_ALT_TEXT, "")
                this._contentType = if (mediaObject.has(Constants.KEY_CONTENT_TYPE)) {
                    mediaObject.getString(Constants.KEY_CONTENT_TYPE)
                } else {
                    ""
                }
                this._posterUrl = if (mediaObject.has(Constants.KEY_POSTER_URL)) {
                    mediaObject.getString(Constants.KEY_POSTER_URL)
                } else {
                    ""
                }
            }

            val actionObject = if (contentObject.has(Constants.KEY_ACTION)) {
                contentObject.getJSONObject(Constants.KEY_ACTION)
            } else {
                null
            }
            if (actionObject != null) {
                val hasUrl = actionObject.has(Constants.KEY_HAS_URL) && actionObject
                    .getBoolean(Constants.KEY_HAS_URL)
                this.hasUrl = hasUrl
                val hasLinks = actionObject.has(Constants.KEY_HAS_LINKS) && actionObject
                    .getBoolean(Constants.KEY_HAS_LINKS)
                this.hasLinks = hasLinks
                val urlObject = if (actionObject.has(Constants.KEY_URL)) {
                    actionObject.getJSONObject(Constants.KEY_URL)
                } else {
                    null
                }
                if (urlObject != null && hasUrl) {
                    val androidObject = if (urlObject.has(Constants.KEY_ANDROID)) {
                        urlObject.getJSONObject(Constants.KEY_ANDROID)
                    } else {
                        null
                    }
                    if (androidObject != null) {
                        this._actionUrl = if (androidObject.has(Constants.KEY_TEXT)) {
                            androidObject.getString(Constants.KEY_TEXT)
                        } else {
                            ""
                        }
                    }
                }
                if (urlObject != null && hasLinks) {
                    this._links = if (actionObject.has(Constants.KEY_LINKS)) {
                        actionObject.getJSONArray(Constants.KEY_LINKS)
                    } else {
                        null
                    }
                }
            }

        } catch (e: JSONException) {
            Logger.v("Unable to init CTInboxMessageContent with JSON - ${e.localizedMessage}")
        }
        return this
    }

    public companion object {

        @JvmField
        public val CREATOR: Parcelable.Creator<CTInboxMessageContent> =
            object : Parcelable.Creator<CTInboxMessageContent> {
            override fun createFromParcel(`in`: Parcel): CTInboxMessageContent = CTInboxMessageContent(`in`)

            override fun newArray(size: Int): Array<CTInboxMessageContent?> = arrayOfNulls(size)
        }
    }
}
