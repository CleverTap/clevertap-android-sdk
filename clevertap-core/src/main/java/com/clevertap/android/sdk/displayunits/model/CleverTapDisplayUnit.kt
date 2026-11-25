package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.displayunits.CTDisplayUnitType
import org.json.JSONArray
import org.json.JSONObject

/**
 * This model class holds the data of an individual Display Unit.
 */
open class CleverTapDisplayUnit : Parcelable {

    /**
     * Background Color
     */
    private var bgColorValue: String? = null

    /**
     * List of Display Content Items
     */
    private var contentsValue: ArrayList<CleverTapDisplayUnitContent>? = null

    /**
     * Custom Key Value Pairs
     */
    private var customExtrasValue: HashMap<String, String>? = null

    private var errorValue: String? = null

    private var jsonObjectValue: JSONObject? = null

    /**
     * Display Type Could be (banner,carousel,custom key value etc.)
     */
    private var typeValue: CTDisplayUnitType? = null

    /**
     * Display unit identifier
     */
    private var unitIDValue: String? = null

    //constructors
    private constructor(
        jsonObject: JSONObject?, unitID: String?, type: CTDisplayUnitType?,
        bgColor: String?, contentArray: ArrayList<CleverTapDisplayUnitContent>?,
        kvObject: JSONObject?, error: String?
    ) {
        this.jsonObjectValue = jsonObject
        this.unitIDValue = unitID
        this.typeValue = type
        this.bgColorValue = bgColor
        this.contentsValue = contentArray
        this.customExtrasValue = getKeyValues(kvObject)
        this.errorValue = error
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    private constructor(`in`: Parcel) {
        try {
            this.unitIDValue = `in`.readString()
            this.typeValue = `in`.readValue(CTDisplayUnitType::class.java.classLoader) as CTDisplayUnitType?
            this.bgColorValue = `in`.readString()

            if (`in`.readByte().toInt() == 0x01) {
                contentsValue = ArrayList()
                `in`.readList(contentsValue!!, CleverTapDisplayUnitContent::class.java.classLoader)
            } else {
                contentsValue = null
            }

            this.customExtrasValue = `in`.readHashMap(null) as HashMap<String, String>?
            this.jsonObjectValue = if (`in`.readByte().toInt() == 0x00) null else JSONObject(`in`.readString()!!)
            this.errorValue = `in`.readString()
        } catch (e: Exception) {
            errorValue = "Error Creating Display Unit from parcel : " + e.localizedMessage
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, errorValue)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Getter for the hex-value background color of the Display Unit e.g. #000000
     *
     * @return String
     */
    open fun getBgColor(): String? {
        return bgColorValue
    }

    /** Kotlin property syntax, equivalent to [getBgColor]. */
    val bgColor: String?
        @JvmSynthetic @JvmName("ktGetBgColor") inline get() = getBgColor()

    /**
     * Getter for the list of Content Display Unit Items.
     *
     * @return ArrayList<CleverTapDisplayUnitContent>
     */
    open fun getContents(): ArrayList<CleverTapDisplayUnitContent>? {
        return contentsValue
    }

    /** Kotlin property syntax, equivalent to [getContents]. */
    val contents: ArrayList<CleverTapDisplayUnitContent>?
        @JvmSynthetic @JvmName("ktGetContents") inline get() = getContents()

    /**
     * Getter for the Key Value pairs of the Display Unit
     *
     * @return HashMap<String, String>
     */
    open fun getCustomExtras(): HashMap<String, String>? {
        return customExtrasValue
    }

    /** Kotlin property syntax, equivalent to [getCustomExtras]. */
    val customExtras: HashMap<String, String>?
        @JvmSynthetic @JvmName("ktGetCustomExtras") inline get() = getCustomExtras()

    /**
     * Returns the per-item `wzrk_*` attribution for the content item at
     * `contentIndex`, including server-supplied element ID/index, title-based
     * CTA, and Android action/data fields.
     *
     * Ready for use with display unit element click/view events. Returns an empty
     * map when the index is invalid or no item-level attribution exists, in which
     * case only unit-level `wzrk_*` attribution is recorded. Applies to all
     * Native Display types.
     *
     * @param contentIndex zero-based index into [getContents]
     * @return item-level attribution; never null
     */
    open fun getMetaDataForContent(contentIndex: Int): HashMap<String, Any?> {
        val contents = contentsValue
        if (contents == null || contentIndex < 0 || contentIndex >= contents.size) {
            Logger.d(
                Constants.FEATURE_DISPLAY_UNIT,
                "Content index $contentIndex out of range for unit $unitIDValue"
            )
            return HashMap()
        }
        // Defensive copy: prevent callers from modifying stored attribution.
        val metaData = contents[contentIndex].getMetaData()
        return if (metaData != null) HashMap(metaData) else HashMap()
    }

    open fun getError(): String? {
        return errorValue
    }

    /** Kotlin property syntax, equivalent to [getError]. */
    val error: String?
        @JvmSynthetic @JvmName("ktGetError") inline get() = getError()

    /**
     * Getter for the JsonObject corresponding to the CleverTapDisplayUnit object
     *
     * @return JSONObject
     */
    open fun getJsonObject(): JSONObject? {
        return jsonObjectValue
    }

    /** Kotlin property syntax, equivalent to [getJsonObject]. */
    val jsonObject: JSONObject?
        @JvmSynthetic @JvmName("ktGetJsonObject") inline get() = getJsonObject()

    /**
     * Getter for the DisplayUnitType of the Display Unit, Refer[CTDisplayUnitType]
     *
     * @return CTDisplayUnitType
     */
    open fun getType(): CTDisplayUnitType? {
        return typeValue
    }

    /** Kotlin property syntax, equivalent to [getType]. */
    val type: CTDisplayUnitType?
        @JvmSynthetic @JvmName("ktGetType") inline get() = getType()

    /**
     * Getter for the unitId of the Display Unit
     *
     * @return String
     */
    open fun getUnitID(): String? {
        return unitIDValue
    }

    /** Kotlin property syntax, equivalent to [getUnitID]. */
    val unitID: String?
        @JvmSynthetic @JvmName("ktGetUnitID") inline get() = getUnitID()

    /**
     * Getter for the WiZRK fields obj to be passed in the data for recording event.
     *
     * @return JSONObject
     */
    open fun getWZRKFields(): JSONObject? {
        try {
            val jsonObject = jsonObjectValue
            if (jsonObject != null) {
                val iterator = jsonObject.keys()
                val `object` = JSONObject()
                while (iterator.hasNext()) {
                    val keyName = iterator.next()
                    if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                        `object`.put(keyName, jsonObject.get(keyName))
                    }
                }
                return `object`
            }
        } catch (e: Exception) {
            //no op
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Error in getting WiZRK fields " + e.localizedMessage)
        }
        return null
    }

    /** Kotlin property syntax, equivalent to [getWZRKFields]. */
    val wzrkFields: JSONObject?
        @JvmSynthetic @JvmName("ktGetWZRKFields") inline get() = getWZRKFields()

    override fun toString(): String {
        try {
            val stringBuilder = StringBuilder()
            stringBuilder.append("[")
            stringBuilder.append(" Unit id- ").append(unitIDValue)
            stringBuilder.append(", Type- ").append(typeValue?.toString())
            stringBuilder.append(", bgColor- ").append(bgColorValue)
            val contents = contentsValue
            if (contents != null && contents.isNotEmpty()) {
                for (i in contents.indices) {
                    val item: CleverTapDisplayUnitContent? = contents[i]
                    if (item != null) {
                        stringBuilder.append(", Content Item:").append(i).append(" ").append(item.toString())
                        stringBuilder.append("\n")
                    }
                }
            }
            if (customExtrasValue != null) {
                stringBuilder.append(", Custom KV:").append(customExtrasValue)
            }
            stringBuilder.append(", JSON -").append(jsonObjectValue)
            stringBuilder.append(", Error-").append(errorValue)
            stringBuilder.append(" ]")
            return stringBuilder.toString()
        } catch (e: Exception) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Exception in toString:$e")
        }
        return super.toString()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(unitIDValue)
        parcel.writeValue(typeValue)
        parcel.writeString(bgColorValue)

        if (contentsValue == null) {
            parcel.writeByte(0x00.toByte())
        } else {
            parcel.writeByte(0x01.toByte())
            parcel.writeList(contentsValue)
        }

        parcel.writeMap(customExtrasValue)
        if (jsonObjectValue == null) {
            parcel.writeByte(0x00.toByte())
        } else {
            parcel.writeByte(0x01.toByte())
            parcel.writeString(jsonObjectValue.toString())
        }
        parcel.writeString(errorValue)
    }

