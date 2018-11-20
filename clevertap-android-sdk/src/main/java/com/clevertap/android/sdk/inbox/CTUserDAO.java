package com.clevertap.android.sdk.inbox;

import android.database.sqlite.SQLiteDatabase;

import com.clevertap.android.sdk.CTInAppBaseFragment;
import com.clevertap.android.sdk.DBAdapter;
import com.clevertap.android.sdk.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class CTUserDAO {
    private String userId;
    private String accountId;
    private String guid;
    private JSONArray newMessages;
    private DBAdapter dbAdapter;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public JSONArray getNewMessages() {
        return newMessages;
    }

    public void setNewMessages(JSONArray newMessages) {
        this.newMessages = newMessages;
    }

    CTUserDAO(String accountId, String guid, String userId, DBAdapter dbAdapter){
        this.accountId = accountId;
        this.guid = guid;
        this.userId = userId;
        this.dbAdapter = dbAdapter;
    }

    public boolean updateMessages(JSONArray inboxMessages){
        newMessages = inboxMessages;
        boolean haveUpdates = false;
        for(int i=0;i<newMessages.length();i++){
            try {
                JSONObject inboxMessage = newMessages.getJSONObject(i);
                if(!inboxMessage.has("id")){
                    //TODO Logging
                }

                //TODO Duplicating logic

                CTMessageDAO messageDAO = CTMessageDAO.initWithJSON(inboxMessage, this.userId);
                ArrayList<CTMessageDAO> messageDAOArrayList = new ArrayList<>();
                if(messageDAO!=null) {
                    messageDAOArrayList.add(messageDAO);
                }
                if(messageDAOArrayList.size()>0){
                    this.dbAdapter.storeMessagesForUser(messageDAOArrayList);
                    haveUpdates = true;
                }
            }catch (JSONException e){
                //TODO logging
            }
        }
        return haveUpdates;
    }
}
