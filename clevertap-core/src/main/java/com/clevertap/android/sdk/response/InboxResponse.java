package com.clevertap.android.sdk.response;

import android.content.Context;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class InboxResponse extends CleverTapResponseDecorator {

    private final Object inboxControllerLock;

    private final BaseCallbackManager callbackManager;

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final Logger logger;

    private final ControllerManager controllerManager;

    public InboxResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config,
            CTLockManager ctLockManager,
            final BaseCallbackManager callbackManager, ControllerManager controllerManager) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        this.callbackManager = callbackManager;
        logger = this.config.getLogger();
        inboxControllerLock = ctLockManager.getInboxControllerLock();
        this.controllerManager = controllerManager;
    }

    //NotificationInbox
    @WorkerThread
    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {

        if (config.isAnalyticsOnly()) {
            logger.verbose(config.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing inbox messages");

            // process PushAmp response
            cleverTapResponse.processResponse(response, stringBody, context);

            return;
        }

        logger.verbose(config.getAccountId(), "Inbox: Processing response");

        if (!response.has(Constants.INBOX_JSON_RESPONSE_KEY)) {
            logger.verbose(config.getAccountId(), "Inbox: Response JSON object doesn't contain the inbox key");
            // process PushAmp response
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }
        try {
            _processInboxMessages(response.getJSONArray(Constants.INBOX_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), "InboxResponse: Failed to parse response", t);
        }

        // process PushAmp response
        cleverTapResponse.processResponse(response, stringBody, context);

    }


    // always call async
    @WorkerThread
    private void _processInboxMessages(JSONArray messages) {
        synchronized (inboxControllerLock) {
            if (controllerManager.getCTInboxController() == null) {
                controllerManager.initializeInbox();
            }
            if (controllerManager.getCTInboxController() != null) {
                boolean update = controllerManager.getCTInboxController().updateMessages(messages);
                if (update) {
                    callbackManager._notifyInboxMessagesDidUpdate();
                }
            }
        }
    }
}
