package com.clevertap.android.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class CTUserDAO {
    private String userId;
    private String accountId;
    private String guid;
    private JSONArray newMessages;
    private DBAdapter dbAdapter;

    String getUserId() {
        return userId;
    }

    void setUserId(String userId) {
        this.userId = userId;
    }

    String getAccountId() {
        return accountId;
    }

    void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    String getGuid() {
        return guid;
    }

    void setGuid(String guid) {
        this.guid = guid;
    }

    JSONArray getNewMessages() {
        return newMessages;
    }

    void setNewMessages(JSONArray newMessages) {
        this.newMessages = newMessages;
    }

    CTUserDAO(String accountId, String guid, String userId){
        this.accountId = accountId;
        this.guid = guid;
        this.userId = userId;
    }

}
