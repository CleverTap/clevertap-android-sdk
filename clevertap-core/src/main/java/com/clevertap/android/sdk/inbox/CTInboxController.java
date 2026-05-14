package com.clevertap.android.sdk.inbox;


import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.utils.Clock;
import com.clevertap.android.sdk.db.DBAdapter;
import com.clevertap.android.sdk.db.dao.PendingDelete;
import com.clevertap.android.sdk.db.dao.PendingRead;
import com.clevertap.android.sdk.response.InboxV2DeliverySource;
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

    private static final long PENDING_DELETE_DEFAULT_TTL_SECONDS = 24L * 60L * 60L;

    // BE's documented indexing window is 1–2 h. 3× as a conservative buffer trades
    // a small flicker risk (if BE indexing slips beyond 6 h) for faster cross-device
    // delete reflection on the firing device. Tunable in one place.
    @VisibleForTesting
    static final long INDEXING_GRACE_SECONDS = 6L * 60L * 60L;

    private final DBAdapter dbAdapter;

    private ArrayList<CTMessageDAO> messages;

    private final Object messagesLock = new Object();

    private final String userId;

    private final boolean videoSupported;

    private final CTLockManager ctLockManager;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final InboxDeleteCoordinator inboxDeleteCoordinator;

    private final Clock clock;

    // always call async
    @WorkerThread
    public CTInboxController(CleverTapInstanceConfig config, String guid, DBAdapter adapter,
                             CTLockManager ctLockManager,
                             BaseCallbackManager callbackManager,
                             boolean videoSupported,
                             InboxDeleteCoordinator inboxDeleteCoordinator) {
        this(config, guid, adapter, ctLockManager, callbackManager, videoSupported,
                inboxDeleteCoordinator, Clock.SYSTEM);
    }

    @VisibleForTesting
    CTInboxController(CleverTapInstanceConfig config, String guid, DBAdapter adapter,
                      CTLockManager ctLockManager,
                      BaseCallbackManager callbackManager,
                      boolean videoSupported,
                      InboxDeleteCoordinator inboxDeleteCoordinator,
                      Clock clock) {
        this.userId = guid;
        this.dbAdapter = adapter;
        this.messages = this.dbAdapter.getMessages(this.userId);
        this.videoSupported = videoSupported;
        this.ctLockManager = ctLockManager;
        this.callbackManager = callbackManager;
        this.config = config;
        this.inboxDeleteCoordinator = inboxDeleteCoordinator;
        this.clock = clock;
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
                    long now = clock.currentTimeSeconds();
                    dbAdapter.addPendingDelete(
                            message.getMessageId(),
                            userId,
                            dao.getWzrkParams(),
                            resolvePendingActionExpiry(dao, now)
                    );
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
                final long now = clock.currentTimeSeconds();
                final Set<String> idSet = new HashSet<>(messageIDs);
                final List<CTInboxMessage> v2Messages = new ArrayList<>();
                final List<PendingDelete> pendingRows = new ArrayList<>();
                synchronized (messagesLock) {
                    for (CTMessageDAO d : messages) {
                        if (!idSet.contains(d.getId())) continue;
                        if (d.getSource() != InboxMessageSource.V2) continue;
                        v2Messages.add(new CTInboxMessage(d.toJSON()));
                        pendingRows.add(new PendingDelete(
                                d.getId(),
                                d.getWzrkParams(),
                                resolvePendingActionExpiry(d, now)
                        ));
                    }
                }

                if (!pendingRows.isEmpty()) {
                    dbAdapter.addPendingDeletes(pendingRows, userId);
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
     *
     * <p>When {@code source} is {@link InboxV2DeliverySource#FETCH} the method
     * additionally runs the cross-device delete sweep:
     * <ol>
     *   <li>Promotes {@code PENDING_INDEXING} rows whose ids appear in the fetch
     *       response to {@code INDEXED} via a targeted UPDATE (never downgrades).</li>
     *   <li>Identifies V2 rows that are {@code INDEXED} (or stale
     *       {@code PENDING_INDEXING} older than {@link #INDEXING_GRACE_SECONDS})
     *       but absent from this response — these are treated as cross-device
     *       deletes and removed from the DB before the post-read cleanup re-read.</li>
     * </ol>
     * When {@code source} is {@link InboxV2DeliverySource#A1} the sweep is
     * skipped: an {@code /a1} payload is not a complete inbox snapshot, so
     * "absent from response" cannot be used as a delete signal.</p>
     */
    @WorkerThread
    public boolean processV2Response(List<CTMessageDAO> incoming, InboxV2DeliverySource source) {
        long nowSec = clock.currentTimeSeconds();
        boolean updated = false;

        // ── FETCH-only: promote PENDING_INDEXING rows that appeared in this
        // response to INDEXED, then sweep V2 rows absent from the authoritative
        // fetch snapshot (cross-device delete signal).
        if (source == InboxV2DeliverySource.FETCH) {
            Set<String> incomingIds = new HashSet<>(incoming.size());
            for (CTMessageDAO dao : incoming) {
                incomingIds.add(dao.getId());
            }
            if (!incomingIds.isEmpty()) {
                config.getLogger().verbose(config.getAccountId(),
                        "InboxV2: markIndexed " + incomingIds.size() + " msg(s)");
                dbAdapter.markIndexed(new ArrayList<>(incomingIds), userId);
            }

            // Sweep: INDEXED V2 rows (and stale PENDING_INDEXING older than
            // the grace cutoff) that are absent from this fetch response are
            // treated as cross-device deletes. Deleted from DB here so the
            // postReadCleanup re-read below won't see them.
            long graceCutoff = nowSec - INDEXING_GRACE_SECONDS;
            Set<String> sweepable = dbAdapter.findSweepableV2Ids(userId, graceCutoff);
            sweepable.removeAll(incomingIds);
            if (!sweepable.isEmpty()) {
                config.getLogger().verbose(config.getAccountId(),
                        "InboxV2: cross-device sweep — removing " + sweepable.size() + " msg(s): " + sweepable);
                dbAdapter.deleteMessagesForIDs(new ArrayList<>(sweepable), userId);
                updated = true;
            } else {
                config.getLogger().verbose(config.getAccountId(),
                        "InboxV2: cross-device sweep — nothing to remove");
            }
        }

        // T6.2: drop AWAITING_CONFIRM pending-deletes whose TTL has elapsed.
        int removedDeletes = dbAdapter.removeExpiredAwaitingConfirm(userId, nowSec);
        if (removedDeletes > 0) {
            config.getLogger().verbose(config.getAccountId(),
                    "InboxV2: removed " + removedDeletes + " expired AWAITING_CONFIRM row(s)");
        }
        // T7.2: drop pending-reads whose TTL has elapsed (server may never echo
        // back e.g. if the message was deleted, retargeted, or expired).
        int removedReads = dbAdapter.removeExpiredPendingReads(userId, nowSec);
        if (removedReads > 0) {
            config.getLogger().verbose(config.getAccountId(),
                    "InboxV2: removed " + removedReads + " expired pending-read row(s)");
        }
        Set<String> pendingDeletes = dbAdapter.getPendingDeleteIds(userId);
        Set<String> pendingReads = dbAdapter.getPendingReads(userId);

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
            // FETCH is the authoritative source — new rows from this response are
            // server-confirmed, so write them as INDEXED directly. ON CONFLICT
            // preserves index_state for existing rows, so this only affects INSERTs.
            if (source == InboxV2DeliverySource.FETCH) {
                for (CTMessageDAO dao : toUpsert) {
                    dao.setIndexState(InboxIndexState.INDEXED);
                }
            }
            dbAdapter.upsertMessages(toUpsert);
        }
        updated = updated || !toUpsert.isEmpty();

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
        final long pendingReadExpiresAt = isV2
                ? resolvePendingActionExpiry(messageDAO, clock.currentTimeSeconds())
                : 0L;

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
                    dbAdapter.addPendingRead(messageId, userId, pendingReadExpiresAt);
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
        final List<PendingRead> pendingReadRows = new ArrayList<>();
        boolean atleastOneMessageIsValid = false;

        synchronized (messagesLock) {
            final long now = clock.currentTimeSeconds();
            for (CTMessageDAO dao : messages) {
                if (!idSet.contains(dao.getId())) continue;
                atleastOneMessageIsValid = true;
                dao.setRead(1);
                if (dao.getSource() == InboxMessageSource.V2) {
                    pendingReadRows.add(new PendingRead(dao.getId(), resolvePendingActionExpiry(dao, now)));
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
                if (!pendingReadRows.isEmpty()) {
                    dbAdapter.addPendingReads(pendingReadRows, userId);
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
                boolean expired = (expires > 0 && clock.currentTimeSeconds() > expires);
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

    private static long resolvePendingActionExpiry(CTMessageDAO dao, long nowSeconds) {
        long ttl = dao == null ? 0L : dao.getExpires();
        if (ttl == 0L) return 0L; // infinite TTL — never expires
        return ttl > 0L ? ttl : nowSeconds + PENDING_DELETE_DEFAULT_TTL_SECONDS;
    }
}
