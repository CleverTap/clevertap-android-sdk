package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.CTJsonConverter.getErrorObject;

import android.content.Context;
import org.json.JSONObject;

class EventProcessor extends BaseEventProcessor {

    private final CTLockManager mCTLockManager;

    private final CoreMetaData mCleverTapMetaData;

    private final CleverTapInstanceConfig mConfig;

    private final ValidationResultStack mValidationResultStack;
    private final LocalDataStore mLocalDataStore;
    private final BaseQueueManager mBaseQueueManager;
    private final BaseDatabaseManager mBaseDatabaseManager;
    private final MainLooperHandler mMainLooperHandler;

    private Runnable pushNotificationViewedRunnable = null;

    EventProcessor(CoreState coreState) {
        mCTLockManager = coreState.getCTLockManager();
        mCleverTapMetaData = coreState.getCoreMetaData();
        mConfig = coreState.getConfig();
        mValidationResultStack = coreState.getValidationResultStack();
        mLocalDataStore = coreState.getLocalDataStore();
        mBaseQueueManager = coreState.getBaseEventQueueManager();
        mBaseDatabaseManager = coreState.getDatabaseManager();
        mMainLooperHandler = coreState.getMainLooperHandler();
    }

    @Override
    void processEvent(final Context context, final JSONObject event, final int eventType) {
        synchronized (mCTLockManager.getEventLock()) {
            try {
                if (CoreMetaData.getActivityCount() == 0) {
                    CoreMetaData.setActivityCount(1);
                }
                String type;
                if (eventType == Constants.PAGE_EVENT) {
                    type = "page";
                } else if (eventType == Constants.PING_EVENT) {
                    type = "ping";
                    attachMeta(event, context);
                    if (event.has("bk")) {
                        mCleverTapMetaData.setBgPing(true);
                        event.remove("bk");
                    }

                    //Add a flag to denote, PING event is for geofences
                    if (mCleverTapMetaData.isLocationForGeofence()) {
                        event.put("gf", true);
                        mCleverTapMetaData.setLocationForGeofence(false);
                        event.put("gfSDKVersion", mCleverTapMetaData.getGeofenceSDKVersion());
                        mCleverTapMetaData.setGeofenceSDKVersion(0);
                    }
                } else if (eventType == Constants.PROFILE_EVENT) {
                    type = "profile";
                } else if (eventType == Constants.DATA_EVENT) {
                    type = "data";
                } else {
                    type = "event";
                }

                // Complete the received event with the other params

                String currentActivityName = mCleverTapMetaData.getScreenName();
                if (currentActivityName != null) {
                    event.put("n", currentActivityName);
                }

                int session = mCleverTapMetaData.getCurrentSession();
                event.put("s", session);
                event.put("pg", CoreMetaData.getActivityCount());
                event.put("type", type);
                event.put("ep", System.currentTimeMillis() / 1000);
                event.put("f", mCleverTapMetaData.isFirstSession());
                event.put("lsl", mCleverTapMetaData.getLastSessionLength());
                attachPackageNameIfRequired(context, event);

                // Report any pending validation error
                ValidationResult vr = mValidationResultStack.popValidationResult();
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                mLocalDataStore.setDataSyncFlag(event);
                mBaseDatabaseManager.queueEventToDB(context, event, eventType);
                updateLocalStore(context, event, eventType);
                mBaseQueueManager.scheduleQueueFlush(context);

            } catch (Throwable e) {
                mConfig.getLogger().verbose(mConfig.getAccountId(), "Failed to queue event: " + event.toString(), e);
            }
        }
    }

    //Session
    private void attachPackageNameIfRequired(final Context context, final JSONObject event) {
        try {
            final String type = event.getString("type");
            // Send it only for app launched events
            if ("event".equals(type) && Constants.APP_LAUNCHED_EVENT.equals(event.getString("evtName"))) {
                event.put("pai", context.getPackageName());
            }
        } catch (Throwable t) {
            // Ignore
        }
    }

    /**
     * Attaches meta info about the current state of the device to an event.
     * Typically, this meta is added only to the ping event.
     */
    private void attachMeta(final JSONObject o, final Context context) {
        // Memory consumption
        try {
            o.put("mc", Utils.getMemoryConsumption());
        } catch (Throwable t) {
            // Ignore
        }

        // Attach the network type
        try {
            o.put("nt", Utils.getCurrentNetworkType(context));
        } catch (Throwable t) {
            // Ignore
        }
    }

    @Override
    void processPushNotificationViewedEvent(final Context context, final JSONObject event) {
        synchronized (mCTLockManager.getEventLock()) {
            try {
                int session = mCleverTapMetaData.getCurrentSession();
                event.put("s", session);
                event.put("type", "event");
                event.put("ep", System.currentTimeMillis() / 1000);
                // Report any pending validation error
                ValidationResult vr = mValidationResultStack.popValidationResult();
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                mConfig.getLogger().verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto DB");
                mBaseDatabaseManager.queuePushNotificationViewedEventToDB(context, event);
                mConfig.getLogger().verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto queue flush");
                schedulePushNotificationViewedQueueFlush(context);
            } catch (Throwable t) {
                mConfig.getLogger()
                        .verbose(mConfig.getAccountId(), "Failed to queue notification viewed event: " + event.toString(), t);
            }
        }
    }

    private void schedulePushNotificationViewedQueueFlush(final Context context) {
        if (pushNotificationViewedRunnable == null) {
            pushNotificationViewedRunnable = new Runnable() {
                @Override
                public void run() {
                    mConfig.getLogger()
                            .verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto queue flush async");
                    mBaseQueueManager.flushQueueAsync(context, EventGroup.PUSH_NOTIFICATION_VIEWED);
                }
            };
        }
        mMainLooperHandler.removeCallbacks(pushNotificationViewedRunnable);
        mMainLooperHandler.post(pushNotificationViewedRunnable);
    }
    //Util
    // only call async
    private void updateLocalStore(final Context context, final JSONObject event, final int type) {
        if (type == Constants.RAISED_EVENT) {
            mLocalDataStore.persistEvent(context, event, type);
        }
    }
}
