package com.clevertap.android.sdk.inapp;

import android.os.Parcel;
import android.os.Parcelable;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

public class CTInAppNotificationMedia implements Parcelable {

    public static final Creator<CTInAppNotificationMedia> CREATOR = new Creator<CTInAppNotificationMedia>() {
        @Override
        public CTInAppNotificationMedia createFromParcel(Parcel in) {
            return new CTInAppNotificationMedia(in);
        }

        @Override
        public CTInAppNotificationMedia[] newArray(int size) {
            return new CTInAppNotificationMedia[size];
        }
    };

    int orientation;

    private String cacheKey;

    private String contentType;

    private String mediaUrl;

    CTInAppNotificationMedia() {
    }

    private CTInAppNotificationMedia(Parcel in) {
        mediaUrl = in.readString();
        contentType = in.readString();
        cacheKey = in.readString();
        orientation = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getOrientation() {
        return orientation;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mediaUrl);
        dest.writeString(contentType);
        dest.writeString(cacheKey);
        dest.writeInt(orientation);
    }

    String getCacheKey() {
        return cacheKey;
    }

    String getContentType() {
        return contentType;
    }

    String getMediaUrl() {
        return mediaUrl;
    }

    @SuppressWarnings("SameParameterValue")
    void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    CTInAppNotificationMedia initWithJSON(JSONObject mediaObject, int orientation) {
        this.orientation = orientation;
        try {
            this.contentType = mediaObject.has(Constants.KEY_CONTENT_TYPE) ? mediaObject
                    .getString(Constants.KEY_CONTENT_TYPE) : "";
            String mediaUrl = mediaObject.has(Constants.KEY_URL) ? mediaObject.getString(Constants.KEY_URL) : "";
            if (!mediaUrl.isEmpty()) {
                if (this.contentType.startsWith("image")) {
                    this.mediaUrl = mediaUrl;
                    if (mediaObject.has("key")) {
                        this.cacheKey = UUID.randomUUID().toString() + mediaObject.getString("key");
                    } else {
                        this.cacheKey = UUID.randomUUID().toString();
                    }
                } else {
                    this.mediaUrl = mediaUrl;
                }
            }
        } catch (JSONException e) {
            Logger.v("Error parsing Media JSONObject - " + e.getLocalizedMessage());
        }
        if (contentType.isEmpty()) {
            return null;
        } else {
            return this;
        }
    }

    boolean isAudio() {
        String contentType = this.getContentType();
        return contentType != null && this.mediaUrl != null && contentType.startsWith("audio");
    }

    boolean isGIF() {
        String contentType = this.getContentType();
        return contentType != null && this.mediaUrl != null && contentType.equals("image/gif");
    }

    boolean isImage() {
        String contentType = this.getContentType();
        return contentType != null && this.mediaUrl != null && contentType.startsWith("image") && !contentType
                .equals("image/gif");
    }

    boolean isVideo() {
        String contentType = this.getContentType();
        return contentType != null && this.mediaUrl != null && contentType.startsWith("video");
    }
}
