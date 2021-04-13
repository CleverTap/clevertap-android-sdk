package com.clevertap.android.sdk.events;

import static com.clevertap.android.sdk.utils.CTJsonConverter.getErrorObject;

import android.content.Context;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.FailureFlushListener;
import com.clevertap.android.sdk.LocalDataStore;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.SessionManager;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.login.IdentityRepo;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.login.LoginInfoProvider;
import com.clevertap.android.sdk.network.BaseNetworkManager;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.MainLooperHandler;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.validation.ValidationResult;
import com.clevertap.android.sdk.validation.ValidationResultStack;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.json.JSONException;
import org.json.JSONObject;

public class EventQueueManager extends BaseEventQueueManager implements FailureFlushListener {

    private Runnable commsRunnable = null;

    private final BaseDatabaseManager baseDatabaseManager;

    private final CoreMetaData cleverTapMetaData;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final CTLockManager ctLockManager;

    private final DeviceInfo deviceInfo;

    private final EventMediator eventMediator;

    private final LocalDataStore localDataStore;

    private final Logger logger;

    private LoginInfoProvider loginInfoProvider;

    private final MainLooperHandler mainLooperHandler;

    private final BaseNetworkManager networkManager;

    private final SessionManager sessionManager;

    private final ValidationResultStack validationResultStack;

    private Runnable pushNotificationViewedRunnable = null;

    public EventQueueManager(final BaseDatabaseManager baseDatabaseManager,
            Context context,
            CleverTapInstanceConfig config,
            EventMediator eventMediator,
            SessionManager sessionManager,
            BaseCallbackManager callbackManager,
            MainLooperHandler mainLooperHandler,
            DeviceInfo deviceInfo,
            ValidationResultStack validationResultStack,
            NetworkManager networkManager,
            CoreMetaData coreMetaData,
            CTLockManager ctLockManager,
            final LocalDataStore localDataStore) {
        this.baseDatabaseManager = baseDatabaseManager;
        this.context = context;
        this.config = config;
        this.eventMediator = eventMediator;
        this.sessionManager = sessionManager;
        this.mainLooperHandler = mainLooperHandler;
        this.deviceInfo = deviceInfo;
        this.validationResultStack = validationResultStack;
        this.networkManager = networkManager;
        this.localDataStore = localDataStore;
        logger = this.config.getLogger();
        cleverTapMetaData = coreMetaData;
        this.ctLockManager = ctLockManager;

        callbackManager.setFailureFlushListener(this);
    }

    // only call async
    @Override
    public void addToQueue(final Context context, final JSONObject event, final int eventType) {
        if (eventType == Constants.NV_EVENT) {
            config.getLogger()
                    .verbose(config.getAccountId(), "Pushing Notification Viewed event onto separate queue");
            processPushNotificationViewedEvent(context, event);
        } else {
            processEvent(context, event, eventType);
        }
    }

    @Override
    public void failureFlush(Context context) {
        scheduleQueueFlush(context);
    }

    @Override
    public void flush() {
        flushQueueAsync(context, EventGroup.REGULAR);
    }

