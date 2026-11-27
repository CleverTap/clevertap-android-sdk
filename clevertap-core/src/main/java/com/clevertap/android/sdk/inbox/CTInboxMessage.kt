package com.clevertap.android.sdk.inbox

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Public facing model class for type of InboxMessage
 */
public open class CTInboxMessage : Parcelable {

    private var _actionUrl: String? = null

    /**
     * Returns the message action URL.
     *
     * Legacy field: it is only restored from a [Parcel] and is never set from the inbox payload, so it
     * is `null` for messages returned by the SDK. Read the value from [getInboxMessageContents] instead.
     *
     * @return always `null` for SDK-built messages
     */
    public open fun getActionUrl(): String? = _actionUrl

    /** Kotlin property syntax, equivalent to [getActionUrl]. */
    public val actionUrl: String?
        @JvmSynthetic @JvmName("ktGetActionUrl") inline get() = getActionUrl()

    private var _bgColor: String? = null

    /**
     * Returns the background color of the message as set on the CleverTap dashboard, as a hex string
     * such as `#FFFFFF`.
     *
     * @return the background color, an empty string if the payload has none, or `null` if the payload
     * has no `msg` object
     */
    public open fun getBgColor(): String? = _bgColor

    /** Kotlin property syntax, equivalent to [getBgColor]. */
    public val bgColor: String?
        @JvmSynthetic @JvmName("ktGetBgColor") inline get() = getBgColor()

    private var _body: String? = null

    /**
     * Returns the message body.
     *
     * Legacy field: it is only restored from a [Parcel] and is never set from the inbox payload, so it
     * is `null` for messages returned by the SDK. Read the value from [getInboxMessageContents] instead.
     *
     * @return always `null` for SDK-built messages
     */
    public open fun getBody(): String? = _body

    /** Kotlin property syntax, equivalent to [getBody]. */
    public val body: String?
        @JvmSynthetic @JvmName("ktGetBody") inline get() = getBody()

    private var _campaignId: String? = null

    /**
     * Returns the id of the campaign that sent this message (the payload's `wzrk_id`).
     *
     * @return the campaign id, or `"0_0"` when the payload has none
     */
    public open fun getCampaignId(): String? = _campaignId

    /** Kotlin property syntax, equivalent to [getCampaignId]. */
    public val campaignId: String?
        @JvmSynthetic @JvmName("ktGetCampaignId") inline get() = getCampaignId()

    private var _customData: JSONObject = JSONObject()

    /**
     * Custom key-value data set on the CleverTap dashboard.
     *
     * Never `null` for a message built from JSON; only a malformed [Parcel] could leave it unset.
     */
    public open fun getCustomData(): JSONObject = _customData

    /** Kotlin property syntax, equivalent to [getCustomData]. */
    public val customData: JSONObject
        @JvmSynthetic @JvmName("ktGetCustomData") inline get() = getCustomData()

    private var _data: JSONObject = JSONObject()

    /**
     * Returns the raw JSON payload this message was built from.
     *
     * @return the message JSON; never `null`
     */
    public open fun getData(): JSONObject = _data

    /** Kotlin property syntax, equivalent to [getData]. */
    public val data: JSONObject
        @JvmSynthetic @JvmName("ktGetData") inline get() = getData()

    private var _date: Long = 0

    /**
     * Returns when the message was created, in Unix epoch seconds.
     *
     * @return the payload's `date`, or the time the message was parsed when the payload has none
     */
    public open fun getDate(): Long = _date

    /** Kotlin property syntax, equivalent to [getDate]. */
    public val date: Long
        @JvmSynthetic @JvmName("ktGetDate") inline get() = getDate()

    private var _expires: Long = 0

    /**
     * Returns when the message expires: the payload's `wzrk_ttl`, in Unix epoch seconds. When the
     * payload has no `wzrk_ttl`, the SDK uses the parse time plus 24 hours, computed in
     * milliseconds.
     *
     * @return the expiry time
     */
    public open fun getExpires(): Long = _expires

    /** Kotlin property syntax, equivalent to [getExpires]. */
    public val expires: Long
        @JvmSynthetic @JvmName("ktGetExpires") inline get() = getExpires()

    private var _imageUrl: String? = null

