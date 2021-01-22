package com.clevertap.android.sdk;

import android.content.Context;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

class EventMediator {

    private final CoreMetaData mCleverTapMetaData;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    public EventMediator(Context context, CleverTapInstanceConfig config, CoreMetaData coreMetaData) {
        mContext = context;
        mConfig = config;
        mCleverTapMetaData = coreMetaData;
    }

    boolean shouldDeferProcessingEvent(JSONObject event, int eventType) {
        //noinspection SimplifiableIfStatement
        if (mConfig.isCreatedPostAppLaunch()) {
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
        return (eventType == Constants.RAISED_EVENT && !mCleverTapMetaData.isAppLaunchPushed());
    }

    boolean shouldDropEvent(JSONObject event, int eventType) {
        if (eventType == Constants.FETCH_EVENT) {
            return false;
        }

        if (mCleverTapMetaData.isCurrentUserOptedOut()) {
            String eventString = event == null ? "null" : event.toString();
            mConfig.getLogger()
                    .debug(mConfig.getAccountId(), "Current user is opted out dropping event: " + eventString);
            return true;
        }

        if (isMuted()) {
            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), "CleverTap is muted, dropping event - " + event.toString());
            return true;
        }

        return false;
    }


    /**
     * @return true if the mute command was sent anytime between now and now - 24 hours.
     */
    private boolean isMuted() {
        final int now = (int) (System.currentTimeMillis() / 1000);
        final int muteTS = StorageHelper.getIntFromPrefs(mContext, mConfig, Constants.KEY_MUTED, 0);

        return now - muteTS < 24 * 60 * 60;
    }
}
