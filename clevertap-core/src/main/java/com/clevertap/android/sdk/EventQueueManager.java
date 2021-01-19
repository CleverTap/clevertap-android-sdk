package com.clevertap.android.sdk;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.login.IdentityRepo;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.login.LoginInfoProvider;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.Future;
import org.json.JSONException;
import org.json.JSONObject;

class EventQueueManager extends BaseQueueManager {

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final DeviceInfo mDeviceInfo;

    private final EventMediator mEventMediator;

    private final EventProcessor mEventProcessor;

    private final BaseDatabaseManager mDatabaseManager;

    private final Logger mLogger;

    private final BaseNetworkManager mNetworkManager;

    private final MainLooperHandler mMainLooperHandler;

    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;

    private final SessionManager mSessionManager;

    private final ValidationResultStack mValidationResultStack;
    private final CoreMetaData mCleverTapMetaData;

    private Runnable commsRunnable = null;

    public EventQueueManager(final CoreState coreState) {
        mEventMediator = coreState.getEventMediator();
        mSessionManager = coreState.getSessionManager();
        mMainLooperHandler = coreState.getMainLooperHandler();
        mPostAsyncSafelyHandler = coreState.getPostAsyncSafelyHandler();
        mEventProcessor = coreState.getEventProcessor();
        mConfig = coreState.getConfig();
        mDeviceInfo = coreState.getDeviceInfo();
        mContext = coreState.getContext();
        mValidationResultStack = coreState.getValidationResultStack();
        mNetworkManager = coreState.getNetworkManager();
        mDatabaseManager = coreState.getDatabaseManager();
        mLogger = mConfig.getLogger();
        mCleverTapMetaData = coreState.getCoreMetaData();
    }

    //Profile
    @Override
    void pushBasicProfile(JSONObject baseProfile) {
        try {
            String guid = getCleverTapID();

            JSONObject profileEvent = new JSONObject();

            if (baseProfile != null && baseProfile.length() > 0) {
                Iterator<String> i = baseProfile.keys();
                IdentityRepo iProfileHandler = IdentityRepoFactory
                        .getRepo(mContext, mConfig, mDeviceInfo, mValidationResultStack);
                LoginInfoProvider loginInfoProvider = new LoginInfoProvider(mContext, mConfig, mDeviceInfo);
                while (i.hasNext()) {
                    String next = i.next();

                    // need to handle command-based JSONObject props here now
                    Object value = null;
                    try {
                        value = baseProfile.getJSONObject(next);
                    } catch (Throwable t) {
                        try {
                            value = baseProfile.get(next);
                        } catch (JSONException e) {
                            //no-op
                        }
                    }

                    if (value != null) {
                        profileEvent.put(next, value);

                        // cache the valid identifier: guid pairs
                        boolean isProfileKey = iProfileHandler.hasIdentity(next);
                        if (isProfileKey) {
                            try {
                                loginInfoProvider.cacheGUIDForIdentifier(guid, next, value.toString());
                            } catch (Throwable t) {
                                // no-op
                            }
                        }
                    }
                }
            }

            try {
                String carrier = mDeviceInfo.getCarrier();
                if (carrier != null && !carrier.equals("")) {
                    profileEvent.put("Carrier", carrier);
                }

                String cc = mDeviceInfo.getCountryCode();
                if (cc != null && !cc.equals("")) {
                    profileEvent.put("cc", cc);
                }

                profileEvent.put("tz", TimeZone.getDefault().getID());

                JSONObject event = new JSONObject();
                event.put("profile", profileEvent);
                queueEvent(mContext, event, Constants.PROFILE_EVENT);
            } catch (JSONException e) {
                mConfig.getLogger()
                        .verbose(mConfig.getAccountId(), "FATAL: Creating basic profile update event failed!");
            }
        } catch (Throwable t) {
            mConfig.getLogger().verbose(mConfig.getAccountId(), "Basic profile sync", t);
        }
    }

    //Event
    @Override
    void pushInitialEventsAsync() {

        mPostAsyncSafelyHandler.postAsyncSafely("CleverTapAPI#pushInitialEventsAsync", new Runnable() {
            @Override
            public void run() {
                try {
                    mConfig.getLogger().verbose(mConfig.getAccountId(), "Queuing daily events");
                    pushBasicProfile(null);
                } catch (Throwable t) {
                    mConfig.getLogger().verbose(mConfig.getAccountId(), "Daily profile sync failed", t);
                }
            }
        });
    }

    /**
     * Adds a new event to the queue, to be sent later.
     *
     * @param context   The Android context
     * @param event     The event to be queued
     * @param eventType The type of event to be queued
     */
    @Override
    public Future<?> queueEvent(final Context context, final JSONObject event, final int eventType) {
        return mPostAsyncSafelyHandler.postAsyncSafely("queueEvent", new Runnable() {
            @Override
            public void run() {
                if (mEventMediator.shouldDropEvent(event, eventType)) {
                    return;
                }
                if (mEventMediator.shouldDeferProcessingEvent(event, eventType)) {
                    mConfig.getLogger().debug(mConfig.getAccountId(),
                            "App Launched not yet processed, re-queuing event " + event + "after 2s");
                    mMainLooperHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mPostAsyncSafelyHandler.postAsyncSafely("queueEventWithDelay", new Runnable() {
                                @Override
                                public void run() {
                                    mSessionManager.lazyCreateSession(context);
                                    pushInitialEventsAsync();
                                    addToQueue(context, event, eventType);
                                }
                            });
                        }
                    }, 2000);
                } else {
                    if (eventType == Constants.FETCH_EVENT) {
                        addToQueue(context, event, eventType);
                    } else {
                        mSessionManager.lazyCreateSession(context);
                        pushInitialEventsAsync();
                        addToQueue(context, event, eventType);
                    }
                }
            }
        });
    }


    // only call async
    @Override
    void addToQueue(final Context context, final JSONObject event, final int eventType) {
        if (eventType == Constants.NV_EVENT) {
            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto separate queue");
            mEventProcessor.processPushNotificationViewedEvent(context, event);
        } else {
            mEventProcessor.processEvent(context, event, eventType);
        }
    }

    @Override
    void clearQueues(final Context context) {
        //TODO
    }

    @Override
    void flush() {
//TOD
    }

    private String getCleverTapID() {
        return mDeviceInfo.getDeviceID();
    }


    @Override
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

    @Override
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

    @Override
        //Event
    void scheduleQueueFlush(final Context context) {
        if (commsRunnable == null) {
            commsRunnable = new Runnable() {
                @Override
                public void run() {
                    flushQueueAsync(context, EventGroup.REGULAR);
                    flushQueueAsync(context, EventGroup.PUSH_NOTIFICATION_VIEWED);
                }
            };
        }
        // Cancel any outstanding send runnables, and issue a new delayed one
        mMainLooperHandler.removeCallbacks(commsRunnable);
        //TODO merge
        mMainLooperHandler.postDelayed(commsRunnable, getDelayFrequency());

        mLogger.verbose(mConfig.getAccountId(), "Scheduling delayed queue flush on main event loop");
    }

}