    /**
     * Returns the message image URL.
     *
     * Legacy field: it is only restored from a [Parcel] and is never set from the inbox payload, so it
     * is `null` for messages returned by the SDK. Read the value from [getInboxMessageContents] instead.
     *
     * @return always `null` for SDK-built messages
     */
    public open fun getImageUrl(): String? = _imageUrl

    /** Kotlin property syntax, equivalent to [getImageUrl]. */
    public val imageUrl: String?
        @JvmSynthetic @JvmName("ktGetImageUrl") inline get() = getImageUrl()

    private val _inboxMessageContents: ArrayList<CTInboxMessageContent> = ArrayList()

    /**
     * Returns an ArrayList of the contents of [CTInboxMessage]
     * For Simple Message and Icon Message templates the size of this ArrayList is by default 1.
     * For Carousel templates, the size of the ArrayList is the number of slides in the Carousel
     *
     * @return ArrayList of [CTInboxMessageContent] objects
     */
    public open fun getInboxMessageContents(): ArrayList<CTInboxMessageContent> = _inboxMessageContents

    /** Kotlin property syntax, equivalent to [getInboxMessageContents]. */
    public val inboxMessageContents: ArrayList<CTInboxMessageContent>
        @JvmSynthetic @JvmName("ktGetInboxMessageContents") inline get() = getInboxMessageContents()

    private var read = false

    private var _messageId: String = ""

    /**
     * Never `null`: "0" when the payload has no id.
     */
    public open fun getMessageId(): String = _messageId

    /** Kotlin property syntax, equivalent to [getMessageId]. */
    public val messageId: String
        @JvmSynthetic @JvmName("ktGetMessageId") inline get() = getMessageId()

    private var _orientation: String? = null

    /**
     * Returns the orientation of the media.
     *
     * @return Returns "l" for landscape
     * Returns "p" for portrait
     */
    public open fun getOrientation(): String? = _orientation

    /** Kotlin property syntax, equivalent to [getOrientation]. */
    public val orientation: String?
        @JvmSynthetic @JvmName("ktGetOrientation") inline get() = getOrientation()

    private val _tags: MutableList<String> = ArrayList()

    /**
     * Returns a List of tags as set on the CleverTap dashboard
     *
     * @return List of Strings
     */
    public open fun getTags(): MutableList<String> = _tags

    /** Kotlin property syntax, equivalent to [getTags]. */
    public val tags: MutableList<String>
        @JvmSynthetic @JvmName("ktGetTags") inline get() = getTags()

    private var _title: String? = null

    /**
     * Returns the message title.
     *
     * Legacy field: it is only restored from a [Parcel] and is never set from the inbox payload, so it
     * is `null` for messages returned by the SDK. Read the value from [getInboxMessageContents] instead.
     *
     * @return always `null` for SDK-built messages
     */
    public open fun getTitle(): String? = _title

    /** Kotlin property syntax, equivalent to [getTitle]. */
    public val title: String?
        @JvmSynthetic @JvmName("ktGetTitle") inline get() = getTitle()

    private var _type: CTInboxMessageType? = null

    /**
     * Returns the template of this message.
     *
     * @return the message type, or `null` if the payload has no `msg` object or an unknown type
     */
    public open fun getType(): CTInboxMessageType? = _type

    /** Kotlin property syntax, equivalent to [getType]. */
    public val type: CTInboxMessageType?
        @JvmSynthetic @JvmName("ktGetType") inline get() = getType()

    private var _wzrkParams: JSONObject? = null

