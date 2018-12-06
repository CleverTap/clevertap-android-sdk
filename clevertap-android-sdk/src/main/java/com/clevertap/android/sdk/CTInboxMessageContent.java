package com.clevertap.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CTInboxMessageContent implements Parcelable {

    private String title;
    private String message;
    private String media;
    private String actionType;
    private String actionUrl;
    private String icon;
    private JSONArray links;

    CTInboxMessageContent(){}

    CTInboxMessageContent initWithJSON(JSONObject contentObject){
        try {
            this.title = contentObject.has("title") ? contentObject.getString("title") : "";
            this.message = contentObject.has("message") ? contentObject.getString("message") : "";
            this.icon = contentObject.has("icon") ? contentObject.getString("icon") : "";
            this.media = contentObject.has("media") ? contentObject.getString("media") : "";
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
        message = in.readString();
        media = in.readString();
        actionType = in.readString();
        actionUrl = in.readString();
        icon = in.readString();
        try {
            links = in.readByte() == 0x00 ? null : new JSONArray(in.readString());
        } catch (JSONException e) {
            Logger.v("Unable to init CTInboxMessageContent with Parcel - "+e.getLocalizedMessage());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(message);
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
}
