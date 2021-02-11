package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.CTJsonConverter.getErrorObject;

import android.content.Context;
import com.clevertap.android.sdk.login.IdentityRepo;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.login.LoginInfoProvider;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.Future;
import org.json.JSONException;
import org.json.JSONObject;

class EventQueueManager extends BaseEventQueueManager implements FailureFlushListener {

    private Runnable commsRunnable = null;

    private final BaseDatabaseManager mBaseDatabaseManager;

    private final CoreMetaData mCleverTapMetaData;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final CTLockManager mCtLockManager;

    private final DeviceInfo mDeviceInfo;

    private final EventMediator mEventMediator;

    private final LocalDataStore mLocalDataStore;

    private final Logger mLogger;

    private final MainLooperHandler mMainLooperHandler;

    private final BaseNetworkManager mNetworkManager;

    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;

    private final SessionManager mSessionManager;

    private final ValidationResultStack mValidationResultStack;

    private Runnable pushNotificationViewedRunnable = null;

    private LoginInfoProvider mLoginInfoProvider;

    public EventQueueManager(final BaseDatabaseManager baseDatabaseManager,
            Context context,
            CleverTapInstanceConfig config,
            EventMediator eventMediator,
            SessionManager sessionManager,
            BaseCallbackManager callbackManager,
            MainLooperHandler mainLooperHandler,
            PostAsyncSafelyHandler postAsyncSafelyHandler,
            DeviceInfo deviceInfo,
            ValidationResultStack validationResultStack,
            NetworkManager networkManager,
            CoreMetaData coreMetaData,
            CTLockManager ctLockManager,
            final LocalDataStore localDataStore) {
        mBaseDatabaseManager = baseDatabaseManager;
        mContext = context;
        mConfig = config;
        mEventMediator = eventMediator;
        mSessionManager = sessionManager;
        mMainLooperHandler = mainLooperHandler;
        mPostAsyncSafelyHandler = postAsyncSafelyHandler;
        mDeviceInfo = deviceInfo;
        mValidationResultStack = validationResultStack;
        mNetworkManager = networkManager;
        mLocalDataStore = localDataStore;
        mLogger = mConfig.getLogger();
        mCleverTapMetaData = coreMetaData;
        mCtLockManager = ctLockManager;

        callbackManager.setFailureFlushListener(this);
    }

    @Override
    public void failureFlush(Context context) {
        scheduleQueueFlush(context);
    }


    @Override
    public void flushQueueSync(final Context context, final EventGroup eventGroup) {
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
                    mNetworkManager.flushDBQueue(context, eventGroup);
                }
            });
        } else {
            mLogger.verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto queue DB flush");
            mNetworkManager.flushDBQueue(context, eventGroup);
        }
    }

    public LoginInfoProvider getLoginInfoProvider() {
        return mLoginInfoProvider;
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

    public void setLoginInfoProvider(final LoginInfoProvider loginInfoProvider) {
        mLoginInfoProvider = loginInfoProvider;
    }


    // only call async
    @Override
    void addToQueue(final Context context, final JSONObject event, final int eventType) {
        if (eventType == Constants.NV_EVENT) {
            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto separate queue");
            processPushNotificationViewedEvent(context, event);
        } else {
            processEvent(context, event, eventType);
        }
    }

    @Override
    void flush() {
        flushQueueAsync(mContext, EventGroup.REGULAR);
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

    void processEvent(final Context context, final JSONObject event, final int eventType) {
        synchronized (mCtLockManager.getEventLock()) {
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

                int session = mCleverTapMetaData.getCurrentSessionId();
                event.put("s", session);
                event.put("pg", CoreMetaData.getActivityCount());
                event.put("type", type);
                event.put("ep", getNow());
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
                scheduleQueueFlush(context);

            } catch (Throwable e) {
                mConfig.getLogger().verbose(mConfig.getAccountId(), "Failed to queue event: " + event.toString(), e);
            }
        }
    }

    @Override
    void pushInitialEventsAsync() {
        if (!mCleverTapMetaData.inCurrentSession()) {
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
    }

    @Override
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

        mMainLooperHandler.postDelayed(commsRunnable, mNetworkManager.getDelayFrequency());

        mLogger.verbose(mConfig.getAccountId(), "Scheduling delayed queue flush on main event loop");
    }

    private String getCleverTapID() {
        return mDeviceInfo.getDeviceID();
    }

    int getNow() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    //Profile
    @Override
    void pushBasicProfile(JSONObject baseProfile) {
        try {
            String guid = getCleverTapID();

            JSONObject profileEvent = new JSONObject();

            // TODO: move to CTJSONConverter which will clone input json using below method
            if (baseProfile != null && baseProfile.length() > 0) {
                Iterator<String> i = baseProfile.keys();
                IdentityRepo iProfileHandler = IdentityRepoFactory
                        .getRepo(mContext, mConfig, mDeviceInfo, mValidationResultStack);
                setLoginInfoProvider(new LoginInfoProvider(mContext, mConfig, mDeviceInfo));
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

                        // TODO abstract out using IdentityIterator and IdentityIterable
                        // cache the valid identifier: guid pairs
                        boolean isProfileKey = iProfileHandler.hasIdentity(next);
                        if (isProfileKey) {
                            try {
                                getLoginInfoProvider().cacheGUIDForIdentifier(guid, next, value.toString());
                            } catch (Throwable t) {
                                // no-op
                            }
                        }
                    }
                }
            }

            try {
                //TODO can be replaced with mDeviceInfo.attachCarrier()
                String carrier = mDeviceInfo.getCarrier();
                if (carrier != null && !carrier.equals("")) {
                    profileEvent.put("Carrier", carrier);
                }

                //TODO can be replaced with mDeviceInfo.attachCC()
                String cc = mDeviceInfo.getCountryCode();
                if (cc != null && !cc.equals("")) {
                    profileEvent.put("cc", cc);
                }

                //TODO can be replaced with mDeviceInfo.attachTZ()
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

    private void schedulePushNotificationViewedQueueFlush(final Context context) {
        if (pushNotificationViewedRunnable == null) {
            pushNotificationViewedRunnable = new Runnable() {
                @Override
                public void run() {
                    mConfig.getLogger()
                            .verbose(mConfig.getAccountId(),
                                    "Pushing Notification Viewed event onto queue flush async");
                    flushQueueAsync(context, EventGroup.PUSH_NOTIFICATION_VIEWED);
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

    void processPushNotificationViewedEvent(final Context context, final JSONObject event) {
        synchronized (mCtLockManager.getEventLock()) {
            try {
                int session = mCleverTapMetaData.getCurrentSessionId();
                event.put("s", session);
                event.put("type", "event");
                event.put("ep", getNow());
                // Report any pending validation error
                ValidationResult vr = mValidationResultStack.popValidationResult();
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                mConfig.getLogger().verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto DB");
                mBaseDatabaseManager.queuePushNotificationViewedEventToDB(context, event);
                mConfig.getLogger()
                        .verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto queue flush");
                schedulePushNotificationViewedQueueFlush(context);
            } catch (Throwable t) {
                mConfig.getLogger()
                        .verbose(mConfig.getAccountId(),
                                "Failed to queue notification viewed event: " + event.toString(), t);
            }
        }
    }

}