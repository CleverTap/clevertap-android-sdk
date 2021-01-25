package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

class InboxResponse extends CleverTapResponseDecorator {

    private final Object inboxControllerLock;

    private final CallbackManager mCallbackManager;

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final Logger mLogger;
    private final ControllerManager mControllerManager;

    InboxResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config, CTLockManager ctLockManager,
            final CallbackManager callbackManager, ControllerManager controllerManager) {
        mCleverTapResponse = cleverTapResponse;
        mConfig = config;
        mCallbackManager = callbackManager;
        mLogger = mConfig.getLogger();
        inboxControllerLock = ctLockManager.getInboxControllerLock();
        mControllerManager = controllerManager;
    }

    //NotificationInbox
    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {

        if (mConfig.isAnalyticsOnly()) {
            mLogger.verbose(mConfig.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing inbox messages");

            // process PushAmp response
            mCleverTapResponse.processResponse(response, stringBody, context);

            return;
        }

        mLogger.verbose(mConfig.getAccountId(), "Inbox: Processing response");

        if (!response.has("inbox_notifs")) {
            mLogger.verbose(mConfig.getAccountId(), "Inbox: Response JSON object doesn't contain the inbox key");
            return;
        }
        try {
            _processInboxMessages(response.getJSONArray("inbox_notifs"));
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), "InboxResponse: Failed to parse response", t);
        }

        // process PushAmp response
        mCleverTapResponse.processResponse(response, stringBody, context);

    }


    // always call async
    private void _processInboxMessages(JSONArray messages) {
        synchronized (inboxControllerLock) {
            if (mControllerManager.getCTInboxController() == null) {
                //mCTInboxController.initializeInbox();
                //TODO logging
            }
            if (mControllerManager.getCTInboxController() != null) {
                boolean update = mControllerManager.getCTInboxController().updateMessages(messages);
                if (update) {
                    mCallbackManager._notifyInboxMessagesDidUpdate();
                }
            }
        }
    }
}