    @Override
    public void flushQueueAsync(final Context context, final EventGroup eventGroup) {
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("CommsManager#flushQueueAsync", new Callable<Void>() {
            @Override
            public Void call() {
                if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
                    logger.verbose(config.getAccountId(),
                            "Pushing Notification Viewed event onto queue flush sync");
                } else {
                    logger.verbose(config.getAccountId(), "Pushing event onto queue flush sync");
                }
                flushQueueSync(context, eventGroup);
                return null;
            }
        });
    }

    @Override
    public void flushQueueSync(final Context context, final EventGroup eventGroup) {
        if (!NetworkManager.isNetworkOnline(context)) {
            logger.verbose(config.getAccountId(), "Network connectivity unavailable. Will retry later");
            return;
        }

        if (cleverTapMetaData.isOffline()) {
            logger.debug(config.getAccountId(),
                    "CleverTap Instance has been set to offline, won't send events queue");
            return;
        }

        if (networkManager.needsHandshakeForDomain(eventGroup)) {
            networkManager.initHandshake(eventGroup, new Runnable() {
                @Override
                public void run() {
                    networkManager.flushDBQueue(context, eventGroup);
                }
            });
        } else {
            logger.verbose(config.getAccountId(), "Pushing Notification Viewed event onto queue DB flush");
            networkManager.flushDBQueue(context, eventGroup);
        }
    }

    public LoginInfoProvider getLoginInfoProvider() {
        return loginInfoProvider;
    }

    public void setLoginInfoProvider(final LoginInfoProvider loginInfoProvider) {
        this.loginInfoProvider = loginInfoProvider;
    }

    public int getNow() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public void processEvent(final Context context, final JSONObject event, final int eventType) {
        synchronized (ctLockManager.getEventLock()) {
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
                        cleverTapMetaData.setBgPing(true);
                        event.remove("bk");
                    }

                    //Add a flag to denote, PING event is for geofences
                    if (cleverTapMetaData.isLocationForGeofence()) {
                        event.put("gf", true);
                        cleverTapMetaData.setLocationForGeofence(false);
                        event.put("gfSDKVersion", cleverTapMetaData.getGeofenceSDKVersion());
                        cleverTapMetaData.setGeofenceSDKVersion(0);
                    }
                } else if (eventType == Constants.PROFILE_EVENT) {
                    type = "profile";
                } else if (eventType == Constants.DATA_EVENT) {
                    type = "data";
                } else {
                    type = "event";
                }

                // Complete the received event with the other params

                String currentActivityName = cleverTapMetaData.getScreenName();
                if (currentActivityName != null) {
                    event.put("n", currentActivityName);
                }

                int session = cleverTapMetaData.getCurrentSessionId();
                event.put("s", session);
                event.put("pg", CoreMetaData.getActivityCount());
                event.put("type", type);
                event.put("ep", getNow());
                event.put("f", cleverTapMetaData.isFirstSession());
                event.put("lsl", cleverTapMetaData.getLastSessionLength());
                attachPackageNameIfRequired(context, event);

                // Report any pending validation error
                ValidationResult vr = validationResultStack.popValidationResult();
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                localDataStore.setDataSyncFlag(event);
                baseDatabaseManager.queueEventToDB(context, event, eventType);
                updateLocalStore(context, event, eventType);
                scheduleQueueFlush(context);

            } catch (Throwable e) {
                config.getLogger().verbose(config.getAccountId(), "Failed to queue event: " + event.toString(), e);
            }
        }
    }

    public void processPushNotificationViewedEvent(final Context context, final JSONObject event) {
        synchronized (ctLockManager.getEventLock()) {
            try {
                int session = cleverTapMetaData.getCurrentSessionId();
                event.put("s", session);
                event.put("type", "event");
                event.put("ep", getNow());
                // Report any pending validation error
                ValidationResult vr = validationResultStack.popValidationResult();
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                config.getLogger().verbose(config.getAccountId(), "Pushing Notification Viewed event onto DB");
                baseDatabaseManager.queuePushNotificationViewedEventToDB(context, event);
                config.getLogger()
                        .verbose(config.getAccountId(), "Pushing Notification Viewed event onto queue flush");
                schedulePushNotificationViewedQueueFlush(context);
            } catch (Throwable t) {
                config.getLogger()
                        .verbose(config.getAccountId(),
                                "Failed to queue notification viewed event: " + event.toString(), t);
            }
        }
    }

    //Profile
    @Override
    public void pushBasicProfile(JSONObject baseProfile) {
        try {
            String guid = getCleverTapID();

            JSONObject profileEvent = new JSONObject();

            if (baseProfile != null && baseProfile.length() > 0) {
                Iterator<String> i = baseProfile.keys();
                IdentityRepo iProfileHandler = IdentityRepoFactory
                        .getRepo(context, config, deviceInfo, validationResultStack);
                setLoginInfoProvider(new LoginInfoProvider(context, config, deviceInfo));
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
                                getLoginInfoProvider().cacheGUIDForIdentifier(guid, next, value.toString());
                            } catch (Throwable t) {
                                // no-op
                            }
                        }
                    }
                }
            }

            try {
                String carrier = deviceInfo.getCarrier();
                if (carrier != null && !carrier.equals("")) {
                    profileEvent.put("Carrier", carrier);
                }

                String cc = deviceInfo.getCountryCode();
                if (cc != null && !cc.equals("")) {
                    profileEvent.put("cc", cc);
                }

                profileEvent.put("tz", TimeZone.getDefault().getID());

                JSONObject event = new JSONObject();
                event.put("profile", profileEvent);
                queueEvent(context, event, Constants.PROFILE_EVENT);
            } catch (JSONException e) {
                config.getLogger()
                        .verbose(config.getAccountId(), "FATAL: Creating basic profile update event failed!");
            }
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Basic profile sync", t);
        }
    }

    @Override
    public void pushInitialEventsAsync() {
        if (!cleverTapMetaData.inCurrentSession()) {
            Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
            task.execute("CleverTapAPI#pushInitialEventsAsync", new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        config.getLogger().verbose(config.getAccountId(), "Queuing daily events");
                        pushBasicProfile(null);
                    } catch (Throwable t) {
                        config.getLogger().verbose(config.getAccountId(), "Daily profile sync failed", t);
                    }
                    return null;
                }
            });
        }
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
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        return task.submit("queueEvent", new Callable<Void>() {
            @Override
            public Void call() {
                if (eventMediator.shouldDropEvent(event, eventType)) {
                    return null;
                }
                if (eventMediator.shouldDeferProcessingEvent(event, eventType)) {
                    config.getLogger().debug(config.getAccountId(),
                            "App Launched not yet processed, re-queuing event " + event + "after 2s");
                    mainLooperHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
                            task.execute("queueEventWithDelay", new Callable<Void>() {
                                @Override
                                public Void call() {
                                    sessionManager.lazyCreateSession(context);
                                    pushInitialEventsAsync();
                                    addToQueue(context, event, eventType);
                                    return null;
                                }
                            });
                        }
                    }, 2000);
                } else {
                    if (eventType == Constants.FETCH_EVENT) {
                        addToQueue(context, event, eventType);
                    } else {
                        sessionManager.lazyCreateSession(context);
                        pushInitialEventsAsync();
                        addToQueue(context, event, eventType);
                    }
                }
                return null;
            }
        });
    }

    @Override
    public void scheduleQueueFlush(final Context context) {
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
        mainLooperHandler.removeCallbacks(commsRunnable);

        mainLooperHandler.postDelayed(commsRunnable, networkManager.getDelayFrequency());

        logger.verbose(config.getAccountId(), "Scheduling delayed queue flush on main event loop");
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

    private String getCleverTapID() {
        return deviceInfo.getDeviceID();
    }

    private void schedulePushNotificationViewedQueueFlush(final Context context) {
        if (pushNotificationViewedRunnable == null) {
            pushNotificationViewedRunnable = new Runnable() {
                @Override
                public void run() {
                    config.getLogger()
                            .verbose(config.getAccountId(),
                                    "Pushing Notification Viewed event onto queue flush async");
                    flushQueueAsync(context, EventGroup.PUSH_NOTIFICATION_VIEWED);
                }
            };
        }
        mainLooperHandler.removeCallbacks(pushNotificationViewedRunnable);
        mainLooperHandler.post(pushNotificationViewedRunnable);
    }

    //Util
    // only call async
    private void updateLocalStore(final Context context, final JSONObject event, final int type) {
        if (type == Constants.RAISED_EVENT) {
            localDataStore.persistEvent(context, event, type);
        }
    }

}