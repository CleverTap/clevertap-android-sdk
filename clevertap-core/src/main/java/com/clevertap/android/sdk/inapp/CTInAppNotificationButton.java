package com.clevertap.android.sdk.inapp;


import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.Constants;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class CTInAppNotificationButton implements Parcelable {

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInAppNotificationButton> CREATOR
            = new Parcelable.Creator<CTInAppNotificationButton>() {
        @Override
        public CTInAppNotificationButton createFromParcel(Parcel in) {
            return new CTInAppNotificationButton(in);
        }

        @Override
        public CTInAppNotificationButton[] newArray(int size) {
            return new CTInAppNotificationButton[size];
        }
    };

    private String backgroundColor;

    private String borderColor;

    private String borderRadius;

    private String error;

    private JSONObject jsonDescription;

    private String text;

    private String textColor;

    private CTInAppAction action;

    CTInAppNotificationButton() {
    }

    @SuppressWarnings("unchecked")
    protected CTInAppNotificationButton(Parcel in) {
        text = in.readString();
        textColor = in.readString();
        backgroundColor = in.readString();
        borderColor = in.readString();
        borderRadius = in.readString();
        try {
            jsonDescription = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        error = in.readString();
        action = in.readParcelable(CTInAppAction.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public HashMap<String, String> getKeyValues() {
        return action != null ? action.getKeyValues() : null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(textColor);
        dest.writeString(backgroundColor);
        dest.writeString(borderColor);
        dest.writeString(borderRadius);
        if (jsonDescription == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(jsonDescription.toString());
        }
        dest.writeString(error);
        dest.writeParcelable(action, flags);
    }


    String getBackgroundColor() {
        return backgroundColor;
    }

    String getBorderColor() {
        return borderColor;
    }

    String getBorderRadius() {
        return borderRadius;
    }

    String getError() {
        return error;
    }

    public String getText() {
        return text;
    }

    String getTextColor() {
        return textColor;
    }

    public CTInAppAction getAction() {
        return action;
    }

    CTInAppNotificationButton initWithJSON(JSONObject jsonObject) {
        jsonDescription = jsonObject;
        text = jsonObject.optString(Constants.KEY_TEXT);
        textColor = jsonObject.optString(Constants.KEY_COLOR, Constants.BLUE);
        backgroundColor = jsonObject.optString(Constants.KEY_BG, Constants.WHITE);
        borderColor = jsonObject.optString(Constants.KEY_BORDER, Constants.WHITE);
        borderRadius = jsonObject.optString(Constants.KEY_RADIUS);
        action = CTInAppAction.createFromJson(jsonObject.optJSONObject(Constants.KEY_ACTIONS));

        return this;
    }
}
