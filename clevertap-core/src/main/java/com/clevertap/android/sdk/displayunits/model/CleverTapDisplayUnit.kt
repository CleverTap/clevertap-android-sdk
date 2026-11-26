package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.displayunits.CTDisplayUnitType
import org.json.JSONObject

/**
 * This model class holds the data of an individual Display Unit.
 */
open class CleverTapDisplayUnit private constructor(
    private var jsonObjectValue: JSONObject?,
    /** Display unit identifier */
    private var unitIDValue: String?,
    /** Display Type Could be (banner,carousel,custom key value etc.) */
    private var typeValue: CTDisplayUnitType?,
    /** Background Color */
    private var bgColorValue: String?,
    /** List of Display Content Items */
    private var contentsValue: ArrayList<CleverTapDisplayUnitContent>?,
    kvObject: JSONObject?,
    private var errorValue: String?
) : Parcelable {

    /** Custom Key Value Pairs */
    private var customExtrasValue: HashMap<String, String>? = getKeyValues(kvObject)

    /**
     * Reads the fields in writeToParcel order. A failure keeps whatever was read so far and records
     * the error, as the Java original did.
     */
    // Kept as a (private) secondary constructor: its synthetic JVM bridge is part of the API dump.
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    private constructor(parcel: Parcel) : this(null, null, null, null, null, null, null) {
        try {
            unitIDValue = parcel.readString()
            typeValue = parcel.readValue(CTDisplayUnitType::class.java.classLoader) as CTDisplayUnitType?
            bgColorValue = parcel.readString()
            if (parcel.readByte().toInt() == 0x01) {
                val contents = ArrayList<CleverTapDisplayUnitContent>()
                contentsValue = contents
                parcel.readList(contents, CleverTapDisplayUnitContent::class.java.classLoader)
            } else {
                contentsValue = null
            }
            customExtrasValue = parcel.readHashMap(null) as HashMap<String, String>?
            jsonObjectValue = if (parcel.readByte().toInt() == 0x00) null else JSONObject(checkNotNull(parcel.readString()))
            errorValue = parcel.readString()
        } catch (e: Exception) {
            val error = "Error Creating Display Unit from parcel : ${e.localizedMessage}"
            errorValue = error
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, error)
        }
    }

    override fun describeContents(): Int = 0

    /**
     * Getter for the hex-value background color of the Display Unit e.g. #000000
     *
     * @return String
     */
    open fun getBgColor(): String? = bgColorValue

    /** Kotlin property syntax, equivalent to [getBgColor]. */
    val bgColor: String?
        @JvmSynthetic @JvmName("ktGetBgColor") inline get() = getBgColor()

    /**
     * Getter for the list of Content Display Unit Items.
     *
     * @return ArrayList<CleverTapDisplayUnitContent>
     */
    open fun getContents(): ArrayList<CleverTapDisplayUnitContent>? = contentsValue

    /** Kotlin property syntax, equivalent to [getContents]. */
    val contents: ArrayList<CleverTapDisplayUnitContent>?
        @JvmSynthetic @JvmName("ktGetContents") inline get() = getContents()

    /**
     * Getter for the Key Value pairs of the Display Unit
     *
     * @return HashMap<String, String>
     */
    open fun getCustomExtras(): HashMap<String, String>? = customExtrasValue

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
        if (contents == null || contentIndex !in contents.indices) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Content index $contentIndex out of range for unit $unitIDValue")
            return HashMap()
        }
        // Defensive copy: prevent callers from modifying stored attribution.
        return contents[contentIndex].getMetaData()?.let { HashMap(it) } ?: HashMap()
    }

    open fun getError(): String? = errorValue

    /** Kotlin property syntax, equivalent to [getError]. */
    val error: String?
        @JvmSynthetic @JvmName("ktGetError") inline get() = getError()

    /**
     * Getter for the JsonObject corresponding to the CleverTapDisplayUnit object
     *
     * @return JSONObject
     */
    open fun getJsonObject(): JSONObject? = jsonObjectValue

    /** Kotlin property syntax, equivalent to [getJsonObject]. */
    val jsonObject: JSONObject?
        @JvmSynthetic @JvmName("ktGetJsonObject") inline get() = getJsonObject()

    /**
     * Getter for the DisplayUnitType of the Display Unit, Refer[CTDisplayUnitType]
     *
     * @return CTDisplayUnitType
     */
    open fun getType(): CTDisplayUnitType? = typeValue

    /** Kotlin property syntax, equivalent to [getType]. */
    val type: CTDisplayUnitType?
        @JvmSynthetic @JvmName("ktGetType") inline get() = getType()

    /**
     * Getter for the unitId of the Display Unit
     *
     * @return String
     */
    open fun getUnitID(): String? = unitIDValue

    /** Kotlin property syntax, equivalent to [getUnitID]. */
    val unitID: String?
        @JvmSynthetic @JvmName("ktGetUnitID") inline get() = getUnitID()

    /**
     * Getter for the WiZRK fields obj to be passed in the data for recording event.
     *
     * @return JSONObject
     */
    open fun getWZRKFields(): JSONObject? {
        val jsonObject = jsonObjectValue ?: return null
        return try {
            val fields = JSONObject()
            for (keyName in jsonObject.keys()) {
                if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                    fields.put(keyName, jsonObject.get(keyName))
                }
            }
            fields
        } catch (e: Exception) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Error in getting WiZRK fields ${e.localizedMessage}")
            null
        }
    }

    /** Kotlin property syntax, equivalent to [getWZRKFields]. */
    val wzrkFields: JSONObject?
        @JvmSynthetic @JvmName("ktGetWZRKFields") inline get() = getWZRKFields()

    override fun toString(): String = try {
        buildString {
            append("[ Unit id- ").append(unitIDValue)
            append(", Type- ").append(typeValue?.toString())
            append(", bgColor- ").append(bgColorValue)
            // elements can be null when the list came from a Parcel
            contentsValue?.forEachIndexed { i, item: CleverTapDisplayUnitContent? ->
                if (item != null) {
                    append(", Content Item:").append(i).append(" ").append(item.toString()).append("\n")
                }
            }
            customExtrasValue?.let { append(", Custom KV:").append(it) }
            append(", JSON -").append(jsonObjectValue)
            append(", Error-").append(errorValue)
            append(" ]")
        }
    } catch (e: Exception) {
        Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Exception in toString:$e")
        super.toString()
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
        val jsonObject = jsonObjectValue
        if (jsonObject == null) {
            parcel.writeByte(0x00.toByte())
        } else {
            parcel.writeByte(0x01.toByte())
            parcel.writeString(jsonObject.toString())
        }
        parcel.writeString(errorValue)
    }

    /**
     * populates the custom key values pairs from json
     *
     * @param kvObj - Custom Key Values
     */
    internal fun getKeyValues(kvObj: JSONObject?): HashMap<String, String>? {
        if (kvObj == null) {
            return null
        }
        return try {
            val keyValues = HashMap<String, String>()
            for (key in kvObj.keys()) {
                val value = kvObj.getString(key)
                if (!key.isNullOrEmpty()) {
                    keyValues[key] = value
                }
            }
            keyValues.ifEmpty { null }
        } catch (e: Exception) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Error in getting Key Value Pairs ${e.localizedMessage}")
            null
        }
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<CleverTapDisplayUnit> = object : Parcelable.Creator<CleverTapDisplayUnit> {
            override fun createFromParcel(parcel: Parcel): CleverTapDisplayUnit = CleverTapDisplayUnit(parcel)

            override fun newArray(size: Int): Array<CleverTapDisplayUnit?> = arrayOfNulls(size)
        }

        /**
         * static method to convert json to Display Unit
         *
         * @param jsonObject - Display Unit Item in Json form
         * @return - CleverTapDisplayUnit - always returns non-null instance
         */
        @JvmStatic
        fun toDisplayUnit(jsonObject: JSONObject?): CleverTapDisplayUnit {
            if (jsonObject == null) {
                // what the Java original reported for its NPE (null message)
                return errorDisplayUnit(null)
            }
            return try {
                val unitID = if (jsonObject.has(Constants.NOTIFICATION_ID_TAG)) {
                    jsonObject.getString(Constants.NOTIFICATION_ID_TAG)
                } else {
                    Constants.TEST_IDENTIFIER
                }
                val displayUnitType = if (jsonObject.has(Constants.KEY_TYPE)) {
                    CTDisplayUnitType.type(jsonObject.getString(Constants.KEY_TYPE))
                } else {
                    null
                }
                val bgColor = if (jsonObject.has(Constants.KEY_BG)) jsonObject.getString(Constants.KEY_BG) else ""

                val contentArrayList = ArrayList<CleverTapDisplayUnitContent>()
                if (jsonObject.has(Constants.KEY_CONTENT)) {
                    val contentArray = jsonObject.getJSONArray(Constants.KEY_CONTENT)
                    for (i in 0 until contentArray.length()) {
                        val displayUnitContent = CleverTapDisplayUnitContent.toContent(contentArray.getJSONObject(i))
                        if (displayUnitContent.getError().isNullOrEmpty()) {
                            contentArrayList.add(displayUnitContent)
                        }
                    }
                }
                //custom KV can be added to Display unit of any types, no need to add type check here
                val customKV = if (jsonObject.has(Constants.KEY_CUSTOM_KV)) {
                    jsonObject.getJSONObject(Constants.KEY_CUSTOM_KV)
                } else {
                    null
                }
                CleverTapDisplayUnit(jsonObject, unitID, displayUnitType, bgColor, contentArrayList, customKV, null)
            } catch (e: Exception) {
                errorDisplayUnit(e.localizedMessage)
            }
        }

        private fun errorDisplayUnit(reason: String?): CleverTapDisplayUnit {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Unable to init CleverTapDisplayUnit with JSON - $reason")
            return CleverTapDisplayUnit(
                null, "", null, null, null, null,
                "Error Creating Display Unit from JSON : $reason"
            )
        }
    }
}
