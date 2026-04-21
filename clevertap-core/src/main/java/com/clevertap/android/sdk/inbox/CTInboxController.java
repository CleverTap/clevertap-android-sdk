package com.clevertap.android.sdk.inbox;


import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.db.DBAdapter;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Controller class which handles Users and Messages for the Notification Inbox
 */
@RestrictTo(Scope.LIBRARY)
public class CTInboxController {

    private final DBAdapter dbAdapter;

    private ArrayList<CTMessageDAO> messages;

    private final Object messagesLock = new Object();

    private final String userId;

    private final boolean videoSupported;

    private final CTLockManager ctLockManager;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final InboxDeleteCoordinator inboxDeleteCoordinator;

    // always call async
    @WorkerThread
    public CTInboxController(CleverTapInstanceConfig config, String guid, DBAdapter adapter,
                             CTLockManager ctLockManager,
                             BaseCallbackManager callbackManager,
                             boolean videoSupported,
                             InboxDeleteCoordinator inboxDeleteCoordinator) {
        this.userId = guid;
        this.dbAdapter = adapter;
        this.messages = this.dbAdapter.getMessages(this.userId);
        this.videoSupported = videoSupported;
        this.ctLockManager = ctLockManager;
        this.callbackManager = callbackManager;
        this.config = config;
        this.inboxDeleteCoordinator = inboxDeleteCoordinator;
    }

    public int count() {
        return getMessages().size();
    }

    @AnyThread
    public String getUserId() {
        return userId;
    }

    @AnyThread
    public void deleteInboxMessage(final CTInboxMessage message) {
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("deleteInboxMessage", new Callable<Void>() {
            @Override
            public Void call() {
                // Look up the DAO directly. Must happen BEFORE
                // the local delete wipes the DAO from cache.
                final CTMessageDAO dao = findMessageById(message.getMessageId());
                final boolean isV2 = dao != null && dao.getSource() == InboxMessageSource.V2;

                if (isV2) {
                    dbAdapter.addPendingDelete(message.getMessageId(), userId);
                }
                synchronized (ctLockManager.getInboxControllerLock()) {
                    boolean update = _deleteMessageWithId(message.getMessageId());
                    if (update) {
                        callbackManager._notifyInboxMessagesDidUpdate();
                    }
                }
                if (isV2 && inboxDeleteCoordinator != null) {
                    inboxDeleteCoordinator.syncDelete(Collections.singletonList(message), userId);
                }
                return null;
            }
        });
    }

    @AnyThread
    public void deleteInboxMessagesForIDs(final ArrayList<String> messageIDs) {
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("deleteInboxMessagesForIDs", new Callable<Void>() {
            @Override
            public Void call() {
                // Partition ids into V1 and V2 BEFORE the local delete wipes
                // the cache.
                final Set<String> idSet = new HashSet<>(messageIDs);
                final List<String> v2Ids = new ArrayList<>();
                final List<CTInboxMessage> v2Messages = new ArrayList<>();
                synchronized (messagesLock) {
                    for (CTMessageDAO d : messages) {
                        if (!idSet.contains(d.getId())) continue;
                        if (d.getSource() != InboxMessageSource.V2) continue;
                        v2Ids.add(d.getId());
                        v2Messages.add(new CTInboxMessage(d.toJSON()));
                    }
                }

                if (!v2Ids.isEmpty()) {
                    dbAdapter.addPendingDeletes(v2Ids, userId);
                }
                synchronized (ctLockManager.getInboxControllerLock()) {
                    boolean update = _deleteMessagesForIds(messageIDs);
                    if (update) {
                        callbackManager._notifyInboxMessagesDidUpdate();
                    }
                }
                if (inboxDeleteCoordinator != null && !v2Messages.isEmpty()) {
                    inboxDeleteCoordinator.syncDelete(v2Messages, userId);
                }
                return null;
            }
        });
    }

    @AnyThread
    private CTInboxMessage getInboxMessageForId(String messageId) {
        CTMessageDAO dao = findMessageById(messageId);
        return dao == null ? null : new CTInboxMessage(dao.toJSON());
    }

    @AnyThread
    public CTMessageDAO getMessageForId(String messageId) {
        return findMessageById(messageId);
    }

