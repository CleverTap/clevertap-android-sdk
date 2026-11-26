package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONObject

/**
 * Content class for holding Display Unit Content Data
 */
public open class CleverTapDisplayUnitContent private constructor(
    private val titleValue: String?,
    private val titleColorValue: String?,
    private val messageValue: String?,
    private val messageColorValue: String?,
    private val iconValue: String?,
    private val mediaValue: String?,
    private var contentTypeValue: String?,
    private val posterUrlValue: String?,
    private val actionUrlValue: String?,
    /**
     * Per-item `wzrk_*` attribution from BE metadata and SDK-derived
     * Android action/data. `null` if no metadata was provided.
     */
    private val metaDataValue: HashMap<String, Any?>?,
    private val errorValue: String?
) : Parcelable {

    // Arguments are evaluated left to right, i.e. in the order writeToParcel wrote them. Kept as a
    // (private) secondary constructor: its synthetic JVM bridge is part of the API dump.
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    private constructor(parcel: Parcel) : this(
        titleValue = parcel.readString(),
        titleColorValue = parcel.readString(),
        messageValue = parcel.readString(),
        messageColorValue = parcel.readString(),
        iconValue = parcel.readString(),
        mediaValue = parcel.readString(),
        contentTypeValue = parcel.readString(),
        posterUrlValue = parcel.readString(),
        actionUrlValue = parcel.readString(),
        metaDataValue = if (parcel.readByte().toInt() == 0x00) {
            null
        } else {
            parcel.readHashMap(null) as HashMap<String, Any?>?
        },
        errorValue = parcel.readString()
    )

    override fun describeContents(): Int = 0

    /**
     * Getter for the action URL of the body of the Display Unit Content
     *
     * @return String
     */
    public open fun getActionUrl(): String? = actionUrlValue

    /** Kotlin property syntax, equivalent to [getActionUrl]. */
    public val actionUrl: String?
        @JvmSynthetic @JvmName("ktGetActionUrl") inline get() = getActionUrl()

    /**
     * Getter for the content type of the media(image/gif/audio/video etc.)
     *
     * Refer [mediaIsImage], [mediaIsGIF], [mediaIsAudio], [mediaIsVideo]
     *
     * @return String
     */
    public open fun getContentType(): String? = contentTypeValue

    internal fun setContentType(contentType: String?) {
        contentTypeValue = contentType
    }

    /** Kotlin property syntax, equivalent to [getContentType]. */
    public var contentType: String?
        @JvmSynthetic @JvmName("ktGetContentType") inline get() = getContentType()
        @JvmSynthetic @JvmName("ktSetContentType") internal inline set(value) = setContentType(value)

    /**
     * Returns the error recorded when this content could not be parsed from its JSON.
     *
     * @return the error message, or `null` if it was built successfully
     */
    public open fun getError(): String? = errorValue

    /** Kotlin property syntax, equivalent to [getError]. */
    public val error: String?
        @JvmSynthetic @JvmName("ktGetError") inline get() = getError()

    /**
     * Getter for the URL as String for the icon in case of Icon Message template
     *
     * @return String
     */
    public open fun getIcon(): String? = iconValue

    /** Kotlin property syntax, equivalent to [getIcon]. */
    public val icon: String?
        @JvmSynthetic @JvmName("ktGetIcon") inline get() = getIcon()

    /**
     * Getter for the media URL of the Display Unit Content
     *
     * @return String
     */
    public open fun getMedia(): String? = mediaValue

    /** Kotlin property syntax, equivalent to [getMedia]. */
    public val media: String?
        @JvmSynthetic @JvmName("ktGetMedia") inline get() = getMedia()

    /**
     * Getter for this item's `wzrk_*` attribution, or `null` if unavailable
     *
     * @return attribution map, or `null`
     */
    // Defensive copy, same reason as CleverTapDisplayUnit#getMetaDataForContent(int).
    public open fun getMetaData(): HashMap<String, Any?>? = metaDataValue?.let { HashMap(it) }

    /** Kotlin property syntax, equivalent to [getMetaData]. */
    public val metaData: HashMap<String, Any?>?
        @JvmSynthetic @JvmName("ktGetMetaData") inline get() = getMetaData()

    /**
     * Getter for the message section of the Display Unit Content
     *
     * @return String
     */
    public open fun getMessage(): String? = messageValue

    /** Kotlin property syntax, equivalent to [getMessage]. */
    public val message: String?
        @JvmSynthetic @JvmName("ktGetMessage") inline get() = getMessage()

    /**
     * Getter for the hex-code value of the message color e.g. #000000
     *
     * @return String
     */
    public open fun getMessageColor(): String? = messageColorValue

    /** Kotlin property syntax, equivalent to [getMessageColor]. */
    public val messageColor: String?
        @JvmSynthetic @JvmName("ktGetMessageColor") inline get() = getMessageColor()

    /**
     * Getter for the URL for the thumbnail of the video
     *
     * @return String
     */
    public open fun getPosterUrl(): String? = posterUrlValue

    /** Kotlin property syntax, equivalent to [getPosterUrl]. */
    public val posterUrl: String?
        @JvmSynthetic @JvmName("ktGetPosterUrl") inline get() = getPosterUrl()

    /**
     * Getter for the title section of the Display Unit Content
     *
     * @return String
     */
    public open fun getTitle(): String? = titleValue

    /** Kotlin property syntax, equivalent to [getTitle]. */
    public val title: String?
        @JvmSynthetic @JvmName("ktGetTitle") inline get() = getTitle()

    /**
     * Getter for the hex-code value of the title color e.g. #000000
     *
     * @return String
     */
    public open fun getTitleColor(): String? = titleColorValue

    /** Kotlin property syntax, equivalent to [getTitleColor]. */
    public val titleColor: String?
        @JvmSynthetic @JvmName("ktGetTitleColor") inline get() = getTitleColor()

    /**
     * Method to check whether media in the [CleverTapDisplayUnitContent] object is an audio.
     *
     * @return boolean - | true, if the media type is audio
     * | false, if the media type is not an audio
     */
    public open fun mediaIsAudio(): Boolean = mediaContentType?.startsWith("audio") == true

    /**
     * Method to check whether media in the [CleverTapDisplayUnitContent] object is a GIF.
     *
     * @return boolean - | true, if the media type is GIF
     * | false, if the media type is not a GIF
     */
    public open fun mediaIsGIF(): Boolean = mediaContentType == "image/gif"

    /**
     * Method to check whether media in the [CleverTapDisplayUnitContent] object is an image.
     *
     * @return boolean - | true, if the media type is image
     * | false, if the media type is not an image
     */
    public open fun mediaIsImage(): Boolean {
        val contentType = mediaContentType
        return contentType != null && contentType.startsWith("image") && contentType != "image/gif"
    }

    /**
     * Method to check whether media in the [CleverTapDisplayUnitContent] object is a video.
     *
     * @return boolean - | true, if the media type is video
     * | false, if the media type is not a video
     */
    public open fun mediaIsVideo(): Boolean = mediaContentType?.startsWith("video") == true

    /** The content type, or null when there is no media. */
    private val mediaContentType: String?
        get() = mediaValue?.let { contentTypeValue }

    override fun toString(): String =
        "[ title:$titleValue, titleColor:$titleColorValue message:$messageValue, messageColor:$messageColorValue, " +
            "media:$mediaValue, contentType:$contentTypeValue, posterUrl:$posterUrlValue, actionUrl:$actionUrlValue, " +
            "icon:$iconValue, metaData:$metaDataValue, error:$errorValue ]"

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

    public companion object {

        /** [Parcelable.Creator] that restores a [CleverTapDisplayUnitContent] from a [Parcel]. */
        @JvmField
        public val CREATOR: Parcelable.Creator<CleverTapDisplayUnitContent> =
            object : Parcelable.Creator<CleverTapDisplayUnitContent> {
                override fun createFromParcel(parcel: Parcel): CleverTapDisplayUnitContent =
                    CleverTapDisplayUnitContent(parcel)

                override fun newArray(size: Int): Array<CleverTapDisplayUnitContent?> = arrayOfNulls(size)
            }

        /**
         * Converts jsonContent to DisplayUnitContent
         *
         * @param contentObject - jsonObject
         * @return - CleverTapDisplayUnitContent - can have empty fields with an error message in case of parsing error
         */
        internal fun toContent(contentObject: JSONObject?): CleverTapDisplayUnitContent {
            if (contentObject == null) {
                // what the Java original reported for its NPE (null message)
                return errorContent(null)
            }
            return try {
                val titleObject = contentObject.objectOrNull(Constants.KEY_TITLE)
                val msgObject = contentObject.objectOrNull(Constants.KEY_MESSAGE)
                val iconObject = contentObject.objectOrNull(Constants.KEY_ICON)
                val mediaObject = contentObject.objectOrNull(Constants.KEY_MEDIA)
                val androidObject = contentObject.objectOrNull(Constants.KEY_ACTION)
                    ?.objectOrNull(Constants.KEY_URL)
                    ?.objectOrNull(Constants.KEY_ANDROID)
                // opt + instanceof, not getString: getString coerces a number to
                // its digits and a JSON null to the literal "null", either of
                // which would then be reported as the tapped url.
                val actionUrl = (androidObject?.opt(Constants.KEY_TEXT) as? String).orEmpty()

                CleverTapDisplayUnitContent(
                    titleValue = titleObject.stringOrEmpty(Constants.KEY_TEXT),
                    titleColorValue = titleObject.stringOrEmpty(Constants.KEY_COLOR),
                    messageValue = msgObject.stringOrEmpty(Constants.KEY_TEXT),
                    messageColorValue = msgObject.stringOrEmpty(Constants.KEY_COLOR),
                    iconValue = iconObject.stringOrEmpty(Constants.KEY_URL),
                    mediaValue = mediaObject.stringOrEmpty(Constants.KEY_URL),
                    contentTypeValue = mediaObject.stringOrEmpty(Constants.KEY_CONTENT_TYPE),
                    posterUrlValue = mediaObject.stringOrEmpty(Constants.KEY_POSTER_URL),
                    actionUrlValue = actionUrl,
                    metaDataValue = parseMetaData(contentObject, actionUrl),
                    errorValue = null
                )
            } catch (e: Exception) {
                errorContent(e.localizedMessage)
            }
        }

        private fun errorContent(reason: String?): CleverTapDisplayUnitContent {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Unable to init CleverTapDisplayUnitContent with JSON - $reason")
            return CleverTapDisplayUnitContent(
                "", "", "", "", "", "", "", "", "", null,
                "Error Creating DisplayUnit Content from JSON : $reason"
            )
        }

        /** The nested object at [key] (throws if present but not an object), or null when absent. */
        private fun JSONObject.objectOrNull(key: String): JSONObject? = if (has(key)) {
            getJSONObject(key)
        } else {
            null
        }

        /** getString(key) when present, "" when absent or when this object is null. */
        private fun JSONObject?.stringOrEmpty(key: String): String =
            if (this != null && has(key)) {
                getString(key)
            } else {
                ""
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
            val metaDataObject = if (contentObject.has(Constants.KEY_METADATA)) {
                contentObject.optJSONObject(Constants.KEY_METADATA)
            } else {
                null
            }
            val metaData = HashMap<String, Any?>()
            for (key in metaDataObject?.keys() ?: emptyList<String>().iterator()) {
                // keys only come from a non-null metaDataObject
                val value = metaDataObject?.opt(key)
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
            val trimmedUrl = actionUrl?.trim { it <= ' ' }.orEmpty()
            if (trimmedUrl.isNotEmpty()) {
                metaData[Constants.KEY_WZRK_ACTION] = Constants.KEY_URL
                metaData[Constants.KEY_WZRK_DATA] = trimmedUrl
            }
            return metaData.ifEmpty { null }
        }
    }
}
