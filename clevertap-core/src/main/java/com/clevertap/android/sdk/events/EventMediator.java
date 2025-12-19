package com.clevertap.android.sdk.events;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.network.NetworkRepo;
import com.clevertap.android.sdk.validation.ValidationConfig;
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

    private final NetworkRepo networkRepo;


    public EventMediator(CleverTapInstanceConfig config, CoreMetaData coreMetaData, NetworkRepo networkRepo) {
        this.config = config;
        this.networkRepo = networkRepo;
        cleverTapMetaData = coreMetaData;
    }

    public boolean shouldDeferProcessingEvent(JSONObject event, int eventType) {
        //noinspection SimplifiableIfStatement
        if (eventType == Constants.DEFINE_VARS_EVENT) {
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

        if (networkRepo.isMuted()) {
            config.getLogger()
                    .verbose(config.getAccountId(), "CleverTap is muted, dropping event - " + event.toString());
            return true;
        }

        if (!cleverTapMetaData.isCurrentUserOptedOut()) {
            return false;
        }

        if (!cleverTapMetaData.getEnabledSystemEvents()) {
            config.getLogger().debug(config.getAccountId(), "Current user is opted out dropping event: " + event);
            // opted-out and system events disabled
            return true;
        }

        if (eventType != Constants.RAISED_EVENT && eventType != Constants.NV_EVENT) {
            // opted-out and system events enabled
            config.getLogger().debug(config.getAccountId(), "This is not RAISED_EVENT or NV_EVENT, not dropping event: " + event);
            return false;
        }

        // opted-out and system events enabled
        // check for Constants.RAISED_EVENT and Constants.NV_EVENT event special cases
        String eName = event != null ? getEventName(event) : null;
        boolean isSystemEvent = ValidationConfig.DEFAULT_RESTRICTED_EVENT_NAMES.contains(eName);
        boolean dropEvent = !isSystemEvent;
        if (dropEvent) {
            config.getLogger().debug(config.getAccountId(), "Current user is opted out dropping event: " + event);
        } else {
            config.getLogger().debug(config.getAccountId(), "This is a system event, not dropping event: " + event);
        }
        return dropEvent;
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
            return JsonUtil.listFromJson(
                    event.getJSONObject(Constants.KEY_EVT_DATA).getJSONArray(Constants.KEY_ITEMS));
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getChargedEventDetails(JSONObject event) {
        try {
            final Object items = event.getJSONObject(Constants.KEY_EVT_DATA).remove(Constants.KEY_ITEMS);
            final Map<String, Object> chargedDetails = JsonUtil.mapFromJson(
                    event.getJSONObject(Constants.KEY_EVT_DATA));
            event.getJSONObject(Constants.KEY_EVT_DATA).put(Constants.KEY_ITEMS, items);
            return chargedDetails;
        } catch (JSONException e) {
            return new HashMap<>();
        }
    }
}
