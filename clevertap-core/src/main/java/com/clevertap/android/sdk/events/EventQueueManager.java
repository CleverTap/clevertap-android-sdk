package com.clevertap.android.sdk.events;

import static com.clevertap.android.sdk.utils.CTJsonConverter.getErrorObject;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreContract;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.FailureFlushListener;
import com.clevertap.android.sdk.SessionManager;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.login.IdentityRepo;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.login.LoginInfoProvider;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.validation.ValidationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class EventQueueManager extends BaseEventQueueManager implements FailureFlushListener {

    private Runnable commsRunnable = null;
    private final EventMediator eventMediator;
    private final NetworkManager networkManager;
    private final SessionManager sessionManager;
    private Runnable pushNotificationViewedRunnable = null;
    private final LoginInfoProvider loginInfoProvider;
    private final CoreContract coreContract;

    public EventQueueManager(
            EventMediator eventMediator,
            SessionManager sessionManager,
            NetworkManager networkManager,
            LoginInfoProvider loginInfoProvider,
            CoreContract coreContract
    ) {
        this.eventMediator = eventMediator;
        this.sessionManager = sessionManager;
        this.networkManager = networkManager;
        this.loginInfoProvider = loginInfoProvider;
        this.coreContract = coreContract;
    }

    // only call async
    @Override
    public void addToQueue(final Context context, final JSONObject event, final int eventType) {
        if (eventType == Constants.NV_EVENT) {
            coreContract.logger()
                    .verbose(coreContract.config().getAccountId(), "Pushing Notification Viewed event onto separate queue");
            processPushNotificationViewedEvent(context, event, eventType);
        } else if(eventType == Constants.DEFINE_VARS_EVENT) {
            processDefineVarsEvent(context, event);
        } else {
            processEvent(context, event, eventType);
        }
    }

    @WorkerThread
    private void processDefineVarsEvent(Context context, JSONObject event) {
        sendImmediately(context, EventGroup.VARIABLES, event);
    }

    @Override
    public void failureFlush(Context context) {
        scheduleQueueFlush(context);
    }

    @Override
    public void flush() {
        flushQueueAsync(coreContract.context(), EventGroup.REGULAR);
    }

    @Override
    public void flushQueueAsync(final Context context, final EventGroup eventGroup) {
        Task<Void> task = coreContract.executors().postAsyncSafelyTask();
        task.execute("CommsManager#flushQueueAsync", new Callable<Void>() {
            @Override
            public Void call() {
                if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
                    coreContract.logger().verbose(coreContract.config().getAccountId(),
                            "Pushing Notification Viewed event onto queue flush sync");
                } else {
                    coreContract.logger().verbose(coreContract.config().getAccountId(), "Pushing event onto queue flush sync");
                }
                flushQueueSync(context, eventGroup);
                return null;
            }
        });
    }

    /**
     * Flushes the events queue synchronously with a default null value for the caller.
     * This is an overloaded method that internally calls {@link EventQueueManager#flushQueueSync(Context, EventGroup, String)}.
     *
     * @param context     The Context object.
     * @param eventGroup  The EventGroup for which the queue needs to be flushed.
     */
    @Override
    public void flushQueueSync(final Context context, final EventGroup eventGroup) {
        flushQueueSync(context,eventGroup,null);
    }

    /**
     * Flushes the events queue synchronously, checking network connectivity, offline mode, and performing handshake if necessary.
     *
     * @param context     The Context object.
     * @param eventGroup  The EventGroup for which the queue needs to be flushed.
     * @param caller      The optional caller identifier.
     */
    @Override
    public void flushQueueSync(final Context context, final EventGroup eventGroup, @Nullable final String caller) {
        flushQueueSync(context,eventGroup,caller, false);
    }

    @Override
    public void flushQueueSync(final Context context, final EventGroup eventGroup,final String caller, final boolean isUserSwitchFlush) {
        // Check if network connectivity is available
        if (!NetworkManager.isNetworkOnline(context)) {
            coreContract.logger().verbose(coreContract.config().getAccountId(), "Network connectivity unavailable. Will retry later");
            coreContract.didNotFlush();
            return;
        }

        // Check if CleverTap instance is set to offline mode
        if (coreContract.coreMetaData().isOffline()) {
            coreContract.logger().debug(coreContract.config().getAccountId(),
                    "CleverTap Instance has been set to offline, won't send events queue");
            coreContract.didNotFlush();
            return;
        }

        // Check if handshake is required for the domain associated with the event group
        if (networkManager.needsHandshakeForDomain(eventGroup)) {
            // Perform handshake and then flush the DB queue
            networkManager.initHandshake(eventGroup, () -> networkManager.flushDBQueue(context, eventGroup,caller, isUserSwitchFlush));
        } else {
            coreContract.logger().verbose(coreContract.config().getAccountId(), "Pushing Notification Viewed event onto queue DB flush");

            // No handshake required, directly flush the DB queue
            networkManager.flushDBQueue(context, eventGroup,caller, isUserSwitchFlush);
        }
    }

    /**
     * This method is currently used only for syncing of variables. If you find it appropriate you
     * can add handling of network error similar to flushQueueSync, also check return value of
     * sendQueue for success.
     */
    @Override
    public void sendImmediately(Context context, EventGroup eventGroup, JSONObject eventData) {
        if (!NetworkManager.isNetworkOnline(context)) {
            coreContract.logger().verbose(coreContract.config().getAccountId(), "Network connectivity unavailable. Event won't be sent.");
            return;
        }

        if (coreContract.coreMetaData().isOffline()) {
            coreContract.logger().debug(coreContract.config().getAccountId(),
                "CleverTap Instance has been set to offline, won't send event");
            return;
        }

        JSONArray singleEventQueue = new JSONArray().put(eventData);

        if (networkManager.needsHandshakeForDomain(eventGroup)) {
            networkManager.initHandshake(
                    eventGroup,
                    () -> networkManager.sendQueue(context, eventGroup, singleEventQueue, null, false)
            );
        } else {
            networkManager.sendQueue(context, eventGroup, singleEventQueue, null, false);
        }
    }

    public int getNow() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public void processEvent(final Context context, final JSONObject event, final int eventType) {
        synchronized (coreContract.ctLockManager().getEventLock()) {
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
                        coreContract.coreMetaData().setBgPing(true);
                        event.remove("bk");
                    }

                    //Add a flag to denote, PING event is for geofences
                    if (coreContract.coreMetaData().isLocationForGeofence()) {
                        event.put("gf", true);
                        coreContract.coreMetaData().setLocationForGeofence(false);
                        event.put("gfSDKVersion", coreContract.coreMetaData().getGeofenceSDKVersion());
                        coreContract.coreMetaData().setGeofenceSDKVersion(0);
                    }
                } else if (eventType == Constants.PROFILE_EVENT) {
                    type = "profile";
                } else if (eventType == Constants.DATA_EVENT) {
                    type = "data";
                } else {
                    type = "event";
                }

                // Complete the received event with the other params

                String currentActivityName = coreContract.coreMetaData().getScreenName();
                if (currentActivityName != null) {
                    event.put("n", currentActivityName);
                }

                int session = coreContract.coreMetaData().getCurrentSessionId();
                event.put("s", session);
                event.put("pg", CoreMetaData.getActivityCount());
                event.put("type", type);
                event.put("ep", getNow());
                event.put("f", coreContract.coreMetaData().isFirstSession());
                event.put("lsl", coreContract.coreMetaData().getLastSessionLength());
                attachPackageNameIfRequired(context, event);

                // Report any pending validation error
                ValidationResult vr = coreContract.validationResultStack().popValidationResult();
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                coreContract.data().getLocalDataStore().setDataSyncFlag(event);
                coreContract.database().queueEventToDB(context, event, eventType);

                coreContract.evaluateInAppForEvent(context, event, eventType);

                scheduleQueueFlush(context);
            } catch (Throwable e) {
                coreContract.logger().verbose(coreContract.config().getAccountId(), "Failed to queue event: " + event.toString(), e);
            }
        }
    }

    public void processPushNotificationViewedEvent(final Context context, final JSONObject event, final int eventType) {
        synchronized (coreContract.ctLockManager().getEventLock()) {
            try {
                int session = coreContract.coreMetaData().getCurrentSessionId();
                event.put("s", session);
                event.put("type", "event");
                event.put("ep", getNow());
                // Report any pending validation error
                ValidationResult vr = coreContract.validationResultStack().popValidationResult();
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                coreContract.logger().verbose(coreContract.config().getAccountId(), "Pushing Notification Viewed event onto DB");
                coreContract.database().queuePushNotificationViewedEventToDB(context, event);
                coreContract.evaluateInAppForEvent(context, event, eventType);
                coreContract.logger()
                        .verbose(coreContract.config().getAccountId(), "Pushing Notification Viewed event onto queue flush");
                schedulePushNotificationViewedQueueFlush(context);
            } catch (Throwable t) {
                coreContract.logger()
                        .verbose(coreContract.config().getAccountId(),
                                "Failed to queue notification viewed event: " + event.toString(), t);
            }
        }
    }

    //Profile
    @Override
    public void pushBasicProfile(JSONObject baseProfile, boolean removeFromSharedPrefs) {
        try {
            String guid = getCleverTapID();

            JSONObject profileEvent = new JSONObject();

            if (baseProfile != null && baseProfile.length() > 0) {
                Iterator<String> i = baseProfile.keys();
                IdentityRepo iProfileHandler = IdentityRepoFactory
                        .getRepo(coreContract.context(), coreContract.config(), coreContract.validationResultStack());
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

                        /*If key is present in IdentitySet and removeFromSharedPrefs is true then
                        proceed to removing PII key(Email) from shared prefs*/

                        if (isProfileKey && !coreContract.deviceInfo().isErrorDeviceId()) {
                            try {
                                if (removeFromSharedPrefs) {
                                    // Remove the value associated with the GUID
                                    loginInfoProvider.removeValueFromCachedGUIDForIdentifier(guid, next);
                                } else {
                                    // Cache the new value for the GUID
                                    loginInfoProvider.cacheGUIDForIdentifier(guid, next, value.toString());
                                }
                            } catch (Throwable t) {
                                // Log or handle the exception if needed; currently no-op
                            }
                        }
                    }
                }
            }

            try {
                String carrier = coreContract.deviceInfo().getCarrier();
                if (carrier != null && !carrier.equals("")) {
                    profileEvent.put("Carrier", carrier);
                }

                String cc = coreContract.deviceInfo().getCountryCode();
                if (cc != null && !cc.equals("")) {
                    profileEvent.put("cc", cc);
                }

                profileEvent.put("tz", TimeZone.getDefault().getID());

                JSONObject event = new JSONObject();
                event.put("profile", profileEvent);
                queueEvent(coreContract.context(), event, Constants.PROFILE_EVENT);
            } catch (JSONException e) {
                coreContract.logger()
                        .verbose(coreContract.config().getAccountId(), "FATAL: Creating basic profile update event failed!");
            }
        } catch (Throwable t) {
            coreContract.logger().verbose(coreContract.config().getAccountId(), "Basic profile sync", t);
        }
    }

    @Override
    public void pushInitialEventsAsync() {
        if (!coreContract.coreMetaData().inCurrentSession()) {
            Task<Void> task = coreContract.executors().postAsyncSafelyTask();
            task.execute("CleverTapAPI#pushInitialEventsAsync", new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        coreContract.logger().verbose(coreContract.config().getAccountId(), "Queuing daily events");
                        pushBasicProfile(null, false);
                    } catch (Throwable t) {
                        coreContract.logger().verbose(coreContract.config().getAccountId(), "Daily profile sync failed", t);
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
        Task<Void> task = coreContract.executors().postAsyncSafelyTask();
        return task.submit("queueEvent", new Callable<Void>() {
            @Override
            @WorkerThread
            public Void call() {
                if (eventMediator.shouldDropEvent(event, eventType)) {
                    return null;
                }
                if (eventMediator.shouldDeferProcessingEvent(event, eventType)) {
                    coreContract.logger().debug(coreContract.config().getAccountId(),
                            "App Launched not yet processed, re-queuing event " + event + "after 2s");
                    coreContract.mainLooperHandler().postDelayed(() -> {
                        Task<Void> task1 = coreContract.executors().postAsyncSafelyTask();
                        task1.execute("queueEventWithDelay", new Callable<Void>() {
                            @Override
                            @WorkerThread
                            public Void call() {
                                sessionManager.lazyCreateSession(context);
                                pushInitialEventsAsync();
                                addToQueue(context, event, eventType);
                                return null;
                            }
                        });
                    }, 2000);
                } else {
                    if (eventType == Constants.FETCH_EVENT || eventType == Constants.NV_EVENT) {
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
        coreContract.mainLooperHandler().removeCallbacks(commsRunnable);

        coreContract.mainLooperHandler().postDelayed(commsRunnable, networkManager.getDelayFrequency());

        coreContract.logger().verbose(coreContract.config().getAccountId(), "Scheduling delayed queue flush on main event loop");
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
        return coreContract.deviceInfo().getDeviceID();
    }

    private void schedulePushNotificationViewedQueueFlush(final Context context) {
        if (pushNotificationViewedRunnable == null) {
            pushNotificationViewedRunnable = new Runnable() {
                @Override
                public void run() {
                    coreContract.logger()
                        .verbose(coreContract.config().getAccountId(),
                            "Pushing Notification Viewed event onto queue flush async");
                    flushQueueAsync(context, EventGroup.PUSH_NOTIFICATION_VIEWED);
                }
            };
        }
        coreContract.mainLooperHandler().removeCallbacks(pushNotificationViewedRunnable);
        coreContract.mainLooperHandler().post(pushNotificationViewedRunnable);
    }
}