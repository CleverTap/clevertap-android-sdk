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
import java.util.Map;

public class PushNotificationHandler implements ActionButtonClickHandler {

    private static class SingletonNotificationHandler {

        private final static PushNotificationHandler INSTANCE = new PushNotificationHandler();
    }

    public static NotificationHandler getPushNotificationHandler() {
        return SingletonNotificationHandler.INSTANCE;
    }

    /**
     * Merges the single nested {@code data} JSON object of a Live Update push into the top-level
     * bundle so downstream routing (Mode B / Push Template selection via {@code pt_id}) and rendering
     * read it like a normal push, and so a Mode A factory can read the fields as flat extras. The
     * presence of {@code pt_id} inside {@code data} distinguishes Mode B (SDK/PT render) from Mode A
     * (client factory).
     *
     * <p>The flatten semantics (string coercion, compact-JSON for nested arrays/objects, JSON-null
     * skip, and root-wins so wrapper/identity/analytics keys are never overwritten) live in
     * {@link LiveActivityPayloadSurfacer#flatten} — a pure, unit-tested seam. This method just applies
     * the result to the bundle.</p>
     */
    static void surfaceLiveActivityPayload(Bundle message) {
        String data = message.getString(Constants.WZRK_LIVE_ACTIVITY_DATA);
        Map<String, String> toSurface = LiveActivityPayloadSurfacer.flatten(data, message.keySet());
        for (Map.Entry<String, String> entry : toSurface.entrySet()) {
            message.putString(entry.getKey(), entry.getValue());
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
                // Live Update: surface the single nested `data` object (incl. pt_id, if any) to the
                // top level so the normal PT/core routing below can read it. A `pt_id` inside `data`
                // selects Mode B (SDK/PT render); its absence means Mode A (client factory). The
                // Mode A vs Mode B decision is finalised in _createNotification.
                if ("true".equalsIgnoreCase(message.getString(Constants.WZRK_LIVE_ACTIVITY, ""))) {
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
