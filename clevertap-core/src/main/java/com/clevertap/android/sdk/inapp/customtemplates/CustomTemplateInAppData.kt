package com.clevertap.android.sdk.inapp.customtemplates

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.clevertap.android.sdk.utils.getStringOrNull
import com.clevertap.android.sdk.utils.readJson
import com.clevertap.android.sdk.utils.writeJson
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONObject

class CustomTemplateInAppData private constructor(parcel: Parcel?) : Parcelable {

    var templateName: String?
        private set

    private var args: JSONObject?

    init {
        templateName = parcel?.readString()
        args = parcel?.readJson()
    }

    constructor(json: JSONObject) : this(null) {
        setFieldsFromJson(json)
    }

    fun getArguments(): Map<String, Any>? {
        return JsonUtil.mapFromJson(args)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(templateName)
        dest.writeJson(args)
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun setFieldsFromJson(json: JSONObject) {
        templateName = json.getStringOrNull(KEY_TEMPLATE_NAME)
        args = json.optJSONObject(KEY_VARS)
    }

    companion object CREATOR : Creator<CustomTemplateInAppData> {

        private const val KEY_TEMPLATE_NAME = "templateName"
        private const val KEY_VARS = "vars"

        override fun createFromParcel(parcel: Parcel): CustomTemplateInAppData {
            return CustomTemplateInAppData(parcel)
        }

        override fun newArray(size: Int): Array<CustomTemplateInAppData?> {
            return arrayOfNulls(size)
        }
    }
}
