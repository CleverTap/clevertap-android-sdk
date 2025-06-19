package com.clevertap.android.sdk.inapp

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.CTInAppAction.CREATOR.createFromJson
import org.json.JSONObject

class CTInAppNotificationButton : Parcelable {

    val backgroundColor: String

    val borderColor: String

    val borderRadius: String

    val text: String

    val textColor: String

    val action: CTInAppAction?

    internal constructor(jsonObject: JSONObject) {
        text = jsonObject.optString(Constants.KEY_TEXT)
        textColor = jsonObject.optString(Constants.KEY_COLOR, Constants.BLUE)
        backgroundColor = jsonObject.optString(Constants.KEY_BG, Constants.WHITE)
        borderColor = jsonObject.optString(Constants.KEY_BORDER, Constants.WHITE)
        borderRadius = jsonObject.optString(Constants.KEY_RADIUS)
        action = createFromJson(jsonObject.optJSONObject(Constants.KEY_ACTIONS))
    }

    private constructor(parcel: Parcel) {
        text = parcel.readString() ?: ""
        textColor = parcel.readString() ?: Constants.BLUE
        backgroundColor = parcel.readString() ?: Constants.WHITE
        borderColor = parcel.readString() ?: Constants.WHITE
        borderRadius = parcel.readString() ?: ""
        action = parcel.readParcelable<CTInAppAction?>(CTInAppAction::class.java.getClassLoader())
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(text)
        dest.writeString(textColor)
        dest.writeString(backgroundColor)
        dest.writeString(borderColor)
        dest.writeString(borderRadius)
        dest.writeParcelable(action, flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CTInAppNotificationButton

        if (backgroundColor != other.backgroundColor) return false
        if (borderColor != other.borderColor) return false
        if (borderRadius != other.borderRadius) return false
        if (text != other.text) return false
        if (textColor != other.textColor) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + borderColor.hashCode()
        result = 31 * result + borderRadius.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + textColor.hashCode()
        result = 31 * result + (action?.hashCode() ?: 0)
        return result
    }


    companion object {

        @JvmField
        val CREATOR = object : Parcelable.Creator<CTInAppNotificationButton> {
            override fun createFromParcel(parcel: Parcel): CTInAppNotificationButton {
                return CTInAppNotificationButton(parcel)
            }

            override fun newArray(size: Int): Array<CTInAppNotificationButton?> {
                return arrayOfNulls<CTInAppNotificationButton>(size)
            }
        }
    }
}
