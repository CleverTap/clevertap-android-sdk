package com.clevertap.android.sdk;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Controller class which handles Users and Messages for the Notification Inbox
 */
class CTInboxController {

    private static ExecutorService es;

    private static long EXECUTOR_THREAD_ID = 0;

    private DBAdapter dbAdapter;

    private ArrayList<CTMessageDAO> messages;

    private final Object messagesLock = new Object();

    private String userId;

    private boolean videoSupported;

    // always call async
    CTInboxController(String guid, DBAdapter adapter, boolean videoSupported) {
        this.userId = guid;
        this.dbAdapter = adapter;
        this.messages = this.dbAdapter.getMessages(this.userId);
        this.videoSupported = videoSupported;
        if (es == null) {
            es = Executors.newFixedThreadPool(1);
        }
    }

    int count() {
        return getMessages().size();
    }

    boolean deleteMessageWithId(final String messageId) {
        CTMessageDAO messageDAO = findMessageById(messageId);
        if (messageDAO == null) {
            return false;
        }
        synchronized (messagesLock) {
            this.messages.remove(messageDAO);
        }
        postAsyncSafely("RunDeleteMessage", new Runnable() {
            @Override
            public void run() {
                dbAdapter.deleteMessageForId(messageId, userId);
            }
        });
        return true;
    }

    CTMessageDAO getMessageForId(String messageId) {
        return findMessageById(messageId);
    }

    ArrayList<CTMessageDAO> getMessages() {
        synchronized (messagesLock) {
            trimMessages();
            return messages;
        }
    }

    ArrayList<CTMessageDAO> getUnreadMessages() {
        ArrayList<CTMessageDAO> unread = new ArrayList<>();
        synchronized (messagesLock) {
            ArrayList<CTMessageDAO> messages = getMessages();
            for (CTMessageDAO message : messages) {
                if (message.isRead() == 0) {
                    unread.add(message);
                }
            }
        }
        return unread;
    }

    boolean markReadForMessageWithId(final String messageId) {
        CTMessageDAO messageDAO = findMessageById(messageId);
        if (messageDAO == null) {
            return false;
        }

        synchronized (messagesLock) {
            messageDAO.setRead(1);
        }

        postAsyncSafely("RunMarkMessageRead", new Runnable() {
            @Override
            public void run() {
                dbAdapter.markReadMessageForId(messageId, userId);
            }
        });
        return true;
    }

    int unreadCount() {
        return getUnreadMessages().size();
    }

    // always call async
    boolean updateMessages(final JSONArray inboxMessages) {
        boolean haveUpdates = false;
        ArrayList<CTMessageDAO> newMessages = new ArrayList<>();

        for (int i = 0; i < inboxMessages.length(); i++) {
            try {
                CTMessageDAO messageDAO = CTMessageDAO.initWithJSON(inboxMessages.getJSONObject(i), this.userId);

                if (messageDAO == null) {
                    continue;
                }

                if (!videoSupported && messageDAO.containsVideoOrAudio()) {
                    Logger.d(
                            "Dropping inbox message containing video/audio as app does not support video. For more information checkout CleverTap documentation.");
                    continue;
                }

                newMessages.add(messageDAO);

                Logger.v("Inbox Message for message id - " + messageDAO.getId() + " added");
            } catch (JSONException e) {
                Logger.d("Unable to update notification inbox messages - " + e.getLocalizedMessage());
            }
        }

        if (newMessages.size() > 0) {
            this.dbAdapter.upsertMessages(newMessages);
            haveUpdates = true;
            Logger.v("New Notification Inbox messages added");
            synchronized (messagesLock) {
                this.messages = this.dbAdapter.getMessages(this.userId);
                trimMessages();
            }
        }
        return haveUpdates;
    }

    private CTMessageDAO findMessageById(String id) {
        synchronized (messagesLock) {
            for (CTMessageDAO message : messages) {
                if (message.getId().equals(id)) {
                    return message;
                }
            }
        }
        Logger.v("Inbox Message for message id - " + id + " not found");
        return null;
    }

    private void trimMessages() {
        ArrayList<CTMessageDAO> toDelete = new ArrayList<>();
        synchronized (messagesLock) {
            for (CTMessageDAO message : this.messages) {
                if (!videoSupported && message.containsVideoOrAudio()) {
                    Logger.d(
                            "Removing inbox message containing video/audio as app does not support video. For more information checkout CleverTap documentation.");
                    toDelete.add(message);
                    continue;
                }
                long expires = message.getExpires();
                boolean expired = (expires > 0 && System.currentTimeMillis() / 1000 > expires);
                if (expired) {
                    Logger.v("Inbox Message: " + message.getId() + " is expired - removing");
                    toDelete.add(message);
                }
            }

            if (toDelete.size() <= 0) {
                return;
            }

            for (CTMessageDAO message : toDelete) {
                deleteMessageWithId(message.getId());
            }
        }
    }

    private static void postAsyncSafely(final String name, final Runnable runnable) {
        try {
            final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;
            if (executeSync) {
                runnable.run();
            } else {
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                        try {
                            Logger.v("CTInboxController Executor Service: Starting task - " + name);
                            runnable.run();
                        } catch (Throwable t) {
                            Logger.v("CTInboxController Executor Service: Failed to complete the scheduled task", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            Logger.v("Failed to submit task to the executor service", t);
        }
    }
}
