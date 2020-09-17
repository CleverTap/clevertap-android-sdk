package com.clevertap.android.sdk.displayunits.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;

import org.json.JSONObject;

/**
 * Content class for holding Display Unit Content Data
 */
public class CleverTapDisplayUnitContent implements Parcelable {
    private String title;
    private String titleColor;
    private String message;
    private String messageColor;
    private String media;
    private String contentType;
    private String posterUrl;
    private String actionUrl;
    private String icon;
    private String error;

    private CleverTapDisplayUnitContent(String title, String titleColor, String message, String messageColor,
                                        String icon, String media, String contentType, String posterUrl,
                                        String actionUrl, String error) {
        this.title = title;
        this.titleColor = titleColor;
        this.message = message;
        this.messageColor = messageColor;
        this.icon = icon;
        this.media = media;
        this.contentType = contentType;
        this.posterUrl = posterUrl;
        this.actionUrl = actionUrl;
        this.error = error;
    }


    private CleverTapDisplayUnitContent(Parcel in) {
        title = in.readString();
        titleColor = in.readString();
        message = in.readString();
        messageColor = in.readString();
        icon = in.readString();
        media = in.readString();
        contentType = in.readString();
        posterUrl = in.readString();
        actionUrl = in.readString();
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
        dest.writeString(error);
    }

