package com.clevertap.android.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


// TODO handle isRead and TTL cases ????  particlarly when there is no updatedMessages coming from the server
/**
 * Controller class which handles Users and Messages for the Notification Inbox
 */
class CTInboxController {
    private int count;
    private int unreadCount;
    private ArrayList<CTMessageDAO> messages,unreadMessages;
    private String userId;
    private CTUserDAO userDAO;
    private DBAdapter dbAdapter;
    static boolean exoPlayerPresent = false;

    CTInboxController(String accountId, String guid, DBAdapter adapter){
        this.userId = accountId + guid;
        this.dbAdapter = adapter;
        this.userDAO = this.dbAdapter.fetchOrCreateUser(this.userId, accountId, guid);
        this.messages = this.dbAdapter.getMessages(this.userId);
        this.unreadMessages = this.dbAdapter.getUnreadMessages(this.userId);
        this.count = this.messages.size();
        this.unreadCount = this.unreadMessages.size();
        exoPlayerPresent = checkForExoPlayer();
    }

    boolean updateMessages(JSONArray inboxMessages){
        return updateUserMessages(inboxMessages);
    }

    boolean deleteMessageWithId(String messageId){
        CTMessageDAO messageDAO = getMessageDaoForId(messageId);
        if (messageDAO == null) {
            return false;
        }
        return this.dbAdapter.deleteMessageForId(messageId);
    }

    boolean markReadForMessageWithId(String messageId){
        CTMessageDAO messageDAO = getMessageDaoForId(messageId);
        if(messageDAO == null) {
            return false;
        }
        boolean marked = this.dbAdapter.markReadMessageForId(messageId);
        this.messages = this.dbAdapter.getMessages(this.userId);
        this.unreadMessages = this.dbAdapter.getUnreadMessages(this.userId);
        this.count = messages.size();
        this.unreadCount = unreadMessages.size();
        return marked;
    }

    JSONObject getMessageForId(String messageId){
        for(CTMessageDAO messageDAO : messages){
            if(messageDAO.getId().equals(messageId)){
                return messageDAO.toJSON();
            }
        }
        Logger.d("Inbox Message for message id - "+messageId+" doesn't exist");
        return null;
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
        return messages;
    }

    ArrayList<CTMessageDAO> getUnreadMessages(){
        return unreadMessages;
    }

    private boolean updateUserMessages(JSONArray inboxMessages){
        userDAO.setNewMessages(inboxMessages);
        boolean haveUpdates = false;
        ArrayList<CTMessageDAO> messageDAOArrayList = new ArrayList<>();
        ArrayList<CTMessageDAO> updateMessageList = new ArrayList<>();

        for(int i=0;i<inboxMessages.length();i++){
            try {
                JSONObject inboxMessage = inboxMessages.getJSONObject(i);
                if(!inboxMessage.has("_id")){
                    Logger.d("Notification inbox message doesn't have _id");
                    break;
                }

                CTMessageDAO messageDAO = CTMessageDAO.initWithJSON(inboxMessage, userDAO.getUserId());

                if(messageDAO != null) {
                    if(!exoPlayerPresent){
                        if(new CTInboxMessage().initWithJSON(messageDAO.toJSON()).getInboxMessageContents().get(0).mediaIsVideo()){
                            Logger.d("Dropping inbox messages containing videos since Exoplayer files are missing. For more information checkout CleverTap documentation.");
                            continue;
                        }
                    }
                    messageDAOArrayList.add(messageDAO);
                    Logger.d("Notification Inbox Message not present, adding values");
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

        this.dbAdapter.cleanUpMessages(this.userId);  // TODO you have to check for TTL even if there are no new user messages coming from the server; same for unread vs read

        this.messages = this.dbAdapter.getMessages(this.userId);
        this.unreadMessages = this.dbAdapter.getUnreadMessages(this.userId);
        this.count = messages.size();
        this.unreadCount = unreadMessages.size();

        return haveUpdates;
    }


    private boolean checkForExoPlayer(){
        boolean exoPlayerPresent = false;
        Class className = null;
        try{
            className = Class.forName("com.google.android.exoplayer2.ExoPlayerFactory");
            className = Class.forName("com.google.android.exoplayer2.source.hls.HlsMediaSource");
            className = Class.forName("com.google.android.exoplayer2.ui.PlayerView");
            Logger.d("ExoPlayer is present");
            exoPlayerPresent = true;
        }catch (Throwable t){
            Logger.d("ExoPlayer library files are missing!!!");
            Logger.d("Please add ExoPlayer dependencies to render Inbox messages playing video. For more information checkout CleverTap documentation.");
            if(className!=null)
                Logger.d("ExoPlayer classes not found "+className.getName());
            else
                Logger.d("ExoPlayer classes not found");
        }
        return exoPlayerPresent;
    }
}
