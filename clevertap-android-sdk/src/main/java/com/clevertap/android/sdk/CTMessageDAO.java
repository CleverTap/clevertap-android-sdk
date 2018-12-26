package com.clevertap.android.sdk;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Message Data Access Object class interfacing with Database
 */
class CTMessageDAO {
    private String id;
    private JSONObject jsonData;
    private boolean read;
    private int date;
    private int expires;
    private String userId;
    private List<String> tags = new ArrayList<>();
    private String campaignId;

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

    String getTags() {
        return TextUtils.join(",",tags);
    }

    void setTags(String tags) {
        String[] tagsArray = tags.split(",");
        this.tags.addAll(Arrays.asList(tagsArray));

    }

    String getCampaignId() {
        return campaignId;
    }

    void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    CTMessageDAO(){}

    private CTMessageDAO(String id, JSONObject jsonData, boolean read, int date, int expires, String userId, JSONArray jsonArray, String campaignId){
        this.id = id;
        this.jsonData = jsonData;
        this.read = read;
        this.date = date;
        this.expires = expires;
        this.userId = userId;
        if(jsonArray != null){
            for(int i =0; i< jsonArray.length(); i++)
            {
                try {
                    this.tags.add(jsonArray.getString(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        this.campaignId = campaignId;
    }

    static CTMessageDAO initWithJSON(JSONObject inboxMessage, String userId){
        try {
            String id = inboxMessage.has("_id") ? inboxMessage.getString("_id") : null;
            int date = inboxMessage.has("date") ? inboxMessage.getInt("date") : -1;
            int expires = inboxMessage.has("ttl") ? inboxMessage.getInt("ttl") : -1;
            JSONObject cellObject = inboxMessage.has("cell") ? inboxMessage.getJSONObject("cell") : null;
            JSONArray jsonArray = inboxMessage.has("tags") ? inboxMessage.getJSONArray("tags") : null;
            String campaignId = inboxMessage.has("wzrk_id") ? inboxMessage.getString("wzrk_id") : null;
            return new CTMessageDAO(id, cellObject, false,date,expires,userId, jsonArray,campaignId);
        }catch (JSONException e){
            Logger.d("Unable to parse Notification inbox message to CTMessageDao - "+e.getLocalizedMessage());
            return null;
        }
    }

    JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id",this.id);
            jsonObject.put("cell",this.jsonData);
            jsonObject.put("isRead",this.read);
            jsonObject.put("date",this.date);
            jsonObject.put("ttl",this.expires);
            JSONArray jsonArray = new JSONArray();
            for(int i=0; i<this.tags.size(); i++){
                jsonArray.put(tags.get(i));
            }
            jsonObject.put("tags",jsonArray);
            jsonObject.put("wzrk_id",campaignId);
            return jsonObject;
        } catch (JSONException e) {
            Logger.v("Unable to convert CTMessageDao to JSON - "+e.getLocalizedMessage());
            return jsonObject;
        }
    }
}