    /**
     * Converts jsonContent to DisplayUnitContent
     *
     * @param contentObject - jsonObject
     * @return - CleverTapDisplayUnitContent - can have empty fields with an error message in case of parsing error
     */
    static CleverTapDisplayUnitContent toContent(JSONObject contentObject) {
        try {
            String title = "", titleColor = "", message = "", messageColor = "",
                    icon = "", media = "", contentType = "", posterUrl = "",
                    actionUrl = "";


            JSONObject titleObject = contentObject.has(Constants.KEY_TITLE) ? contentObject.getJSONObject(Constants.KEY_TITLE) : null;
            if (titleObject != null) {
                title = titleObject.has(Constants.KEY_TEXT) ? titleObject.getString(Constants.KEY_TEXT) : "";
                titleColor = titleObject.has(Constants.KEY_COLOR) ? titleObject.getString(Constants.KEY_COLOR) : "";
            }
            JSONObject msgObject = contentObject.has(Constants.KEY_MESSAGE) ? contentObject.getJSONObject(Constants.KEY_MESSAGE) : null;
            if (msgObject != null) {
                message = msgObject.has(Constants.KEY_TEXT) ? msgObject.getString(Constants.KEY_TEXT) : "";
                messageColor = msgObject.has(Constants.KEY_COLOR) ? msgObject.getString(Constants.KEY_COLOR) : "";
            }
            JSONObject iconObject = contentObject.has(Constants.KEY_ICON) ? contentObject.getJSONObject(Constants.KEY_ICON) : null;
            if (iconObject != null) {
                icon = iconObject.has(Constants.KEY_URL) ? iconObject.getString(Constants.KEY_URL) : "";
            }
            JSONObject mediaObject = contentObject.has(Constants.KEY_MEDIA) ? contentObject.getJSONObject(Constants.KEY_MEDIA) : null;
            if (mediaObject != null) {
                media = mediaObject.has(Constants.KEY_URL) ? mediaObject.getString(Constants.KEY_URL) : "";
                contentType = mediaObject.has(Constants.KEY_CONTENT_TYPE) ? mediaObject.getString(Constants.KEY_CONTENT_TYPE) : "";
                posterUrl = mediaObject.has(Constants.KEY_POSTER_URL) ? mediaObject.getString(Constants.KEY_POSTER_URL) : "";
            }

            JSONObject actionObject = contentObject.has(Constants.KEY_ACTION) ? contentObject.getJSONObject(Constants.KEY_ACTION) : null;
            if (actionObject != null) {
                JSONObject urlObject = actionObject.has(Constants.KEY_URL) ? actionObject.getJSONObject(Constants.KEY_URL) : null;
                if (urlObject != null) {
                    JSONObject androidObject = urlObject.has(Constants.KEY_ANDROID) ? urlObject.getJSONObject(Constants.KEY_ANDROID) : null;
                    if (androidObject != null) {
                        actionUrl = androidObject.has(Constants.KEY_TEXT) ? androidObject.getString(Constants.KEY_TEXT) : "";
                    }
                }
            }

            return new CleverTapDisplayUnitContent(title, titleColor, message, messageColor,
                    icon, media, contentType, posterUrl,
                    actionUrl, null);

        } catch (Exception e) {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT,"Unable to init CleverTapDisplayUnitContent with JSON - " + e.getLocalizedMessage());
            return new CleverTapDisplayUnitContent("", "", "", "", "", "", "", "", "", "Error Creating DisplayUnit Content from JSON : " + e.getLocalizedMessage());
        }
    }

    /**
     * Getter for the title section of the Display Unit Content
     * @return String
     */
    public String getTitle() {
        return title;
    }

    /**
     * Getter for the message section of the Display Unit Content
     * @return String
     */
    public String getMessage() {
        return message;
    }

    /**
     * Getter for the media URL of the Display Unit Content
     * @return String
     */
    @SuppressWarnings("unused")
    public String getMedia() {
        return media;
    }

    /**
     * Getter for the action URL of the body of the Display Unit Content
     * @return String
     */
    @SuppressWarnings("unused")
    public String getActionUrl() {
        return actionUrl;
    }

    /**
     * Getter for the URL as String for the icon in case of Icon Message template
     * @return String
     */
    @SuppressWarnings("unused")
    public String getIcon() {
        return icon;
    }

    /**
     * Getter for the hex-code value of the title color e.g. #000000
     * @return String
     */
    @SuppressWarnings("unused")
    public String getTitleColor() {
        return titleColor;
    }

    /**
     * Getter for the hex-code value of the message color e.g. #000000
     * @return String
     */
    public String getMessageColor() {
        return messageColor;
    }

    /**
     * Getter for the URL for the thumbnail of the video
     * @return String
     */
    @SuppressWarnings("unused")
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Getter for the content type of the media(image/gif/audio/video etc.)
     *
     * Refer{@link #mediaIsImage()}, {@link #mediaIsGIF()},
     *      {@link #mediaIsAudio()} ,{@link #mediaIsVideo()}
     * @return String
     */
    @SuppressWarnings("unused")
    public String getContentType() {
        return contentType;
    }

    /**
     * Method to check whether media in the {@link CleverTapDisplayUnitContent} object is an image.
     *
     * @return boolean - | true, if the media type is image
     *                   | false, if the media type is not an image
     */
    @SuppressWarnings({"unused","WeakerAccess"})
    public boolean mediaIsImage() {
        return contentType != null && this.media != null && contentType.startsWith("image") && !contentType.equals("image/gif");
    }

    /**
     * Method to check whether media in the {@link CleverTapDisplayUnitContent} object is a GIF.
     *
     * @return boolean - | true, if the media type is GIF
     *                   | false, if the media type is not a GIF
     */
    @SuppressWarnings({"unused","WeakerAccess"})
    public boolean mediaIsGIF() {
        return contentType != null && this.media != null && contentType.equals("image/gif");
    }

    /**
     * Method to check whether media in the {@link CleverTapDisplayUnitContent} object is a video.
     *
     * @return boolean - | true, if the media type is video
     *                   | false, if the media type is not a video
     */
    @SuppressWarnings({"unused","WeakerAccess"})
    public boolean mediaIsVideo() {
        return contentType != null && this.media != null && contentType.startsWith("video");
    }

    /**
     * Method to check whether media in the {@link CleverTapDisplayUnitContent} object is an audio.
     *
     * @return boolean - | true, if the media type is audio
     *                   | false, if the media type is not an audio
     */
    @SuppressWarnings({"unused","WeakerAccess"})
    public boolean mediaIsAudio() {
        return contentType != null && this.media != null && contentType.startsWith("audio");
    }

    public String getError() {
        return error;
    }

    public static final Creator<CleverTapDisplayUnitContent> CREATOR = new Creator<CleverTapDisplayUnitContent>() {
        @Override
        public CleverTapDisplayUnitContent createFromParcel(Parcel in) {
            return new CleverTapDisplayUnitContent(in);
        }

        @Override
        public CleverTapDisplayUnitContent[] newArray(int size) {
            return new CleverTapDisplayUnitContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + " title:" + title + ", titleColor:" + titleColor + " message:" + message + ", messageColor:" + messageColor + ", media:" + media + ", contentType:" + contentType + ", posterUrl:" + posterUrl + ", actionUrl:" + actionUrl + ", icon:" + icon + ", error:" + error + " ]";
    }
}