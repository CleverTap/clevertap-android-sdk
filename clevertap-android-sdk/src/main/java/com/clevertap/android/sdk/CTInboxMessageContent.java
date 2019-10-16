package com.clevertap.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Public model class for the "msg" object from notification inbox payload
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CTInboxMessageContent implements Parcelable {

    private String title;
    private String titleColor;
    private String message;
    private String messageColor;
    private String media;
    private Boolean hasUrl;
    private Boolean hasLinks;
    private String actionUrl;
    private String icon;
    private JSONArray links;
    private String contentType;
    private String posterUrl;

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

    CTInboxMessageContent initWithJSON(JSONObject contentObject) {
        try {
            JSONObject titleObject = contentObject.has("title") ? contentObject.getJSONObject("title") : null;
            if (titleObject != null) {
                this.title = titleObject.has("text") ? titleObject.getString("text") : "";
                this.titleColor = titleObject.has("color") ? titleObject.getString("color") : "";
            }
            JSONObject msgObject = contentObject.has("message") ? contentObject.getJSONObject("message") : null;
            if (msgObject != null) {
                this.message = msgObject.has("text") ? msgObject.getString("text") : "";
                this.messageColor = msgObject.has("color") ? msgObject.getString("color") : "";
            }
            JSONObject iconObject = contentObject.has("icon") ? contentObject.getJSONObject("icon") : null;
            if (iconObject != null) {
                this.icon = iconObject.has("url") ? iconObject.getString("url") : "";
            }
            JSONObject mediaObject = contentObject.has("media") ? contentObject.getJSONObject("media") : null;
            if (mediaObject != null) {
                this.media = mediaObject.has("url") ? mediaObject.getString("url") : "";
                this.contentType = mediaObject.has("content_type") ? mediaObject.getString("content_type") : "";
                this.posterUrl = mediaObject.has("poster") ? mediaObject.getString("poster") : "";
            }

            JSONObject actionObject = contentObject.has("action") ? contentObject.getJSONObject("action") : null;
            if (actionObject != null) {
                this.hasUrl = actionObject.has("hasUrl") && actionObject.getBoolean("hasUrl");
                this.hasLinks = actionObject.has("hasLinks") && actionObject.getBoolean("hasLinks");
                JSONObject urlObject = actionObject.has("url") ? actionObject.getJSONObject("url") : null;
                if (urlObject != null && this.hasUrl) {
                    JSONObject androidObject = urlObject.has("android") ? urlObject.getJSONObject("android") : null;
                    if (androidObject != null) {
                        this.actionUrl = androidObject.has("text") ? androidObject.getString("text") : "";
                    }
                }
                if (urlObject != null && this.hasLinks) {
                    this.links = actionObject.has("links") ? actionObject.getJSONArray("links") : null;
                }
            }

        } catch (JSONException e) {
            Logger.v("Unable to init CTInboxMessageContent with JSON - " + e.getLocalizedMessage());
        }
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
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

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInboxMessageContent> CREATOR = new Parcelable.Creator<CTInboxMessageContent>() {
        @Override
        public CTInboxMessageContent createFromParcel(Parcel in) {
            return new CTInboxMessageContent(in);
        }

        @Override
        public CTInboxMessageContent[] newArray(int size) {
            return new CTInboxMessageContent[size];
        }
    };

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
     * Returns the type for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String "copy" for Copy Text
     * String "url" for URLs
     */
    public String getLinktype(JSONObject jsonObject) {
        if (jsonObject == null) return null;
        try {
            return jsonObject.has("type") ? jsonObject.getString("type") : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Type with JSON - " + e.getLocalizedMessage());
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
        if (jsonObject == null) return null;
        try {
            return jsonObject.has("text") ? jsonObject.getString("text") : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text with JSON - " + e.getLocalizedMessage());
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
        if (jsonObject == null) return "";
        try {
            JSONObject copyObject = jsonObject.has("copyText") ? jsonObject.getJSONObject("copyText") : null;
            if (copyObject != null) {
                return copyObject.has("text") ? copyObject.getString("text") : "";
            } else {
                return "";
            }
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text with JSON - " + e.getLocalizedMessage());
            return "";
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
        if (jsonObject == null) return null;
        try {
            JSONObject urlObject = jsonObject.has("url") ? jsonObject.getJSONObject("url") : null;
            if (urlObject == null) return null;
            JSONObject androidObject = urlObject.has("android") ? urlObject.getJSONObject("android") : null;
            if (androidObject != null) {
                return androidObject.has("text") ? androidObject.getString("text") : "";
            } else {
                return "";
            }
        } catch (JSONException e) {
            Logger.v("Unable to get Link URL with JSON - " + e.getLocalizedMessage());
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
        if (jsonObject == null) return null;
        try {
            return jsonObject.has("color") ? jsonObject.getString("color") : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text Color with JSON - " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Returns the background color for the JSONObject of Link provided
     *
     * @param jsonObject of Link
     * @return String
     */
    public String getLinkBGColor(JSONObject jsonObject) {
        if (jsonObject == null) return null;
        try {
            return jsonObject.has("bg") ? jsonObject.getString("bg") : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text Color with JSON - " + e.getLocalizedMessage());
            return null;
        }
    }


    /**
     * Returns the Key Value pair with for the JSONObject of Link provided
     *
     * @param jsonObject
     * @return
     */
    public HashMap<String, String> getLinkKeyValue(JSONObject jsonObject) {
        if (jsonObject == null || !jsonObject.has(Constants.KEY_KV)) return null;
        try {
            JSONObject keyValues = jsonObject.getJSONObject(Constants.KEY_KV);
            if (keyValues != null) {
                Iterator<String> keys = keyValues.keys();
                HashMap<String, String> keyValuesMap = new HashMap<>();
                if (keys != null) {
                    String key, value;
                    while (keys.hasNext()) {
                        key = keys.next();
                        value = keyValues.getString(key);
                        if (!TextUtils.isEmpty(key)) {
                            keyValuesMap.put(key, value);
                        }
                    }
                }
                return !keyValuesMap.isEmpty() ? keyValuesMap : null;
            } else {
                return null;
            }
        } catch (JSONException e) {
            Logger.v("Unable to get Link Key Value with JSON - " + e.getLocalizedMessage());
            return null;
        }
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
     * Method to check whether media in the {@link CTInboxMessageContent} object is an image.
     *
     * @return true if the media type is image
     * false if the media type is not an image
     */
    public boolean mediaIsImage() {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.startsWith("image") && !contentType.equals("image/gif");
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
     * Method to check whether media in the {@link CTInboxMessageContent} object is a video.
     *
     * @return true if the media type is video
     * false if the media type is not a video
     */
    public boolean mediaIsVideo() {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.startsWith("video");
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
}