    @AnyThread
    public ArrayList<CTMessageDAO> getMessages() {
        synchronized (messagesLock) {
            trimMessages();
            return messages;
        }
    }

    @AnyThread
    public ArrayList<CTMessageDAO> getUnreadMessages() {
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

    @AnyThread
    public void markReadInboxMessage(final CTInboxMessage message) {
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("markReadInboxMessage", new Callable<Void>() {
            @Override
            public Void call() {
                synchronized (ctLockManager.getInboxControllerLock()) {
                    boolean read = _markReadForMessageWithId(message.getMessageId());
                    if (read) {
                        callbackManager._notifyInboxMessagesDidUpdate();
                    }
                }
                return null;
            }
        });
    }

    @AnyThread
    public void markReadInboxMessagesForIDs(final ArrayList<String> messageIDs) {
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("markReadInboxMessagesForIDs", new Callable<Void>() {
            @Override
            public Void call() {
                synchronized (ctLockManager.getInboxControllerLock()) {
                    boolean read = _markReadForMessagesWithIds(messageIDs);
                    if (read) {
                        callbackManager._notifyInboxMessagesDidUpdate();
                    }
                }
                return null;
            }
        });
    }

    @AnyThread
    public int unreadCount() {
        return getUnreadMessages().size();
    }

    // always call async
    @WorkerThread
    public boolean updateMessages(final JSONArray inboxMessages) {
        Logger.v( "CTInboxController:updateMessages() called");

        boolean haveUpdates = false;
        ArrayList<CTMessageDAO> newMessages = new ArrayList<>();

        for (int i = 0; i < inboxMessages.length(); i++) {
            try {
                CTMessageDAO messageDAO = CTMessageDAO.initWithJSON(
                        inboxMessages.getJSONObject(i), this.userId, InboxMessageSource.V1);

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

    /**
     * Applies an already-parsed V2 message list to the store.
     * Caller must hold {@code inboxControllerLock}; firing the UI callback
     * is the caller's responsibility (mirrors the V1 {@link #updateMessages}
     * contract). The dual-filter math lives in {@link InboxV2Merger} as pure
     * functions; this method only sequences DB reads/writes around it.
     */
    @WorkerThread
    public boolean processV2Response(List<CTMessageDAO> incoming) {
        Set<String> pendingDeletes = dbAdapter.getPendingDeletes(userId);
        Set<String> pendingReads = dbAdapter.getPendingReads(userId);
        long nowSec = System.currentTimeMillis() / 1000L;

        // Server-caught-up: any incoming message with isRead=1 that was still
        // pending locally confirms the read — drop the pending row. Must run
        // BEFORE preWriteFilter, which mutates the incoming DAOs in-place via
        // the pending-read override and would make every pending id look
        // server-confirmed.
        if (!pendingReads.isEmpty()) {
            List<String> confirmedReads = new ArrayList<>();
            for (CTMessageDAO dao : incoming) {
                if (dao.isRead() == 1 && pendingReads.contains(dao.getId())) {
                    confirmedReads.add(dao.getId());
                }
            }
            if (!confirmedReads.isEmpty()) {
                dbAdapter.removePendingReads(confirmedReads, userId);
            }
        }

        List<CTMessageDAO> toUpsert = InboxV2Merger.INSTANCE.preWriteFilter(
                incoming, pendingDeletes, pendingReads, videoSupported, nowSec);
        if (!toUpsert.isEmpty()) {
            dbAdapter.upsertMessages(toUpsert);
        }
        boolean updated = !toUpsert.isEmpty();

        synchronized (messagesLock) {
            List<CTMessageDAO> full = dbAdapter.getMessages(userId);
            CleanupResult cleanup = InboxV2Merger.INSTANCE.postReadCleanup(
                    full, pendingDeletes, pendingReads, videoSupported, nowSec);

            if (!cleanup.getToDelete().isEmpty()) {
                dbAdapter.deleteMessagesForIDs(cleanup.getToDelete(), userId);
                updated = true;
            }
            this.messages = new ArrayList<>(cleanup.getFinalList());
        }
        return updated;
    }

    @AnyThread
    boolean _deleteMessageWithId(final String messageId) {
        CTMessageDAO messageDAO = findMessageById(messageId);
        if (messageDAO == null) {
            return false;
        }
        synchronized (messagesLock) {
            this.messages.remove(messageDAO);
        }

        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("RunDeleteMessage", new Callable<Void>() {
            @Override
            @WorkerThread
            public Void call() {
                dbAdapter.deleteMessageForId(messageId, userId);
                return null;
            }
        });
        return true;
    }

    @AnyThread
    boolean _deleteMessagesForIds(final ArrayList<String> messageIDs) {
        ArrayList<CTMessageDAO> messageDAOList = new ArrayList<>();
        for (String messageID : messageIDs) {
            CTMessageDAO messageDAO = findMessageById(messageID);
            if (messageDAO == null) {
                continue;
            }
            messageDAOList.add(messageDAO);
        }
        if (messageDAOList.isEmpty())
            return false;

        synchronized (messagesLock) {
            this.messages.removeAll(messageDAOList);
        }

        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("RunDeleteMessagesForIDs", new Callable<Void>() {
            @Override
            @WorkerThread
            public Void call() {
                dbAdapter.deleteMessagesForIDs(messageIDs, userId);
                return null;
            }
        });
        return true;
    }

    @AnyThread
    boolean _markReadForMessageWithId(final String messageId) {
        CTMessageDAO messageDAO = findMessageById(messageId);
        if (messageDAO == null) {
            return false;
        }

        final boolean isV2 = messageDAO.getSource() == InboxMessageSource.V2;

        synchronized (messagesLock) {
            messageDAO.setRead(1);
        }
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.addOnSuccessListener(unused -> callbackManager._notifyInboxMessagesDidUpdate() );//  //OR callbackManager.getInboxListener().inboxMessagesDidUpdate();
        task.addOnFailureListener(e -> Logger.d("Failed to update message read state for id:"+messageId,e));

        task.execute("RunMarkMessageRead", new Callable<Void>() {
            @Override
            @WorkerThread
            public Void call() {
                dbAdapter.markReadMessageForId(messageId, userId);
                if (isV2) {
                    dbAdapter.addPendingRead(messageId, userId);
                }
                return null;
            }
        });
        return true;
    }

    @AnyThread
    boolean _markReadForMessagesWithIds(final ArrayList<String> messageIDs) {
        // One pass over `messages` under a single lock
        final Set<String> idSet = new HashSet<>(messageIDs);
        final ArrayList<String> v2Ids = new ArrayList<>();
        boolean atleastOneMessageIsValid = false;

        synchronized (messagesLock) {
            for (CTMessageDAO dao : messages) {
                if (!idSet.contains(dao.getId())) continue;
                atleastOneMessageIsValid = true;
                dao.setRead(1);
                if (dao.getSource() == InboxMessageSource.V2) {
                    v2Ids.add(dao.getId());
                }
            }
        }

        if (!atleastOneMessageIsValid)
            return false;

        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.addOnSuccessListener(unused -> callbackManager._notifyInboxMessagesDidUpdate());
        task.addOnFailureListener(e -> Logger.d("Failed to update message read state for ids:" + messageIDs, e));

        task.execute("RunMarkMessagesReadForIDs", new Callable<Void>() {
            @Override
            @WorkerThread
            public Void call() {
                dbAdapter.markReadMessagesForIds(messageIDs, userId);
                if (!v2Ids.isEmpty()) {
                    dbAdapter.addPendingReads(v2Ids, userId);
                }
                return null;
            }
        });
        return true;
    }

    /**
     * Single entry point for callers that need to branch on V1 vs V2 without
     * reading source off the public {@link CTInboxMessage}. Looks up the DAO
     * in the in-memory list under {@code messagesLock}.
     *
     * @return {@code true} only when the message exists in cache and is V2.
     *         Unknown ids and V1 messages both return {@code false} — both
     *         are treated as "do not perform V2-specific behavior".
     */
    @AnyThread
    public boolean isV2Message(final String id) {
        if (id == null) return false;
        synchronized (messagesLock) {
            for (CTMessageDAO message : messages) {
                if (id.equals(message.getId())) {
                    return message.getSource() == InboxMessageSource.V2;
                }
            }
        }
        return false;
    }

    @AnyThread
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

    @AnyThread
    private void trimMessages() {
        Logger.v( "CTInboxController:trimMessages() called");
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
                _deleteMessageWithId(message.getId());
            }
        }
    }
}
