package com.clevertap.android.sdk.response;

import static com.clevertap.android.sdk.utils.CTJsonConverter.pushIdsToJSONArray;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;

import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PushAmpResponse extends CleverTapResponseDecorator {

    private final String accountId;

    private final Context context;

    private final ILogger logger;

    private final BaseDatabaseManager baseDatabaseManager;
    private PushProviders pushProviders;

    private CTPushAmpListener pushAmpListener;

    public PushAmpResponse(
            Context context,
            String accountId,
            ILogger logger,
            BaseDatabaseManager dbManager
    ) {
        this.context = context;
        this.accountId = accountId;
        this.logger = logger;
        this.baseDatabaseManager = dbManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        //Handle Pull Notifications response
        try {
            if (response.has("pushamp_notifs")) {
                logger.verbose(accountId, "Processing pushamp messages...");
                JSONObject pushAmpObject = response.getJSONObject("pushamp_notifs");
                final JSONArray pushNotifications = pushAmpObject.getJSONArray("list");
                if (pushNotifications.length() > 0) {
                    logger.verbose(accountId, "Handling Push payload locally");
                    handlePushNotificationsInResponse(pushNotifications);
                }
                if (pushAmpObject.has("pf")) {
                    try {
                        int frequency = pushAmpObject.getInt("pf");
                        if (pushProviders != null) {
                            pushProviders.updatePingFrequencyIfNeeded(context, frequency);
                        }
                    } catch (Throwable t) {
                        logger
                                .verbose("Error handling ping frequency in response : " + t.getMessage());
                    }

                }
                if (pushAmpObject.optBoolean("ack", false)) {
                    logger.verbose("Received ACK - true");
                    String[] pushIds = baseDatabaseManager.loadDBAdapter(context).fetchPushNotificationIds();
                    JSONArray rtlArray = pushIdsToJSONArray(pushIds);
                    String[] rtlStringArray = new String[rtlArray.length()];
                    for (int i = 0; i < rtlStringArray.length; i++) {
                        rtlStringArray[i] = rtlArray.getString(i);
                    }
                    logger.verbose("Updating RTL values...");
                    baseDatabaseManager.loadDBAdapter(context).updatePushNotificationIds(rtlStringArray);
                } else {
                    logger.verbose("Received ACK - false");
                }
            }
        } catch (Throwable t) {
            //Ignore
        }
    }

    //PN
    @SuppressWarnings("rawtypes")
    private void handlePushNotificationsInResponse(JSONArray pushNotifications) {
        try {
            for (int i = 0; i < pushNotifications.length(); i++) {
                Bundle pushBundle = new Bundle();
                JSONObject pushObject = pushNotifications.getJSONObject(i);
                if (pushObject.has("wzrk_ttl")) {
                    pushBundle.putLong("wzrk_ttl", pushObject.getLong("wzrk_ttl"));
                }

                Iterator iterator = pushObject.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next().toString();
                    pushBundle.putString(key, pushObject.getString(key));
                }
                if (!pushBundle.isEmpty() && !baseDatabaseManager.loadDBAdapter(context)
                        .doesPushNotificationIdExist(pushObject.getString("wzrk_pid"))) {
                    logger.verbose("Creating Push Notification locally");
                    if (pushAmpListener != null) {
                        pushAmpListener.onPushAmpPayloadReceived(pushBundle);
                    } else {
                        PushNotificationHandler.getPushNotificationHandler()
                                .onMessageReceived(context, pushBundle, PushConstants.FCM.toString());
                    }
                } else {
                    logger.verbose(accountId,
                            "Push Notification already shown, ignoring local notification :" + pushObject
                                    .getString("wzrk_pid"));
                }
            }
        } catch (JSONException e) {
            logger.verbose(accountId, "Error parsing push notification JSON");
        }
    }

    public void setPushProviders(PushProviders pushProviders) {
        this.pushProviders = pushProviders;
    }

    public void setPushAmpListener(CTPushAmpListener pushAmpListener) {
        // todo someone should set me, please fix.
        this.pushAmpListener = pushAmpListener;
    }
}
