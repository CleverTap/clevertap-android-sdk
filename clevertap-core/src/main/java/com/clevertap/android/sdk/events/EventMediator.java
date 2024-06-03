package com.clevertap.android.sdk.events;

import static com.clevertap.android.sdk.Constants.DATE_PREFIX;
import static com.clevertap.android.sdk.Constants.KEY_NEW_VALUE;
import static com.clevertap.android.sdk.Constants.KEY_OLD_VALUE;
import static com.clevertap.android.sdk.Constants.keysToSkipForUserAttributesEvaluation;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.LocalDataStore;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ProfileValueHandler;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.variables.JsonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EventMediator {

    private final CoreMetaData cleverTapMetaData;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final LocalDataStore localDataStore;

    private final ProfileValueHandler profileValueHandler;

    public EventMediator(Context context, CleverTapInstanceConfig config, CoreMetaData coreMetaData, LocalDataStore localDataStore, ProfileValueHandler profileValueHandler) {
        this.context = context;
        this.config = config;
        this.localDataStore = localDataStore;
        this.profileValueHandler = profileValueHandler;
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

    public Map<String, Map<String, Object>> getUserAttributeChangeProperties(final JSONObject event) {
        Map<String, Map<String, Object>> userAttributesChangeProperties = new HashMap<>();
        Map<String, Object> fieldsToPersistLocally = new HashMap<>();
        JSONObject profile = event.optJSONObject(Constants.PROFILE);

        if(profile == null)
            return userAttributesChangeProperties;

        Iterator<?> keys = profile.keys();

        while (keys.hasNext()) {
            try {
                String key = (String) keys.next();

                //Todo - Check if these default fields need to be considered for evaluation as a product requirement
                if (keysToSkipForUserAttributesEvaluation.contains(key)) {
                    continue;
                }
                Object oldValue = localDataStore.getProfileValueForKey(key);
                Object newValue = profile.get(key);

                if(newValue instanceof JSONObject) {
                    JSONObject obj = (JSONObject) newValue;
                    String commandIdentifier = obj.keys().next();
                    switch(commandIdentifier) {
                        case Constants.COMMAND_INCREMENT:
                        case Constants.COMMAND_DECREMENT:
                            newValue = profileValueHandler.handleIncrementDecrementValues((Number) obj.get(commandIdentifier), commandIdentifier, (Number) oldValue);
                            break;
                        case Constants.COMMAND_DELETE:
                            newValue = null;
                            break;
                        case Constants.COMMAND_SET:
                        case Constants.COMMAND_ADD:
                        case Constants.COMMAND_REMOVE:
                            newValue = profileValueHandler.computeMultiValues(key, ((JSONArray) obj.get(commandIdentifier)), commandIdentifier, oldValue);
                            break;
                    }
                }
                else if(newValue instanceof String) {
                    if(((String) newValue).startsWith(DATE_PREFIX)) {
                        newValue = ((String) newValue).substring(DATE_PREFIX.length());
                    }
                }

                Map<String, Object> properties = new HashMap<>();
                if(oldValue != null)
                    properties.put(KEY_OLD_VALUE, oldValue);
                if(newValue != null)
                    properties.put(KEY_NEW_VALUE, newValue);

                userAttributesChangeProperties.put(key, properties);
                fieldsToPersistLocally.put(key, newValue);
            } catch (JSONException e) {
                // no-op
            }
        }


        localDataStore.updateProfileFields(fieldsToPersistLocally);
        return userAttributesChangeProperties;
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
