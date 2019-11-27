package com.clevertap.android.sdk.ads.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.clevertap.android.sdk.Logger;

import org.json.JSONObject;

/**
 * Content class for holding AdContent Data
 */
public class CTAdUnitContent implements Parcelable {
    private String title;
    private String titleColor;
    private String message;
    private String messageColor;
    private String media;
    private String contentType;
    private String posterUrl;
    private String actionUrl;
    private String icon;
    private Boolean hasUrl;
    private String error;

    private CTAdUnitContent(String title, String titleColor, String message, String messageColor,
                            String icon, String media, String contentType, String posterUrl,
                            String actionUrl, boolean hasUrl, String error) {
        this.title = title;
        this.titleColor = titleColor;
        this.message = message;
        this.messageColor = messageColor;
        this.icon = icon;
        this.media = media;
        this.contentType = contentType;
        this.posterUrl = posterUrl;
        this.actionUrl = actionUrl;
        this.hasUrl = hasUrl;
        this.error = error;
    }


    private CTAdUnitContent(Parcel in) {
        title = in.readString();
        titleColor = in.readString();
        message = in.readString();
        messageColor = in.readString();
        icon = in.readString();
        media = in.readString();
        contentType = in.readString();
        posterUrl = in.readString();
        actionUrl = in.readString();
        hasUrl = in.readByte() != 0x00;
        error = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(titleColor);
        dest.writeString(message);
        dest.writeString(messageColor);
        dest.writeString(icon);
        dest.writeString(media);
        dest.writeString(contentType);
        dest.writeString(posterUrl);
        dest.writeString(actionUrl);
        dest.writeByte((byte) (hasUrl ? 0x01 : 0x00));
        dest.writeString(error);
    }

    /**
     * Converts jsonContent to ADUnitContent
     * @param contentObject - jsonObject
     * @return - CTAdUnitContent Obj
     */
    static CTAdUnitContent toContent(JSONObject contentObject) {
        try {
            String title = "", titleColor = "", message = "", messageColor = "",
                    icon = "", media = "", contentType = "", posterUrl = "",
                    actionUrl = "";

            boolean hasUrl = false;

            JSONObject titleObject = contentObject.has("title") ? contentObject.getJSONObject("title") : null;
            if (titleObject != null) {
                title = titleObject.has("text") ? titleObject.getString("text") : "";
                titleColor = titleObject.has("color") ? titleObject.getString("color") : "";
            }
            JSONObject msgObject = contentObject.has("message") ? contentObject.getJSONObject("message") : null;
            if (msgObject != null) {
                message = msgObject.has("text") ? msgObject.getString("text") : "";
                messageColor = msgObject.has("color") ? msgObject.getString("color") : "";
            }
            JSONObject iconObject = contentObject.has("icon") ? contentObject.getJSONObject("icon") : null;
            if (iconObject != null) {
                icon = iconObject.has("url") ? iconObject.getString("url") : "";
            }
            JSONObject mediaObject = contentObject.has("media") ? contentObject.getJSONObject("media") : null;
            if (mediaObject != null) {
                media = mediaObject.has("url") ? mediaObject.getString("url") : "";
                contentType = mediaObject.has("content_type") ? mediaObject.getString("content_type") : "";
                posterUrl = mediaObject.has("poster") ? mediaObject.getString("poster") : "";
            }

            JSONObject actionObject = contentObject.has("action") ? contentObject.getJSONObject("action") : null;
            if (actionObject != null) {
                hasUrl = actionObject.has("hasUrl") && actionObject.getBoolean("hasUrl");
                JSONObject urlObject = actionObject.has("url") ? actionObject.getJSONObject("url") : null;
                if (urlObject != null && hasUrl) {
                    JSONObject androidObject = urlObject.has("android") ? urlObject.getJSONObject("android") : null;
                    if (androidObject != null) {
                        actionUrl = androidObject.has("text") ? androidObject.getString("text") : "";
                    }
                }
            }

            return new CTAdUnitContent(title, titleColor, message, messageColor,
                    icon, media, contentType, posterUrl,
                    actionUrl, hasUrl, null);

        } catch (Exception e) {
            Logger.v("Unable to init CTAdUnitContent with JSON - " + e.getLocalizedMessage());
            return new CTAdUnitContent("", "", "", "", "", "", "", "", "", false, "Error Creating AdUnit Content from JSON : " + e.getLocalizedMessage());
        }
    }

    /**
     * @return the title section of the AdUnit Content
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the message section of the AdUnit Content
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the media URL of the AdUnit Content
     */
    @SuppressWarnings("unused")
    public String getMedia() {
        return media;
    }

    /**
     * @return the action URL of the body of the AdUnit Content
     */
    @SuppressWarnings("unused")
    public String getActionUrl() {
        return actionUrl;
    }

    /**
     * @return the URL as String for the icon in case of Icon Message template
     */
    @SuppressWarnings("unused")
    public String getIcon() {
        return icon;
    }

    /**
     * @return the hexcode value of the title color as String
     */
    @SuppressWarnings("unused")
    public String getTitleColor() {
        return titleColor;
    }

    /**
     * @return the hexcode value of the message color as String
     */
    public String getMessageColor() {
        return messageColor;
    }

    /**
     * @return URL for the thumbnail of the video
     */
    @SuppressWarnings("unused")
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * @return the content type of the media
     */
    @SuppressWarnings("unused")
    public String getContentType() {
        return contentType;
    }

    /**
     * Method to check whether media in the {@link CTAdUnitContent} object is an image.
     *
     * @return true if the media type is image
     * false if the media type is not an image
     */
    @SuppressWarnings("unused")
    public boolean mediaIsImage() {
        return contentType != null && this.media != null && contentType.startsWith("image") && !contentType.equals("image/gif");
    }

    /**
     * Method to check whether media in the {@link CTAdUnitContent} object is a GIF.
     *
     * @return true if the media type is GIF
     * false if the media type is not a GIF
     */
    @SuppressWarnings("unused")
    public boolean mediaIsGIF() {
        return contentType != null && this.media != null && contentType.equals("image/gif");
    }

    /**
     * Method to check whether media in the {@link CTAdUnitContent} object is a video.
     *
     * @return true if the media type is video
     * false if the media type is not a video
     */
    @SuppressWarnings("unused")
    public boolean mediaIsVideo() {
        return contentType != null && this.media != null && contentType.startsWith("video");
    }

    /**
     * Method to check whether media in the {@link CTAdUnitContent} object is an audio.
     *
     * @return true if the media type is audio
     * false if the media type is not an audio
     */
    @SuppressWarnings("unused")
    public boolean mediaIsAudio() {
        return contentType != null && this.media != null && contentType.startsWith("audio");
    }

    public String getError() {
        return error;
    }

    @SuppressWarnings("unused")
    public Boolean getHasUrl() {
        return hasUrl;
    }

    public static final Creator<CTAdUnitContent> CREATOR = new Creator<CTAdUnitContent>() {
        @Override
        public CTAdUnitContent createFromParcel(Parcel in) {
            return new CTAdUnitContent(in);
        }

        @Override
        public CTAdUnitContent[] newArray(int size) {
            return new CTAdUnitContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + "title:" + title + "titleColor:" + titleColor + "message:" + message + "messageColor:" + messageColor + "media:" + media + "contentType:" + contentType + "posterUrl:" + posterUrl + "actionUrl:" + actionUrl + "icon:" + icon + "hasUrl:" + hasUrl + "error:" + error + "]";
    }
}