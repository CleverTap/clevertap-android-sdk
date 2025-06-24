package com.clevertap.android.sdk.inapp

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.KEY_FALLBACK_NOTIFICATION_SETTINGS
import com.clevertap.android.sdk.inapp.InAppActionType.OPEN_URL
import com.clevertap.android.sdk.inapp.InAppActionType.CLOSE
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData
import com.clevertap.android.sdk.utils.getStringOrNull
import org.json.JSONObject

internal class CTInAppAction private constructor(parcel: Parcel?) : Parcelable {

    var type: InAppActionType?
        private set

    var actionUrl: String?
        private set

    var keyValues: HashMap<String, String>?
        private set

    var customTemplateInAppData: CustomTemplateInAppData?
        private set

    @get:JvmName("shouldFallbackToSettings")
    var shouldFallbackToSettings: Boolean = false
        private set

    init {
        type = parcel?.readString()?.let { InAppActionType.fromString(it) }
        actionUrl = parcel?.readString()
        keyValues = parcel?.readHashMap(null) as? HashMap<String, String>
        customTemplateInAppData = parcel?.readParcelable(CustomTemplateInAppData::class.java.getClassLoader())
        shouldFallbackToSettings = parcel?.readByte()?.toInt() != 0x00
    }

    private constructor(json: JSONObject) : this(null) {
        setFieldsFromJson(json)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(type?.toString())
        dest.writeString(actionUrl)
        dest.writeMap(keyValues)
        dest.writeParcelable(customTemplateInAppData, flags)
        dest.writeByte((if (shouldFallbackToSettings) 0x01 else 0x00).toByte())
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun setFieldsFromJson(json: JSONObject) {
        type = json.getStringOrNull(Constants.KEY_TYPE)?.let { InAppActionType.fromString(it) }
        actionUrl = json.getStringOrNull(Constants.KEY_ANDROID)
        customTemplateInAppData = CustomTemplateInAppData.createFromJson(json)
        shouldFallbackToSettings = json.optBoolean(KEY_FALLBACK_NOTIFICATION_SETTINGS)

        if (Constants.KEY_KV.equals(json.optString(Constants.KEY_TYPE), ignoreCase = true)
            && json.has(Constants.KEY_KV)
        ) {
            val keyValuesJson = json.optJSONObject(Constants.KEY_KV)
            val map = keyValues ?: HashMap()
            if (keyValuesJson != null) {
                var value: String?
                for (key in keyValuesJson.keys()) {
                    value = keyValuesJson.optString(key)
                    if (value.isNotEmpty()) {
                        map[key] = value
                    }
                }
                keyValues = map
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CTInAppAction

        if (shouldFallbackToSettings != other.shouldFallbackToSettings) return false
        if (type != other.type) return false
        if (actionUrl != other.actionUrl) return false
        if (keyValues != other.keyValues) return false
        if (customTemplateInAppData != other.customTemplateInAppData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shouldFallbackToSettings.hashCode()
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (actionUrl?.hashCode() ?: 0)
        result = 31 * result + (keyValues?.hashCode() ?: 0)
        result = 31 * result + (customTemplateInAppData?.hashCode() ?: 0)
        return result
    }


    companion object CREATOR : Creator<CTInAppAction> {

        override fun createFromParcel(parcel: Parcel): CTInAppAction {
            return CTInAppAction(parcel)
        }

        override fun newArray(size: Int): Array<CTInAppAction?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        fun createFromJson(json: JSONObject?): CTInAppAction? {
            if (json == null) {
                return null
            }
            return CTInAppAction(json)
        }

        @JvmStatic
        fun createOpenUrlAction(url: String): CTInAppAction {
            return CTInAppAction(null).apply {
                type = OPEN_URL
                actionUrl = url
            }
        }

        @JvmStatic
        fun createCloseAction(): CTInAppAction {
            return CTInAppAction(null).apply {
                type = CLOSE
            }
        }
    }
}

enum class InAppActionType(private val stringValue: String) {
    CLOSE("close"),
    OPEN_URL("url"),
    KEY_VALUES("kv"),
    CUSTOM_CODE("custom-code"),
    REQUEST_FOR_PERMISSIONS(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION);

    override fun toString() = stringValue

    companion object {

        fun fromString(string: String): InAppActionType? {
            return values().firstOrNull { it.stringValue == string }
        }
    }
}
