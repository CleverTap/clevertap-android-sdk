package com.clevertap.android.sdk.inapp

import android.os.Parcel
import android.os.Parcelable
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.CTInAppAction.CREATOR.createFromJson
import org.json.JSONObject


internal class CTInAppNotificationButton : Parcelable {

    val backgroundColor: String?

    val borderColor: String?

    val borderRadius: String?

    val text: String?

    val textColor: String?

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
        text = parcel.readString()
        textColor = parcel.readString()
        backgroundColor = parcel.readString()
        borderColor = parcel.readString()
        borderRadius = parcel.readString()
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


    companion object {
        @JvmField
        val CREATOR
                : Parcelable.Creator<CTInAppNotificationButton> =

            object : Parcelable.Creator<CTInAppNotificationButton> {
                override fun createFromParcel(parcel: Parcel): CTInAppNotificationButton {
                    return CTInAppNotificationButton(parcel)
                }

                override fun newArray(size: Int): Array<CTInAppNotificationButton?> {
                    return arrayOfNulls<CTInAppNotificationButton>(size)
                }
            }
    }
}
