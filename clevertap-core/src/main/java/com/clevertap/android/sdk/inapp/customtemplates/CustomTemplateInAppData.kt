package com.clevertap.android.sdk.inapp.customtemplates

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.copyFrom
import com.clevertap.android.sdk.inapp.CTInAppType
import com.clevertap.android.sdk.inapp.customtemplates.TemplateArgumentType.FILE
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.utils.getStringOrNull
import com.clevertap.android.sdk.utils.readJson
import com.clevertap.android.sdk.utils.writeJson
import org.json.JSONObject

internal class CustomTemplateInAppData private constructor(parcel: Parcel?) : Parcelable {

    var templateName: String?
        private set

    /**
     * Whether this in-app template was triggered from another template as an action or it was the main template in
     * the notification.
     */
    var isAction = false

    private var templateId: String?
    private var templateDescription: String?
    private var args: JSONObject?

    init {
        templateName = parcel?.readString()
        isAction = parcel?.readByte() != 0.toByte()
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

    fun getFileArgsUrls(templatesManager: TemplatesManager): List<Pair<String, CtCacheType>> {
        return templateName?.let { name ->
            templatesManager.getTemplate(name)?.args
                ?.filter { arg -> arg.type == FILE && args?.has(arg.name) ?: false }
                ?.mapNotNull { arg -> args?.getString(arg.name) }
                ?.map { url ->
                    Pair(url, CtCacheType.FILES)
                }
        } ?: emptyList()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(templateName)
        dest.writeByte(if (isAction) 1 else 0)
        dest.writeString(templateId)
        dest.writeString(templateDescription)
        dest.writeJson(args)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun writeFieldsToJson(json: JSONObject) {
        json.put(KEY_TEMPLATE_NAME, templateName)
        json.put(KEY_IS_ACTION, isAction)
        json.put(KEY_TEMPLATE_ID, templateId)
        json.put(KEY_TEMPLATE_DESCRIPTION, templateDescription)
        json.put(KEY_VARS, args)
    }

    fun copy(): CustomTemplateInAppData {
        val copy = CustomTemplateInAppData(null)
        copy.templateName = templateName
        copy.isAction = isAction
        copy.templateId = templateId
        copy.templateDescription = templateDescription
        args?.let {
            val argsJsonCopy = JSONObject()
            argsJsonCopy.copyFrom(it)
            copy.args = argsJsonCopy
        }
        return copy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomTemplateInAppData

        if (templateName != other.templateName) return false
        if (isAction != other.isAction) return false
        if (templateId != other.templateId) return false
        if (templateDescription != other.templateDescription) return false
        if (args?.toString() != other.args?.toString()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = templateName?.hashCode() ?: 0
        result = 31 * result + isAction.hashCode()
        result = 31 * result + (templateId?.hashCode() ?: 0)
        result = 31 * result + (templateDescription?.hashCode() ?: 0)
        result = 31 * result + (args?.toString()?.hashCode() ?: 0)
        return result
    }


    private fun setFieldsFromJson(json: JSONObject) {
        templateName = json.getStringOrNull(KEY_TEMPLATE_NAME)
        isAction = json.optBoolean(KEY_IS_ACTION)
        templateId = json.getStringOrNull(KEY_TEMPLATE_ID)
        templateDescription = json.getStringOrNull(KEY_TEMPLATE_DESCRIPTION)
        args = json.optJSONObject(KEY_VARS)
    }

    companion object CREATOR : Creator<CustomTemplateInAppData> {

        private const val KEY_IS_ACTION = "isAction"
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
