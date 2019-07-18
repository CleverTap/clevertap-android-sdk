package com.clevertap.android.sdk;


import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

class CTInAppNotificationButton implements Parcelable {

    private String text;
    private String textColor;
    private String backgroundColor;
    private String actionUrl;
    private JSONObject jsonDescription;
    private String error;
    private String borderColor;
    private String borderRadius;

    String getText() {
        return text;
    }

    void setText(String text) {
        this.text = text;
    }

    String getTextColor() {
        return textColor;
    }

    @SuppressWarnings({"unused"})
    void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    String getBackgroundColor() {
        return backgroundColor;
    }

    void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    String getActionUrl() {
        return actionUrl;
    }

    @SuppressWarnings({"unused"})
    void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    @SuppressWarnings({"unused"})
    JSONObject getJsonDescription() {
        return jsonDescription;
    }

    @SuppressWarnings({"unused"})
    void setJsonDescription(JSONObject jsonDescription) {
        this.jsonDescription = jsonDescription;
    }

    String getError() {
        return error;
    }

    void setError(String error) {
        this.error = error;
    }

    String getBorderColor() {
        return borderColor;
    }

    @SuppressWarnings({"unused"})
    void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    String getBorderRadius() {
        return borderRadius;
    }

    @SuppressWarnings({"unused"})
    void setBorderRadius(String borderRadius) {
        this.borderRadius = borderRadius;
    }

    CTInAppNotificationButton(){}

    CTInAppNotificationButton initWithJSON(JSONObject jsonObject){
        try{
            this.jsonDescription = jsonObject;
            this.text = jsonObject.has("text") ? jsonObject.getString("text") : "";
            this.textColor = jsonObject.has("color") ? jsonObject.getString("color") : Constants.BLUE;
            this.backgroundColor = jsonObject.has("bg") ? jsonObject.getString("bg") : Constants.WHITE;
            this.borderColor = jsonObject.has("border") ? jsonObject.getString("border") : Constants.WHITE;
            this.borderRadius = jsonObject.has("radius") ? jsonObject.getString("radius") : "";

            JSONObject actions = jsonObject.has("actions") ? jsonObject.getJSONObject("actions") : null;
            if(actions!=null){
                String action = actions.has("android") ? actions.getString("android") : "";
                if(!action.isEmpty()){
                    this.actionUrl = action;
                }
            }
        }catch (JSONException e){
            this.error = "Invalid JSON";
        }
        return this;
    }

    protected CTInAppNotificationButton(Parcel in) {
        text = in.readString();
        textColor = in.readString();
        backgroundColor = in.readString();
        actionUrl = in.readString();
        borderColor = in.readString();
        borderRadius = in.readString();

        try {
            jsonDescription = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        error = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(textColor);
        dest.writeString(backgroundColor);
        dest.writeString(actionUrl);
        dest.writeString(borderColor);
        dest.writeString(borderRadius);

        if (jsonDescription == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(jsonDescription.toString());
        }
        dest.writeString(error);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInAppNotificationButton> CREATOR = new Parcelable.Creator<CTInAppNotificationButton>() {
        @Override
        public CTInAppNotificationButton createFromParcel(Parcel in) {
            return new CTInAppNotificationButton(in);
        }

        @Override
        public CTInAppNotificationButton[] newArray(int size) {
            return new CTInAppNotificationButton[size];
        }
    };
}
