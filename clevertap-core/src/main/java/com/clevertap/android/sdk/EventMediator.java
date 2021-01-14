package com.clevertap.android.sdk;

import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

public class EventMediator {

    public EventMediator(final CleverTapMetaData cleverTapMetaData, final CleverTapInstanceConfig config) {
        mCleverTapMetaData = cleverTapMetaData;
        mConfig = config;
    }

    private final CleverTapMetaData mCleverTapMetaData;

    private final CleverTapInstanceConfig mConfig;

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

        if (mCleverTapMetaData.isMuted()) {
            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), "CleverTap is muted, dropping event - " + event.toString());
            return true;
        }

        return false;
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
}