    public constructor(jsonObject: JSONObject) {
        this._data = jsonObject
        try {
            this._messageId = if (jsonObject.has(Constants.KEY_ID)) {
                jsonObject.getString(Constants.KEY_ID)
            } else {
                "0"
            }
            this._campaignId = if (jsonObject.has(Constants.NOTIFICATION_ID_TAG)) {
                jsonObject.getString(Constants.NOTIFICATION_ID_TAG)
            } else {
                "0_0"
            }
            this._date = if (jsonObject.has(Constants.KEY_DATE)) {
                jsonObject.getLong(Constants.KEY_DATE)
            } else {
                System.currentTimeMillis() / 1000
            }
            this._expires = if (jsonObject.has(Constants.KEY_WZRK_TTL)) {
                jsonObject.getLong(Constants.KEY_WZRK_TTL)
            } else {
                System.currentTimeMillis() + Constants.ONE_DAY_IN_MILLIS
            }
            this.read = jsonObject.has(Constants.KEY_IS_READ) && jsonObject.getBoolean(Constants.KEY_IS_READ)
            if (jsonObject.has(Constants.KEY_TAGS)) {
                val tagsArray = jsonObject.getJSONArray(Constants.KEY_TAGS)
                for (i in 0 until tagsArray.length()) {
                    this._tags.add(tagsArray.getString(i))
                }
            }
            if (jsonObject.has(Constants.KEY_MSG)) {
                val cellObject = jsonObject.getJSONObject(Constants.KEY_MSG)
                this._type = if (cellObject.has(Constants.KEY_TYPE)) {
                    CTInboxMessageType.fromString(cellObject.getString(Constants.KEY_TYPE))
                } else {
                    CTInboxMessageType.fromString("")
                }
                this._bgColor = if (cellObject.has(Constants.KEY_BG)) {
                    cellObject.getString(Constants.KEY_BG)
                } else {
                    ""
                }
                if (cellObject.has(Constants.KEY_CONTENT)) {
                    val contentArray = cellObject.getJSONArray(Constants.KEY_CONTENT)
                    for (i in 0 until contentArray.length()) {
                        this._inboxMessageContents.add(
                            CTInboxMessageContent().initWithJSON(contentArray.getJSONObject(i))
                        )
                    }
                }

                if (cellObject.has(Constants.KEY_CUSTOM_KV)) {
                    val customKVJsonArray = cellObject.getJSONArray(Constants.KEY_CUSTOM_KV)
                    for (i in 0 until customKVJsonArray.length()) {
                        val customKVJSONObject = customKVJsonArray.getJSONObject(i)
                        if (customKVJSONObject.has(Constants.KEY_KEY)) {
                            val key = customKVJSONObject.getString(Constants.KEY_KEY)
                            if (customKVJSONObject.has(Constants.KEY_VALUE)) {
                                val value = customKVJSONObject.getJSONObject(Constants.KEY_VALUE)
                                _customData.put(key, value.getString(Constants.KEY_TEXT))
                            }
                        }
                    }
                }

                this._orientation = if (cellObject.has(Constants.KEY_ORIENTATION)) {
                    cellObject.getString(Constants.KEY_ORIENTATION)
                } else {
                    ""
                }
            }
            this._wzrkParams = if (jsonObject.has(Constants.KEY_WZRK_PARAMS)) {
                jsonObject.getJSONObject(Constants.KEY_WZRK_PARAMS)
            } else {
                null
            }
        } catch (e: JSONException) {
            Logger.v("Unable to init CTInboxMessage with JSON - ${e.localizedMessage}")
        }
    }

    // The 0x00 branches below are only reachable with a Parcel that this class did not write
    // (writeToParcel always writes customData, tags and inboxMessageContents); they leave the
    // non-null defaults in place instead of storing null.
    // Private, as in Java: a JVM-public (Parcel) overload would make Java `new CTInboxMessage(null)`
    // ambiguous with the (JSONObject) constructor. Only CREATOR calls it.
    @Suppress("DEPRECATION")
    private constructor(`in`: Parcel) {
        try {
            _title = `in`.readString()
            _body = `in`.readString()
            _imageUrl = `in`.readString()
            _actionUrl = `in`.readString()
            _date = `in`.readLong()
            _expires = `in`.readLong()
            _messageId = `in`.readString() ?: _messageId
            if (`in`.readByte().toInt() != 0x00) {
                _data = JSONObject(`in`.readNonNullString())
            }
            if (`in`.readByte().toInt() != 0x00) {
                _customData = JSONObject(`in`.readNonNullString())
            }
            read = `in`.readByte().toInt() != 0x00
            _type = `in`.readValue(CTInboxMessageType::class.java.classLoader) as CTInboxMessageType?
            if (`in`.readByte().toInt() == 0x01) {
                `in`.readList(_tags, String::class.java.classLoader)
            }
            _bgColor = `in`.readString()
            if (`in`.readByte().toInt() == 0x01) {
                `in`.readList(_inboxMessageContents, CTInboxMessageContent::class.java.classLoader)
            }
            _orientation = `in`.readString()
            _campaignId = `in`.readString()
            _wzrkParams = if (`in`.readByte().toInt() == 0x00) { null } else { JSONObject(`in`.readNonNullString()) }
        } catch (e: JSONException) {
            Logger.v("Unable to parse CTInboxMessage from parcel - ${e.localizedMessage}")
        }
    }

