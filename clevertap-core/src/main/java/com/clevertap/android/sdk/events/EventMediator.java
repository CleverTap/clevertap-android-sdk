package com.clevertap.android.sdk.events;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.variables.JsonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        if(eventType == Constants.DEFINE_VARS_EVENT){
            return false;
        }
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
        if (eventType == Constants.FETCH_EVENT || eventType == Constants.DEFINE_VARS_EVENT) {
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

    public boolean isAppLaunchedEvent(JSONObject event) {
        try {
            return event.has(Constants.KEY_EVT_NAME)
                    && event.getString(Constants.KEY_EVT_NAME).equals(Constants.APP_LAUNCHED_EVENT);
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean isEvent(JSONObject event) {
        return event.has(Constants.KEY_EVT_NAME);
    }

    public String getEventName(JSONObject event) {
        try {
            return event.getString(Constants.KEY_EVT_NAME);
        } catch (JSONException e) {
            return null;
        }
    }

    public Map<String, Object> getEventProperties(JSONObject event) {
        if (event.has(Constants.KEY_EVT_NAME) && event.has(Constants.KEY_EVT_DATA)) {
            try {
                return JsonUtil.mapFromJson(event.getJSONObject(Constants.KEY_EVT_DATA));
            } catch (JSONException e) {
                Logger.v("Could not convert JSONObject to Map - " + e
                        .getMessage());
            }
        }
        return new HashMap<>();
    }

    public boolean isChargedEvent(JSONObject event) {
        try {
            return event.has(Constants.KEY_EVT_NAME)
                    && event.getString(Constants.KEY_EVT_NAME).equals(Constants.CHARGED_EVENT);
        } catch (JSONException e) {
            return false;
        }
    }

    public List<Map<String, Object>> getChargedEventItemDetails(JSONObject event) {
        try {
            return JsonUtil.listFromJson(event.getJSONArray(Constants.KEY_ITEMS));
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getChargedEventDetails(JSONObject event) {
        try {
            final Object items = event.remove(Constants.KEY_ITEMS);
            final Map<String, Object> chargedDetails = JsonUtil.mapFromJson(event);
            event.put(Constants.KEY_ITEMS, items);
            return chargedDetails;
        } catch (JSONException e) {
            return new HashMap<>();
        }
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
