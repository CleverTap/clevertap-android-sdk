package com.clevertap.android.sdk.pushnotification;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler;
import com.clevertap.android.sdk.interfaces.NotificationHandler;
import java.util.Iterator;
import org.json.JSONObject;

public class PushNotificationHandler implements ActionButtonClickHandler {

    private static class SingletonNotificationHandler {

        private final static PushNotificationHandler INSTANCE = new PushNotificationHandler();
    }

    public static NotificationHandler getPushNotificationHandler() {
        return SingletonNotificationHandler.INSTANCE;
    }

    /**
     * Merges the nested {@code la_pn} JSON payload of a Live Update push into the top-level bundle
     * so downstream routing (Push Template selection via {@code pt_id}) and rendering read it like a
     * normal push. Nested values (incl. JSON arrays like {@code pt_progress_segments}) are stored as
     * strings. Only invoked for Mode B (no client factory).
     */
    private static void surfaceLiveActivityPayload(Bundle message) {
        String laPn = message.getString(Constants.WZRK_LIVE_ACTIVITY_PN);
        if (laPn == null || laPn.isEmpty()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(laPn);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);
                message.putString(key, value instanceof String ? (String) value : value.toString());
            }
        } catch (Throwable t) {
            Logger.d(LOG_TAG, "Failed to surface la_pn payload", t);
        }
    }

    public static boolean isForPushTemplates(Bundle extras) {
        if (extras == null) {
            return false;
        }
        String pt_id = extras.getString("pt_id");
        return !(("0").equals(pt_id) || pt_id == null || pt_id.isEmpty());
    }

    private boolean isForSignedCall(Bundle extras) {
        if (extras == null) {
            return false;
        }
        String source = extras.getString("source");
        return (("signedcall").equals(source));
    }

    private PushNotificationHandler() {
        // NO-OP
    }

    @Override
    public boolean onActionButtonClick(final Context context, final Bundle extras, final int notificationId) {
        return false;
    }

    @Override
    public synchronized boolean onMessageReceived(
            final Context applicationContext,
            final Bundle message,
            final String pushType
    ) {
        message.putLong(Constants.OMR_INVOKE_TIME_IN_MILLIS,System.currentTimeMillis());
        CleverTapAPI cleverTapAPI = CleverTapAPI
                .getGlobalInstance(applicationContext, getAccountIdFromNotificationBundle(message));
        NotificationInfo info = CleverTapAPI.getNotificationInfo(message);

        if (info.fromCleverTap) {
            if (cleverTapAPI != null) {
                cleverTapAPI.getCoreState().getConfig().log(LOG_TAG,
                        pushType + "received notification from CleverTap: " + message.toString());
                // Live Update Mode B: when no client factory is registered, surface the nested
                // la_pn payload (incl. pt_id) to the top level so the normal PT/core routing below
                // renders it. Factory (Mode A) takes precedence and is handled in _createNotification.
                if ("true".equalsIgnoreCase(message.getString(Constants.WZRK_LIVE_ACTIVITY, ""))
                        && CleverTapAPI.getNotificationFactory() == null) {
                    surfaceLiveActivityPayload(message);
                }
                if (isForPushTemplates(message) && CleverTapAPI.getNotificationHandler() != null) {
                    // render push template
                    CleverTapAPI.getNotificationHandler().onMessageReceived(applicationContext, message, pushType);
                } else if (isForSignedCall(message) && CleverTapAPI.getSignedCallNotificationHandler() != null) {
                    // handle voip push payload
                    CleverTapAPI.getSignedCallNotificationHandler().onMessageReceived(applicationContext, message, pushType);
                } else {
                    // render core push
                    cleverTapAPI.renderPushNotificationOnCallerThread(new CoreNotificationRenderer(), applicationContext, message);
                    //CleverTapAPI.createNotification(applicationContext, message);
                }
            } else {
                Logger.d(LOG_TAG, pushType + "received notification from CleverTap: " + message.toString());
                Logger.d(LOG_TAG, pushType + " not renderning since cleverTapAPI is null");
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onNewToken(final Context applicationContext, final String token, final PushType pushType) {
        CleverTapAPI.tokenRefresh(applicationContext, token, pushType);
        return true;
    }
}