    override fun describeContents(): Int = 0

    /**
     * Returns an ArrayList of CTInboxMessageData(url, contentDescription) of the Carousel Images
     *
     * @return ArrayList of CTInboxImageData
     */
    public open fun getCarouselImagesData(): ArrayList<CTInboxImageData> =
        getInboxMessageContents().mapTo(ArrayList()) { content ->
            // CTInboxImageData takes non-null values; Java callers got this NullPointerException too.
            CTInboxImageData(
                content.getMedia() ?: throw NullPointerException("Carousel content without media"),
                content.getMediaContentDescription()
                    ?: throw NullPointerException("Carousel content without media alt text")
            )
        }

    /** Kotlin property syntax, equivalent to [getCarouselImagesData]. */
    public val carouselImagesData: ArrayList<CTInboxImageData>
        @JvmSynthetic @JvmName("ktGetCarouselImagesData") inline get() = getCarouselImagesData()

    /**
     * Returns an ArrayList of String URLs of the Carousel Images
     *
     * @return ArrayList of Strings
     */
    public open fun getCarouselImages(): ArrayList<String?> =
        getInboxMessageContents().mapTo(ArrayList()) { it.getMedia() }

    /** Kotlin property syntax, equivalent to [getCarouselImages]. */
    public val carouselImages: ArrayList<String?>
        @JvmSynthetic @JvmName("ktGetCarouselImages") inline get() = getCarouselImages()

    /**
     * Returns a JSONObject of wzrk_* parameters.
     *
     * @return JSONObject of wzrk_* parameters
     */
    public open fun getWzrkParams(): JSONObject = _wzrkParams ?: JSONObject()

    /** Kotlin property syntax, equivalent to [getWzrkParams]. */
    public val wzrkParams: JSONObject
        @JvmSynthetic @JvmName("ktGetWzrkParams") inline get() = getWzrkParams()

    /**
     * Returns whether the message has been marked as read.
     *
     * @return `true` if the message is read
     * @see com.clevertap.android.sdk.CleverTapAPI.markReadInboxMessage
     */
    public open fun isRead(): Boolean = read

    /** Kotlin property syntax, equivalent to [isRead]. */
    public val isRead: Boolean
        @JvmSynthetic @JvmName("ktIsRead") inline get() = isRead()

    internal fun setRead(read: Boolean) {
        this.read = read
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(_title)
        dest.writeString(_body)
        dest.writeString(_imageUrl)
        dest.writeString(_actionUrl)
        dest.writeLong(_date)
        dest.writeLong(_expires)
        dest.writeString(_messageId)
        dest.writeByte(0x01.toByte())
        dest.writeString(_data.toString())
        dest.writeByte(0x01.toByte())
        dest.writeString(_customData.toString())
        dest.writeByte((if (read) { 0x01 } else { 0x00 }).toByte())
        dest.writeValue(_type)
        dest.writeByte(0x01.toByte())
        dest.writeList(_tags)
        dest.writeString(_bgColor)
        dest.writeByte(0x01.toByte())
        dest.writeList(_inboxMessageContents)
        dest.writeString(_orientation)
        dest.writeString(_campaignId)
        val wzrkParams = _wzrkParams
        if (wzrkParams == null) {
            dest.writeByte(0x00.toByte())
        } else {
            dest.writeByte(0x01.toByte())
            dest.writeString(wzrkParams.toString())
        }
    }

    /** A string this class wrote behind a 0x01 flag; Java handed a null one to JSONObject, which threw. */
    private fun Parcel.readNonNullString(): String =
        readString() ?: throw NullPointerException("Malformed CTInboxMessage parcel")

    public companion object {

        /** [Parcelable.Creator] that restores a [CTInboxMessage] from a [Parcel]. */
        @JvmField
        public val CREATOR: Parcelable.Creator<CTInboxMessage> = object : Parcelable.Creator<CTInboxMessage> {
            override fun createFromParcel(`in`: Parcel): CTInboxMessage = CTInboxMessage(`in`)

            override fun newArray(size: Int): Array<CTInboxMessage?> = arrayOfNulls(size)
        }

        /**
         * Returns [CREATOR], for callers that need a getter.
         *
         * @return the [Parcelable.Creator] of [CTInboxMessage]
         */
        @JvmStatic
        public fun getCREATOR(): Parcelable.Creator<CTInboxMessage> = CREATOR
    }
}
