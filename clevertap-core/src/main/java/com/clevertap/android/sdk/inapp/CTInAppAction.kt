package com.clevertap.android.sdk.inapp

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.KEY_FALLBACK_NOTIFICATION_SETTINGS
import com.clevertap.android.sdk.inapp.InAppActionType.OPEN_URL
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
    }

    private constructor(json: JSONObject) : this(null) {
        setFieldsFromJson(json)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(type?.toString())
        dest.writeString(actionUrl)
        dest.writeMap(keyValues)
        dest.writeParcelable(customTemplateInAppData, flags)
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
    }
}

internal enum class InAppActionType(private val stringValue: String) {
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