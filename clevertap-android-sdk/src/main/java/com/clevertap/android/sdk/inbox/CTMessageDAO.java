package com.clevertap.android.sdk.inbox;

import org.json.JSONException;
import org.json.JSONObject;

public class CTMessageDAO {
    private String id;
    private JSONObject jsonData;
    private boolean read;
    private int date;
    private int expires;
    private String userId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JSONObject getJsonData() {
        return jsonData;
    }

    public void setJsonData(JSONObject jsonData) {
        this.jsonData = jsonData;
    }

    public int isRead() {
        if(read){
            return 1;
        }else{
            return 0;
        }
    }

    public void setRead(int read) {
        if(read == 1)
            this.read = true;
        else
            this.read = false;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getExpires() {
        return expires;
    }

    public void setExpires(int expires) {
        this.expires = expires;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    CTMessageDAO(String id, JSONObject jsonData, boolean read, int date, int expires, String userId){
        this.id = id;
        this.jsonData = jsonData;
        this.read = read;
        this.date = date;
        this.expires = expires;
        this.userId = userId;
    }

    static CTMessageDAO initWithJSON(JSONObject inboxMessage, String userId){
        try {
            String id = inboxMessage.has("id") ? inboxMessage.getString("id") : null;
            int date = inboxMessage.has("epoch") ? inboxMessage.getInt("epoch") : -1;
            int expires = inboxMessage.has("ttl") ? inboxMessage.getInt("ttl") : -1;
            return new CTMessageDAO(id, inboxMessage, false,date,expires,userId);
        }catch (JSONException e){
            //TODO Logging
            return null;
        }
    }
}
