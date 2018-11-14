package com.clevertap.android.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CTInboxMessage {
    String id;
    String title;
    String body;
    String imageUrl;
    String actionUrl;
    JSONObject kv;
    String date;
    String expires;
    boolean isRead;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public JSONObject getKv() {
        return kv;
    }

    public void setKv(JSONObject kv) {
        this.kv = kv;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public CTInboxMessage initWithJSON(JSONObject jsonObject){

        try {
            if (jsonObject.has("id")) {
                this.id = jsonObject.getString("id");
            }
            if(jsonObject.has("title")){
                this.title = jsonObject.getString("title");
            }
            if(jsonObject.has("body")){
                this.body = jsonObject.getString("body");
            }
            if(jsonObject.has("imageUrl")){
                this.imageUrl = jsonObject.getString("imageUrl");
            }
            if(jsonObject.has("actionUrl")){
                this.actionUrl = jsonObject.getString("actionurl");
            }
            if(jsonObject.has("kv")){
                this.kv = jsonObject.getJSONObject("kv");
            }
            if(jsonObject.has("date")){
                this.date = jsonObject.getString("date");
            }
            if(jsonObject.has("expires")){
                this.expires = jsonObject.getString("expires");
            }
            if(jsonObject.has("isRead")){
                this.isRead = jsonObject.getBoolean("isRead");
            }
            return this;
        }catch (JSONException e){
            return null;
        }
    }
}