    /**
     * populates the custom key values pairs from json
     *
     * @param kvObj - Custom Key Values
     */
    internal fun getKeyValues(kvObj: JSONObject?): HashMap<String, String>? {
        try {
            if (kvObj != null) {
                val keys: Iterator<String>? = kvObj.keys()
                if (keys != null) {
                    var key: String
                    var value: String
                    var hashMap: HashMap<String, String>? = null
                    while (keys.hasNext()) {
                        key = keys.next()
                        value = kvObj.getString(key)
                        if (!TextUtils.isEmpty(key)) {
                            if (hashMap == null) {
                                hashMap = HashMap()
                            }
                            hashMap[key] = value
                        }
                    }
                    return hashMap
                }
            }
        } catch (e: Exception) {
            //no op
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Error in getting Key Value Pairs " + e.localizedMessage)
        }
        return null
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<CleverTapDisplayUnit> = object : Parcelable.Creator<CleverTapDisplayUnit> {
            override fun createFromParcel(`in`: Parcel): CleverTapDisplayUnit {
                return CleverTapDisplayUnit(`in`)
            }

            override fun newArray(size: Int): Array<CleverTapDisplayUnit?> {
                return arrayOfNulls(size)
            }
        }

        /**
         * static method to convert json to Display Unit
         *
         * @param jsonObject - Display Unit Item in Json form
         * @return - CleverTapDisplayUnit - always returns non-null instance
         */
        @JvmStatic
        fun toDisplayUnit(jsonObject: JSONObject?): CleverTapDisplayUnit {
            //logic to convert json obj to item
            try {
                val unitID = if (jsonObject!!.has(Constants.NOTIFICATION_ID_TAG)) jsonObject
                    .getString(Constants.NOTIFICATION_ID_TAG) else Constants.TEST_IDENTIFIER
                val displayUnitType = if (jsonObject.has(Constants.KEY_TYPE)) CTDisplayUnitType
                    .type(jsonObject.getString(Constants.KEY_TYPE)) else null

                val bgColor = if (jsonObject.has(Constants.KEY_BG)) jsonObject.getString(Constants.KEY_BG) else ""

                val contentArray: JSONArray? = if (jsonObject.has(Constants.KEY_CONTENT)) jsonObject
                    .getJSONArray(Constants.KEY_CONTENT) else null
                val contentArrayList = ArrayList<CleverTapDisplayUnitContent>()
                if (contentArray != null) {
                    for (i in 0 until contentArray.length()) {
                        val displayUnitContent = CleverTapDisplayUnitContent
                            .toContent(contentArray.getJSONObject(i))
                        if (TextUtils.isEmpty(displayUnitContent.getError())) {
                            contentArrayList.add(displayUnitContent)
                        }
                    }
                }
                var customKV: JSONObject? = null
                //custom KV can be added to Display unit of any types, no need to add type check here
                if (jsonObject.has(Constants.KEY_CUSTOM_KV)) {
                    customKV = jsonObject.getJSONObject(Constants.KEY_CUSTOM_KV)
                }
                return CleverTapDisplayUnit(
                    jsonObject, unitID, displayUnitType, bgColor, contentArrayList, customKV,
                    null
                )
            } catch (e: Exception) {
                Logger.d(
                    Constants.FEATURE_DISPLAY_UNIT,
                    "Unable to init CleverTapDisplayUnit with JSON - " + e.localizedMessage
                )
                return CleverTapDisplayUnit(
                    null, "", null, null, null, null,
                    "Error Creating Display Unit from JSON : " + e.localizedMessage
                )
            }
        }
    }
}
