package com.clevertap.android.sdk;

import org.json.JSONException;
import org.json.JSONObject;

class CTMessageDAO {
    private String id;
    private JSONObject jsonData;
    private boolean read;
    private int date;
    private int expires;
    private String userId;

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    JSONObject getJsonData() {
        return jsonData;
    }

    void setJsonData(JSONObject jsonData) {
        this.jsonData = jsonData;
    }

    int isRead() {
        if(read){
            return 1;
        }else{
            return 0;
        }
    }

    void setRead(int read) {
        if(read == 1)
            this.read = true;
        else
            this.read = false;
    }

    int getDate() {
        return date;
    }

    void setDate(int date) {
        this.date = date;
    }

    int getExpires() {
        return expires;
    }

    void setExpires(int expires) {
        this.expires = expires;
    }

    String getUserId() {
        return userId;
    }

    void setUserId(String userId) {
        this.userId = userId;
    }

    CTMessageDAO(){}

    private CTMessageDAO(String id, JSONObject jsonData, boolean read, int date, int expires, String userId){
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
            int date = inboxMessage.has("date") ? inboxMessage.getInt("date") : -1;
            int expires = inboxMessage.has("ttl") ? inboxMessage.getInt("ttl") : -1;
            return new CTMessageDAO(id, inboxMessage, false,date,expires,userId);
        }catch (JSONException e){
            Logger.d("Unable to parse Notification inbox message to CTMessageDao - "+e.getLocalizedMessage());
            return null;
        }
    }

    JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id",this.id);
            jsonObject.put("json",this.jsonData);
            jsonObject.put("read",this.read);
            jsonObject.put("date",this.date);
            jsonObject.put("ttl",this.expires);
            return jsonObject;
        } catch (JSONException e) {
            Logger.v("Unable to convert CTMessageDao to JSON - "+e.getLocalizedMessage());
            return jsonObject;
        }
    }
}
