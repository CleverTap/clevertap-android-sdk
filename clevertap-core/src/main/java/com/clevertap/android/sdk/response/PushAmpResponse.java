package com.clevertap.android.sdk.response;

import static com.clevertap.android.sdk.utils.CTJsonConverter.getRenderedTargetList;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PushAmpResponse extends CleverTapResponseDecorator {

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final Logger logger;

    private final ControllerManager controllerManager;

    private final BaseDatabaseManager baseDatabaseManager;

    public PushAmpResponse(
            Context context,
            CleverTapInstanceConfig config,
            BaseDatabaseManager dbManager,
            BaseCallbackManager callbackManager,
            ControllerManager controllerManager
    ) {
        this.context = context;
        this.config = config;
        logger = this.config.getLogger();
        this.baseDatabaseManager = dbManager;
        this.callbackManager = callbackManager;
        this.controllerManager = controllerManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        //Handle Pull Notifications response
        if (config.isAnalyticsOnly()) {
            logger.verbose(config.getAccountId(),
                    "CleverTap instance is configured to analytics only, not processing push amp response");
            return;
        }
        try {
            if (response.has("pushamp_notifs")) {
                logger.verbose(config.getAccountId(), "Processing pushamp messages...");
                JSONObject pushAmpObject = response.getJSONObject("pushamp_notifs");
                final JSONArray pushNotifications = pushAmpObject.getJSONArray("list");
                if (pushNotifications.length() > 0) {
                    logger.verbose(config.getAccountId(), "Handling Push payload locally");
                    handlePushNotificationsInResponse(pushNotifications);
                }
                if (pushAmpObject.has("pf")) {
                    try {
                        int frequency = pushAmpObject.getInt("pf");
                        controllerManager.getPushProviders().updatePingFrequencyIfNeeded(context, frequency);
                    } catch (Throwable t) {
                        logger
                                .verbose("Error handling ping frequency in response : " + t.getMessage());
                    }

                }
                if (pushAmpObject.has("ack")) {
                    boolean ack = pushAmpObject.getBoolean("ack");
                    logger.verbose("Received ACK -" + ack);
                    if (ack) {
                        JSONArray rtlArray = getRenderedTargetList(baseDatabaseManager.loadDBAdapter(context));
                        String[] rtlStringArray = new String[0];
                        if (rtlArray != null) {
                            rtlStringArray = new String[rtlArray.length()];
                        }
                        for (int i = 0; i < rtlStringArray.length; i++) {
                            rtlStringArray[i] = rtlArray.getString(i);
                        }
                        logger.verbose("Updating RTL values...");
                        baseDatabaseManager.loadDBAdapter(context).updatePushNotificationIds(rtlStringArray);
                    }
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
                    if (callbackManager.getPushAmpListener() != null) {
                        callbackManager.getPushAmpListener().onPushAmpPayloadReceived(pushBundle);
                    } else {
                        PushNotificationHandler.getPushNotificationHandler()
                                .onMessageReceived(context, pushBundle, PushType.FCM.toString());
                    }
                } else {
                    logger.verbose(config.getAccountId(),
                            "Push Notification already shown, ignoring local notification :" + pushObject
                                    .getString("wzrk_pid"));
                }
            }
        } catch (JSONException e) {
            logger.verbose(config.getAccountId(), "Error parsing push notification JSON");
        }
    }
}
