package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

public class QueueManager {

    private final CoreMetaData mCleverTapMetaData;

    private final CleverTapState mCleverTapState;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final BaseDatabaseManager mDatabaseManager;

    private final Logger mLogger;

    private final BaseNetworkManager mNetworkManager;

    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;

    QueueManager(Context context, CleverTapInstanceConfig config, CoreMetaData cleverTapMetaData,
            CleverTapState cleverTapState) {
        mContext = context;
        mConfig = config;
        mCleverTapMetaData = cleverTapMetaData;
        mCleverTapState = cleverTapState;

        mLogger = mConfig.getLogger();
        mNetworkManager = mCleverTapState.getNetworkManager();
        mDatabaseManager = mCleverTapState.getDatabaseManager();
        mPostAsyncSafelyHandler = mCleverTapState.getPostAsyncSafelyHandler();
    }

    void addToQueue(final Context context, final JSONObject event, final int eventType) {
        //TODO
    }

    void clearQueues(final Context context) {
        //TODO
    }

    void flush() {

    }

    void flushQueueAsync(final Context context, final EventGroup eventGroup) {
        mPostAsyncSafelyHandler.postAsyncSafely("CommsManager#flushQueueAsync", new Runnable() {
            @Override
            public void run() {
                if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
                    mLogger.verbose(mConfig.getAccountId(),
                            "Pushing Notification Viewed event onto queue flush sync");
                } else {
                    mLogger.verbose(mConfig.getAccountId(), "Pushing event onto queue flush sync");
                }
                flushQueueSync(context, eventGroup);
            }
        });
    }

    void flushQueueSync(final Context context, final EventGroup eventGroup) {
        if (!NetworkManager.isNetworkOnline(context)) {
            mLogger.verbose(mConfig.getAccountId(), "Network connectivity unavailable. Will retry later");
            return;
        }

        if (mCleverTapMetaData.isOffline()) {
            mLogger.debug(mConfig.getAccountId(),
                    "CleverTap Instance has been set to offline, won't send events queue");
            return;
        }

        if (mNetworkManager.needsHandshakeForDomain(eventGroup)) {
            mNetworkManager.initHandshake(eventGroup, new Runnable() {
                @Override
                public void run() {
                    mDatabaseManager.flushDBQueue(context, eventGroup);
                }
            });
        } else {
            mLogger.verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto queue DB flush");
            mDatabaseManager.flushDBQueue(context, eventGroup);
        }
    }

    void scheduleQueueFlush(final Context context) {

    }

}
