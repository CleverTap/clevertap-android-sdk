package com.clevertap.android.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Controller class which handles Users and Messages for the Notification Inbox
 */
class CTInboxController {
    private boolean initialized;
    private int count;
    private int unreadCount;
    private ArrayList<CTMessageDAO> messages,unreadMessages;
    private String accountId;
    private String guid;
    private String userId;
    CTInboxListener listener;
    CTInboxPrivateListener privateListener;
    private CTUserDAO userDAO;
    private DBAdapter dbAdapter;

    private CTInboxController(String accountId, String guid, DBAdapter adapter){
        this.accountId = accountId;
        this.guid = guid;
        this.userId = this.accountId + this.guid;
        this.dbAdapter = adapter;
        this.userDAO = this.dbAdapter.fetchOrCreateUser(this.userId,this.accountId,this.guid);
        this.messages = this.dbAdapter.getMessages(this.userId);
        this.unreadMessages = this.dbAdapter.getUnreadMessages(this.userId);
        this.count = this.messages.size();
        this.unreadCount = this.unreadMessages.size();
        this.initialized = true;
    }

    static CTInboxController initWithAccountId(String accountId, String guid, DBAdapter adapter){
        try{
            return new CTInboxController(accountId,guid, adapter);
        }catch (Throwable t){
            return null;
        }
    }

    void updateMessages(JSONArray inboxMessages){
        if(!this.isInitialized()) return;

        boolean haveUpdates = updateUserMessages(inboxMessages);
        if(haveUpdates){
            notifyUpdate();
        }
    }

    void deleteMessageWithId(String messageId){
        CTMessageDAO messageDAO = getMessageDaoForId(messageId);
        if(messageDAO!=null){
            boolean deletedMessage =  this.dbAdapter.deleteMessageForId(messageId);
            if(deletedMessage) {
                notifyUpdate();
            }
        }
    }

    void markReadForMessageWithId(String messageId){
        CTMessageDAO messageDAO = getMessageDaoForId(messageId);
        if(messageDAO != null){
            boolean marked = this.dbAdapter.markReadMessageForId(messageId);
            if(marked){
                notifyUpdate();
            }
        }
    }

    JSONObject getMessageForId(String messageId){
        if(this.isInitialized()) {
            for(CTMessageDAO messageDAO : messages){
                if(messageDAO.getId().equals(messageId)){
                    return messageDAO.toJSON();
                }
            }
            Logger.d("Inbox Message for message id - "+messageId+" doesn't exist");
            return null;
        }
        else {
            return null;
        }
    }

    private CTMessageDAO getMessageDaoForId(String messageId){
        return this.dbAdapter.getMessageForId(messageId);
    }

    int count(){
        return count;
    }

    int unreadCount(){
        return unreadCount;
    }

    ArrayList<CTMessageDAO> getMessages(){
        if(this.isInitialized()){
            return messages;
        }else{
            return null;
        }
    }

    ArrayList<CTMessageDAO> getUnreadMessages(){
        if(this.isInitialized()){
            return unreadMessages;
        }else{
            return null;
        }
    }

    boolean isInitialized() {
        return initialized;
    }

    CTInboxListener getListener() {
        return listener;
    }

    private void notifyUpdate(){
        if(listener!=null){
            listener.inboxMessagesDidUpdate();
        }
        if(privateListener != null){
            privateListener.privateInboxMessagesDidUpdate();
        }
    }

    void notifyInitialized(){
        if(privateListener != null){
            privateListener.privateInboxDidInitialize();
        }
    }

    /**
     * Adds or updates inbox messages
     * @param inboxMessages
     * @return
     */
    private boolean updateUserMessages(JSONArray inboxMessages){
        userDAO.setNewMessages(inboxMessages);
        boolean haveUpdates = false;
        ArrayList<CTMessageDAO> messageDAOArrayList = new ArrayList<>();
        ArrayList<CTMessageDAO> updateMessageList = new ArrayList<>();
        for(int i=0;i<inboxMessages.length();i++){
            try {
                JSONObject inboxMessage = inboxMessages.getJSONObject(i);
                if(!inboxMessage.has("_id")){
                    Logger.d("Notification inbox message doesn't have _id, adding for test message");
                    inboxMessage.put("_id","000");
                }

                CTMessageDAO messageDAO = CTMessageDAO.initWithJSON(inboxMessage, userDAO.getUserId());

                if(messageDAO != null) {
                    if (getMessageDaoForId(inboxMessage.getString("_id")).getId()!=null) {
                        if(getMessageDaoForId(inboxMessage.getString("_id")).getId().equals(inboxMessage.getString("_id"))) {
                            Logger.d("Notification Inbox Message already present, updating values");
                            updateMessageList.add(messageDAO);
                        }
                    }else{
                        messageDAOArrayList.add(messageDAO);
                        Logger.d("Notification Inbox Message not present, adding values");
                    }
                }

            }catch (JSONException e){
                Logger.d("Unable to update notification inbox messages - "+e.getLocalizedMessage());
            }
        }

        if(messageDAOArrayList.size()>0){
            this.dbAdapter.storeMessagesForUser(messageDAOArrayList);
            haveUpdates = true;
            Logger.d("Notification Inbox messages added");
        }

        if(updateMessageList.size()>0){
            this.dbAdapter.updateMessagesForUser(updateMessageList);
            haveUpdates = true;
            Logger.d("Notification Inbox messages updated");
        }

        //TODO uncomment in the end
        //this.dbAdapter.cleanUpMessages(this.userId);

        this.messages = this.dbAdapter.getMessages(this.userId);
        this.unreadMessages = this.dbAdapter.getUnreadMessages(this.userId);
        this.count = messages.size();
        this.unreadCount = unreadMessages.size();

        return haveUpdates;
    }
}
