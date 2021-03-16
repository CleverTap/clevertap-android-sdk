package com.clevertap.android.sdk.events;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.StorageHelper;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

public class EventMediator {

    private final CoreMetaData cleverTapMetaData;

    private final CleverTapInstanceConfig config;

    private final Context context;

    public EventMediator(Context context, CleverTapInstanceConfig config, CoreMetaData coreMetaData) {
        this.context = context;
        this.config = config;
        cleverTapMetaData = coreMetaData;
    }

    public boolean shouldDeferProcessingEvent(JSONObject event, int eventType) {
        //noinspection SimplifiableIfStatement
        if (config.isCreatedPostAppLaunch()) {
            return false;
        }
        if (event.has("evtName")) {
            try {
                if (Arrays.asList(Constants.SYSTEM_EVENTS).contains(event.getString("evtName"))) {
                    return false;
                }
            } catch (JSONException e) {
                //no-op
            }
        }
        return (eventType == Constants.RAISED_EVENT && !cleverTapMetaData.isAppLaunchPushed());
    }

    public boolean shouldDropEvent(JSONObject event, int eventType) {
        if (eventType == Constants.FETCH_EVENT) {
            return false;
        }

        if (cleverTapMetaData.isCurrentUserOptedOut()) {
            String eventString = event == null ? "null" : event.toString();
            config.getLogger()
                    .debug(config.getAccountId(), "Current user is opted out dropping event: " + eventString);
            return true;
        }

        if (isMuted()) {
            config.getLogger()
                    .verbose(config.getAccountId(), "CleverTap is muted, dropping event - " + event.toString());
            return true;
        }

        return false;
    }


    /**
     * @return true if the mute command was sent anytime between now and now - 24 hours.
     */
    private boolean isMuted() {
        final int now = (int) (System.currentTimeMillis() / 1000);
        final int muteTS = StorageHelper.getIntFromPrefs(context, config, Constants.KEY_MUTED, 0);

        return now - muteTS < 24 * 60 * 60;
    }
}
