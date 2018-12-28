package com.clevertap.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CTInboxMessageContent implements Parcelable {

    private String title;
    private String titleColor;
    private String message;
    private String messageColor;
    private String media;
    private String actionType;
    private String actionUrl;
    private String icon;
    private JSONArray links;
    private String contentType;

    CTInboxMessageContent(){}

    CTInboxMessageContent initWithJSON(JSONObject contentObject){
        try {
            JSONObject titleObject = contentObject.has("title") ? contentObject.getJSONObject("title") : null;
            if(titleObject != null) {
                this.title = titleObject.has("text") ? titleObject.getString("text") : "";
                this.titleColor = titleObject.has("color") ? titleObject.getString("color") : "";
            }
            JSONObject msgObject = contentObject.has("message") ? contentObject.getJSONObject("message") : null;
            if(msgObject != null) {
                this.message = msgObject.has("text") ? msgObject.getString("text") : "";
                this.messageColor = msgObject.has("color") ? msgObject.getString("color") : "";
            }
            JSONObject iconObject = contentObject.has("icon") ? contentObject.getJSONObject("icon") : null;
            if(iconObject != null){
                this.icon =  contentObject.has("url") ? contentObject.getString("url") : "";
            }
            JSONObject mediaObject = contentObject.has("media") ? contentObject.getJSONObject("media") : null;
            if(mediaObject != null){
                this.media = mediaObject.has("url") ? mediaObject.getString("url") : "";
                this.contentType = mediaObject.has("content_type") ? mediaObject.getString("content_type") : "";
            }

            JSONObject action = contentObject.has("action") ? contentObject.getJSONObject("action") : null;
            if(action != null){
                this.actionType = action.has("type") ? action.getString("type") : "";
                JSONObject urlObject = action.has("url") ? action.getJSONObject("url") : null;
                if(urlObject != null){
                    this.actionUrl = urlObject.has("android") ? urlObject.getString("android") : "";
                }
            }
            this.links = contentObject.has("links") ? contentObject.getJSONArray("links") : null;

        } catch (JSONException e) {
            Logger.v("Unable to init CTInboxMessageContent with JSON - "+e.getLocalizedMessage());
        }
        return this;
    }

    protected CTInboxMessageContent(Parcel in) {
        title = in.readString();
        titleColor = in.readString();
        message = in.readString();
        messageColor = in.readString();
        media = in.readString();
        actionType = in.readString();
        actionUrl = in.readString();
        icon = in.readString();
        try {
            links = in.readByte() == 0x00 ? null : new JSONArray(in.readString());
        } catch (JSONException e) {
            Logger.v("Unable to init CTInboxMessageContent with Parcel - "+e.getLocalizedMessage());
        }
        contentType = in.readString();
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
        dest.writeString(actionType);
        dest.writeString(actionUrl);
        dest.writeString(icon);
        if (links == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(links.toString());
        }
        dest.writeString(contentType);
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public JSONArray getLinks() {
        return links;
    }

    public void setLinks(JSONArray links) {
        this.links = links;
    }

    public String getTitleColor() {
        return titleColor;
    }

    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    public String getMessageColor() {
        return messageColor;
    }

    public void setMessageColor(String messageColor) {
        this.messageColor = messageColor;
    }

    public String getLinktype(JSONObject jsonObject){
        if(jsonObject == null) return null;
        try {
            return jsonObject.has("type") ? jsonObject.getString("type") : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Type with JSON - "+e.getLocalizedMessage());
            return null;
        }
    }

    public String getLinkText(JSONObject jsonObject){
        if(jsonObject == null) return null;
        try {
            return jsonObject.has("text") ? jsonObject.getString("text") : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text with JSON - "+e.getLocalizedMessage());
            return null;
        }
    }

    public String getLinkUrl(JSONObject jsonObject){
        if(jsonObject == null) return null;
        try {
            JSONObject urlObject =  jsonObject.has("url") ? jsonObject.getJSONObject("url") : null;
            if(urlObject == null) return null;
            return urlObject.has("android") ? urlObject.getString("android") : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link URL with JSON - "+e.getLocalizedMessage());
            return null;
        }
    }

    public String getLinkColor(JSONObject jsonObject){
        if(jsonObject == null) return null;
        try {
            return jsonObject.has("color") ? jsonObject.getString("color") : "";
        } catch (JSONException e) {
            Logger.v("Unable to get Link Text Color with JSON - "+e.getLocalizedMessage());
            return null;
        }
    }

    public String getContentType() {
        return contentType;
    }

    public boolean mediaIsImage() {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.startsWith("image") && !contentType.equals("image/gif");
    }

    public boolean mediaIsGIF () {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.equals("image/gif");
    }

    public boolean mediaIsVideo () {
        String contentType = this.getContentType();
        return contentType != null && this.media != null && contentType.startsWith("video");
    }
}
