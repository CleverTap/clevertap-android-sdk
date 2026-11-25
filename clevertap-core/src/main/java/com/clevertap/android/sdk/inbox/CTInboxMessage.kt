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
open class CTInboxMessage : Parcelable {

    private var _actionUrl: String? = null

    open fun getActionUrl(): String? = _actionUrl

    /** Kotlin property syntax, equivalent to [getActionUrl]. */
    val actionUrl: String?
        @JvmSynthetic @JvmName("ktGetActionUrl") inline get() = getActionUrl()

    private var _bgColor: String? = null

    open fun getBgColor(): String? = _bgColor

    /** Kotlin property syntax, equivalent to [getBgColor]. */
    val bgColor: String?
        @JvmSynthetic @JvmName("ktGetBgColor") inline get() = getBgColor()

    private var _body: String? = null

    open fun getBody(): String? = _body

    /** Kotlin property syntax, equivalent to [getBody]. */
    val body: String?
        @JvmSynthetic @JvmName("ktGetBody") inline get() = getBody()

    private var _campaignId: String? = null

    open fun getCampaignId(): String? = _campaignId

    /** Kotlin property syntax, equivalent to [getCampaignId]. */
    val campaignId: String?
        @JvmSynthetic @JvmName("ktGetCampaignId") inline get() = getCampaignId()

    private var _customData: JSONObject = JSONObject()

    /**
     * Custom key-value data set on the CleverTap dashboard.
     *
     * Never `null` for a message built from JSON; only a malformed [Parcel] could leave it unset.
     */
    open fun getCustomData(): JSONObject = _customData

    /** Kotlin property syntax, equivalent to [getCustomData]. */
    val customData: JSONObject
        @JvmSynthetic @JvmName("ktGetCustomData") inline get() = getCustomData()

    private var _data: JSONObject = JSONObject()

    open fun getData(): JSONObject = _data

    /** Kotlin property syntax, equivalent to [getData]. */
    val data: JSONObject
        @JvmSynthetic @JvmName("ktGetData") inline get() = getData()

    private var _date: Long = 0

    open fun getDate(): Long = _date

    /** Kotlin property syntax, equivalent to [getDate]. */
    val date: Long
        @JvmSynthetic @JvmName("ktGetDate") inline get() = getDate()

    private var _expires: Long = 0

    open fun getExpires(): Long = _expires

    /** Kotlin property syntax, equivalent to [getExpires]. */
    val expires: Long
        @JvmSynthetic @JvmName("ktGetExpires") inline get() = getExpires()

    private var _imageUrl: String? = null

    open fun getImageUrl(): String? = _imageUrl

    /** Kotlin property syntax, equivalent to [getImageUrl]. */
    val imageUrl: String?
        @JvmSynthetic @JvmName("ktGetImageUrl") inline get() = getImageUrl()

    private var _inboxMessageContents: ArrayList<CTInboxMessageContent> = ArrayList()

    /**
     * Returns an ArrayList of the contents of [CTInboxMessage]
     * For Simple Message and Icon Message templates the size of this ArrayList is by default 1.
     * For Carousel templates, the size of the ArrayList is the number of slides in the Carousel
     *
     * @return ArrayList of [CTInboxMessageContent] objects
     */
    open fun getInboxMessageContents(): ArrayList<CTInboxMessageContent> = _inboxMessageContents

    /** Kotlin property syntax, equivalent to [getInboxMessageContents]. */
    val inboxMessageContents: ArrayList<CTInboxMessageContent>
        @JvmSynthetic @JvmName("ktGetInboxMessageContents") inline get() = getInboxMessageContents()

    private var read = false

    private var _messageId: String = ""

    /**
     * Never `null`: "0" when the payload has no id.
     */
    open fun getMessageId(): String = _messageId

    /** Kotlin property syntax, equivalent to [getMessageId]. */
    val messageId: String
        @JvmSynthetic @JvmName("ktGetMessageId") inline get() = getMessageId()

    private var _orientation: String? = null

    /**
     * Returns the orientation of the media.
     *
     * @return Returns "l" for landscape
     * Returns "p" for portrait
     */
    open fun getOrientation(): String? = _orientation

