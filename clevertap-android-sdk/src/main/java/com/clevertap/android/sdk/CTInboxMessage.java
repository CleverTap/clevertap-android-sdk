package com.clevertap.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;


public class CTInboxMessage implements Parcelable {
    private String title;
    private String body;
    private String imageUrl;
    private String actionUrl;
    private int date;
    private int expires;
    private String messageId;
    private JSONObject data;
    private JSONObject customData;
    private boolean isRead;
    private String type;

    public CTInboxMessage initWithJSON(JSONObject jsonObject){
        this.data = jsonObject;
        try {
            this.messageId = jsonObject.has("id") ? jsonObject.getString("id") : "";
            this.title = jsonObject.has("title") ? jsonObject.getString("title") : "";
            this.body = jsonObject.has("body") ? jsonObject.getString("body") : "";
            this.imageUrl = jsonObject.has("imageUrl") ? jsonObject.getString("imageUrl") : "";
            this.actionUrl = jsonObject.has("actionUrl") ? jsonObject.getString("actionUrl") : "";
            this.date = jsonObject.has("date") ? jsonObject.getInt("date") : -1;
            this.expires = jsonObject.has("expires") ? jsonObject.getInt("expires") : -1;
            this.isRead = jsonObject.has("isRead") && jsonObject.getBoolean("isRead");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return this;
    }

    public CTInboxMessage(){}

    private CTInboxMessage(Parcel in) {
        try {
            title = in.readString();
            body = in.readString();
            imageUrl = in.readString();
            actionUrl = in.readString();
            date = in.readInt();
            expires = in.readInt();
            messageId = in.readString();
            data = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            customData = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            isRead = in.readByte() != 0x00;
            type = in.readString();
        }catch (JSONException e){
           Logger.v("Unable to parse CTInboxMessage from parcel - "+e.getLocalizedMessage());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(body);
        dest.writeString(imageUrl);
        dest.writeString(actionUrl);
        dest.writeInt(date);
        dest.writeInt(expires);
        dest.writeString(messageId);
        if (data == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(data.toString());
        }
        if (customData == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(customData.toString());
        }
        dest.writeByte((byte) (isRead ? 0x01 : 0x00));
        dest.writeString(type);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInboxMessage> CREATOR = new Parcelable.Creator<CTInboxMessage>() {
        @Override
        public CTInboxMessage createFromParcel(Parcel in) {
            return new CTInboxMessage(in);
        }

        @Override
        public CTInboxMessage[] newArray(int size) {
            return new CTInboxMessage[size];
        }
    };

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public int getDate() {
        return date;
    }

    public int getExpires() {
        return expires;
    }

    public String getMessageId() {
        return messageId;
    }

    public JSONObject getData() {
        return data;
    }

    public JSONObject getCustomData() {
        return customData;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getType() {
        return type;
    }

    public static Creator<CTInboxMessage> getCREATOR() {
        return CREATOR;
    }
}
