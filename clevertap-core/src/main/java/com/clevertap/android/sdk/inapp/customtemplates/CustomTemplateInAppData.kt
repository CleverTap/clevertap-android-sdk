package com.clevertap.android.sdk.inapp.customtemplates

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.copyFrom
import com.clevertap.android.sdk.inapp.CTInAppType
import com.clevertap.android.sdk.utils.getStringOrNull
import com.clevertap.android.sdk.utils.readJson
import com.clevertap.android.sdk.utils.writeJson
import org.json.JSONObject

internal class CustomTemplateInAppData private constructor(parcel: Parcel?) : Parcelable {

    var templateName: String?
        private set

    private var templateId: String?
    private var templateDescription: String?
    private var args: JSONObject?

    init {
        templateName = parcel?.readString()
        templateId = parcel?.readString()
        templateDescription = parcel?.readString()
        args = parcel?.readJson()
    }

    private constructor(json: JSONObject) : this(null) {
        setFieldsFromJson(json)
    }

    fun getArguments(): JSONObject? {
        return args?.let {
            val copy = JSONObject()
            copy.copyFrom(it)
            copy
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(templateName)
        dest.writeString(templateId)
        dest.writeString(templateDescription)
        dest.writeJson(args)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun writeFieldsToJson(json: JSONObject) {
        json.put(KEY_TEMPLATE_NAME, templateName)
        json.put(KEY_TEMPLATE_ID, templateId)
        json.put(KEY_TEMPLATE_DESCRIPTION, templateDescription)
        json.put(KEY_VARS, args)
    }

    private fun setFieldsFromJson(json: JSONObject) {
        templateName = json.getStringOrNull(KEY_TEMPLATE_NAME)
        templateId = json.getStringOrNull(KEY_TEMPLATE_ID)
        templateDescription = json.getStringOrNull(KEY_TEMPLATE_DESCRIPTION)
        args = json.optJSONObject(KEY_VARS)
    }

    companion object CREATOR : Creator<CustomTemplateInAppData> {

        private const val KEY_TEMPLATE_NAME = "templateName"
        private const val KEY_TEMPLATE_ID = "templateId"
        private const val KEY_TEMPLATE_DESCRIPTION = "templateDescription"
        private const val KEY_VARS = "vars"

        override fun createFromParcel(parcel: Parcel): CustomTemplateInAppData {
            return CustomTemplateInAppData(parcel)
        }

        override fun newArray(size: Int): Array<CustomTemplateInAppData?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        fun createFromJson(inApp: JSONObject?): CustomTemplateInAppData? {
            if (inApp == null) {
                return null
            }
            val inAppType = CTInAppType.fromString(inApp.optString(Constants.KEY_TYPE))
            return if (CTInAppType.CTInAppTypeCustomCodeTemplate == inAppType) {
                CustomTemplateInAppData(inApp)
            } else {
                null
            }
        }
    }
}
