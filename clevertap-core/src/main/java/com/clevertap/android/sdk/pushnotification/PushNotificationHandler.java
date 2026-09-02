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
     * Merges the single nested {@code data} JSON object of a Live Update push into the top-level bundle
     * so downstream routing (Mode B / Push Template selection via {@code pt_id}) and rendering read it
     * like a normal push, and so a Mode A factory can read the fields as flat extras. The {@code data}
     * object holds the render content for both modes; the presence of {@code pt_id} inside it is what
     * distinguishes Mode B (SDK/PT render) from Mode A (client factory).
     *
     * <p><b>Values are surfaced as strings — intentionally.</b> An FCM data message is a
     * {@code Map<String,String>}, so a normal (flat) Push Template campaign already delivers every
     * value ({@code pt_progress}, {@code pt_promote}, and JSON arrays like {@code pt_progress_segments})
     * as a string, and the renderers read them back via {@code getString(...)} and parse. Surfacing
     * {@code data} as strings keeps Mode B byte-for-byte identical to that flat path — the same
     * template renders the same way whether the payload arrived flat or nested. Preserving primitive
     * types ({@code putInt}/{@code putBoolean}) would make those {@code getString(...)} reads return
     * {@code null} and break rendering. Nested objects/arrays are serialized back to compact JSON
     * ({@code JSONArray/JSONObject.toString()}), which is exactly what the renderers re-parse, so lists
     * are handled correctly. JSON {@code null} values are skipped (never written as the literal
     * "null").</p>
     *
     * <p><b>Root wins for identity/transport/analytics keys.</b> A key already present at the top
     * level is never overwritten by {@code data} — so the wrapper keys that drive dedup
     * ({@code wzrk_pid}), the in-place notification id ({@code wzrk_activityId}), attribution
     * ({@code wzrk_id}/{@code wzrk_campaignId}/…) and analytics cannot be corrupted even if the
     * backend accidentally duplicates them inside {@code data}. {@code data} supplies the render
     * keys (which live only inside it: {@code pt_id}, {@code nt}, {@code nm}, {@code pt_progress_*},
     * {@code wzrk_cid}, {@code pr}, …).</p>
     */
    private static void surfaceLiveActivityPayload(Bundle message) {
        String laPn = message.getString(Constants.WZRK_LIVE_ACTIVITY_DATA);
        if (laPn == null || laPn.isEmpty()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(laPn);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (message.containsKey(key)) {
                    // Root-level value is authoritative; do not let data overwrite it.
                    continue;
                }
                Object value = json.get(key);
                if (value == null || value == JSONObject.NULL) {
                    // Skip JSON null rather than writing the literal string "null".
                    continue;
                }
                // Strings pass through; numbers/booleans and nested objects/arrays serialize to their
                // string / compact-JSON form (parity with a flat FCM Push Template payload — see javadoc).
                message.putString(key, value instanceof String ? (String) value : value.toString());
            }
        } catch (Throwable t) {
            Logger.d(LOG_TAG, "Failed to surface Live Update data payload", t);
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
