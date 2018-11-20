package com.clevertap.android.sdk.inbox;

import com.clevertap.android.sdk.DBAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class InboxController {
    private boolean initialized;
    private int count;
    private int unreadCount;
    private HashMap messages,unreadMessages;
    private String accountId;
    private String guid;
    private String userId;
    private boolean userTableCreated = false;
    private boolean messagesTableCreated = false;
    public InboxUpdateListener listener;
    private CTUserDAO userDAO;
    private DBAdapter dbAdapter;

    private InboxController(String accountId, String guid, DBAdapter adapter){
        this.accountId = accountId;
        this.guid = guid;
        this.initialized = true;
        this.userId = this.accountId + this.guid;
        this.dbAdapter = adapter;
        if (!userTableCreated) {
            userTableCreated = this.dbAdapter.createUserTable();
        }
        if (userTableCreated) {
            userDAO = new CTUserDAO(this.accountId,this.guid,this.userId,adapter);
            int returnCode = this.dbAdapter.storeInboxUser(userDAO);
        }
    }

    public static InboxController initWithAccountId(String accountId, String guid, DBAdapter adapter){
        try{
            return new InboxController(accountId,guid, adapter);
        }catch (Throwable t){
            return null;
        }
    }

    public void updateMessages(JSONArray inboxMessages){
        if(!this.isInitialized()) return;

        boolean haveUpdates = userDAO.updateMessages(inboxMessages);
        if(haveUpdates){
            notifyUpdate();
        }
    }

    public void deleteMessageWithId(String messageId){
        CTMessageDAO messageDAO = getMessageForId(messageId);
        if(messageDAO!=null){
            boolean deletedMessage =  this.dbAdapter.deleteMessageForId(messageId);
            if(deletedMessage) {
                notifyUpdate();
            }
        }
    }

    public void markReadForMessageWithId(String messageId){
        CTMessageDAO messageDAO = getMessageForId(messageId);
        if(messageDAO != null){
            boolean marked = this.dbAdapter.markReadMessageForId(messageId);
            if(marked){
                notifyUpdate();
            }
        }
    }

    private CTMessageDAO getMessageForId(String messageId){
        if(this.isInitialized())
            return this.dbAdapter.getMessageForId(messageId);
        else
            return null;
    }

    public int count(){
        if(this.isInitialized()){
            return userDAO.getNewMessages().length();
        }else{
            return -1;
        }
    }

    public int unreadCount(){
        if(this.isInitialized()){
            return this.dbAdapter.getUnreadCount();
        }else{
            return -1;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    int getCount() {
        return count;
    }

    int getUnreadCount() {
        return unreadCount;
    }

    HashMap getMessages() {
        return messages;
    }

    HashMap getUnreadMessages() {
        return unreadMessages;
    }

    String getAccountId() {
        return accountId;
    }

    String getGuid() {
        return guid;
    }

    String getUserId() {
        return userId;
    }

    boolean isUserTableCreated() {
        return userTableCreated;
    }

    InboxUpdateListener getListener() {
        return listener;
    }

    private void notifyUpdate(){
        if(listener!=null){
            listener.inboxMessagesDidUpdate();
        }
    }
}
