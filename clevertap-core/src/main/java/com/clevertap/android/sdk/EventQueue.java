package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.login.IdentityRepo;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.login.LoginInfoProvider;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.Future;
import org.json.JSONException;
import org.json.JSONObject;

class EventQueue {

    private final Context mContext;

    private final EventMediator mEventMediator;

    private final SessionHandler mSessionHandler;

    private final MainLooperHandler mMainLooperHandler;

    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;

    private final EventProcessor mEventProcessor;

    private final BaseCTApiListener mBaseCTApiListener;

    private final CleverTapInstanceConfig mConfig;
    private final DeviceInfo mDeviceInfo;

    EventQueue(final EventMediator eventMediator,
            final BaseCTApiListener baseCTApiListener, final SessionHandler sessionHandler,
            final MainLooperHandler mainLooperHandler,
            final PostAsyncSafelyHandler postAsyncSafelyHandler,
            final EventProcessor eventProcessor) {
        mEventMediator = eventMediator;
        mSessionHandler = sessionHandler;
        mMainLooperHandler = mainLooperHandler;
        mPostAsyncSafelyHandler = postAsyncSafelyHandler;
        mEventProcessor = eventProcessor;
        mBaseCTApiListener = baseCTApiListener;
        mConfig = baseCTApiListener.config();
        mDeviceInfo = baseCTApiListener.deviceInfo();
        mContext = baseCTApiListener.context();
    }

    /**
     * Adds a new event to the queue, to be sent later.
     *
     * @param context   The Android context
     * @param event     The event to be queued
     * @param eventType The type of event to be queued
     */
    Future<?> queueEvent(final Context context, final JSONObject event, final int eventType) {
        return mPostAsyncSafelyHandler.postAsyncSafely("queueEvent", new Runnable() {
            @Override
            public void run() {
                if (mEventMediator.shouldDropEvent(event, eventType)) {
                    return;
                }
                if (mEventMediator.shouldDeferProcessingEvent(event, eventType)) {
                    mConfig.getLogger().debug(mConfig.getAccountId(),
                            "App Launched not yet processed, re-queuing event " + event + "after 2s");
                    mMainLooperHandler.getMainLooperHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mPostAsyncSafelyHandler.postAsyncSafely("queueEventWithDelay", new Runnable() {
                                @Override
                                public void run() {
                                    mSessionHandler.lazyCreateSession(context);
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
                        mSessionHandler.lazyCreateSession(context);
                        pushInitialEventsAsync();
                        addToQueue(context, event, eventType);
                    }
                }
            }
        });
    }

    // only call async
    private void addToQueue(final Context context, final JSONObject event, final int eventType) {
        if (eventType == Constants.NV_EVENT) {
            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), "Pushing Notification Viewed event onto separate queue");
            mEventProcessor.processPushNotificationViewedEvent(context, event);
        } else {
            mEventProcessor.processEvent(context, event, eventType);
        }
    }

    //Event
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

    //Profile
    void pushBasicProfile(JSONObject baseProfile) {
        try {
            String guid = getCleverTapID();

            JSONObject profileEvent = new JSONObject();

            if (baseProfile != null && baseProfile.length() > 0) {
                Iterator<String> i = baseProfile.keys();
                IdentityRepo iProfileHandler = IdentityRepoFactory.getRepo(mBaseCTApiListener);
                LoginInfoProvider loginInfoProvider = new LoginInfoProvider(mBaseCTApiListener);
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

    private String getCleverTapID() {
        return mDeviceInfo.getDeviceID();
    }

}