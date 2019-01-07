package com.clevertap.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Public facing model class for type of InboxMessage
 */
public class CTInboxMessage implements Parcelable {
    private String title;
    private String body;
    private String imageUrl;
    private String actionUrl;
    private long date;
    private long expires;
    private String messageId;
    private JSONObject data;
    private JSONObject customData;
    private boolean isRead;
    private CTInboxMessageType type;
    private List<String> tags = new ArrayList<>();
    private String bgColor;
    private ArrayList<CTInboxMessageContent> inboxMessageContents = new ArrayList<>();
    private String orientation;
    private String campaignId;


    CTInboxMessage initWithJSON(JSONObject jsonObject){
        this.data = jsonObject;
        try {
            this.messageId = jsonObject.has("id") ? jsonObject.getString("id") : "0";
            this.campaignId = jsonObject.has("wzrk_id") ? jsonObject.getString("wzrk_id") : "0_0";
            this.date = jsonObject.has("date") ? jsonObject.getLong("date") : System.currentTimeMillis()/1000;
            this.expires = jsonObject.has("wzrk_ttl") ? jsonObject.getLong("wzrk_ttl") : 1000*60*60*24;
            this.isRead = jsonObject.has("isRead") && jsonObject.getBoolean("isRead");
            JSONObject cellObject = jsonObject.has("msg") ? jsonObject.getJSONObject("msg") : null;
            if(cellObject != null){
                this.type = cellObject.has("type") ? CTInboxMessageType.fromString(cellObject.getString("type")) : CTInboxMessageType.fromString("");
                this.bgColor = cellObject.has("bg") ? cellObject.getString("bg") : "";
                JSONArray contentArray = cellObject.has("content") ? cellObject.getJSONArray("content") : null;
                if(contentArray != null){
                    for(int i=0 ; i<contentArray.length(); i++){
                        CTInboxMessageContent ctInboxMessageContent = new CTInboxMessageContent().initWithJSON(contentArray.getJSONObject(i));
                        this.inboxMessageContents.add(ctInboxMessageContent);
                    }
                }
                this.orientation = cellObject.has("orientation") ? cellObject.getString("orientation") : "";
                JSONArray tagsArray = cellObject.has("tags") ? cellObject.getJSONArray("tags") : null;
                if(tagsArray != null){
                    for(int i=0; i< tagsArray.length(); i++){
                        this.tags.add(tagsArray.getString(i));
                    }
                }
            }
        } catch (JSONException e) {
            Logger.v("Unable to init CTInboxMessage with JSON - "+e.getLocalizedMessage());
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
            date = in.readLong();
            expires = in.readLong();
            messageId = in.readString();
            data = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            customData = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            isRead = in.readByte() != 0x00;
            type = (CTInboxMessageType) in.readValue(CTInboxMessageType.class.getClassLoader());
            if (in.readByte() == 0x01) {
                tags = new ArrayList<String>();
                in.readList(tags, String.class.getClassLoader());
            } else {
                tags = null;
            }
            bgColor = in.readString();
            if (in.readByte() == 0x01) {
                inboxMessageContents = new ArrayList<CTInboxMessageContent>();
                in.readList(inboxMessageContents, CTInboxMessageContent.class.getClassLoader());
            } else {
                inboxMessageContents = null;
            }
            orientation = in.readString();
            campaignId = in.readString();
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
        dest.writeLong(date);
        dest.writeLong(expires);
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
        dest.writeValue(type);
        if (tags == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(tags);
        }
        dest.writeString(bgColor);
        if (inboxMessageContents == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(inboxMessageContents);
        }
        dest.writeString(orientation);
        dest.writeString(campaignId);
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

    public long getDate() {
        return date;
    }

    public long getExpires() {
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

    public List<String> getTags() {
        return tags;
    }

    public String getBgColor() {
        return bgColor;
    }

    public ArrayList<CTInboxMessageContent> getInboxMessageContents() {
        return inboxMessageContents;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public CTInboxMessageType getType() {
        return type;
    }

    public static Creator<CTInboxMessage> getCREATOR() {
        return CREATOR;
    }

    public ArrayList<String> getCarouselImages(){
        ArrayList<String> carouselImages = new ArrayList<>();
        for(CTInboxMessageContent ctInboxMessageContent: getInboxMessageContents()){
            carouselImages.add(ctInboxMessageContent.getMedia());
        }
        return carouselImages;
    }

    public String getOrientation() {
        return orientation;
    }
}