    /** Kotlin property syntax, equivalent to [getOrientation]. */
    val orientation: String?
        @JvmSynthetic @JvmName("ktGetOrientation") inline get() = getOrientation()

    private var _tags: MutableList<String> = ArrayList()

    /**
     * Returns a List of tags as set on the CleverTap dashboard
     *
     * @return List of Strings
     */
    open fun getTags(): MutableList<String> = _tags

    /** Kotlin property syntax, equivalent to [getTags]. */
    val tags: MutableList<String>
        @JvmSynthetic @JvmName("ktGetTags") inline get() = getTags()

    private var _title: String? = null

    open fun getTitle(): String? = _title

    /** Kotlin property syntax, equivalent to [getTitle]. */
    val title: String?
        @JvmSynthetic @JvmName("ktGetTitle") inline get() = getTitle()

    private var _type: CTInboxMessageType? = null

    open fun getType(): CTInboxMessageType? = _type

    /** Kotlin property syntax, equivalent to [getType]. */
    val type: CTInboxMessageType?
        @JvmSynthetic @JvmName("ktGetType") inline get() = getType()

    private var _wzrkParams: JSONObject? = null

    constructor(jsonObject: JSONObject) {
        this._data = jsonObject
        try {
            this._messageId = if (jsonObject.has(Constants.KEY_ID)) jsonObject.getString(Constants.KEY_ID) else "0"
            this._campaignId = if (jsonObject.has(Constants.NOTIFICATION_ID_TAG)) jsonObject
                .getString(Constants.NOTIFICATION_ID_TAG) else "0_0"
            this._date = if (jsonObject.has(Constants.KEY_DATE)) jsonObject.getLong(Constants.KEY_DATE)
            else System.currentTimeMillis() / 1000
            this._expires = if (jsonObject.has(Constants.KEY_WZRK_TTL)) jsonObject.getLong(Constants.KEY_WZRK_TTL)
            else System.currentTimeMillis() + 1000 * 60 * 60 * 24
            this.read = jsonObject.has(Constants.KEY_IS_READ) && jsonObject.getBoolean(Constants.KEY_IS_READ)
            val tagsArray = if (jsonObject.has(Constants.KEY_TAGS)) jsonObject.getJSONArray(Constants.KEY_TAGS)
            else null
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    this._tags.add(tagsArray.getString(i))
                }
            }
            val cellObject = if (jsonObject.has(Constants.KEY_MSG)) jsonObject.getJSONObject(Constants.KEY_MSG)
            else null
            if (cellObject != null) {
                this._type = if (cellObject.has(Constants.KEY_TYPE)) CTInboxMessageType
                    .fromString(cellObject.getString(Constants.KEY_TYPE)) else CTInboxMessageType.fromString("")
                this._bgColor = if (cellObject.has(Constants.KEY_BG)) cellObject.getString(Constants.KEY_BG) else ""
                val contentArray = if (cellObject.has(Constants.KEY_CONTENT)) cellObject
                    .getJSONArray(Constants.KEY_CONTENT) else null
                if (contentArray != null) {
                    for (i in 0 until contentArray.length()) {
                        val ctInboxMessageContent = CTInboxMessageContent()
                            .initWithJSON(contentArray.getJSONObject(i))
                        this._inboxMessageContents.add(ctInboxMessageContent)
                    }
                }

                val customKVJsonArray: JSONArray? = if (cellObject.has(Constants.KEY_CUSTOM_KV)) cellObject
                    .getJSONArray(Constants.KEY_CUSTOM_KV) else null
                if (customKVJsonArray != null) {
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

                this._orientation = if (cellObject.has(Constants.KEY_ORIENTATION)) cellObject
                    .getString(Constants.KEY_ORIENTATION) else ""
            }
            this._wzrkParams = if (jsonObject.has(Constants.KEY_WZRK_PARAMS)) jsonObject
                .getJSONObject(Constants.KEY_WZRK_PARAMS) else null
        } catch (e: JSONException) {
            Logger.v("Unable to init CTInboxMessage with JSON - " + e.localizedMessage)
        }
    }

    // The 0x00 branches below are only reachable with a Parcel that this class did not write
    // (writeToParcel always writes customData, tags and inboxMessageContents); they leave the
    // non-null defaults in place instead of storing null.
    @Suppress("DEPRECATION")
    internal constructor(`in`: Parcel) {
        try {
            _title = `in`.readString()
            _body = `in`.readString()
            _imageUrl = `in`.readString()
            _actionUrl = `in`.readString()
            _date = `in`.readLong()
            _expires = `in`.readLong()
            _messageId = `in`.readString() ?: _messageId
            if (`in`.readByte().toInt() != 0x00) {
                _data = JSONObject(`in`.readString()!!)
            }
            if (`in`.readByte().toInt() != 0x00) {
                _customData = JSONObject(`in`.readString()!!)
            }
            read = `in`.readByte().toInt() != 0x00
            _type = `in`.readValue(CTInboxMessageType::class.java.classLoader) as CTInboxMessageType?
            if (`in`.readByte().toInt() == 0x01) {
                _tags = ArrayList()
                `in`.readList(_tags, String::class.java.classLoader)
            }
            _bgColor = `in`.readString()
            if (`in`.readByte().toInt() == 0x01) {
                _inboxMessageContents = ArrayList()
                `in`.readList(_inboxMessageContents, CTInboxMessageContent::class.java.classLoader)
            }
            _orientation = `in`.readString()
            _campaignId = `in`.readString()
            _wzrkParams = if (`in`.readByte().toInt() == 0x00) null else JSONObject(`in`.readString()!!)
        } catch (e: JSONException) {
            Logger.v("Unable to parse CTInboxMessage from parcel - " + e.localizedMessage)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Returns an ArrayList of CTInboxMessageData(url, contentDescription) of the Carousel Images
     *
     * @return ArrayList of CTInboxImageData
     */
    open fun getCarouselImagesData(): ArrayList<CTInboxImageData> {
        val carouselImages = ArrayList<CTInboxImageData>()
        for (ctInboxMessageContent in getInboxMessageContents()) {
            carouselImages.add(
                // CTInboxImageData takes non-null values; a null here threw from Java as well.
                CTInboxImageData(
                    ctInboxMessageContent.getMedia()!!,
                    ctInboxMessageContent.getMediaContentDescription()!!
                )
            )
        }
        return carouselImages
    }

    /** Kotlin property syntax, equivalent to [getCarouselImagesData]. */
    val carouselImagesData: ArrayList<CTInboxImageData>
        @JvmSynthetic @JvmName("ktGetCarouselImagesData") inline get() = getCarouselImagesData()

    /**
     * Returns an ArrayList of String URLs of the Carousel Images
     *
     * @return ArrayList of Strings
     */
    open fun getCarouselImages(): ArrayList<String?> {
        val carouselImages = ArrayList<String?>()
        for (ctInboxMessageContent in getInboxMessageContents()) {
            carouselImages.add(ctInboxMessageContent.getMedia())
        }
        return carouselImages
    }

    /** Kotlin property syntax, equivalent to [getCarouselImages]. */
    val carouselImages: ArrayList<String?>
        @JvmSynthetic @JvmName("ktGetCarouselImages") inline get() = getCarouselImages()

    /**
     * Returns a JSONObject of wzrk_* parameters.
     *
     * @return JSONObject of wzrk_* parameters
     */
    open fun getWzrkParams(): JSONObject {
        return _wzrkParams ?: JSONObject()
    }

    /** Kotlin property syntax, equivalent to [getWzrkParams]. */
    val wzrkParams: JSONObject
        @JvmSynthetic @JvmName("ktGetWzrkParams") inline get() = getWzrkParams()

    open fun isRead(): Boolean {
        return read
    }

    /** Kotlin property syntax, equivalent to [isRead]. */
    val isRead: Boolean
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
        dest.writeByte((if (read) 0x01 else 0x00).toByte())
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

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<CTInboxMessage> = object : Parcelable.Creator<CTInboxMessage> {
            override fun createFromParcel(`in`: Parcel): CTInboxMessage {
                return CTInboxMessage(`in`)
            }

            override fun newArray(size: Int): Array<CTInboxMessage?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun getCREATOR(): Parcelable.Creator<CTInboxMessage> {
            return CREATOR
        }
    }
}
