package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections

/**
 * Content class for holding Display Unit Content Data
 */
open class CleverTapDisplayUnitContent : Parcelable {

    private var actionUrlValue: String?

    private var contentTypeValue: String?

    private var errorValue: String?

    private var iconValue: String?

    private var mediaValue: String?

    /**
     * Per-item `wzrk_*` attribution from BE metadata and SDK-derived
     * Android action/data. `null` if no metadata was provided.
     */
    private var metaDataValue: HashMap<String, Any?>?

    private var messageValue: String?

    private var messageColorValue: String?

    private var posterUrlValue: String?

    private var titleValue: String?

    private var titleColorValue: String?

    private constructor(
        title: String?, titleColor: String?, message: String?, messageColor: String?,
        icon: String?, media: String?, contentType: String?, posterUrl: String?,
        actionUrl: String?, metaData: HashMap<String, Any?>?, error: String?
    ) {
        this.titleValue = title
        this.titleColorValue = titleColor
        this.messageValue = message
        this.messageColorValue = messageColor
        this.iconValue = icon
        this.mediaValue = media
        this.contentTypeValue = contentType
        this.posterUrlValue = posterUrl
        this.actionUrlValue = actionUrl
        this.metaDataValue = metaData
        this.errorValue = error
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    private constructor(`in`: Parcel) {
        titleValue = `in`.readString()
        titleColorValue = `in`.readString()
        messageValue = `in`.readString()
        messageColorValue = `in`.readString()
        iconValue = `in`.readString()
        mediaValue = `in`.readString()
        contentTypeValue = `in`.readString()
        posterUrlValue = `in`.readString()
        actionUrlValue = `in`.readString()
        metaDataValue = if (`in`.readByte().toInt() == 0x00) null else `in`.readHashMap(null) as HashMap<String, Any?>?
        errorValue = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Getter for the action URL of the body of the Display Unit Content
     *
     * @return String
     */
    open fun getActionUrl(): String? {
        return actionUrlValue
    }

    /** Kotlin property syntax, equivalent to [getActionUrl]. */
    val actionUrl: String?
        @JvmSynthetic @JvmName("ktGetActionUrl") inline get() = getActionUrl()

    /**
     * Getter for the content type of the media(image/gif/audio/video etc.)
     *
     * Refer [mediaIsImage], [mediaIsGIF], [mediaIsAudio], [mediaIsVideo]
     *
     * @return String
     */
    open fun getContentType(): String? {
        return contentTypeValue
    }

    internal fun setContentType(contentType: String?) {
        this.contentTypeValue = contentType
    }

    /** Kotlin property syntax, equivalent to [getContentType]. */
    var contentType: String?
        @JvmSynthetic @JvmName("ktGetContentType") inline get() = getContentType()
        @JvmSynthetic @JvmName("ktSetContentType") internal inline set(value) = setContentType(value)

    open fun getError(): String? {
        return errorValue
    }

    /** Kotlin property syntax, equivalent to [getError]. */
    val error: String?
        @JvmSynthetic @JvmName("ktGetError") inline get() = getError()

    /**
     * Getter for the URL as String for the icon in case of Icon Message template
     *
     * @return String
     */
    open fun getIcon(): String? {
        return iconValue
    }

    /** Kotlin property syntax, equivalent to [getIcon]. */
    val icon: String?
        @JvmSynthetic @JvmName("ktGetIcon") inline get() = getIcon()

    /**
     * Getter for the media URL of the Display Unit Content
     *
     * @return String
     */
    open fun getMedia(): String? {
        return mediaValue
    }

    /** Kotlin property syntax, equivalent to [getMedia]. */
    val media: String?
        @JvmSynthetic @JvmName("ktGetMedia") inline get() = getMedia()

    /**
     * Getter for this item's `wzrk_*` attribution, or `null` if unavailable
     *
     * @return attribution map, or `null`
     */
    open fun getMetaData(): HashMap<String, Any?>? {
        // Defensive copy, same reason as CleverTapDisplayUnit#getMetaDataForContent(int).
        return if (metaDataValue != null) HashMap(metaDataValue!!) else null
    }

    /** Kotlin property syntax, equivalent to [getMetaData]. */
    val metaData: HashMap<String, Any?>?
        @JvmSynthetic @JvmName("ktGetMetaData") inline get() = getMetaData()

    /**
     * Getter for the message section of the Display Unit Content
     *
     * @return String
     */
    open fun getMessage(): String? {
        return messageValue
    }

    /** Kotlin property syntax, equivalent to [getMessage]. */
    val message: String?
        @JvmSynthetic @JvmName("ktGetMessage") inline get() = getMessage()

    /**
     * Getter for the hex-code value of the message color e.g. #000000
     *
     * @return String
     */
    open fun getMessageColor(): String? {
        return messageColorValue
    }

    /** Kotlin property syntax, equivalent to [getMessageColor]. */
    val messageColor: String?
        @JvmSynthetic @JvmName("ktGetMessageColor") inline get() = getMessageColor()

    /**
     * Getter for the URL for the thumbnail of the video
     *
     * @return String
     */
    open fun getPosterUrl(): String? {
        return posterUrlValue
    }

    /** Kotlin property syntax, equivalent to [getPosterUrl]. */
    val posterUrl: String?
        @JvmSynthetic @JvmName("ktGetPosterUrl") inline get() = getPosterUrl()

    /**
     * Getter for the title section of the Display Unit Content
     *
     * @return String
     */
    open fun getTitle(): String? {
        return titleValue
    }

    /** Kotlin property syntax, equivalent to [getTitle]. */
    val title: String?
        @JvmSynthetic @JvmName("ktGetTitle") inline get() = getTitle()

    /**
     * Getter for the hex-code value of the title color e.g. #000000
     *
     * @return String
     */
    open fun getTitleColor(): String? {
        return titleColorValue
    }

    /** Kotlin property syntax, equivalent to [getTitleColor]. */
    val titleColor: String?
        @JvmSynthetic @JvmName("ktGetTitleColor") inline get() = getTitleColor()

    /**
     * Method to check whether media in the [CleverTapDisplayUnitContent] object is an audio.
     *
     * @return boolean - | true, if the media type is audio
     * | false, if the media type is not an audio
     */
    open fun mediaIsAudio(): Boolean {
        val contentType = contentTypeValue
        return contentType != null && this.mediaValue != null && contentType.startsWith("audio")
    }

    /**
     * Method to check whether media in the [CleverTapDisplayUnitContent] object is a GIF.
     *
     * @return boolean - | true, if the media type is GIF
     * | false, if the media type is not a GIF
     */
    open fun mediaIsGIF(): Boolean {
        val contentType = contentTypeValue
        return contentType != null && this.mediaValue != null && contentType == "image/gif"
    }

    /**
     * Method to check whether media in the [CleverTapDisplayUnitContent] object is an image.
     *
     * @return boolean - | true, if the media type is image
     * | false, if the media type is not an image
     */
    open fun mediaIsImage(): Boolean {
        val contentType = contentTypeValue
        return contentType != null && this.mediaValue != null && contentType.startsWith("image") &&
                contentType != "image/gif"
    }

    /**
     * Method to check whether media in the [CleverTapDisplayUnitContent] object is a video.
     *
     * @return boolean - | true, if the media type is video
     * | false, if the media type is not a video
     */
    open fun mediaIsVideo(): Boolean {
        val contentType = contentTypeValue
        return contentType != null && this.mediaValue != null && contentType.startsWith("video")
    }

    override fun toString(): String {
        return ("[" + " title:" + titleValue + ", titleColor:" + titleColorValue + " message:" + messageValue +
                ", messageColor:" + messageColorValue + ", media:" + mediaValue + ", contentType:" +
                contentTypeValue + ", posterUrl:" + posterUrlValue + ", actionUrl:" + actionUrlValue +
                ", icon:" + iconValue + ", metaData:" + metaDataValue + ", error:" + errorValue + " ]")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(titleValue)
        dest.writeString(titleColorValue)
        dest.writeString(messageValue)
        dest.writeString(messageColorValue)
        dest.writeString(iconValue)
        dest.writeString(mediaValue)
        dest.writeString(contentTypeValue)
        dest.writeString(posterUrlValue)
        dest.writeString(actionUrlValue)
        if (metaDataValue == null) {
            dest.writeByte(0x00.toByte())
        } else {
            dest.writeByte(0x01.toByte())
            dest.writeMap(metaDataValue)
        }
        dest.writeString(errorValue)
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<CleverTapDisplayUnitContent> =
            object : Parcelable.Creator<CleverTapDisplayUnitContent> {
                override fun createFromParcel(`in`: Parcel): CleverTapDisplayUnitContent {
                    return CleverTapDisplayUnitContent(`in`)
                }

                override fun newArray(size: Int): Array<CleverTapDisplayUnitContent?> {
                    return arrayOfNulls(size)
                }
            }

        /**
         * Converts jsonContent to DisplayUnitContent
         *
         * @param contentObject - jsonObject
         * @return - CleverTapDisplayUnitContent - can have empty fields with an error message in case of parsing error
         */
        internal fun toContent(contentObject: JSONObject?): CleverTapDisplayUnitContent {
            try {
                contentObject!!
                var title = ""
                var titleColor = ""
                var message = ""
                var messageColor = ""
                var icon = ""
                var media = ""
                var contentType = ""
                var posterUrl = ""
                var actionUrl = ""

                val titleObject = if (contentObject.has(Constants.KEY_TITLE)) contentObject
                    .getJSONObject(Constants.KEY_TITLE) else null
                if (titleObject != null) {
                    title = if (titleObject.has(Constants.KEY_TEXT)) titleObject.getString(Constants.KEY_TEXT) else ""
                    titleColor =
                        if (titleObject.has(Constants.KEY_COLOR)) titleObject.getString(Constants.KEY_COLOR) else ""
                }
                val msgObject = if (contentObject.has(Constants.KEY_MESSAGE)) contentObject
                    .getJSONObject(Constants.KEY_MESSAGE) else null
                if (msgObject != null) {
                    message = if (msgObject.has(Constants.KEY_TEXT)) msgObject.getString(Constants.KEY_TEXT) else ""
                    messageColor =
                        if (msgObject.has(Constants.KEY_COLOR)) msgObject.getString(Constants.KEY_COLOR) else ""
                }
                val iconObject = if (contentObject.has(Constants.KEY_ICON)) contentObject
                    .getJSONObject(Constants.KEY_ICON) else null
                if (iconObject != null) {
                    icon = if (iconObject.has(Constants.KEY_URL)) iconObject.getString(Constants.KEY_URL) else ""
                }
                val mediaObject = if (contentObject.has(Constants.KEY_MEDIA)) contentObject
                    .getJSONObject(Constants.KEY_MEDIA) else null
                if (mediaObject != null) {
                    media = if (mediaObject.has(Constants.KEY_URL)) mediaObject.getString(Constants.KEY_URL) else ""
                    contentType = if (mediaObject.has(Constants.KEY_CONTENT_TYPE)) mediaObject
                        .getString(Constants.KEY_CONTENT_TYPE) else ""
                    posterUrl = if (mediaObject.has(Constants.KEY_POSTER_URL)) mediaObject
                        .getString(Constants.KEY_POSTER_URL) else ""
                }

                val actionObject = if (contentObject.has(Constants.KEY_ACTION)) contentObject
                    .getJSONObject(Constants.KEY_ACTION) else null
                if (actionObject != null) {
                    val urlObject = if (actionObject.has(Constants.KEY_URL)) actionObject
                        .getJSONObject(Constants.KEY_URL) else null
                    if (urlObject != null) {
                        val androidObject = if (urlObject.has(Constants.KEY_ANDROID)) urlObject
                            .getJSONObject(Constants.KEY_ANDROID) else null
                        if (androidObject != null) {
                            // opt + instanceof, not getString: getString coerces a number to
                            // its digits and a JSON null to the literal "null", either of
                            // which would then be reported as the tapped url.
                            val textValue = androidObject.opt(Constants.KEY_TEXT)
                            actionUrl = if (textValue is String) textValue else ""
                        }
                    }
                }

                val metaData = parseMetaData(contentObject, actionUrl)

                return CleverTapDisplayUnitContent(
                    title, titleColor, message, messageColor,
                    icon, media, contentType, posterUrl,
                    actionUrl, metaData, null
                )
            } catch (e: Exception) {
                Logger.d(
                    Constants.FEATURE_DISPLAY_UNIT,
                    "Unable to init CleverTapDisplayUnitContent with JSON - " + e.localizedMessage
                )
                return CleverTapDisplayUnitContent(
                    "", "", "", "", "", "", "", "", "", null,
                    "Error Creating DisplayUnit Content from JSON : " + e.localizedMessage
                )
            }
        }

        /**
         * Builds per-item `wzrk_*` attribution by merging BE metadata with
         * SDK-derived Android action/data. The url pair does not depend on BE metadata,
         * so an item with a url but no (or empty, or non-object) metadata still carries
         * it, matching iOS. Returns `null` only when there is nothing to attribute.
         *
         * @param contentObject raw `content[]` item
         * @param actionUrl resolved Android action URL
         * @return attribution map, or `null` if it would be empty
         */
        private fun parseMetaData(contentObject: JSONObject, actionUrl: String?): HashMap<String, Any?>? {
            val metaDataObject = if (contentObject.has(Constants.KEY_METADATA))
                contentObject.optJSONObject(Constants.KEY_METADATA) else null
            val metaData = HashMap<String, Any?>()
            val keys: Iterator<String> = metaDataObject?.keys() ?: Collections.emptyIterator()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = metaDataObject!!.opt(key)
                // Nested objects, arrays and JSON nulls (opt returns the JSONObject.NULL
                // sentinel, not null) cannot be written to a Parcel, and the unit is
                // Parcelable, so keeping one would crash the host app on the next parcel.
                if (value == null || value === JSONObject.NULL || value is JSONObject || value is JSONArray) {
                    Logger.d(
                        Constants.FEATURE_DISPLAY_UNIT,
                        "Skipping non-primitive metadata value for key: $key"
                    )
                    continue
                }
                metaData[key] = value
            }
            // SDK-derived pair last, so it wins over anything the BE sent: only the SDK
            // knows which platform's url actually ran. With no url there is nothing to
            // derive and the BE's pair, if any, stays.
            // trim { it <= ' ' } matches java.lang.String#trim exactly.
            val trimmedUrl = actionUrl?.trim { it <= ' ' } ?: ""
            if (trimmedUrl.isNotEmpty()) {
                metaData[Constants.KEY_WZRK_ACTION] = Constants.KEY_URL
                metaData[Constants.KEY_WZRK_DATA] = trimmedUrl
            }
            return if (metaData.isEmpty()) null else metaData
        }
    }
}
