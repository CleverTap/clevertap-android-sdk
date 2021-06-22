package com.clevertap.android.sdk.inbox;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Public model class for the "msg" object from notification inbox payload
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@RestrictTo(Scope.LIBRARY)
public class CTInboxMessageContent implements Parcelable {

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInboxMessageContent> CREATOR
            = new Parcelable.Creator<CTInboxMessageContent>() {
        @Override
        public CTInboxMessageContent createFromParcel(Parcel in) {
            return new CTInboxMessageContent(in);
        }

        @Override
        public CTInboxMessageContent[] newArray(int size) {
            return new CTInboxMessageContent[size];
        }
    };

    private String actionUrl;

    private String contentType;

    private Boolean hasLinks;

    private Boolean hasUrl;

    private String icon;

    private JSONArray links;

    private String media;

    private String message;

    private String messageColor;

    private String posterUrl;

    private String title;

    private String titleColor;

    CTInboxMessageContent() {
    }

    protected CTInboxMessageContent(Parcel in) {
        title = in.readString();
        titleColor = in.readString();
        message = in.readString();
        messageColor = in.readString();
        media = in.readString();
        hasUrl = in.readByte() != 0x00;
        hasLinks = in.readByte() != 0x00;
        actionUrl = in.readString();
        icon = in.readString();
        try {
            links = in.readByte() == 0x00 ? null : new JSONArray(in.readString());
        } catch (JSONException e) {
            Logger.v("Unable to init CTInboxMessageContent with Parcel - " + e.getLocalizedMessage());
        }
        contentType = in.readString();
        posterUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Return the action URL of the body of the inbox message
     *
     * @return String
     */
    public String getActionUrl() {
        return actionUrl;
    }

    void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    /**
     * Returns the content type of the media
     *
     * @return String
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the URL as String for the icon in case of Icon Message template
     *
     * @return String
     */
    public String getIcon() {
        return icon;
    }

    void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Returns the background color for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String
     */
    public String getLinkBGColor(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            return jsonObject.has(Constants.KEY_BG) ? jsonObject.getString(Constants.KEY_BG) : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text Color with JSON - " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Returns the text color for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String
     */
    public String getLinkColor(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            return jsonObject.has(Constants.KEY_COLOR) ? jsonObject.getString(Constants.KEY_COLOR) : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text Color with JSON - " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Returns the text for the JSONObject of Link provided
     * The JSONObject of Link provided should be of the type "copy"
     *
     * @param jsonObject of Link
     * @return String
     */
    public String getLinkCopyText(JSONObject jsonObject) {
        if (jsonObject == null) {
            return "";
        }
        try {
            JSONObject copyObject = jsonObject.has("copyText") ? jsonObject.getJSONObject("copyText") : null;
            if (copyObject != null) {
                return copyObject.has(Constants.KEY_TEXT) ? copyObject.getString(Constants.KEY_TEXT) : "";
            } else {
                return "";
            }
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text with JSON - " + e.getLocalizedMessage());
            return "";
        }
    }

    /**
     * Returns the Key Value pair with for the JSONObject of Link provided
     */
    public HashMap<String, String> getLinkKeyValue(JSONObject jsonObject) {
        if (jsonObject == null || !jsonObject.has(Constants.KEY_KV)) {
            return null;
        }
        try {
            JSONObject keyValues = jsonObject.getJSONObject(Constants.KEY_KV);
                Iterator<String> keys = keyValues.keys();
                HashMap<String, String> keyValuesMap = new HashMap<>();
                    String key, value;
                    while (keys.hasNext()) {
                        key = keys.next();
                        value = keyValues.getString(key);
                        if (!TextUtils.isEmpty(key)) {
                            keyValuesMap.put(key, value);
                        }
                    }
                return !keyValuesMap.isEmpty() ? keyValuesMap : null;

        } catch (JSONException e) {
            Logger.v("Unable to get Link Key Value with JSON - " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Returns the text for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String
     */
    public String getLinkText(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            return jsonObject.has(Constants.KEY_TEXT) ? jsonObject.getString(Constants.KEY_TEXT) : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text with JSON - " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Returns the text for the JSONObject of Link provided
     * The JSONObject of Link provided should be of the type "url"
     *
     * @param jsonObject of Link
     * @return String
     */
    public String getLinkUrl(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            JSONObject urlObject = jsonObject.has(Constants.KEY_URL) ? jsonObject.getJSONObject(Constants.KEY_URL)
                    : null;
            if (urlObject == null) {
                return null;
            }
            JSONObject androidObject = urlObject.has(Constants.KEY_ANDROID) ? urlObject
                    .getJSONObject(Constants.KEY_ANDROID) : null;
            if (androidObject != null) {
                return androidObject.has(Constants.KEY_TEXT) ? androidObject.getString(Constants.KEY_TEXT) : "";
            } else {
                return "";
            }
        } catch (JSONException e) {
            Logger.v("Unable to get Link URL with JSON - " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Returns a JSONArray of Call to Action buttons
     *
     * @return JSONArray
     */
    public JSONArray getLinks() {
        return links;
    }

    void setLinks(JSONArray links) {
        this.links = links;
    }

    /**
     * Returns the type for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String "copy" for Copy Text
     * String "url" for URLs
     */
    public String getLinktype(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            return jsonObject.has(Constants.KEY_TYPE) ? jsonObject.getString(Constants.KEY_TYPE) : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Type with JSON - " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Returns the media URL of the inbox message
     *
     * @return String
     */
    public String getMedia() {
        return media;
    }

    void setMedia(String media) {
        this.media = media;
    }

    /**
     * Returns the message section of the inbox message
     *
     * @return String
     */
    public String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the hexcode value of the message color as String
     *
     * @return String
     */
    public String getMessageColor() {
        return messageColor;
    }

    void setMessageColor(String messageColor) {
        this.messageColor = messageColor;
    }

    /**
     * Returns URL for the thumbnail of the video
     *
     * @return String
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    /**
     * Returns the title section of the inbox message
     *
     * @return String
     */
    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the hexcode value of the title color as String
     *
     * @return String
     */
    public String getTitleColor() {
        return titleColor;
    }

    void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    /**
     * Method to check whether media in the {@link CTInboxMessageContent} object is an audio.
     *
     * @return true if the media type is audio
     * false if the media type is not an audio
     */
    public boolean mediaIsAudio() {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.startsWith("audio");
    }

    /**
     * Method to check whether media in the {@link CTInboxMessageContent} object is an GIF.
     *
     * @return true if the media type is GIF
     * false if the media type is not an GIF
     */
    public boolean mediaIsGIF() {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.equals("image/gif");
    }

    /**
     * Method to check whether media in the {@link CTInboxMessageContent} object is an image.
     *
     * @return true if the media type is image
     * false if the media type is not an image
     */
    public boolean mediaIsImage() {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.startsWith("image") && !contentType
                .equals("image/gif");
    }

    /**
     * Method to check whether media in the {@link CTInboxMessageContent} object is a video.
     *
     * @return true if the media type is video
     * false if the media type is not a video
     */
    public boolean mediaIsVideo() {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.startsWith("video");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(titleColor);
        dest.writeString(message);
        dest.writeString(messageColor);
        dest.writeString(media);
        dest.writeByte((byte) (hasUrl ? 0x01 : 0x00));
        dest.writeByte((byte) (hasLinks ? 0x01 : 0x00));
        dest.writeString(actionUrl);
        dest.writeString(icon);
        if (links == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(links.toString());
        }
        dest.writeString(contentType);
        dest.writeString(posterUrl);
    }

    CTInboxMessageContent initWithJSON(JSONObject contentObject) {
        try {
            JSONObject titleObject = contentObject.has(Constants.KEY_TITLE) ? contentObject
                    .getJSONObject(Constants.KEY_TITLE) : null;
            if (titleObject != null) {
                this.title = titleObject.has(Constants.KEY_TEXT) ? titleObject.getString(Constants.KEY_TEXT) : "";
                this.titleColor = titleObject.has(Constants.KEY_COLOR) ? titleObject.getString(Constants.KEY_COLOR)
                        : "";
            }
            JSONObject msgObject = contentObject.has(Constants.KEY_MESSAGE) ? contentObject
                    .getJSONObject(Constants.KEY_MESSAGE) : null;
            if (msgObject != null) {
                this.message = msgObject.has(Constants.KEY_TEXT) ? msgObject.getString(Constants.KEY_TEXT) : "";
                this.messageColor = msgObject.has(Constants.KEY_COLOR) ? msgObject.getString(Constants.KEY_COLOR)
                        : "";
            }
            JSONObject iconObject = contentObject.has(Constants.KEY_ICON) ? contentObject
                    .getJSONObject(Constants.KEY_ICON) : null;
            if (iconObject != null) {
                this.icon = iconObject.has(Constants.KEY_URL) ? iconObject.getString(Constants.KEY_URL) : "";
            }
            JSONObject mediaObject = contentObject.has(Constants.KEY_MEDIA) ? contentObject
                    .getJSONObject(Constants.KEY_MEDIA) : null;
            if (mediaObject != null) {
                this.media = mediaObject.has(Constants.KEY_URL) ? mediaObject.getString(Constants.KEY_URL) : "";
                this.contentType = mediaObject.has(Constants.KEY_CONTENT_TYPE) ? mediaObject
                        .getString(Constants.KEY_CONTENT_TYPE) : "";
                this.posterUrl = mediaObject.has(Constants.KEY_POSTER_URL) ? mediaObject
                        .getString(Constants.KEY_POSTER_URL) : "";
            }

            JSONObject actionObject = contentObject.has(Constants.KEY_ACTION) ? contentObject
                    .getJSONObject(Constants.KEY_ACTION) : null;
            if (actionObject != null) {
                this.hasUrl = actionObject.has(Constants.KEY_HAS_URL) && actionObject
                        .getBoolean(Constants.KEY_HAS_URL);
                this.hasLinks = actionObject.has(Constants.KEY_HAS_LINKS) && actionObject
                        .getBoolean(Constants.KEY_HAS_LINKS);
                JSONObject urlObject = actionObject.has(Constants.KEY_URL) ? actionObject
                        .getJSONObject(Constants.KEY_URL) : null;
                if (urlObject != null && this.hasUrl) {
                    JSONObject androidObject = urlObject.has(Constants.KEY_ANDROID) ? urlObject
                            .getJSONObject(Constants.KEY_ANDROID) : null;
                    if (androidObject != null) {
                        this.actionUrl = androidObject.has(Constants.KEY_TEXT) ? androidObject
                                .getString(Constants.KEY_TEXT) : "";
                    }
                }
                if (urlObject != null && this.hasLinks) {
                    this.links = actionObject.has(Constants.KEY_LINKS) ? actionObject
                            .getJSONArray(Constants.KEY_LINKS) : null;
                }
            }

        } catch (JSONException e) {
            Logger.v("Unable to init CTInboxMessageContent with JSON - " + e.getLocalizedMessage());
        }
        return this;
    }
}
