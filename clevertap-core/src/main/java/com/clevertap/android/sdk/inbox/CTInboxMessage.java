package com.clevertap.android.sdk.inbox;

import android.os.Parcel;
import android.os.Parcelable;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Public facing model class for type of InboxMessage
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class CTInboxMessage implements Parcelable {

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

    private String actionUrl;

    private String bgColor;

    private String body;

    private String campaignId;

    private JSONObject customData;

    private JSONObject data;

    private long date;

    private long expires;

    private String imageUrl;

    private ArrayList<CTInboxMessageContent> inboxMessageContents = new ArrayList<>();

    private boolean isRead;

    private String messageId;

    private String orientation;

    private List<String> tags = new ArrayList<>();

    private String title;

    private CTInboxMessageType type;

    private JSONObject wzrkParams;

    public static Creator<CTInboxMessage> getCREATOR() {
        return CREATOR;
    }

    public CTInboxMessage(JSONObject jsonObject) {
        this.data = jsonObject;
        try {
            this.messageId = jsonObject.has(Constants.KEY_ID) ? jsonObject.getString(Constants.KEY_ID) : "0";
            this.campaignId = jsonObject.has(Constants.NOTIFICATION_ID_TAG) ? jsonObject
                    .getString(Constants.NOTIFICATION_ID_TAG) : "0_0";
            this.date = jsonObject.has(Constants.KEY_DATE) ? jsonObject.getLong(Constants.KEY_DATE)
                    : System.currentTimeMillis() / 1000;
            this.expires = jsonObject.has(Constants.KEY_WZRK_TTL) ? jsonObject.getLong(Constants.KEY_WZRK_TTL)
                    : System.currentTimeMillis() + 1000 * 60 * 60 * 24;
            this.isRead = jsonObject.has(Constants.KEY_IS_READ) && jsonObject.getBoolean(Constants.KEY_IS_READ);
            JSONArray tagsArray = jsonObject.has(Constants.KEY_TAGS) ? jsonObject.getJSONArray(Constants.KEY_TAGS)
                    : null;
            if (tagsArray != null) {
                for (int i = 0; i < tagsArray.length(); i++) {
                    this.tags.add(tagsArray.getString(i));
                }
            }
            JSONObject cellObject = jsonObject.has(Constants.KEY_MSG) ? jsonObject.getJSONObject(Constants.KEY_MSG)
                    : null;
            if (cellObject != null) {
                this.type = cellObject.has(Constants.KEY_TYPE) ? CTInboxMessageType
                        .fromString(cellObject.getString(Constants.KEY_TYPE)) : CTInboxMessageType.fromString("");
                this.bgColor = cellObject.has(Constants.KEY_BG) ? cellObject.getString(Constants.KEY_BG) : "";
                JSONArray contentArray = cellObject.has(Constants.KEY_CONTENT) ? cellObject
                        .getJSONArray(Constants.KEY_CONTENT) : null;
                if (contentArray != null) {
                    for (int i = 0; i < contentArray.length(); i++) {
                        CTInboxMessageContent ctInboxMessageContent = new CTInboxMessageContent()
                                .initWithJSON(contentArray.getJSONObject(i));
                        this.inboxMessageContents.add(ctInboxMessageContent);
                    }
                }
                this.orientation = cellObject.has(Constants.KEY_ORIENTATION) ? cellObject
                        .getString(Constants.KEY_ORIENTATION) : "";
            }
            this.wzrkParams = jsonObject.has(Constants.KEY_WZRK_PARAMS) ? jsonObject
                    .getJSONObject(Constants.KEY_WZRK_PARAMS) : null;
        } catch (JSONException e) {
            Logger.v("Unable to init CTInboxMessage with JSON - " + e.getLocalizedMessage());
        }
    }

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
                tags = new ArrayList<>();
                in.readList(tags, String.class.getClassLoader());
            } else {
                tags = null;
            }
            bgColor = in.readString();
            if (in.readByte() == 0x01) {
                inboxMessageContents = new ArrayList<>();
                in.readList(inboxMessageContents, CTInboxMessageContent.class.getClassLoader());
            } else {
                inboxMessageContents = null;
            }
            orientation = in.readString();
            campaignId = in.readString();
            wzrkParams = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
        } catch (JSONException e) {
            Logger.v("Unable to parse CTInboxMessage from parcel - " + e.getLocalizedMessage());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public String getBgColor() {
        return bgColor;
    }

    public String getBody() {
        return body;
    }

    public String getCampaignId() {
        return campaignId;
    }

    /**
     * Returns an ArrayList of String URLs of the Carousel Images
     *
     * @return ArrayList of Strings
     */
    public ArrayList<String> getCarouselImages() {
        ArrayList<String> carouselImages = new ArrayList<>();
        for (CTInboxMessageContent ctInboxMessageContent : getInboxMessageContents()) {
            carouselImages.add(ctInboxMessageContent.getMedia());
        }
        return carouselImages;
    }

    public JSONObject getCustomData() {
        return customData;
    }

    public JSONObject getData() {
        return data;
    }

    public long getDate() {
        return date;
    }

    public long getExpires() {
        return expires;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Returns an ArrayList of the contents of {@link CTInboxMessage}
     * For Simple Message and Icon Message templates the size of this ArrayList is by default 1.
     * For Carousel templates, the size of the ArrayList is the number of slides in the Carousel
     *
     * @return ArrayList of {@link CTInboxMessageContent} objects
     */
    public ArrayList<CTInboxMessageContent> getInboxMessageContents() {
        return inboxMessageContents;
    }

    public String getMessageId() {
        return messageId;
    }

    /**
     * Returns the orientation of the media.
     *
     * @return Returns "l" for landscape
     * Returns "p" for portrait
     */
    public String getOrientation() {
        return orientation;
    }

    /**
     * Returns a List of tags as set on the CleverTap dashboard
     *
     * @return List of Strings
     */
    public List<String> getTags() {
        return tags;
    }

    public String getTitle() {
        return title;
    }

    public CTInboxMessageType getType() {
        return type;
    }

    /**
     * Returns a JSONObject of wzrk_* parameters.
     *
     * @return JSONObject of wzrk_* parameters
     */
    public JSONObject getWzrkParams() {
        return wzrkParams == null ? new JSONObject() : wzrkParams;
    }

    public boolean isRead() {
        return isRead;
    }

    void setRead(boolean read) {
        isRead = read;
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
        if (wzrkParams == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(wzrkParams.toString());
        }
    }
}
