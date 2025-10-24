package com.clevertap.android.sdk.response;

import android.content.Context;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.features.InboxLiveCallbacks;
import com.clevertap.android.sdk.inbox.CTInboxController;

import org.json.JSONArray;
import org.json.JSONObject;

public class InboxResponse extends CleverTapResponseDecorator {

    private final Object inboxControllerLock;

    private final ILogger logger;

    private final String accountId;

    private CTInboxController controller;

    private InboxLiveCallbacks callbacks;

    public InboxResponse(
            String accountId,
            ILogger logger,
            CTLockManager ctLockManager
    ) {
        this.logger = logger;
        this.accountId = accountId;
        this.inboxControllerLock = ctLockManager.getInboxControllerLock();
    }

    //NotificationInbox
    @WorkerThread
    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {

        logger.verbose(accountId, "Inbox: Processing response");

        if (!response.has(Constants.INBOX_JSON_RESPONSE_KEY)) {
            logger.verbose(accountId, "Inbox: Response JSON object doesn't contain the inbox key");
            // process PushAmp response
            return;
        }
        try {
            _processInboxMessages(response.getJSONArray(Constants.INBOX_JSON_RESPONSE_KEY));
        } catch (Throwable t) {
            logger.verbose(accountId, "InboxResponse: Failed to parse response", t);
        }
    }


    // always call async
    @WorkerThread
    private void _processInboxMessages(JSONArray messages) {
        synchronized (inboxControllerLock) {
            if (controller == null) {
                //controllerManager.initializeInbox();
                // todo lp check if this is really needed, why do we load inbox on data reception.
            }
            if (controller != null) {
                boolean update = controller.updateMessages(messages);
                if (update) {
                    callbacks._notifyInboxMessagesDidUpdate();
                }
            }
        }
    }

    public void setController(CTInboxController controller) {
        this.controller = controller;
    }

    public void setCallbacks(InboxLiveCallbacks callbacks) {
        this.callbacks = callbacks;
    }
}
